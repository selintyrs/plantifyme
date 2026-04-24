<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { matchResults, savedListId, itemIdMap } from '$lib/stores/shoppingList.js';
	import { dictionaryStore } from '$lib/stores/dictionary.js';

	let product = $state(null);
	let genericFood = $state(null);
	let isLoading = $state(true);
	let error = $state(null);
	let activeTab = $state('produkt');

	// 0 = try local file, 1 = try imageUrl from DB, 2 = show placeholder
	let imgStage = $state(0);

	let alternatives = $state(null);
	let alternativesLoading = $state(false);
	let altImgStages = $state({});
	let activeFilter = $state(null);
	let replacedProductName = $state(null);

	const filters = [
		{ id: 'cheaper',   label: 'Günstiger' },
		{ id: 'healthier', label: 'Mehr Protein' },
		{ id: 'lowcal',    label: 'Weniger Kalorien' }
	];

	let replacingId = $derived($page.url.searchParams.get('replacing'));

	let isInCurrentList = $derived.by(() => {
		const currentId = replacingId ?? $page.params.productId;
		return $matchResults.some(r => r.suggestedProduct?.id === currentId);
	});

	let isSavedProduct = $state(false);
	let savedProductId = $state(null);
	let savedToastVisible = $state(false);
	let savedToastMessage = $state('');
	let toastTimer;

	let diyRecipes = $state([]);
	let diyDisplayName = $state('');
	let diyModalOpen = $state(false);
	let diyCheckedMap = $state({});
	let diyReplacedVisible = $state(false);

	// ── Eier accordion state ──────────────────────────────────────────────
	let expandedRecipeId = $state(null);
	let eierChecked = $state({});

	const EIER_USE_CASE_ORDER = [
		'Süsses Backen & Teige',
		'Herzhafte Gerichte & Frühstück',
		'Aufschlagen & Mousse',
		'Binden',
		'Glasieren & Bräunen',
	];

	let isEierMode = $derived(diyDisplayName === 'Eier');

	let eierGrouped = $derived.by(() => {
		if (!isEierMode) return {};
		const groups = {};
		for (const recipe of diyRecipes) {
			const uc = recipe.useCase || 'Sonstiges';
			if (!groups[uc]) groups[uc] = [];
			groups[uc].push(recipe);
		}
		const ordered = {};
		for (const uc of EIER_USE_CASE_ORDER) {
			if (groups[uc]) ordered[uc] = groups[uc];
		}
		for (const uc of Object.keys(groups)) {
			if (!ordered[uc]) ordered[uc] = groups[uc];
		}
		return ordered;
	});

	let eierHasSelection = $derived.by(() => {
		for (const [recipeId, ingMap] of Object.entries(eierChecked)) {
			if (Object.values(ingMap).some(v => v)) return true;
		}
		return false;
	});


	const tabs = [
		{ id: 'produkt', label: 'Produkt' },
		{ id: 'naehrwerte', label: 'Nährwerte' },
		{ id: 'alternativen', label: 'Alternativen' },
		{ id: 'diy', label: 'DIY' }
	];

	const tabsAlt = [
		{ id: 'produkt', label: 'Produkt' },
		{ id: 'naehrwerte', label: 'Nährwerte' },
		{ id: 'diy', label: 'DIY' }
	];

	const tabsC = [
		{ id: 'naehrwerte', label: 'Nährwerte' }
	];

	let isFromAlternative = $derived($page.url.searchParams.get('from') === 'alternative');
	let isFromSaved = $derived($page.url.searchParams.get('from') === 'saved');

	let activeTabs = $derived.by(() => {
		const base = isFromAlternative ? tabsAlt : tabs;
		return diyRecipes.length === 0 ? base.filter(t => t.id !== 'diy') : base;
	});

	$effect(() => {
		const productId = $page.params.productId;
		const type = $page.url.searchParams.get('type');
		if (type === 'C') {
			loadGenericFood(productId);
		} else {
			loadProduct(productId);
		}
	});

	async function loadProduct(productId) {
		isLoading = true;
		error = null;
		imgStage = 0;
		genericFood = null;
		alternatives = null;
		altImgStages = {};
		activeFilter = null;
		activeTab = 'produkt';
		isSavedProduct = false;
		savedProductId = null;
		diyRecipes = [];
		diyDisplayName = '';
		try {
			const res = await fetch(`/api/products/${productId}`);
			if (res.status === 404) {
				error = 'Produkt nicht gefunden.';
				return;
			}
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			product = await res.json();
			checkIfSaved(productId);
			loadDiyRecipes(productId);
		} catch (e) {
			error = 'Fehler beim Laden des Produkts.';
			console.error(e);
		} finally {
			isLoading = false;
		}
	}

	async function checkIfSaved(productId) {
		try {
			const res = await fetch('/api/saved-products?userId=temp-user');
			if (!res.ok) return;
			const list = await res.json();
			const found = list.find(p => p.productId === productId);
			if (found) {
				isSavedProduct = true;
				savedProductId = found.id;
			}
		} catch (e) {
			console.error('Saved-Status prüfen fehlgeschlagen:', e);
		}
	}

	async function toggleSaved() {
		if (isSavedProduct) {
			try {
				const res = await fetch(`/api/saved-products/${savedProductId}`, {
					method: 'DELETE',
					headers: { 'X-User-Id': 'temp-user' },
				});
				if (res.ok) {
					isSavedProduct = false;
					savedProductId = null;
					showSavedToast('Produkt entfernt');
				}
			} catch (e) {
				console.error('Produkt entfernen fehlgeschlagen:', e);
			}
		} else {
			try {
				const res = await fetch('/api/saved-products', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json', 'X-User-Id': 'temp-user' },
					body: JSON.stringify({ productId: product.id, type: product.type }),
				});
				if (res.status === 201) {
					const saved = await res.json();
					isSavedProduct = true;
					savedProductId = saved.id;
					showSavedToast('Produkt gespeichert');
				} else if (res.status === 409) {
					isSavedProduct = true;
				}
			} catch (e) {
				console.error('Produkt speichern fehlgeschlagen:', e);
			}
		}
	}

	function showSavedToast(message) {
		savedToastMessage = message;
		savedToastVisible = true;
		clearTimeout(toastTimer);
		toastTimer = setTimeout(() => { savedToastVisible = false; }, 2500);
	}

	async function loadGenericFood(foodId) {
		isLoading = true;
		error = null;
		product = null;
		activeTab = 'naehrwerte';
		try {
			const res = await fetch(`/api/generic-foods/${foodId}`);
			if (res.status === 404) {
				error = 'Produkt nicht gefunden.';
				return;
			}
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			genericFood = await res.json();
		} catch (e) {
			error = 'Fehler beim Laden des Produkts.';
			console.error(e);
		} finally {
			isLoading = false;
		}
	}

	function localImageUrl(p) {
		const sm = p.supermarket?.toLowerCase() ?? 'unknown';
		return `/images/products/${sm}/${p.articleId}.jpg`;
	}

	function handleImgError() {
		if (imgStage === 0 && product?.imageUrl) {
			imgStage = 1;
		} else {
			imgStage = 2;
		}
	}


	async function loadAlternatives(filter = null) {
		alternativesLoading = true;
		alternatives = null;
		try {
			const url = filter
				? `/api/products/${$page.params.productId}/alternatives?filter=${filter}`
				: `/api/products/${$page.params.productId}/alternatives`;
			const res = await fetch(url);
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			alternatives = await res.json();
			altImgStages = {};
		} catch (e) {
			console.error('Alternativen laden fehlgeschlagen:', e);
			alternatives = [];
		} finally {
			alternativesLoading = false;
		}
	}

	async function toggleFilter(filterId) {
		if (activeFilter === filterId) {
			activeFilter = null;
			await loadAlternatives(null);
		} else {
			activeFilter = filterId;
			await loadAlternatives(filterId);
		}
	}

	function replaceProduct(newProduct) {
		const targetId = replacingId ?? $page.params.productId;
		const localMatch = $matchResults.find(r => r.suggestedProduct?.id === targetId);
		const backendItemId = localMatch ? $itemIdMap[localMatch.itemId] : null;

		matchResults.update(prev =>
			prev.map(r =>
				r.suggestedProduct?.id === targetId
					? { ...r, suggestedProduct: newProduct }
					: r
			)
		);
		replacedProductName = newProduct.name;

		if ($savedListId && backendItemId) {
			fetch(`/api/shoppinglists/${$savedListId}/items/${backendItemId}`, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json', 'X-User-Id': 'temp-user' },
				body: JSON.stringify({ productId: newProduct.id }),
			}).catch(e => console.error('Replace sync fehlgeschlagen:', e));
		}

		setTimeout(() => goto('/liste'), 1500);
	}

	function altLocalImageUrl(alt) {
		const sm = alt.supermarket?.toLowerCase() ?? 'unknown';
		return `/images/products/${sm}/${alt.articleId}.jpg`;
	}

	function handleAltImgError(altId, alt) {
		const stage = altImgStages[altId] ?? 0;
		if (stage === 0 && alt.imageUrl) {
			altImgStages = { ...altImgStages, [altId]: 1 };
		} else {
			altImgStages = { ...altImgStages, [altId]: 2 };
		}
	}

	async function loadDiyRecipes(productId) {
		const match = $matchResults.find(r => r.suggestedProduct?.id === productId);
		diyDisplayName = match?.displayName || '';
		const subcategory = match?.subcategory || '';
		if (!diyDisplayName) return;
		try {
			const res = await fetch(`/api/diy-recipes?displayName=${encodeURIComponent(diyDisplayName)}&subcategory=${encodeURIComponent(subcategory)}`);
			if (!res.ok) return;
			diyRecipes = await res.json();
		} catch (e) {
			console.error('DIY-Rezepte laden fehlgeschlagen:', e);
		}
	}

	function openDiyModal() {
		const recipe = diyRecipes[0];
		if (!recipe) return;
		const map = {};
		(recipe.ingredients || []).forEach((ing, i) => {
			if (ing.name !== 'Wasser') {
				map[i] = true;
			}
		});
		diyCheckedMap = map;
		diyModalOpen = true;
	}

	function toggleEierCard(recipe) {
		if (expandedRecipeId === recipe.id) {
			expandedRecipeId = null;
		} else {
			expandedRecipeId = recipe.id;
			if (!eierChecked[recipe.id]) {
				const map = {};
				(recipe.ingredients || []).forEach((ing, i) => {
					if (ing.name !== 'Wasser') map[i] = true;
				});
				eierChecked = { ...eierChecked, [recipe.id]: map };
			}
		}
	}

	function toggleEierIngredient(recipeId, index) {
		const current = eierChecked[recipeId] ?? {};
		eierChecked = { ...eierChecked, [recipeId]: { ...current, [index]: !current[index] } };
	}

	async function submitEier() {
		if (!$savedListId) return;
		const ingredients = [];
		for (const [recipeId, ingMap] of Object.entries(eierChecked)) {
			const recipe = diyRecipes.find(r => r.id === recipeId);
			if (!recipe) continue;
			(recipe.ingredients || []).forEach((ing, i) => {
				if (ing.name !== 'Wasser' && ingMap[i]) {
					ingredients.push({ name: ing.name, amount: ing.amount, unit: ing.unit, type: 'C' });
				}
			});
		}
		if (ingredients.length === 0) return;
		try {
			const res = await fetch(`/api/shoppinglists/${$savedListId}/items/batch`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', 'X-User-Id': 'temp-user' },
				body: JSON.stringify({ recipeTitle: 'Plantified Eier', ingredients }),
			});
			if (res.ok) {
				diyReplacedVisible = true;
				setTimeout(() => goto('/liste'), 1500);
			}
		} catch (e) {
			console.error('Eier-Zutaten hinzufügen fehlgeschlagen:', e);
		}
	}

	async function submitDiy() {
		const recipe = diyRecipes[0];
		if (!recipe || !$savedListId) return;

		const match = $matchResults.find(r => r.suggestedProduct?.id === product.id);
		const backendItemId = match ? $itemIdMap[match.itemId] : null;
		if (!backendItemId) return;

		const selectedIndexes = Object.entries(diyCheckedMap)
			.filter(([, v]) => v)
			.map(([k]) => parseInt(k))
			.sort((a, b) => a - b);

		try {
			const res = await fetch(
				`/api/shoppinglists/${$savedListId}/items/${backendItemId}/replace-with-diy`,
				{
					method: 'POST',
					headers: { 'Content-Type': 'application/json', 'X-User-Id': 'temp-user' },
					body: JSON.stringify({
						diyRecipeId: recipe.id,
						selectedIndexes,
						originalDisplayName: diyDisplayName,
					}),
				}
			);
			if (res.ok) {
				const updatedList = await res.json();

				const oldMatch = $matchResults.find(r => r.suggestedProduct?.id === product.id);
				matchResults.update(prev => prev.filter(r => r.suggestedProduct?.id !== product.id));

				function findUiCategory(item) {
					const entry = $dictionaryStore.find(d => d.displayName === item.displayName);
					if (entry) return entry.uiCategory;
					if (item.type === 'C') return 'Saucen & Gewürze';
					return 'Sonstiges';
				}

				const newDiyItems = (updatedList.items || []).filter(item => item.diyGroupLabel);
				const newMatches = newDiyItems.map(item => ({
					itemId: item.itemId,
					type: item.type,
					displayName: item.displayName || item.name,
					uiCategory: findUiCategory(item),
					suggestedProduct: item.type !== 'C' && item.productId
						? { id: item.productId, name: item.name, price: item.price }
						: null,
					genericFood: item.type === 'C' && item.genericFoodId
						? { id: item.genericFoodId, name: item.name }
						: null,
					diyGroupLabel: item.diyGroupLabel,
				}));
				matchResults.update(prev => [...prev, ...newMatches]);

				itemIdMap.update(prev => {
					const updated = { ...prev };
					for (const item of newDiyItems) {
						updated[item.itemId] = item.itemId;
					}
					return updated;
				});

				diyModalOpen = false;
				diyReplacedVisible = true;
				setTimeout(() => goto('/liste'), 1500);
			}
		} catch (e) {
			console.error('DIY Replace fehlgeschlagen:', e);
		}
	}
</script>

<svelte:head>
	<title>{product ? product.name : 'Produkt'} – Plantify</title>
</svelte:head>

<!-- ── Loading ───────────────────────────────────────────────────────────── -->
{#if isLoading}
	<div class="loading-screen">
		<div class="spinner"></div>
	</div>

<!-- ── Error ─────────────────────────────────────────────────────────────── -->
{:else if error}
	<div class="error-screen">
		<p class="error-text">{error}</p>
		<button class="btn-back-text" onclick={() => history.back()}>Zurück</button>
	</div>

<!-- ── Product ────────────────────────────────────────────────────────────── -->
{:else if product}
	<!-- Top bar (sticky) -->
	<div class="top-bar">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
		</button>
		<button class="icon-btn" aria-label="Speichern" onclick={toggleSaved}>
			{#if isSavedProduct}
				<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>
			{:else}
				<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>
			{/if}
		</button>
	</div>

	<!-- Product image -->
	<div class="image-wrapper">
		{#if imgStage < 2}
			<img
				src={imgStage === 0 ? localImageUrl(product) : product.imageUrl}
				alt={product.name}
				class="product-img"
				onerror={handleImgError}
			/>
		{:else}
			<div class="img-placeholder">
				<span>Kein Bild</span>
			</div>
		{/if}
	</div>

	<!-- Product info -->
	<div class="product-info">
		{#if product.brand}
			<p class="brand">{product.brand}</p>
		{/if}
		<h1 class="product-name">{product.name}</h1>
		<div class="price-row">
			<p class="price">CHF {product.price?.toFixed(2)}</p>
			{#if isFromAlternative && isInCurrentList}
				<button class="btn-replace-main" onclick={() => replaceProduct(product)}>
					Ersetzen
				</button>
			{/if}
		</div>
	</div>

	<!-- Tab bar (sticky, below info) -->
	<div class="tab-bar-wrapper">
		<div class="tab-bar" role="tablist">
			{#each activeTabs as tab}
				<button
					class="tab"
					class:tab-active={activeTab === tab.id}
					role="tab"
					aria-selected={activeTab === tab.id}
					onclick={() => { activeTab = tab.id; if (tab.id === 'alternativen' && alternatives === null) loadAlternatives(); }}
				>
					{tab.label}
				</button>
			{/each}
		</div>
	</div>

	<!-- Tab content -->
	<div class="tab-content">
		{#if activeTab === 'produkt'}
			<!-- Badges -->
			{#if product.isBio || product.isGlutenFree || product.hasAdditives}
				<div class="badges-row">
					{#if product.isBio}
						<span class="badge badge-green">
							<svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/><path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/></svg>
							Bio
						</span>
					{/if}
					{#if product.isGlutenFree}
						<span class="badge badge-green">Glutenfrei</span>
					{/if}
					{#if product.hasAdditives}
						<span class="badge badge-gray">Mit Zusatzstoffen</span>
					{/if}
				</div>
			{/if}

			<!-- Ingredients -->
			{#if product.ingredients && product.ingredients.length > 0}
				<div class="section">
					<h2 class="section-title">Zutaten</h2>
					<p class="section-text">{product.ingredients.join(', ')}</p>
				</div>
			{/if}

			<!-- Allergens contains -->
			{#if product.allergensContains}
				<div class="section">
					<h2 class="section-title">Enthält</h2>
					<p class="section-text">{product.allergensContains}</p>
				</div>
			{/if}

			<!-- Allergens may contain -->
			{#if product.allergensMayContain}
				<div class="section">
					<h2 class="section-title">Kann enthalten</h2>
					<p class="section-text">{product.allergensMayContain}</p>
				</div>
			{/if}

		{:else if activeTab === 'naehrwerte'}
			<div class="section">
				<table class="nutrition-table">
					<thead>
						<tr>
							<th>Nährwert</th>
							<th>{['ml','l','cl'].includes(product.unit?.toLowerCase()) ? 'pro 100 ml' : 'pro 100 g'}</th>
						</tr>
					</thead>
					<tbody>
						<tr class="row-odd"><td>Energie</td><td>{product.energyKj ?? '–'} kJ / {product.energyKcal ?? '–'} kcal</td></tr>
						<tr class="row-even"><td>Fett</td><td>{product.fatG ?? '–'} g</td></tr>
						<tr class="row-odd"><td>davon gesättigte Fettsäuren</td><td>{product.saturatedFatG ?? '–'} g</td></tr>
						<tr class="row-even"><td>Kohlenhydrate</td><td>{product.carbsG ?? '–'} g</td></tr>
						<tr class="row-odd"><td>davon Zucker</td><td>{product.sugarG ?? '–'} g</td></tr>
						<tr class="row-even"><td>Protein</td><td>{product.proteinG ?? '–'} g</td></tr>
						<tr class="row-odd"><td>Salz</td><td>{product.saltG ?? '–'} g</td></tr>
					</tbody>
				</table>
				<p class="per-unit-label">{['ml','l','cl'].includes(product.unit?.toLowerCase()) ? 'Pro 100ml' : 'Pro 100g'}</p>
			</div>

		{:else if activeTab === 'alternativen'}
			<div class="filter-bar">
				{#each filters as f}
					<button
						class="filter-pill"
						class:filter-pill-active={activeFilter === f.id}
						onclick={() => toggleFilter(f.id)}
					>
						{f.label}
					</button>
				{/each}
			</div>
			{#if alternativesLoading}
				<div class="tab-placeholder">
					<p>Alternativen werden geladen...</p>
				</div>
			{:else if alternatives !== null && alternatives.length === 0}
				<div class="tab-placeholder tab-placeholder-center">
					<p class="no-alternatives">Keine Alternativen gefunden</p>
				</div>
			{:else if alternatives}
				<div class="alternatives-list">
					{#each alternatives as alt, i (alt.id)}
						{@const altStage = altImgStages[alt.id] ?? 0}
						<div class="alt-card" class:alt-card-last={i === alternatives.length - 1}>
							<div class="alt-img-wrapper">
								{#if altStage < 2}
									<img
										src={altStage === 0 ? altLocalImageUrl(alt) : alt.imageUrl}
										alt={alt.name}
										class="alt-img"
										onerror={() => handleAltImgError(alt.id, alt)}
									/>
								{:else}
									<div class="alt-img-placeholder"></div>
								{/if}
							</div>
							<div class="alt-info">
								<span class="alt-name">{alt.name}</span>
								{#if alt.brand}
									<span class="alt-brand">{alt.brand}</span>
								{/if}
								<span class="alt-price">CHF {alt.price?.toFixed(2)}</span>
							</div>
							<div class="alt-actions">
								<button
									class="btn-alt-info"
									onclick={() => goto(`/products/${alt.id}?from=${isFromSaved ? 'saved' : 'alternative'}&replacing=${$page.params.productId}`)}
									aria-label="Details"
								>
									<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
								</button>
								{#if !isFromSaved && isInCurrentList}
									<button class="btn-replace" onclick={() => replaceProduct(alt)}>
										Ersetzen
									</button>
								{/if}
							</div>
						</div>
					{/each}
				</div>
			{/if}

		{:else if activeTab === 'diy'}
			{#if isEierMode}
				<!-- ── Eier Spezial-UI ────────────────────────────────────── -->
				<div class="section">
					<h2 class="diy-title">Wie man Eier ersetzt</h2>
					<p class="diy-description">Eier erfüllen beim Kochen verschiedene Zwecke. Wähle unten deinen Anwendungsfall, um die perfekte pflanzliche Alternative zu finden.</p>
				</div>
				<div class="eier-groups">
					{#each Object.entries(eierGrouped) as [useCase, recipes]}
						<p class="eier-use-case-label">{useCase}</p>
						{#each recipes as recipe (recipe.id)}
							{@const expanded = expandedRecipeId === recipe.id}
							{@const checkedMap = eierChecked[recipe.id] ?? {}}
							<div class="eier-card" class:eier-card-expanded={expanded}>
								<button class="eier-card-header" onclick={() => toggleEierCard(recipe)}>
									<span class="eier-card-title">{recipe.title}</span>
									<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
										{#if expanded}
											<polyline points="18 15 12 9 6 15"/>
										{:else}
											<polyline points="6 9 12 15 18 9"/>
										{/if}
									</svg>
								</button>
								{#if expanded}
									{#if recipe.description}
										<p class="eier-card-desc">{recipe.description}</p>
									{/if}
									<div class="eier-ing-list">
										{#each (recipe.ingredients || []) as ing, i}
											{#if ing.name !== 'Wasser'}
												<button
													class="eier-ing-row"
													class:eier-ing-unchecked={!checkedMap[i]}
													onclick={() => toggleEierIngredient(recipe.id, i)}
												>
													<div class="diy-checkbox" class:diy-checkbox-checked={checkedMap[i]}>
														{#if checkedMap[i]}
															<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
														{/if}
													</div>
													<span class="eier-ing-name">{ing.name}</span>
													<span class="eier-ing-qty">{ing.amount != null ? ing.amount : ''}{ing.unit ? '\u00a0' + ing.unit : ''}</span>
												</button>
											{/if}
										{/each}
									</div>
								{/if}
							</div>
						{/each}
					{/each}
				</div>
				<div class="eier-sticky-action">
					{#if $savedListId}
						<button class="btn-eier-add" disabled={!eierHasSelection} onclick={submitEier}>
							Ausgewählte zur Liste hinzufügen
						</button>
					{:else}
						<button class="btn-eier-add" disabled>
							Ausgewählte zur Liste hinzufügen
						</button>
						<p class="diy-hint">Speichere zuerst deine Liste, um Zutaten hinzuzufügen.</p>
					{/if}
				</div>
			{:else}
				<!-- ── Normaler DIY-Tab ────────────────────────────────────── -->
				{#each diyRecipes as recipe}
					<div class="diy-recipe">
						<div class="section">
							<h2 class="diy-title">{recipe.title}</h2>
							{#if recipe.description}
								<p class="diy-description">{recipe.description}</p>
							{/if}
						</div>
						<div class="section">
							<h3 class="diy-subtitle">Zutaten</h3>
							<div class="diy-ingredient-list">
								{#each (recipe.ingredients || []).filter(i => i.name !== 'Wasser') as ing}
									<div class="diy-ingredient-row">
										<span class="diy-ing-qty">{ing.amount != null ? ing.amount : ''}{ing.unit ? '\u00a0' + ing.unit : ''}</span>
										<span class="diy-ing-name">{ing.name}</span>
									</div>
								{/each}
							</div>
						</div>
						<div class="diy-actions">
							{#if $savedListId}
								<button class="btn-diy-add" onclick={openDiyModal}>
									Zutaten zur Liste hinzufügen
								</button>
							{:else}
								<button class="btn-diy-add" disabled>
									Zutaten zur Liste hinzufügen
								</button>
								<p class="diy-hint">Speichere zuerst deine Liste, um DIY-Zutaten hinzuzufügen.</p>
							{/if}
							<button class="btn-diy-recipe" disabled title="Bald verfügbar">
								Zum Rezept
							</button>
						</div>
					</div>
				{/each}
			{/if}
		{/if}
	</div>

<!-- ── Generic Food (Typ C) ───────────────────────────────────────────────── -->
{:else if genericFood}
	<div class="top-bar">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
		</button>
		<div></div>
	</div>

	<div class="image-wrapper">
		{#if genericFood.imageUrl}
			<img src={genericFood.imageUrl} alt={genericFood.name} class="product-img" />
		{:else}
			<div class="img-placeholder"><span>Kein Bild</span></div>
		{/if}
	</div>

	<div class="product-info">
		{#if genericFood.category}
			<p class="brand">{genericFood.category}</p>
		{/if}
		<h1 class="product-name">{genericFood.name}</h1>
	</div>

	<div class="tab-bar-wrapper">
		<div class="tab-bar" role="tablist">
			{#each tabsC as tab}
				<button
					class="tab"
					class:tab-active={activeTab === tab.id}
					role="tab"
					aria-selected={activeTab === tab.id}
					onclick={() => { activeTab = tab.id; }}
				>
					{tab.label}
				</button>
			{/each}
		</div>
	</div>

	<div class="tab-content">
		<div class="section">
			<table class="nutrition-table">
				<thead>
					<tr>
						<th>Nährwert</th>
						<th>pro 100 g</th>
					</tr>
				</thead>
				<tbody>
					<tr class="row-odd"><td>Energie</td><td>{genericFood.kcalPer100g ?? '–'} kcal</td></tr>
					<tr class="row-even"><td>Fett</td><td>{genericFood.fatPer100g ?? '–'} g</td></tr>
					<tr class="row-odd"><td>davon gesättigte Fettsäuren</td><td>{genericFood.saturatedFatPer100g ?? '–'} g</td></tr>
					<tr class="row-even"><td>Kohlenhydrate</td><td>{genericFood.carbsPer100g ?? '–'} g</td></tr>
					<tr class="row-odd"><td>davon Zucker</td><td>{genericFood.sugarPer100g ?? '–'} g</td></tr>
					<tr class="row-even"><td>Protein</td><td>{genericFood.proteinPer100g ?? '–'} g</td></tr>
					<tr class="row-odd"><td>Salz</td><td>{genericFood.saltPer100g ?? '–'} g</td></tr>
				</tbody>
			</table>
		</div>
	</div>
{/if}

<!-- ── Saved toast ─────────────────────────────────────────────────────────── -->
{#if savedToastVisible}
	<div class="saved-toast">{savedToastMessage}</div>
{/if}

<!-- ── DIY ingredients modal ─────────────────────────────────────────────── -->
{#if diyModalOpen}
	<div class="diy-backdrop" role="presentation" onclick={() => { diyModalOpen = false; }}>
		<div class="diy-sheet" role="dialog" aria-modal="true" tabindex="-1" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()}>
			<div class="diy-sheet-header">
				<h2 class="diy-sheet-title">Produkte entfernen</h2>
				<button class="icon-btn" onclick={() => { diyModalOpen = false; }} aria-label="Schliessen">
					<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
				</button>
			</div>
			<p class="diy-sheet-sub">Wähle die Zutaten ab, die du nicht auf deiner Einkaufsliste benötigst (z.B. weil du sie schon zuhause hast).</p>
			<div class="diy-check-list">
				{#each (diyRecipes[0]?.ingredients || []) as ing, i}
					{#if ing.name !== 'Wasser'}
						<button
							class="diy-check-row"
							class:diy-check-row-unchecked={!diyCheckedMap[i]}
							onclick={() => { diyCheckedMap = { ...diyCheckedMap, [i]: !diyCheckedMap[i] }; }}
						>
							<div class="diy-checkbox" class:diy-checkbox-checked={diyCheckedMap[i]}>
								{#if diyCheckedMap[i]}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
							<span class="diy-check-name">{ing.name}</span>
							<span class="diy-check-amount">{ing.amount != null ? ing.amount : ''}{ing.unit ? '\u00a0' + ing.unit : ''}</span>
						</button>
					{/if}
				{/each}
			</div>
			<div class="diy-sheet-actions">
				<button class="btn-diy-weiter" onclick={submitDiy}>Weiter</button>
				<button class="btn-diy-cancel" onclick={() => { diyModalOpen = false; }}>Abbrechen</button>
			</div>
		</div>
	</div>
{/if}

<!-- ── DIY replace confirmation ───────────────────────────────────────────── -->
{#if diyReplacedVisible}
	<div class="replace-backdrop">
		<div class="replace-modal">
			<div class="replace-check">
				<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
					<polyline points="20 6 9 17 4 12"/>
				</svg>
			</div>
			<p class="replace-title">Produkt wurde durch DIY-Zutaten ersetzt</p>
		</div>
	</div>
{/if}

<!-- ── Replace confirmation modal ────────────────────────────────────────── -->
{#if replacedProductName}
	<div class="replace-backdrop">
		<div class="replace-modal">
			<div class="replace-check">
				<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
					<polyline points="20 6 9 17 4 12"/>
				</svg>
			</div>
			<p class="replace-title">Produkt wurde ersetzt</p>
			<p class="replace-name">{replacedProductName}</p>
		</div>
	</div>
{/if}

<style>
	/* ── Loading / Error ──────────────────────────────────────────────────── */
	.loading-screen {
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 100dvh;
	}

	.spinner {
		width: 44px;
		height: 44px;
		border-radius: 50%;
		border: 4px solid var(--color-primary-light);
		border-top-color: var(--color-primary);
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin { to { transform: rotate(360deg); } }

	.error-screen {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 100dvh;
		gap: 16px;
		padding: 24px;
	}

	.error-text {
		font-size: 1rem;
		color: var(--color-text-light);
		text-align: center;
	}

	.btn-back-text {
		color: var(--color-primary);
		font-size: 0.9375rem;
		font-weight: 600;
	}

	/* ── Top bar ──────────────────────────────────────────────────────────── */
	.top-bar {
		position: sticky;
		top: 0;
		z-index: 30;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 16px 10px;
		background: var(--color-bg);
	}

	.icon-btn {
		color: var(--color-primary);
		padding: 4px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	/* ── Image ────────────────────────────────────────────────────────────── */
	.image-wrapper {
		width: 100%;
		height: 250px;
		background: #F9FAFB;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
	}

	.product-img {
		width: 100%;
		height: 100%;
		object-fit: contain;
	}

	.img-placeholder {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
		background: #F3F4F6;
		color: #9CA3AF;
		font-size: 0.875rem;
	}

	/* ── Product info ─────────────────────────────────────────────────────── */
	.product-info {
		padding: 16px 16px 12px;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.brand {
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}

	.product-name {
		font-size: 1.375rem;
		font-weight: 700;
		color: var(--color-text);
		line-height: 1.3;
	}

	.price-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	.price {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-primary);
	}

	.btn-replace-main {
		padding: 10px 20px;
		border-radius: 24px;
		background: #2D7D46;
		color: #ffffff;
		font-size: 0.9375rem;
		font-weight: 700;
		white-space: nowrap;
		flex-shrink: 0;
	}

	/* ── Tab bar ──────────────────────────────────────────────────────────── */
	.tab-bar-wrapper {
		position: sticky;
		top: 52px;
		z-index: 20;
		background: var(--color-bg);
		border-bottom: 1px solid var(--color-border);
	}

	.tab-bar {
		display: flex;
		overflow-x: auto;
		scrollbar-width: none;
		padding: 0 8px;
	}

	.tab-bar::-webkit-scrollbar { display: none; }

	.tab {
		flex-shrink: 0;
		padding: 10px 14px;
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text-light);
		border-bottom: 2px solid transparent;
		transition: color 0.15s ease, border-color 0.15s ease;
		white-space: nowrap;
	}

	.tab-active {
		color: var(--color-primary);
		font-weight: 700;
		border-bottom-color: var(--color-primary);
	}

	/* ── Tab content ──────────────────────────────────────────────────────── */
	.tab-content {
		padding-bottom: 40px;
	}

	/* ── Badges ───────────────────────────────────────────────────────────── */
	.badges-row {
		display: flex;
		gap: 8px;
		overflow-x: auto;
		scrollbar-width: none;
		padding: 12px 16px;
	}

	.badges-row::-webkit-scrollbar { display: none; }

	.badge {
		display: inline-flex;
		align-items: center;
		gap: 5px;
		padding: 5px 12px;
		border-radius: 20px;
		font-size: 0.8125rem;
		font-weight: 500;
		white-space: nowrap;
		flex-shrink: 0;
	}

	.badge-green {
		background: var(--color-primary-light);
		color: var(--color-primary);
	}

	.badge-gray {
		background: #F3F4F6;
		color: var(--color-text-light);
	}

	/* ── Sections ─────────────────────────────────────────────────────────── */
	.section {
		padding: 16px 16px 0;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.section-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.section-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		line-height: 1.6;
	}

	/* ── Tab placeholders ─────────────────────────────────────────────────── */
	.tab-placeholder {
		padding: 32px 16px;
		color: var(--color-text-light);
		font-size: 0.9375rem;
	}

	.tab-placeholder-center {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 16px;
		padding: 64px 32px;
		text-align: center;
		color: #D1D5DB;
	}

	/* ── Nutrition table ──────────────────────────────────────────────────── */
	.nutrition-table {
		width: 100%;
		border-collapse: collapse;
		font-size: 0.9375rem;
	}

	.nutrition-table th {
		text-align: left;
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-light);
		padding: 0 0 10px;
		border-bottom: 1px solid var(--color-border);
	}

	.nutrition-table th:last-child {
		text-align: right;
	}

	.nutrition-table td {
		padding: 10px 0;
		color: var(--color-text);
		border-bottom: 1px solid var(--color-border);
	}

	.nutrition-table td:last-child {
		text-align: right;
		color: var(--color-text-light);
	}

	.nutrition-table .row-even td {
		background: #ffffff;
	}

	.nutrition-table .row-odd td {
		background: #f4f9f6;
	}

	.per-unit-label {
		text-align: center;
		font-size: 0.8125rem;
		color: var(--color-text-light);
		margin-top: 12px;
	}

	/* ── Alternatives ─────────────────────────────────────────────────────── */
	.filter-bar {
		display: flex;
		gap: 8px;
		overflow-x: auto;
		scrollbar-width: none;
		padding: 12px 16px;
	}

	.filter-bar::-webkit-scrollbar { display: none; }

	.filter-pill {
		flex-shrink: 0;
		padding: 7px 16px;
		border-radius: 20px;
		font-size: 0.875rem;
		font-weight: 500;
		border: 1.5px solid var(--color-border);
		background: #ffffff;
		color: var(--color-text);
		transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
	}

	.filter-pill-active {
		background: #2D7D46;
		color: #ffffff;
		border-color: #2D7D46;
	}

	.no-alternatives {
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}

	.alternatives-list {
		padding: 0 16px;
	}

	.alt-card {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 14px 0;
		border-bottom: 1px solid var(--color-border);
	}

	.alt-card-last {
		border-bottom: none;
	}

	.alt-img-wrapper {
		width: 60px;
		height: 60px;
		flex-shrink: 0;
		border-radius: 12px;
		overflow: hidden;
		background: #F9FAFB;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.alt-img {
		width: 100%;
		height: 100%;
		object-fit: contain;
	}

	.alt-img-placeholder {
		width: 100%;
		height: 100%;
		background: #F3F4F6;
	}

	.alt-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.alt-name {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
		line-height: 1.3;
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	.alt-brand {
		font-size: 0.8125rem;
		color: var(--color-text-light);
	}

	.alt-price {
		font-size: 0.875rem;
		font-weight: 700;
		color: var(--color-primary);
	}

	.alt-actions {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-shrink: 0;
	}

	.btn-alt-info {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: #F3F4F6;
		color: var(--color-text-light);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.btn-replace {
		padding: 8px 16px;
		border-radius: 20px;
		background: var(--color-primary);
		color: #ffffff;
		font-size: 0.875rem;
		font-weight: 600;
		white-space: nowrap;
	}

	/* ── Replace confirmation modal ───────────────────────────────────────── */
	.replace-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.35);
		z-index: 100;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.replace-modal {
		background: var(--color-bg);
		border-radius: 20px;
		padding: 32px 28px;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		width: min(320px, 85vw);
		box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
	}

	.replace-check {
		width: 56px;
		height: 56px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.replace-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
		text-align: center;
	}

	.replace-name {
		font-size: 0.875rem;
		color: var(--color-text-light);
		text-align: center;
		line-height: 1.4;
	}

	/* ── DIY Tab ─────────────────────────────────────────────────────────── */
	.diy-recipe {
		display: flex;
		flex-direction: column;
		gap: 0;
	}

	.diy-title {
		font-size: 1.375rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.diy-description {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		line-height: 1.6;
	}

	.diy-subtitle {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
	}

	.diy-ingredient-list {
		display: flex;
		flex-direction: column;
		gap: 6px;
	}

	.diy-ingredient-row {
		display: flex;
		align-items: baseline;
		gap: 8px;
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.diy-ing-qty {
		color: var(--color-text-light);
		min-width: 60px;
		flex-shrink: 0;
	}

	.diy-ing-name {
		color: var(--color-text);
	}

	/* ── DIY action buttons ───────────────────────────────────────────────── */
	.diy-actions {
		padding: 24px 16px 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.btn-diy-add {
		width: 100%;
		padding: 16px;
		border-radius: 16px;
		background: var(--color-primary);
		color: #fff;
		font-size: 1.0625rem;
		font-weight: 700;
		text-align: center;
		box-shadow: 0 8px 30px rgba(45, 125, 70, 0.3);
		transition: background 0.15s ease;
	}

	.btn-diy-add:active {
		background: var(--color-primary-hover);
	}

	.btn-diy-add:disabled {
		background: #d1d5db;
		box-shadow: none;
		cursor: not-allowed;
	}

	.btn-diy-recipe {
		width: 100%;
		padding: 16px;
		border-radius: 16px;
		border: 1.5px solid var(--color-primary);
		background: var(--color-bg);
		color: var(--color-primary);
		font-size: 1.0625rem;
		font-weight: 700;
		text-align: center;
	}

	.btn-diy-recipe:disabled {
		border-color: #d1d5db;
		color: #d1d5db;
		cursor: not-allowed;
	}

	.diy-hint {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		text-align: center;
		line-height: 1.4;
	}

	/* ── Eier accordion ───────────────────────────────────────────────────── */
	.eier-groups {
		padding: 0 16px;
	}

	.eier-use-case-label {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--color-primary);
		letter-spacing: 0.05em;
		text-transform: uppercase;
		margin-top: 20px;
		margin-bottom: 10px;
	}

	.eier-card {
		border: 1.5px solid var(--color-border);
		border-radius: 14px;
		margin-bottom: 8px;
		overflow: hidden;
		transition: border-color 0.15s ease;
	}

	.eier-card-expanded {
		border-color: var(--color-primary);
	}

	.eier-card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		padding: 14px 16px;
		background: transparent;
		color: var(--color-text);
		text-align: left;
		cursor: pointer;
	}

	.eier-card-title {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.eier-card-desc {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		padding: 0 16px 12px;
	}

	.eier-ing-list {
		display: flex;
		flex-direction: column;
		padding: 0 12px 12px;
		gap: 4px;
	}

	.eier-ing-row {
		display: flex;
		align-items: center;
		gap: 10px;
		width: 100%;
		padding: 8px 4px;
		background: transparent;
		text-align: left;
		border-radius: 8px;
		transition: background 0.1s ease;
	}

	.eier-ing-row:active {
		background: var(--color-primary-light);
	}

	.eier-ing-unchecked {
		opacity: 0.45;
	}

	.eier-ing-name {
		flex: 1;
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.eier-ing-qty {
		font-size: 0.875rem;
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	.eier-sticky-action {
		position: sticky;
		bottom: 0;
		padding: 12px 16px 24px;
		background: var(--color-bg);
		border-top: 1px solid var(--color-border);
		margin-top: 16px;
	}

	.btn-eier-add {
		width: 100%;
		padding: 16px;
		border-radius: 14px;
		background: var(--color-primary);
		color: #fff;
		font-size: 1rem;
		font-weight: 600;
		text-align: center;
		transition: background 0.15s ease;
	}

	.btn-eier-add:disabled {
		opacity: 0.45;
	}

	.btn-eier-add:not(:disabled):active {
		background: var(--color-primary-hover);
	}

	/* ── DIY modal (bottom sheet) ─────────────────────────────────────────── */
	.diy-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 60;
		display: flex;
		align-items: flex-end;
		justify-content: center;
	}

	.diy-sheet {
		width: 100%;
		max-width: 390px;
		max-height: 80dvh;
		background: var(--color-bg);
		border-radius: 20px 20px 0 0;
		padding: 0 0 24px;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.diy-sheet::before {
		content: '';
		display: block;
		width: 40px;
		height: 4px;
		background: #d1d5db;
		border-radius: 2px;
		margin: 12px auto;
		flex-shrink: 0;
	}

	.diy-sheet-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 8px 20px 4px;
		flex-shrink: 0;
	}

	.diy-sheet-title {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.diy-sheet-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		padding: 0 20px 12px;
		flex-shrink: 0;
	}

	.diy-check-list {
		flex: 1;
		overflow-y: auto;
		padding: 4px 20px;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.diy-check-row {
		display: flex;
		align-items: center;
		gap: 14px;
		padding: 14px 16px;
		border-radius: 14px;
		border: 1.5px solid var(--color-primary-light);
		background: var(--color-primary-light);
		width: 100%;
		text-align: left;
		transition: background 0.12s ease, border-color 0.12s ease;
	}

	.diy-check-row-unchecked {
		background: var(--color-bg);
		border-color: var(--color-border);
	}

	.diy-checkbox {
		width: 28px;
		height: 28px;
		border-radius: 50%;
		border: 2px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		background: var(--color-bg);
		transition: background 0.12s ease, border-color 0.12s ease;
	}

	.diy-checkbox-checked {
		background: var(--color-primary);
		border-color: var(--color-primary);
	}

	.diy-check-name {
		flex: 1;
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.diy-check-amount {
		font-size: 0.875rem;
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	.diy-sheet-actions {
		padding: 16px 20px 0;
		display: flex;
		flex-direction: column;
		gap: 8px;
		flex-shrink: 0;
	}

	.btn-diy-weiter {
		width: 100%;
		padding: 14px;
		border-radius: 14px;
		background: var(--color-primary);
		color: #fff;
		font-size: 0.9375rem;
		font-weight: 700;
		text-align: center;
	}

	.btn-diy-cancel {
		width: 100%;
		padding: 10px;
		background: none;
		color: var(--color-text-light);
		font-size: 0.9375rem;
		font-weight: 500;
		text-align: center;
	}

	/* ── Saved toast ──────────────────────────────────────────────────────── */
	.saved-toast {
		position: fixed;
		top: 16px;
		left: 50%;
		transform: translateX(-50%);
		background: var(--color-primary);
		color: #ffffff;
		padding: 10px 20px;
		border-radius: 20px;
		font-size: 0.875rem;
		font-weight: 600;
		z-index: 200;
		white-space: nowrap;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
	}
</style>
