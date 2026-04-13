<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { matchResults } from '$lib/stores/shoppingList.js';

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
		try {
			const res = await fetch(`/api/products/${productId}`);
			if (res.status === 404) {
				error = 'Produkt nicht gefunden.';
				return;
			}
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			product = await res.json();
		} catch (e) {
			error = 'Fehler beim Laden des Produkts.';
			console.error(e);
		} finally {
			isLoading = false;
		}
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
		return `/images/products/${sm}/${p.articleId}.png`;
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
		matchResults.update(prev =>
			prev.map(r =>
				r.suggestedProduct?.id === targetId
					? { ...r, suggestedProduct: newProduct }
					: r
			)
		);
		replacedProductName = newProduct.name;
		setTimeout(() => goto('/liste'), 1500);
	}

	function altLocalImageUrl(alt) {
		const sm = alt.supermarket?.toLowerCase() ?? 'unknown';
		return `/images/products/${sm}/${alt.articleId}.png`;
	}

	function handleAltImgError(altId, alt) {
		const stage = altImgStages[altId] ?? 0;
		if (stage === 0 && alt.imageUrl) {
			altImgStages = { ...altImgStages, [altId]: 1 };
		} else {
			altImgStages = { ...altImgStages, [altId]: 2 };
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
		<button class="icon-btn" aria-label="Speichern">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>
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
			{#if isFromAlternative}
				<button class="btn-replace-main" onclick={() => replaceProduct(product)}>
					Ersetzen
				</button>
			{/if}
		</div>
	</div>

	<!-- Tab bar (sticky, below info) -->
	<div class="tab-bar-wrapper">
		<div class="tab-bar" role="tablist">
			{#each (isFromAlternative ? tabsAlt : tabs) as tab}
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
									onclick={() => goto(`/products/${alt.id}?from=alternative&replacing=${$page.params.productId}`)}
									aria-label="Details"
								>
									<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
								</button>
								<button class="btn-replace" onclick={() => replaceProduct(alt)}>
									Ersetzen
								</button>
							</div>
						</div>
					{/each}
				</div>
			{/if}

		{:else if activeTab === 'diy'}
			<div class="tab-placeholder tab-placeholder-center">
				<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
					<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
					<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
				</svg>
				<p class="diy-text">Bald verfügbar – Hier findest du Rezepte, um vegane Alternativen selbst herzustellen.</p>
			</div>
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

	.diy-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		line-height: 1.6;
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
</style>
