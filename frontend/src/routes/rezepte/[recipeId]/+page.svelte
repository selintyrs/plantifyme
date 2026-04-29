<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { savedListId, matchResults, itemIdMap, isVeganized, shoppingListStore, resetForLoad, resetAll } from '$lib/stores/shoppingList.js';
	import { dictionaryStore } from '$lib/stores/dictionary.js';
	import { supermarketStore } from '$lib/stores/supermarket.js';
	import { authFetch } from '$lib/api.js';

	let recipeId = $page.params.recipeId;
	let recipe = $state(null);
	let isLoading = $state(true);
	let error = $state(null);
	let activeTab = $state('zutaten');
	let currentServings = $state(2);
	let defaultServings = $state(2);
	let isSaved = $state(false);
	let toastVisible = $state(false);
	let toastMessage = $state('');

	let ingredientModalOpen = $state(false);
	let ingredientCheckedMap = $state({});
	let addToListToastVisible = $state(false);
	let addToListToastMessage = $state('');

	function showAddedToListToast(count) {
		addToListToastMessage = count === 1
			? '1 Zutat zur Liste hinzugefügt'
			: `${count} Zutaten zur Liste hinzugefügt`;
		addToListToastVisible = true;
		setTimeout(() => { addToListToastVisible = false; }, 2500);
	}
	let supermarketModalOpen = $state(false);
	let selectedIngredients = $state([]);
	let saveConfirmModalOpen = $state(false);

	onMount(async () => {
		try {
			const res = await authFetch(`/api/saved-recipes/${recipeId}`);
			if (res.ok) {
				recipe = await res.json();
				currentServings = recipe.servings || 2;
				defaultServings = recipe.servings || 2;
				isSaved = recipe.saved || recipe.isSaved || false;
			} else {
				error = 'Rezept nicht gefunden.';
			}
		} catch (e) {
			error = 'Fehler beim Laden des Rezepts.';
			console.error(e);
		} finally {
			isLoading = false;
		}
	});

	function scaledAmount(originalAmount) {
		if (!originalAmount) return null;
		const scaled = (originalAmount / defaultServings) * currentServings;
		return Math.round(scaled * 100) / 100;
	}

	function formatAmount(amount) {
		if (!amount && amount !== 0) return '';
		if (amount === Math.floor(amount)) return amount.toString();
		return amount.toFixed(1).replace(/\.0$/, '');
	}

	function formatIngredientAmount(ing) {
		const scaled = scaledAmount(ing.amount);
		if (!scaled && scaled !== 0) return '';
		return `${formatAmount(scaled)}${ing.unit ? ' ' + ing.unit : ''}`;
	}

	function incrementServings() { if (currentServings < 12) currentServings++; }
	function decrementServings() { if (currentServings > 1) currentServings--; }

	async function toggleSaved() {
		if (isSaved) {
			try {
				const res = await authFetch(`/api/saved-recipes/${recipeId}`, { method: 'DELETE' });
				if (res.ok) {
					isSaved = false;
					showToast('Aus gespeicherten Rezepten entfernt');
				}
			} catch (e) {
				console.error('Entfernen fehlgeschlagen:', e);
			}
		} else {
			try {
				const res = await authFetch(`/api/saved-recipes/${recipeId}/save`, { method: 'POST' });
				if (res.ok) {
					isSaved = true;
					showToast('Rezept gespeichert');
				}
			} catch (e) {
				console.error('Speichern fehlgeschlagen:', e);
			}
		}
	}

	function showToast(message) {
		toastMessage = message;
		toastVisible = true;
		setTimeout(() => { toastVisible = false; }, 2500);
	}

	function handleBack() {
		if (isSaved) {
			goto('/rezepte');
		} else {
			saveConfirmModalOpen = true;
		}
	}

	async function saveAndGoBack() {
		try {
			const res = await authFetch(`/api/saved-recipes/${recipeId}/save`, { method: 'POST' });
			if (res.ok) {
				isSaved = true;
			}
		} catch (e) {
			console.error('Speichern fehlgeschlagen:', e);
		}
		saveConfirmModalOpen = false;
		goto('/rezepte');
	}

	function discardAndGoBack() {
		saveConfirmModalOpen = false;
		goto('/rezepte');
	}

	function openIngredientModal() {
		const map = {};
		(recipe.ingredients || []).forEach((_, i) => { map[i] = true; });
		ingredientCheckedMap = map;
		ingredientModalOpen = true;
	}

	function collectSelectedIngredients() {
		return Object.entries(ingredientCheckedMap)
			.filter(([, v]) => v)
			.map(([k]) => {
				const ing = recipe.ingredients[parseInt(k)];
				return {
					name: ing.name,
					amount: scaledAmount(ing.amount),
					unit: ing.unit,
					type: ing.type || 'C',
					matchingDisplayName: ing.matchingDisplayName,
					matchingSubcategory: ing.matchingSubcategory
				};
			});
	}

	async function submitToShoppingList() {
		ingredientModalOpen = false;
		selectedIngredients = collectSelectedIngredients();

		if ($savedListId) {
			await addToExistingList($savedListId);
		} else if ($supermarketStore) {
			await createListFromRecipe($supermarketStore);
		} else {
			supermarketModalOpen = true;
		}
	}

	function findUiCategory(item) {
		if (item.uiCategory && item.uiCategory !== '') return item.uiCategory;

		const dict = $dictionaryStore;
		if (dict && dict.length > 0) {
			const entry = dict.find(e => e.displayName === item.displayName);
			if (entry) return entry.uiCategory;
		}

		if (item.type === 'C') {
			const name = (item.displayName || item.name || '').toLowerCase();

			const gemuese = ['tomaten', 'kartoffeln', 'zwiebeln', 'zwiebel', 'knoblauch',
				'paprika', 'karotten', 'brokkoli', 'zucchetti', 'avocado', 'gurke',
				'pilze', 'pak choi', 'lauch', 'frühlingszwiebeln', 'sojasprossen',
				'mais', 'erbsen', 'edamame', 'jalapeños', 'bananen', 'äpfel',
				'zitronen', 'limetten', 'heidelbeeren', 'erdbeeren', 'salat',
				'cherrytomaten', 'rotkraut', 'champignons'];
			if (gemuese.some(g => name.includes(g))) return 'Obst & Gemüse';

			const gewuerze = ['salz', 'pfeffer', 'oregano', 'basilikum', 'thymian',
				'kurkuma', 'zimt', 'muskat', 'dill', 'salbei', 'petersilie',
				'kreuzkümmel', 'chiliflocken', 'paprikapulver', 'knoblauchpulver',
				'ingwer', 'kapern', 'schnittlauch'];
			if (gewuerze.some(g => name.includes(g))) return 'Kräuter & Gewürze';

			const oele = ['olivenöl', 'rapsöl', 'kokosöl', 'sesamöl', 'öl'];
			if (oele.some(g => name.includes(g))) return 'Öle & Fette';

			const nuesse = ['cashews', 'walnüsse', 'erdnüsse', 'pinienkerne',
				'haselnüsse', 'sesam', 'chiasamen', 'kokosflocken'];
			if (nuesse.some(g => name.includes(g))) return 'Nüsse & Samen';

			const getreide = ['haferflocken', 'mehl', 'reis', 'paniermehl',
				'backpulver', 'hefeflocken', 'rollgerste', 'quinoa'];
			if (getreide.some(g => name.includes(g))) return 'Getreide & Hülsenfrüchte';
		}

		return 'Sonstiges';
	}

	async function addToExistingList(listId) {
		try {
			await authFetch(`/api/saved-recipes/${recipeId}/save`, { method: 'POST' });
			isSaved = true;
		} catch (e) { /* not critical */ }

		try {
			const res = await authFetch(`/api/shoppinglists/${listId}/items/batch`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ recipeTitle: recipe.title, ingredients: selectedIngredients })
			});
			if (res.ok) {
				const updatedList = await res.json();
				const expectedLabel = 'Zutat für ' + recipe.title;
				const newItems = (updatedList.items || []).filter(item => item.diyGroupLabel === expectedLabel);

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
					uiCategory: findUiCategory(item)
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
					diyGroupLabel: item.diyGroupLabel || null
				}));

				shoppingListStore.update(prev => [...prev, ...newStoreItems]);
				matchResults.update(prev => [...prev, ...newMatches]);
				itemIdMap.update(prev => {
					const updated = { ...prev };
					for (const item of newItems) { updated[item.itemId] = item.itemId; }
					return updated;
				});
				isVeganized.set(true);

				showAddedToListToast(selectedIngredients.length);
			} else {
				showToast('Fehler beim Hinzufügen');
			}
		} catch (e) {
			console.error('Zur Liste hinzufügen fehlgeschlagen:', e);
			showToast('Fehler beim Hinzufügen');
		}
	}

	async function createListFromRecipe(supermarket) {
		supermarketModalOpen = false;

		const apiSupermarket = supermarket === 'Migros' ? 'Migros' : supermarket.toLowerCase();

		try {
			await authFetch(`/api/saved-recipes/${recipeId}/save`, { method: 'POST' });
			isSaved = true;
		} catch (e) { /* not critical */ }

		try {
			const res = await authFetch('/api/shoppinglists/from-recipe', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ recipeTitle: recipe.title, supermarket: apiSupermarket, ingredients: selectedIngredients })
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
					uiCategory: findUiCategory(item)
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
					diyGroupLabel: item.diyGroupLabel || null
				}));

				resetForLoad(storeItems);
				matchResults.set(newMatches);
				isVeganized.set(true);

				showAddedToListToast(selectedIngredients.length);
			} else {
				showToast('Fehler beim Erstellen der Liste');
			}
		} catch (e) {
			console.error('Liste erstellen fehlgeschlagen:', e);
			showToast('Fehler beim Erstellen der Liste');
		}
	}

	function difficultyLabel(d) {
		if (d === 'easy') return 'Einfach';
		if (d === 'medium') return 'Mittel';
		if (d === 'hard') return 'Schwer';
		return d || '–';
	}
</script>

<!-- Toast -->
{#if toastVisible}
	<div class="toast">{toastMessage}</div>
{/if}

<!-- Add-to-list toast -->
{#if addToListToastVisible}
	<div class="add-toast">
		<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
			<polyline points="20 6 9 17 4 12"/>
		</svg>
		{addToListToastMessage}
	</div>
{/if}

<!-- Save-Confirmation Modal -->
{#if saveConfirmModalOpen}
	<div class="confirm-backdrop" onclick={() => saveConfirmModalOpen = false} role="presentation">
		<div class="confirm-dialog" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" tabindex="-1">
			<h3 class="confirm-title">Rezept speichern?</h3>
			<p class="confirm-text">
				Möchtest du das Rezept speichern, bevor du zurückgehst?
				Ungespeicherte Rezepte werden nach 7 Tagen automatisch gelöscht.
			</p>
			<div class="confirm-actions">
				<button class="confirm-btn-primary" onclick={saveAndGoBack}>Ja, speichern</button>
				<button class="confirm-btn-secondary" onclick={discardAndGoBack}>Nein, verwerfen</button>
				<button class="confirm-btn-cancel" onclick={() => saveConfirmModalOpen = false}>Abbrechen</button>
			</div>
		</div>
	</div>
{/if}

<!-- Supermarkt-Auswahl Modal -->
{#if supermarketModalOpen}
	<div class="modal-backdrop" onclick={() => supermarketModalOpen = false} role="presentation">
		<div class="modal-sheet" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label="Supermarkt auswählen" tabindex="-1">
			<div class="modal-header">
				<span class="modal-title">Neuer Einkaufszettel</span>
				<button class="modal-close" onclick={() => supermarketModalOpen = false} aria-label="Schliessen">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M18 6 6 18M6 6l12 12"/>
					</svg>
				</button>
			</div>
			<p class="modal-subtitle">Wähle deinen Supermarkt. Die Zutaten werden mit Produkten aus deinem Supermarkt abgeglichen.</p>
			<div class="supermarket-list">
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Migros')}>
					<span class="supermarket-name">Migros</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m9 18 6-6-6-6"/>
					</svg>
				</button>
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Coop')}>
					<span class="supermarket-name">Coop</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m9 18 6-6-6-6"/>
					</svg>
				</button>
				<button class="supermarket-btn" onclick={() => createListFromRecipe('Aldi')}>
					<span class="supermarket-name">Aldi</span>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m9 18 6-6-6-6"/>
					</svg>
				</button>
			</div>
			<div class="modal-footer">
				<button class="modal-cancel" onclick={() => supermarketModalOpen = false}>Abbrechen</button>
			</div>
		</div>
	</div>
{/if}

<!-- Ingredient Modal -->
{#if ingredientModalOpen}
	<div class="modal-backdrop" onclick={() => ingredientModalOpen = false} role="presentation">
		<div class="modal-sheet" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label="Zutaten auswählen" tabindex="-1">
			<div class="modal-header">
				<span class="modal-title">Zutaten auswählen</span>
				<button class="modal-close" onclick={() => ingredientModalOpen = false} aria-label="Schliessen">
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M18 6 6 18M6 6l12 12"/>
					</svg>
				</button>
			</div>
			<p class="modal-subtitle">Wähle die Zutaten ab, die du nicht auf deiner Einkaufsliste benötigst (z.B. weil du sie schon zuhause hast).</p>
			<div class="modal-list">
				{#each recipe.ingredients || [] as ing, i}
					<label class="modal-item">
						<input type="checkbox" class="modal-checkbox" bind:checked={ingredientCheckedMap[i]} />
						<span class="modal-item-name">{ing.name}</span>
						<span class="modal-item-amount">{formatIngredientAmount(ing)}</span>
					</label>
				{/each}
			</div>
			<div class="modal-footer">
				<button class="modal-add-btn" onclick={submitToShoppingList}>Hinzufügen</button>
				<button class="modal-cancel" onclick={() => ingredientModalOpen = false}>Abbrechen</button>
			</div>
		</div>
	</div>
{/if}

<div class="page">
	<!-- Top Bar -->
	<div class="top-bar">
		<button class="icon-btn" onclick={handleBack} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		{#if recipe}
			<button class="icon-btn" onclick={toggleSaved} aria-label={isSaved ? 'Gespeichert' : 'Speichern'}>
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill={isSaved ? 'currentColor' : 'none'} stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
				</svg>
			</button>
		{/if}
	</div>

	{#if isLoading}
		<div class="spinner-wrapper">
			<div class="spinner"></div>
		</div>
	{:else if error}
		<div class="error-wrapper">
			<p class="error-text">{error}</p>
			<button class="back-link" onclick={() => goto('/rezepte')}>Zurück zu Rezepten</button>
		</div>
	{:else if recipe}
		<!-- Recipe Image -->
		{#if recipe.imageUrl}
			<img src={recipe.imageUrl} alt={recipe.title} class="recipe-img" />
		{:else}
			<div class="recipe-img-placeholder">{recipe.title}</div>
		{/if}

		<!-- Recipe Info -->
		<div class="recipe-info">
			<span class="recipe-label">Rezept</span>
			<h1>{recipe.title}</h1>
			{#if recipe.description}
				<p class="recipe-desc">{recipe.description}</p>
			{/if}
		</div>

		<!-- Info Badges -->
		<div class="badges">
			{#if recipe.prepTimeMin}
				<div class="badge">
					<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
					</svg>
					<span>{recipe.prepTimeMin} Min</span>
				</div>
			{/if}
			{#if recipe.difficulty}
				<div class="badge">
					<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
					</svg>
					<span>{difficultyLabel(recipe.difficulty)}</span>
				</div>
			{/if}
			<div class="badge">
				<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
					<path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
				</svg>
				<span>{currentServings} Portionen</span>
			</div>
		</div>

		<!-- Tab Bar -->
		<div class="tab-bar">
			<button class="tab" class:tab-active={activeTab === 'zutaten'} onclick={() => activeTab = 'zutaten'}>
				Zutaten
			</button>
			<button class="tab" class:tab-active={activeTab === 'zubereitung'} onclick={() => activeTab = 'zubereitung'}>
				Zubereitung
			</button>
			<button class="tab" class:tab-active={activeTab === 'naehrwerte'} onclick={() => activeTab = 'naehrwerte'}>
				Nährwerte
			</button>
		</div>

		<!-- Tab: Zutaten -->
		{#if activeTab === 'zutaten'}
			<div class="servings-row">
				<span class="servings-label">Portionen</span>
				<div class="servings-controls">
					<button class="servings-btn" onclick={decrementServings} disabled={currentServings <= 1} aria-label="Weniger">−</button>
					<span class="servings-count">{currentServings}</span>
					<button class="servings-btn" onclick={incrementServings} disabled={currentServings >= 12} aria-label="Mehr">+</button>
				</div>
			</div>

			<div class="ingredient-list">
				{#each recipe.ingredients || [] as ing}
					<div class="ingredient-row">
						<span class="ingredient-amount">{formatIngredientAmount(ing)}</span>
						<span class="ingredient-name">{ing.name}</span>
					</div>
				{/each}
			</div>

			<div class="list-btn-wrapper">
				<button class="list-btn" onclick={openIngredientModal}>
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/>
						<path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/>
					</svg>
					Zur Einkaufsliste hinzufügen
				</button>
			</div>

			<div class="adjust-link-wrapper">
				<button class="adjust-link" onclick={() => goto('/rezepte/anpassen/' + (recipe.baseRecipeId || recipeId))}>
					Rezept anpassen ›
				</button>
			</div>
		{/if}

		<!-- Tab: Zubereitung -->
		{#if activeTab === 'zubereitung'}
			<div class="steps-list">
				{#each recipe.instructions || [] as step, i}
					<div class="step">
						<p class="step-header">Schritt {step.step || i + 1}/{(recipe.instructions || []).length}</p>
						<p class="step-text">{step.text}</p>
					</div>
				{/each}
			</div>
		{/if}

		<!-- Tab: Nährwerte -->
		{#if activeTab === 'naehrwerte'}
			<h2 class="nutrition-heading">Nährwerte pro Portion</h2>
			{#if recipe.totalNutritionPerServing}
				{@const n = recipe.totalNutritionPerServing}
				<table class="nutrition-table">
					<tbody>
						<tr class="nutrition-row">
							<td class="nutrition-label">Energie</td>
							<td class="nutrition-value">{n.kcal} kcal</td>
						</tr>
						<tr class="nutrition-row nutrition-row-alt">
							<td class="nutrition-label">Fett</td>
							<td class="nutrition-value">{n.fat} g</td>
						</tr>
						<tr class="nutrition-row">
							<td class="nutrition-label">davon gesättigte Fettsäuren</td>
							<td class="nutrition-value">{n.saturatedFat} g</td>
						</tr>
						<tr class="nutrition-row nutrition-row-alt">
							<td class="nutrition-label">Kohlenhydrate</td>
							<td class="nutrition-value">{n.carbs} g</td>
						</tr>
						<tr class="nutrition-row">
							<td class="nutrition-label">davon Zucker</td>
							<td class="nutrition-value">{n.sugar} g</td>
						</tr>
						<tr class="nutrition-row nutrition-row-alt">
							<td class="nutrition-label">Protein</td>
							<td class="nutrition-value">{n.protein} g</td>
						</tr>
						<tr class="nutrition-row">
							<td class="nutrition-label">Salz</td>
							<td class="nutrition-value">{n.salt} g</td>
						</tr>
					</tbody>
				</table>
				<p class="nutrition-hint">Geschätzte Werte basierend auf Standard-Nährwertangaben</p>
			{:else}
				<p class="nutrition-empty">Keine Nährwerte verfügbar</p>
			{/if}
		{/if}
	{/if}
</div>

<style>
	.page {
		padding: 0 0 100px;
	}

	/* Top Bar */
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

	/* Recipe image */
	.recipe-img {
		width: 100%;
		height: 220px;
		object-fit: cover;
		display: block;
	}

	.recipe-img-placeholder {
		width: 100%;
		height: 220px;
		background: #F3F4F6;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1rem;
		color: #9CA3AF;
		text-align: center;
		padding: 16px;
		box-sizing: border-box;
	}

	/* Recipe info */
	.recipe-info {
		padding: 16px;
	}

	.recipe-label {
		font-size: 0.8125rem;
		color: var(--color-text-light);
	}

	h1 {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 2px 0 0;
	}

	.recipe-desc {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		margin: 6px 0 0;
	}

	/* Badges */
	.badges {
		display: flex;
		justify-content: center;
		gap: 24px;
		padding: 16px;
		border-bottom: 1px solid var(--color-border);
	}

	.badge {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		color: var(--color-primary);
	}

	.badge span {
		font-size: 0.75rem;
		color: var(--color-text-light);
		text-align: center;
	}

	/* Tab Bar */
	.tab-bar {
		display: flex;
		border-bottom: 1px solid var(--color-border);
		position: sticky;
		top: 60px;
		background: white;
		z-index: 9;
	}

	.tab {
		flex: 1;
		padding: 14px;
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text-light);
		background: none;
		border: none;
		border-bottom: 2px solid transparent;
		cursor: pointer;
		transition: color 0.15s, border-color 0.15s;
		margin-bottom: -1px;
	}

	.tab-active {
		color: var(--color-primary);
		font-weight: 700;
		border-bottom-color: var(--color-primary);
	}

	/* Servings */
	.servings-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16px;
	}

	.servings-label {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.servings-controls {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.servings-btn {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		border: 1.5px solid var(--color-border);
		background: white;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1.125rem;
		cursor: pointer;
		color: var(--color-text);
		line-height: 1;
		padding: 0;
	}

	.servings-btn:disabled {
		opacity: 0.3;
		cursor: not-allowed;
	}

	.servings-count {
		font-size: 1.125rem;
		font-weight: 700;
		width: 40px;
		text-align: center;
		color: var(--color-text);
	}

	/* Ingredient list */
	.ingredient-list {
		padding: 0 16px;
	}

	.ingredient-row {
		display: flex;
		align-items: center;
		padding: 12px 0;
		border-bottom: 1px solid var(--color-border);
	}

	.ingredient-row:last-child {
		border-bottom: none;
	}

	.ingredient-amount {
		font-size: 0.875rem;
		color: var(--color-primary);
		font-weight: 600;
		min-width: 70px;
	}

	.ingredient-name {
		font-size: 0.9375rem;
		color: var(--color-text);
		flex: 1;
	}

	/* Add to list */
	.list-btn-wrapper {
		margin-top: 20px;
		padding: 0 16px 16px;
	}

	.list-btn {
		width: 100%;
		background: var(--color-primary);
		color: white;
		border: none;
		border-radius: 14px;
		padding: 16px;
		font-size: 1.0625rem;
		font-weight: 700;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 10px;
		box-shadow: 0 8px 30px rgba(45, 125, 70, 0.3);
	}

	.adjust-link-wrapper {
		display: flex;
		justify-content: center;
		padding: 4px 16px 16px;
	}

	.adjust-link {
		font-size: 0.875rem;
		color: var(--color-primary);
		font-weight: 500;
		background: none;
		border: none;
		cursor: pointer;
		padding: 4px;
	}

	/* Steps */
	.steps-list {
		padding: 16px;
	}

	.step {
		margin-bottom: 24px;
	}

	.step:last-child {
		margin-bottom: 0;
	}

	.step-header {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0 0 8px;
	}

	.step-text {
		font-size: 0.9375rem;
		color: var(--color-text);
		line-height: 1.7;
		margin: 0;
	}

	/* Spinner / Error */
	.spinner-wrapper {
		display: flex;
		justify-content: center;
		padding: 80px 0;
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

	.error-wrapper {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 80px 16px;
		gap: 16px;
	}

	.error-text {
		font-size: 1rem;
		color: var(--color-text-light);
		margin: 0;
	}

	.back-link {
		font-size: 0.9375rem;
		color: var(--color-primary);
		font-weight: 600;
		background: none;
		border: none;
		cursor: pointer;
	}

	/* Toast */
	.toast {
		position: fixed;
		top: 16px;
		left: 50%;
		transform: translateX(-50%);
		background: var(--color-primary);
		color: white;
		padding: 10px 20px;
		border-radius: 20px;
		font-size: 0.9375rem;
		font-weight: 500;
		z-index: 200;
		white-space: nowrap;
	}

	/* Add-to-list toast */
	.add-toast {
		position: fixed;
		bottom: calc(72px + 24px);
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 12px 20px;
		background: var(--color-primary);
		color: #fff;
		font-size: 0.9375rem;
		font-weight: 600;
		border-radius: 24px;
		box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
		z-index: 100;
		animation: toastFadeIn 0.2s ease-out;
	}

	@keyframes toastFadeIn {
		from { opacity: 0; transform: translate(-50%, 8px); }
		to { opacity: 1; transform: translate(-50%, 0); }
	}

	/* Ingredient Modal */
	.modal-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 60;
		display: flex;
		align-items: flex-end;
		justify-content: center;
	}

	.modal-sheet {
		background: white;
		border-radius: 20px 20px 0 0;
		width: 100%;
		max-width: 390px;
		max-height: 80dvh;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.modal-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 20px 20px 8px;
		flex-shrink: 0;
	}

	.modal-title {
		font-size: 1.0625rem;
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
		padding: 0;
	}

	.modal-subtitle {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		line-height: 1.5;
		margin: 0 0 12px;
		padding: 0 20px;
		flex-shrink: 0;
	}

	.modal-list {
		overflow-y: auto;
		flex: 1;
		padding: 0 20px;
	}

	.modal-item {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 0;
		border-bottom: 1px solid var(--color-border);
		cursor: pointer;
	}

	.modal-item:last-child {
		border-bottom: none;
	}

	.modal-checkbox {
		width: 20px;
		height: 20px;
		accent-color: var(--color-primary);
		cursor: pointer;
		flex-shrink: 0;
	}

	.modal-item-name {
		flex: 1;
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.modal-item-amount {
		font-size: 0.875rem;
		color: var(--color-primary);
		font-weight: 600;
	}

	.modal-footer {
		padding: 16px 20px 88px;
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.modal-add-btn {
		width: 100%;
		background: var(--color-primary);
		color: white;
		border: none;
		border-radius: 14px;
		padding: 14px;
		font-size: 1rem;
		font-weight: 700;
		cursor: pointer;
	}

	.modal-cancel {
		background: none;
		border: none;
		color: var(--color-text-light);
		font-size: 0.9375rem;
		cursor: pointer;
		padding: 4px;
		text-align: center;
	}

	.supermarket-list {
		display: flex;
		flex-direction: column;
		padding: 0 20px;
		gap: 8px;
	}

	.supermarket-btn {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		padding: 16px;
		border: 1.5px solid var(--color-border);
		border-radius: 14px;
		background: white;
		cursor: pointer;
		font-size: 1rem;
		color: var(--color-text);
	}

	.supermarket-btn:hover {
		border-color: var(--color-primary);
		background: #F0FBF4;
	}

	.supermarket-name {
		font-weight: 600;
	}

	/* Nutrition */
	.nutrition-heading {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
		padding: 16px;
		margin: 0;
	}

	.nutrition-table {
		width: 100%;
		border-collapse: collapse;
	}

	.nutrition-row td {
		padding: 12px 16px;
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.nutrition-row-alt td {
		background: #F9FAFB;
	}

	.nutrition-label {
		color: var(--color-text);
	}

	.nutrition-value {
		text-align: right;
		font-weight: 600;
		color: var(--color-text);
	}

	.nutrition-hint {
		font-size: 0.75rem;
		color: var(--color-text-light);
		text-align: center;
		padding: 12px;
		margin: 0;
	}

	.nutrition-empty {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		text-align: center;
		padding: 32px 16px;
		margin: 0;
	}

	/* Save-confirmation dialog (centered) */
	.confirm-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 60;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 24px;
	}

	.confirm-dialog {
		width: 100%;
		max-width: 340px;
		background: var(--color-bg);
		border-radius: 16px;
		padding: 24px 20px;
		display: flex;
		flex-direction: column;
		gap: 12px;
		box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
	}

	.confirm-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
		text-align: center;
	}

	.confirm-text {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		margin: 0;
		text-align: center;
	}

	.confirm-actions {
		display: flex;
		flex-direction: column;
		gap: 8px;
		margin-top: 8px;
	}

	.confirm-btn-primary {
		padding: 12px;
		border-radius: 12px;
		background: var(--color-primary);
		color: #fff;
		font-size: 0.9375rem;
		font-weight: 600;
		border: none;
		cursor: pointer;
		transition: background 0.15s ease;
	}

	.confirm-btn-primary:active {
		background: var(--color-primary-hover);
	}

	.confirm-btn-secondary {
		padding: 12px;
		border-radius: 12px;
		background: transparent;
		color: var(--color-text);
		font-size: 0.9375rem;
		font-weight: 500;
		border: 1.5px solid var(--color-border);
		cursor: pointer;
	}

	.confirm-btn-cancel {
		padding: 8px;
		background: none;
		border: none;
		color: var(--color-text-light);
		font-size: 0.875rem;
		cursor: pointer;
	}
</style>
