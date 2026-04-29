import { writable, get } from 'svelte/store';
import { browser } from '$app/environment';

const STORAGE_KEY = 'plantify_shoppinglist_state';

function loadState() {
	if (!browser) return null;
	try {
		const stored = sessionStorage.getItem(STORAGE_KEY);
		return stored ? JSON.parse(stored) : null;
	} catch {
		return null;
	}
}

const initial = loadState();

export const shoppingListStore = writable(initial?.items ?? []);
export const isVeganized = writable(initial?.isVeganized ?? false);
export const matchResults = writable(initial?.matchResults ?? []);
export const savedListId = writable(initial?.savedListId ?? null);
export const itemIdMap = writable(initial?.itemIdMap ?? {});

let nextId = initial?.nextId ?? 1;

function saveState() {
	if (!browser) return;
	try {
		const state = {
			items: get(shoppingListStore),
			isVeganized: get(isVeganized),
			matchResults: get(matchResults),
			savedListId: get(savedListId),
			itemIdMap: get(itemIdMap),
			nextId,
			timestamp: Date.now(),
		};
		sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
	} catch (e) {
		console.warn('Failed to persist shopping list state:', e);
	}
}

let saveTimeout;
function debouncedSave() {
	clearTimeout(saveTimeout);
	saveTimeout = setTimeout(saveState, 100);
}

if (browser) {
	shoppingListStore.subscribe(debouncedSave);
	isVeganized.subscribe(debouncedSave);
	matchResults.subscribe(debouncedSave);
	savedListId.subscribe(debouncedSave);
	itemIdMap.subscribe(debouncedSave);
}

export function addItem(entry, subcategory = '') {
	shoppingListStore.update((items) => {
		const exists = items.find(
			(i) => i.frontendDictionaryId === entry.id && i.subcategory === subcategory
		);
		if (exists) {
			return items.map((i) =>
				i.frontendDictionaryId === entry.id && i.subcategory === subcategory
					? { ...i, quantity: i.quantity + 1 }
					: i
			);
		}
		return [
			...items,
			{
				id: nextId++,
				displayName: entry.displayName,
				type: entry.type,
				subcategory,
				quantity: 1,
				frontendDictionaryId: entry.id,
				uiCategory: entry.uiCategory ?? 'Sonstiges'
			}
		];
	});
}

export function removeItem(id) {
	shoppingListStore.update((items) => items.filter((i) => i.id !== id));
}

export function updateQuantity(id, delta) {
	shoppingListStore.update((items) =>
		items.map((i) => (i.id === id ? { ...i, quantity: i.quantity + delta } : i))
	);
}

export function getItems() {
	return get(shoppingListStore);
}

export function resetForLoad(storeItems) {
	shoppingListStore.set(storeItems);
	nextId = storeItems.length > 0 ? Math.max(...storeItems.map((i) => i.id)) + 1 : 1;
}

export function resetAll() {
	shoppingListStore.set([]);
	matchResults.set([]);
	isVeganized.set(false);
	savedListId.set(null);
	itemIdMap.set({});
	nextId = 1;
	if (browser) {
		try { sessionStorage.removeItem(STORAGE_KEY); } catch {}
	}
}
