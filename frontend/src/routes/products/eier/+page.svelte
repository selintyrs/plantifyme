<script>
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { savedListId, matchResults, shoppingListStore, isVeganized, resetAll, resetForLoad, itemIdMap } from '$lib/stores/shoppingList.js';
	import { supermarketStore } from '$lib/stores/supermarket.js';
	import { dictionaryStore } from '$lib/stores/dictionary.js';
	import { authFetch } from '$lib/api.js';

	let recipes = $state([]);
	let isLoading = $state(true);
	let error = $state(null);
	let addedVisible = $state(false);
	let supermarketModalOpen = $state(false);

	let expandedRecipes = $state({});
	let checkedIngredients = $state({});

	const USE_CASE_ORDER = [
		'Süsses Backen & Teige',
		'Herzhafte Gerichte & Frühstück',
		'Aufschlagen & Mousse',
		'Binden',
		'Glasieren & Bräunen',
	];

	let grouped = $derived.by(() => {
		const groups = {};
		for (const r of recipes) {
			const uc = r.useCase || 'Sonstiges';
			if (!groups[uc]) groups[uc] = [];
			groups[uc].push(r);
		}
		const ordered = {};
		for (const uc of USE_CASE_ORDER) {
			if (groups[uc]) ordered[uc] = groups[uc];
		}
		for (const uc of Object.keys(groups)) {
			if (!ordered[uc]) ordered[uc] = groups[uc];
		}
		return ordered;
	});

	let hasSelection = $derived.by(() => {
		for (const ingMap of Object.values(checkedIngredients)) {
			if (Object.values(ingMap).some(Boolean)) return true;
		}
		return false;
	});

	let selectedIngredients = $derived.by(() => {
		const result = [];
		for (const [recipeId, ingMap] of Object.entries(checkedIngredients)) {
			const recipe = recipes.find(r => r.id === recipeId);
			if (!recipe) continue;
			(recipe.ingredients || []).forEach((ing, i) => {
				if (ing.name !== 'Wasser' && ingMap[i]) {
					result.push({
					name: ing.name,
					amount: ing.amount,
					unit: ing.unit,
					type: ing.type || 'C',
					matchingDisplayName: ing.matchingDisplayName || null,
					matchingSubcategory: ing.matchingSubcategory || null,
				});
				}
			});
		}
		return result;
	});

	onMount(async () => {
		try {
			const res = await authFetch('/api/diy-recipes?displayName=Eier');
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			recipes = await res.json();
		} catch (e) {
			error = 'Rezepte konnten nicht geladen werden.';
			console.error(e);
		} finally {
			isLoading = false;
		}
	});

	function toggleCard(recipe) {
		const wasExpanded = expandedRecipes[recipe.id];
		expandedRecipes = { ...expandedRecipes, [recipe.id]: !wasExpanded };
		if (!wasExpanded && !checkedIngredients[recipe.id]) {
			checkedIngredients = { ...checkedIngredients, [recipe.id]: {} };
		}
	}

	function toggleIngredient(recipeId, index) {
		const current = checkedIngredients[recipeId] ?? {};
		checkedIngredients = { ...checkedIngredients, [recipeId]: { ...current, [index]: !current[index] } };
	}

	function findUiCategory(item) {
		const dict = $dictionaryStore;
		if (dict?.length > 0) {
			const entry = dict.find(e => e.displayName === (item.displayName || item.name));
			if (entry) return entry.uiCategory;
		}
		return 'Sonstiges';
	}

	async function handleAddToList() {
		if (!hasSelection) return;
		if ($savedListId) {
			await addToExistingList($savedListId);
		} else if ($isVeganized && $matchResults.length > 0) {
			await addToLocalVeganizedList();
		} else {
			supermarketModalOpen = true;
		}
	}

	async function addToLocalVeganizedList() {
		addedVisible = true;

		const supermarket = $supermarketStore;
		if (!supermarket) {
			addedVisible = false;
			supermarketModalOpen = true;
			return;
		}

		try {
			const apiSupermarket = supermarket === 'Migros' ? 'Migros' : supermarket.toLowerCase();
			const res = await authFetch('/api/shoppinglists/from-recipe', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({
					recipeTitle: 'Plantified Eier',
					supermarket: apiSupermarket,
					ingredients: selectedIngredients,
				}),
			});

			if (!res.ok) throw new Error('Match failed');
			const tempList = await res.json();
			const newItems = tempList.items || [];

			if (tempList.id) {
				authFetch(`/api/shoppinglists/${tempList.id}`, { method: 'DELETE' })
					.catch(e => console.warn('Cleanup failed:', e));
			}

			const currentMax = $shoppingListStore.length > 0
				? Math.max(...$shoppingListStore.map(i => i.id))
				: 0;

			const newStoreItems = newItems.map((item, idx) => ({
				id: currentMax + idx + 1,
				displayName: item.displayName || item.name,
				type: item.type,
				subcategory: item.subcategory || '',
				quantity: item.quantity || 1,
				frontendDictionaryId: null,
				uiCategory: findUiCategory(item),
			}));

			const newMatches = newItems.map((item, idx) => ({
				itemId: (currentMax + idx + 1).toString(),
				type: item.type,
				displayName: item.displayName || item.name,
				subcategory: item.subcategory || '',
				uiCategory: findUiCategory(item),
				suggestedProduct: (item.type === 'A' || item.type === 'B') && item.productId
					? { id: item.productId, name: item.name, price: item.price }
					: null,
				genericFood: item.type === 'C' && item.genericFoodId
					? { id: item.genericFoodId, name: item.name }
					: null,
				diyGroupLabel: item.diyGroupLabel || 'Zutat für Plantified Eier',
				fallbackDiyRecipeId: item.fallbackDiyRecipeId ?? null,
				fallbackDiyRecipeTitle: item.fallbackDiyRecipeTitle ?? null,
			}));

			shoppingListStore.update(prev => [...prev, ...newStoreItems]);
			matchResults.update(prev => [...prev, ...newMatches]);

			removeEggItemsLocal();

			setTimeout(() => goto('/liste'), 1500);
		} catch (e) {
			console.error('Lokales Hinzufügen fehlgeschlagen:', e);
			addedVisible = false;
		}
	}

	async function addToExistingList(listId) {
		try {
			const res = await authFetch(`/api/shoppinglists/${listId}/items/batch`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ recipeTitle: 'Plantified Eier', ingredients: selectedIngredients }),
			});
			if (res.ok) {
				const updatedList = await res.json();
				const newItems = (updatedList.items || []).filter(item => item.diyGroupLabel === 'Zutat für Plantified Eier');
				const currentMax = $shoppingListStore.length > 0 ? Math.max(...$shoppingListStore.map(i => i.id)) : 0;

				const newStoreItems = newItems.map((item, idx) => ({
					id: currentMax + idx + 1,
					displayName: item.displayName || item.name,
					type: item.type,
					subcategory: item.subcategory || '',
					quantity: item.quantity || 1,
					frontendDictionaryId: null,
					uiCategory: findUiCategory(item),
				}));

				const newMatches = newItems.map((item, idx) => ({
					itemId: (currentMax + idx + 1).toString(),
					type: item.type,
					displayName: item.displayName || item.name,
					subcategory: item.subcategory || '',
					uiCategory: findUiCategory(item),
					suggestedProduct: (item.type === 'A' || item.type === 'B') && item.productId
						? { id: item.productId, name: item.name, price: item.price }
						: null,
					genericFood: item.type === 'C' && item.genericFoodId
						? { id: item.genericFoodId, name: item.name }
						: null,
					diyGroupLabel: item.diyGroupLabel || null,
					fallbackDiyRecipeId: item.fallbackDiyRecipeId ?? null,
					fallbackDiyRecipeTitle: item.fallbackDiyRecipeTitle ?? null,
				}));

				shoppingListStore.update(prev => [...prev, ...newStoreItems]);
				matchResults.update(prev => [...prev, ...newMatches]);
				itemIdMap.update(prev => {
					const updated = { ...prev };
					for (const item of newItems) updated[item.itemId] = item.itemId;
					return updated;
				});
				isVeganized.set(true);

				await removeEggItems(listId);

				addedVisible = true;
				setTimeout(() => goto('/liste'), 1500);
			}
		} catch (e) {
			console.error('Zutaten hinzufügen fehlgeschlagen:', e);
		}
	}

	async function removeEggItems(listId) {
		const eggItems = $shoppingListStore.filter(item => item.displayName?.toLowerCase() === 'eier');
		if (eggItems.length === 0) return;

		const map = $itemIdMap;
		for (const eggItem of eggItems) {
			const backendItemId = map[eggItem.id.toString()];
			if (!backendItemId) continue;
			try {
				await authFetch(`/api/shoppinglists/${listId}/items/${backendItemId}`, { method: 'DELETE' });
			} catch (e) {
				console.warn('Egg item delete failed:', e);
			}
		}

		const eggItemIds = eggItems.map(item => item.id.toString());
		shoppingListStore.update(prev => prev.filter(item => item.displayName?.toLowerCase() !== 'eier'));
		matchResults.update(prev => prev.filter(r => !eggItemIds.includes(r.itemId)));
		itemIdMap.update(prev => {
			const updated = { ...prev };
			for (const id of eggItemIds) delete updated[id];
			return updated;
		});
	}

	function removeEggItemsLocal() {
		const eggItemIds = $shoppingListStore
			.filter(item => item.displayName?.toLowerCase() === 'eier')
			.map(item => item.id.toString());
		if (eggItemIds.length === 0) return;

		shoppingListStore.update(prev => prev.filter(item => item.displayName?.toLowerCase() !== 'eier'));
		matchResults.update(prev => prev.filter(r => !eggItemIds.includes(r.itemId)));
	}

	async function createListFromRecipe(supermarket) {
		supermarketModalOpen = false;
		addedVisible = true;
		const apiSupermarket = supermarket === 'Migros' ? 'Migros' : supermarket.toLowerCase();
		try {
			const res = await authFetch('/api/shoppinglists/from-recipe', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ recipeTitle: 'Plantified Eier', supermarket: apiSupermarket, ingredients: selectedIngredients }),
			});
			if (res.ok) {
				const newList = await res.json();
				resetAll();
				savedListId.set(newList.id);
				supermarketStore.set(supermarket);

				const listItems = newList.items || [];
				const storeItems = listItems.map((item, idx) => ({
					id: idx + 1,
					displayName: item.displayName || item.name,
					type: item.type,
					subcategory: item.subcategory || '',
					quantity: item.quantity || 1,
					frontendDictionaryId: null,
					uiCategory: findUiCategory(item),
				}));

				const newMatches = listItems.map((item, idx) => ({
					itemId: (idx + 1).toString(),
					type: item.type,
					displayName: item.displayName || item.name,
					subcategory: item.subcategory || '',
					uiCategory: findUiCategory(item),
					suggestedProduct: (item.type === 'A' || item.type === 'B') && item.productId
						? { id: item.productId, name: item.name, price: item.price, supermarket }
						: null,
					genericFood: item.type === 'C' && item.genericFoodId
						? { id: item.genericFoodId, name: item.name }
						: null,
					diyGroupLabel: item.diyGroupLabel || null,
					fallbackDiyRecipeId: item.fallbackDiyRecipeId ?? null,
					fallbackDiyRecipeTitle: item.fallbackDiyRecipeTitle ?? null,
				}));

				resetForLoad(storeItems);
				matchResults.set(newMatches);
				isVeganized.set(true);
				setTimeout(() => goto('/liste'), 1500);
			} else {
				addedVisible = false;
			}
		} catch (e) {
			console.error('Liste erstellen fehlgeschlagen:', e);
			addedVisible = false;
		}
	}
</script>

<div class="page">
	<div class="top-bar">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		<span class="top-title">Wie man Eier ersetzt</span>
		<div class="icon-btn-placeholder"></div>
	</div>

	{#if isLoading}
		<div class="spinner-wrapper"><div class="spinner"></div></div>
	{:else if error}
		<div class="error-state"><p class="error-text">{error}</p></div>
	{:else}
		<p class="intro-text">Eier erfüllen beim Kochen verschiedene Zwecke. Wähle unten deinen Anwendungsfall, um die perfekte pflanzliche Alternative zu finden.</p>

		<div class="groups">
			{#each Object.entries(grouped) as [useCase, ucRecipes]}
				<p class="use-case-label">{useCase}</p>
				{#each ucRecipes as recipe (recipe.id)}
					{@const expanded = !!expandedRecipes[recipe.id]}
					{@const checkedMap = checkedIngredients[recipe.id] ?? {}}
					<div class="recipe-card" class:recipe-card-expanded={expanded}>
						<div class="card-header" onclick={() => toggleCard(recipe)} role="button" tabindex="0" onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') toggleCard(recipe); }}>
							<span class="card-title">{recipe.title}</span>
							<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
								{#if expanded}
									<polyline points="18 15 12 9 6 15"/>
								{:else}
									<polyline points="6 9 12 15 18 9"/>
								{/if}
							</svg>
						</div>
						{#if expanded}
							{#if recipe.description}
								<p class="card-desc">{recipe.description}</p>
							{/if}
							<div class="ing-list">
								{#each (recipe.ingredients || []) as ing, i}
									{#if ing.name !== 'Wasser'}
										<button
											class="ing-row"
											class:ing-unchecked={!checkedMap[i]}
											onclick={(e) => { e.stopPropagation(); toggleIngredient(recipe.id, i); }}
										>
											<div class="checkbox" class:checkbox-checked={checkedMap[i]}>
												{#if checkedMap[i]}
													<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
												{/if}
											</div>
											<span class="ing-name">{ing.name}</span>
											{#if ing.amount != null || ing.unit}
												<span class="ing-qty">{ing.amount != null ? ing.amount : ''}{ing.unit ? '\u00a0' + ing.unit : ''}</span>
											{/if}
										</button>
									{/if}
								{/each}
							</div>
						{/if}
					</div>
				{/each}
			{/each}
		</div>

		<div class="sticky-action">
			<button
				class="btn-add"
				disabled={!hasSelection}
				onclick={handleAddToList}
			>
				Ausgewählte zur Liste hinzufügen
			</button>
		</div>
	{/if}
</div>

<!-- ── Supermarkt Modal ──────────────────────────────────────────────────── -->
{#if supermarketModalOpen}
	<div class="modal-backdrop" onclick={() => supermarketModalOpen = false} role="presentation">
		<div class="modal-sheet" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" tabindex="-1">
			<div class="modal-header">
				<span class="modal-title">Neuer Einkaufszettel</span>
				<button class="modal-close" onclick={() => supermarketModalOpen = false} aria-label="Schliessen">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
				</button>
			</div>
			<p class="modal-subtitle">Wähle deinen Supermarkt. Die Zutaten werden mit Produkten aus deinem Supermarkt abgeglichen.</p>
			<div class="supermarket-list">
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Migros')}>
					<span class="supermarket-name">Migros</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
				</button>
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Coop')}>
					<span class="supermarket-name">Coop</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
				</button>
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Aldi')}>
					<span class="supermarket-name">Aldi</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
				</button>
			</div>
			<div class="modal-footer">
				<button class="modal-cancel" onclick={() => supermarketModalOpen = false}>Abbrechen</button>
			</div>
		</div>
	</div>
{/if}

<!-- ── Added confirmation ─────────────────────────────────────────────────── -->
{#if addedVisible}
	<div class="added-confirmation">
		<div class="added-circle">
			<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<polyline points="20 6 9 17 4 12"/>
			</svg>
		</div>
		<p class="added-text">Zur Einkaufsliste hinzugefügt</p>
	</div>
{/if}

<style>
	.page {
		padding-bottom: 180px;
	}

	.top-bar {
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 12px 16px;
		background: var(--color-bg, white);
	}

	.top-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.icon-btn {
		width: 36px;
		height: 36px;
		border: none;
		background: transparent;
		color: var(--color-text);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		border-radius: 50%;
		padding: 0;
	}

	.icon-btn:hover {
		background: var(--color-border);
	}

	.icon-btn-placeholder {
		width: 36px;
		height: 36px;
	}

	.spinner-wrapper {
		display: flex;
		justify-content: center;
		padding: 60px 0;
	}

	.spinner {
		width: 32px;
		height: 32px;
		border: 3px solid var(--color-border);
		border-top-color: var(--color-primary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	.error-state {
		display: flex;
		justify-content: center;
		padding: 60px 16px;
	}

	.error-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}

	.intro-text {
		padding: 16px;
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		margin: 0;
	}

	.groups {
		padding-bottom: 8px;
	}

	.use-case-label {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-primary);
		letter-spacing: 0.08em;
		text-transform: uppercase;
		margin: 20px 16px 10px;
	}

	.recipe-card {
		margin: 0 16px 8px;
		border: 1.5px solid var(--color-border);
		border-radius: 14px;
		background: var(--color-bg);
		overflow: hidden;
		transition: border-color 0.15s ease;
	}

	.recipe-card-expanded {
		border-color: var(--color-primary);
	}

	.card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		padding: 14px 16px;
		text-align: left;
		cursor: pointer;
	}

	.card-title {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.card-header svg {
		flex-shrink: 0;
		color: var(--color-text-light);
	}

	.card-desc {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		line-height: 1.5;
		padding: 0 16px 12px;
		margin: 0;
	}

	.ing-list {
		display: flex;
		flex-direction: column;
		padding: 0 12px 12px;
		gap: 2px;
	}

	.ing-row {
		display: flex;
		align-items: center;
		gap: 10px;
		width: 100%;
		padding: 8px 4px;
		background: transparent;
		text-align: left;
		border-radius: 8px;
		transition: background 0.1s ease;
		cursor: pointer;
	}

	.ing-row:active {
		background: var(--color-primary-light);
	}

	.ing-unchecked {
		opacity: 0.4;
	}

	.checkbox {
		width: 20px;
		height: 20px;
		border-radius: 4px;
		border: 1.5px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		transition: background 0.12s ease, border-color 0.12s ease;
	}

	.checkbox-checked {
		background: var(--color-primary);
		border-color: var(--color-primary);
	}

	.ing-name {
		flex: 1;
		font-size: 0.875rem;
		color: var(--color-text);
	}

	.ing-qty {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	/* ── Sticky button ──────────────────────────────────────────────────── */
	.sticky-action {
		position: sticky;
		bottom: 80px;
		padding: 16px;
	}

	.btn-add {
		width: 100%;
		background: var(--color-primary);
		color: #fff;
		border-radius: 14px;
		padding: 16px;
		font-size: 1rem;
		font-weight: 600;
		text-align: center;
		box-shadow: 0 8px 30px rgba(45, 125, 70, 0.3);
		transition: background 0.15s ease, opacity 0.15s ease;
	}

	.btn-add:disabled {
		opacity: 0.5;
		cursor: not-allowed;
		box-shadow: none;
	}

	.btn-add:not(:disabled):active {
		background: var(--color-primary-hover);
	}

	/* ── Supermarkt Modal ───────────────────────────────────────────────── */
	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 50;
		display: flex;
		align-items: flex-end;
	}

	.modal-sheet {
		width: 100%;
		max-width: 390px;
		margin: 0 auto;
		background: var(--color-bg);
		border-radius: 20px 20px 0 0;
		padding: 20px 16px 32px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.modal-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.modal-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.modal-close {
		width: 32px;
		height: 32px;
		border: none;
		background: transparent;
		color: var(--color-text-light);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		border-radius: 50%;
	}

	.modal-subtitle {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		margin: 0;
	}

	.supermarket-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.supermarket-btn {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 14px 16px;
		border: 1.5px solid var(--color-border);
		border-radius: 14px;
		background: var(--color-bg);
		color: var(--color-text);
		font-size: 0.9375rem;
		font-weight: 500;
		cursor: pointer;
		transition: border-color 0.12s ease;
	}

	.supermarket-btn:active {
		border-color: var(--color-primary);
	}

	.supermarket-name {
		font-weight: 600;
	}

	.modal-footer {
		padding-top: 4px;
	}

	.modal-cancel {
		width: 100%;
		padding: 12px;
		font-size: 0.9375rem;
		color: var(--color-text-light);
		background: none;
		border: none;
		cursor: pointer;
		text-align: center;
	}

	/* ── Added confirmation ─────────────────────────────────────────────── */
	.added-confirmation {
		position: fixed;
		inset: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 16px;
		background: rgba(255, 255, 255, 0.9);
		z-index: 60;
	}

	.added-circle {
		width: 72px;
		height: 72px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.added-text {
		font-size: 1.0625rem;
		font-weight: 600;
		color: var(--color-text);
	}
</style>
