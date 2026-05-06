import { get } from 'svelte/store';
import { authFetch } from '$lib/api.js';
import {
	savedListId,
	matchResults,
	itemIdMap,
	isVeganized,
	shoppingListStore,
	resetForLoad,
	resetAll,
} from '$lib/stores/shoppingList.js';
import { supermarketStore } from '$lib/stores/supermarket.js';

function nextItemId() {
	const list = get(shoppingListStore);
	return list.length > 0 ? Math.max(...list.map((i) => i.id)) : 0;
}

function buildStoreItem(item, id, findUiCategory) {
	return {
		id,
		displayName: item.displayName || item.name,
		type: item.type,
		subcategory: item.subcategory || '',
		quantity: item.quantity || 1,
		frontendDictionaryId: null,
		uiCategory: findUiCategory(item),
	};
}

function buildMatch(item, id, findUiCategory, extraMatchFields, suggestedProductExtras = {}) {
	return {
		itemId: id.toString(),
		type: item.type,
		displayName: item.displayName || item.name,
		subcategory: item.subcategory || '',
		uiCategory: findUiCategory(item),
		suggestedProduct:
			(item.type === 'A' || item.type === 'B') && item.productId
				? { id: item.productId, name: item.name, price: item.price, ...suggestedProductExtras }
				: null,
		genericFood:
			item.type === 'C' && item.genericFoodId ? { id: item.genericFoodId, name: item.name } : null,
		diyGroupLabel: item.diyGroupLabel || null,
		...extraMatchFields(item),
	};
}

/**
 * Append ingredients to an existing shopping list via
 * POST /api/shoppinglists/{listId}/items/batch and merge the new items
 * into the local stores. Filters returned items by diyGroupLabel
 * so only items belonging to this batch are picked up.
 */
export async function appendToExistingList({
	listId,
	ingredients,
	recipeName,
	findUiCategory,
	extraMatchFields = () => ({}),
	fetchFn = authFetch,
}) {
	const res = await fetchFn(`/api/shoppinglists/${listId}/items/batch`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ recipeTitle: recipeName, ingredients }),
	});
	if (!res.ok) throw new Error(`Batch-add failed: HTTP ${res.status}`);

	const updatedList = await res.json();
	const expectedLabel = 'Zutat für ' + recipeName;
	const newItems = (updatedList.items || []).filter((item) => item.diyGroupLabel === expectedLabel);

	const baseId = nextItemId();
	const newStoreItems = newItems.map((item, idx) =>
		buildStoreItem(item, baseId + idx + 1, findUiCategory)
	);
	const newMatches = newItems.map((item, idx) =>
		buildMatch(item, baseId + idx + 1, findUiCategory, extraMatchFields)
	);

	shoppingListStore.update((prev) => [...prev, ...newStoreItems]);
	matchResults.update((prev) => [...prev, ...newMatches]);
	itemIdMap.update((prev) => {
		const updated = { ...prev };
		for (const item of newItems) updated[item.itemId] = item.itemId;
		return updated;
	});
	isVeganized.set(true);

	return { newItems, newStoreItems, newMatches };
}

/**
 * Create a new shopping list from ingredients via
 * POST /api/shoppinglists/from-recipe. Replaces local stores
 * (resetAll → savedListId / supermarketStore / resetForLoad / matchResults).
 */
export async function createListFromIngredients({
	supermarket,
	ingredients,
	recipeName,
	findUiCategory,
	extraMatchFields = () => ({}),
	fetchFn = authFetch,
}) {
	const apiSupermarket = supermarket === 'Migros' ? 'Migros' : supermarket.toLowerCase();
	const res = await fetchFn('/api/shoppinglists/from-recipe', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ recipeTitle: recipeName, supermarket: apiSupermarket, ingredients }),
	});
	if (!res.ok) throw new Error(`List creation failed: HTTP ${res.status}`);

	const newList = await res.json();
	resetAll();
	savedListId.set(newList.id);
	supermarketStore.set(supermarket);

	const listItems = newList.items || [];
	const storeItems = listItems.map((item, idx) =>
		buildStoreItem(item, idx + 1, findUiCategory)
	);
	const newMatches = listItems.map((item, idx) =>
		buildMatch(item, idx + 1, findUiCategory, extraMatchFields, { supermarket })
	);

	resetForLoad(storeItems);
	matchResults.set(newMatches);
	isVeganized.set(true);

	return { newList, storeItems, newMatches };
}

/**
 * Egg-DIY-special: when the user has already veganized a local list but
 * has no persisted savedListId, run match against a temporary list,
 * delete it server-side, and merge the matches into the local stores
 * without touching savedListId.
 */
export async function addIngredientsLocallyVeganized({
	supermarket,
	ingredients,
	recipeName,
	findUiCategory,
	extraMatchFields = () => ({}),
	fetchFn = authFetch,
}) {
	const apiSupermarket = supermarket === 'Migros' ? 'Migros' : supermarket.toLowerCase();
	const res = await fetchFn('/api/shoppinglists/from-recipe', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ recipeTitle: recipeName, supermarket: apiSupermarket, ingredients }),
	});
	if (!res.ok) throw new Error(`Match failed: HTTP ${res.status}`);

	const tempList = await res.json();
	const newItems = tempList.items || [];

	if (tempList.id) {
		fetchFn(`/api/shoppinglists/${tempList.id}`, { method: 'DELETE' }).catch((e) =>
			console.warn('Cleanup failed:', e)
		);
	}

	const baseId = nextItemId();
	const fallbackLabel = 'Zutat für ' + recipeName;

	const newStoreItems = newItems.map((item, idx) =>
		buildStoreItem(item, baseId + idx + 1, findUiCategory)
	);
	const newMatches = newItems.map((item, idx) => {
		const m = buildMatch(item, baseId + idx + 1, findUiCategory, extraMatchFields);
		if (!m.diyGroupLabel) m.diyGroupLabel = fallbackLabel;
		return m;
	});

	shoppingListStore.update((prev) => [...prev, ...newStoreItems]);
	matchResults.update((prev) => [...prev, ...newMatches]);

	return { newItems, newStoreItems, newMatches };
}

/**
 * 3-stage cascade for the standard recipe → shopping-list takeover flow:
 *   1. $savedListId set → append to that list
 *   2. $supermarketStore set → create a new list
 *   3. neither → invoke onSupermarketRequired (caller opens the modal)
 *
 * Errors thrown from beforeApiCall propagate to onError. If the caller
 * wants beforeApiCall failures to be silent (e.g. auto-save), they
 * should wrap their own try/catch inside the callback.
 *
 * Returns { mode: 'appended' | 'created' | 'modal' | 'error' }.
 */
export async function addIngredientsToShoppingList({
	ingredients,
	recipeName,
	findUiCategory,
	extraMatchFields,
	beforeApiCall,
	onSupermarketRequired,
	onSuccess,
	onError,
	fetchFn = authFetch,
}) {
	const listId = get(savedListId);
	const supermarket = get(supermarketStore);

	if (!listId && !supermarket) {
		onSupermarketRequired?.();
		return { mode: 'modal' };
	}

	try {
		if (beforeApiCall) await beforeApiCall();
		if (listId) {
			await appendToExistingList({
				listId,
				ingredients,
				recipeName,
				findUiCategory,
				extraMatchFields,
				fetchFn,
			});
			onSuccess?.(ingredients.length, 'appended');
			return { mode: 'appended' };
		}
		await createListFromIngredients({
			supermarket,
			ingredients,
			recipeName,
			findUiCategory,
			extraMatchFields,
			fetchFn,
		});
		onSuccess?.(ingredients.length, 'created');
		return { mode: 'created' };
	} catch (e) {
		onError?.(e);
		return { mode: 'error', error: e };
	}
}
