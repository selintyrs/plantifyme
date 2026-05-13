<script>
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { get } from 'svelte/store';
	import {
		matchResults,
		isVeganized,
		savedListId,
		itemIdMap,
		resetForLoad,
	} from '$lib/stores/shoppingList.js';
	import { supermarketStore } from '$lib/stores/supermarket.js';
	import { dictionaryStore, loadDictionary } from '$lib/stores/dictionary.js';
	import { authFetch } from '$lib/api.js';

	let lists = $state([]);
	let isLoading = $state(true);
	const validTabs = ['listen', 'produkte', 'rezepte'];
	const tabParam = $page.url.searchParams.get('tab') ?? '';
	let activeTab = $state(validTabs.includes(tabParam) ? tabParam : 'listen');
	let pendingDeleteId = $state(null);

	let savedProducts = $state([]);
	let productsLoaded = $state(false);
	let pendingDeleteProductId = $state(null);
	let prodImgErrors = $state({});

	let savedRecipes = $state([]);
	let pendingDeleteRecipeId = $state(null);
	let recipesLoaded = $state(false);

	const tabs = [
		{ id: 'listen',   label: 'Listen' },
		{ id: 'produkte', label: 'Produkte' },
		{ id: 'rezepte',  label: 'Rezepte' },
	];

	$effect(() => {
		loadLists();
		if (activeTab === 'produkte') loadSavedProducts();
		if (activeTab === 'rezepte') loadSavedRecipes();
	});

	async function loadLists() {
		isLoading = true;
		try {
			const res = await authFetch('/api/shoppinglists');
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			const data = await res.json();
			lists = data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
		} catch (e) {
			console.error('Listen laden fehlgeschlagen:', e);
			lists = [];
		} finally {
			isLoading = false;
		}
	}

	async function loadSavedRecipes() {
		try {
			const res = await authFetch('/api/saved-recipes');
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			savedRecipes = await res.json();
		} catch (e) {
			console.error('Rezepte laden fehlgeschlagen:', e);
			savedRecipes = [];
		} finally {
			recipesLoaded = true;
		}
	}

	async function loadSavedProducts() {
		try {
			const res = await authFetch('/api/saved-products');
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			savedProducts = await res.json();
		} catch (e) {
			console.error('Favoriten laden fehlgeschlagen:', e);
			savedProducts = [];
		} finally {
			productsLoaded = true;
		}
	}

	async function confirmDeleteProduct(id) {
		try {
			const res = await authFetch(`/api/saved-products/${id}`, {
				method: 'DELETE',
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			savedProducts = savedProducts.filter((p) => p.id !== id);
		} catch (e) {
			console.error('Favorit löschen fehlgeschlagen:', e);
		} finally {
			pendingDeleteProductId = null;
		}
	}

	async function confirmDelete(listId) {
		try {
			const res = await authFetch(`/api/shoppinglists/${listId}`, {
				method: 'DELETE',
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			lists = lists.filter((l) => l.id !== listId);
		} catch (e) {
			console.error('Liste löschen fehlgeschlagen:', e);
		} finally {
			pendingDeleteId = null;
		}
	}

	async function confirmDeleteRecipe(id) {
		try {
			const res = await authFetch(`/api/saved-recipes/${id}`, {
				method: 'DELETE',
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			savedRecipes = savedRecipes.filter((r) => r.id !== id);
		} catch (e) {
			console.error('Rezept löschen fehlgeschlagen:', e);
		} finally {
			pendingDeleteRecipeId = null;
		}
	}

	async function loadListIntoStore(list) {
		await loadDictionary();
		const items = list.items || [];
		const dictionary = get(dictionaryStore) ?? [];

		const storeItems = items.map((item, index) => {
			const displayName = item.displayName || item.name;
			const dictEntry = dictionary.find(
				(e) => e.displayName === displayName && e.type === item.type
			);
			return {
				id: index + 1,
				displayName,
				type: item.type,
				subcategory: item.subcategory || '',
				quantity: item.quantity,
				frontendDictionaryId: dictEntry?.id ?? null,
				uiCategory: dictEntry?.uiCategory ?? 'Sonstiges',
			};
		});

		const idMap = {};
		items.forEach((item, index) => {
			idMap[(index + 1).toString()] = item.itemId;
		});

		resetForLoad(storeItems);
		matchResults.set([]);
		isVeganized.set(false);
		supermarketStore.set(list.supermarket);
		savedListId.set(list.id);
		itemIdMap.set(idMap);

		goto('/liste?reload=1');
	}

	function formatDate(dateStr) {
		if (!dateStr) return '';
		const d = new Date(dateStr);
		return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()}`;
	}
</script>

<svelte:head>
	<title>Gespeicherte Listen – Plantify-Me</title>
</svelte:head>

<div class="page">
	<!-- Header -->
	<div class="header">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
				fill="none" stroke="currentColor" stroke-width="2"
				stroke-linecap="round" stroke-linejoin="round">
				<polyline points="15 18 9 12 15 6"/>
			</svg>
		</button>
		<h1>Gespeicherte Listen</h1>
		<div class="header-spacer"></div>
	</div>

	<!-- Tabs -->
	<div class="tab-bar">
		{#each tabs as tab}
			<button
				class="tab-btn"
				class:tab-active={activeTab === tab.id}
				onclick={() => { activeTab = tab.id; if (tab.id === 'produkte' && !productsLoaded) loadSavedProducts(); if (tab.id === 'rezepte' && !recipesLoaded) loadSavedRecipes(); }}
			>
				{tab.label}
			</button>
		{/each}
	</div>

	<!-- Tab: Listen -->
	{#if activeTab === 'listen'}
		{#if isLoading}
			<div class="loading-wrap">
				<div class="spinner"></div>
			</div>
		{:else if lists.length === 0}
			<div class="empty-state">
				<div class="empty-icon">
					<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"
						viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
						<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
					</svg>
				</div>
				<p class="empty-title">Keine gespeicherten Listen</p>
				<p class="empty-sub">Veganisiere eine Einkaufsliste und speichere sie, um sie hier zu sehen.</p>
			</div>
		{:else}
			<div class="list-cards">
				{#each lists as list (list.id)}
					<div class="card" role="button" tabindex="0"
						onclick={() => { if (pendingDeleteId !== list.id) loadListIntoStore(list); }}
						onkeydown={(e) => { if (e.key === 'Enter') loadListIntoStore(list); }}>

						<div class="card-main">
							<div class="card-info">
								<p class="card-name">{list.name}</p>
								<div class="card-meta">
									<span class="card-supermarket">{list.supermarket}</span>
									<span class="card-dot">·</span>
									<span class="card-date">{formatDate(list.createdAt)}</span>
								</div>
							</div>
							<div class="card-right">
								<p class="card-count">{(list.items || []).length} Produkte</p>
								{#if pendingDeleteId === list.id}
									<div class="delete-confirm" role="presentation" onclick={(e) => e.stopPropagation()}>
										<span class="delete-label">Löschen?</span>
										<button class="btn-confirm" onclick={() => confirmDelete(list.id)}>Ja</button>
										<button class="btn-cancel" onclick={() => { pendingDeleteId = null; }}>Nein</button>
									</div>
								{:else}
									<button class="trash-btn" aria-label="Liste löschen"
										onclick={(e) => { e.stopPropagation(); pendingDeleteId = list.id; }}>
										<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
											viewBox="0 0 24 24" fill="none" stroke="currentColor"
											stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
											<polyline points="3 6 5 6 21 6"/>
											<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
											<path d="M10 11v6"/><path d="M14 11v6"/>
											<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
										</svg>
									</button>
								{/if}
							</div>
						</div>
					</div>
				{/each}
			</div>
		{/if}

	<!-- Tab: Produkte -->
	{:else if activeTab === 'produkte'}
		{#if !productsLoaded}
			<div class="loading-wrap">
				<div class="spinner"></div>
			</div>
		{:else if savedProducts.length === 0}
			<div class="empty-state">
				<div class="empty-icon">
					<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"
						viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
						<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
					</svg>
				</div>
				<p class="empty-title">Keine gespeicherten Produkte</p>
				<p class="empty-sub">Tippe auf das Lesezeichen-Symbol auf einer Produktdetailseite, um Produkte zu speichern.</p>
			</div>
		{:else}
			<div class="prod-cards">
				{#each savedProducts as p (p.id)}
					<div class="prod-card" role="button" tabindex="0"
						onclick={() => { if (pendingDeleteProductId !== p.id) goto(`/products/${p.productId}?type=${p.type}&from=saved`); }}
						onkeydown={(e) => { if (e.key === 'Enter') goto(`/products/${p.productId}?type=${p.type}&from=saved`); }}>
						<div class="prod-card-inner">
							<div class="prod-img-wrapper">
								{#if !prodImgErrors[p.id] && p.imageUrl}
									<img src={p.imageUrl} alt={p.name} class="prod-img"
										onerror={() => { prodImgErrors = { ...prodImgErrors, [p.id]: true }; }} />
								{:else}
									<div class="prod-img-placeholder"></div>
								{/if}
							</div>
							<div class="prod-info">
								<p class="prod-name">{p.name}</p>
								{#if p.brand}
									<p class="prod-brand">{p.brand}</p>
								{/if}
								<div class="prod-meta">
									<span class="prod-supermarket">{p.supermarket}</span>
									{#if p.price != null}
										<span class="card-dot">·</span>
										<span class="prod-price">CHF {p.price.toFixed(2)}</span>
									{/if}
								</div>
							</div>
							<div class="prod-right">
								{#if pendingDeleteProductId === p.id}
									<div class="delete-confirm" role="presentation" onclick={(e) => e.stopPropagation()}>
										<span class="delete-label">Löschen?</span>
										<button class="btn-confirm" onclick={() => confirmDeleteProduct(p.id)}>Ja</button>
										<button class="btn-cancel" onclick={() => { pendingDeleteProductId = null; }}>Nein</button>
									</div>
								{:else}
									<button class="trash-btn" aria-label="Favorit entfernen"
										onclick={(e) => { e.stopPropagation(); pendingDeleteProductId = p.id; }}>
										<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
											viewBox="0 0 24 24" fill="none" stroke="currentColor"
											stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
											<polyline points="3 6 5 6 21 6"/>
											<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
											<path d="M10 11v6"/><path d="M14 11v6"/>
											<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
										</svg>
									</button>
								{/if}
							</div>
						</div>
					</div>
				{/each}
			</div>
		{/if}

	<!-- Tab: Rezepte -->
	{:else}
		{#if !recipesLoaded}
			<div class="loading-wrap">
				<div class="spinner"></div>
			</div>
		{:else if savedRecipes.length === 0}
			<div class="empty-state">
				<div class="empty-icon">
					<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"
						viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
						<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
						<path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
					</svg>
				</div>
				<p class="empty-title">Noch keine Rezepte gespeichert</p>
				<p class="empty-sub">Speichere Rezepte auf der Rezept-Detailseite, um sie hier zu finden.</p>
			</div>
		{:else}
			<div class="recipe-cards">
				{#each savedRecipes as recipe (recipe.id)}
					<div
						class="recipe-card"
						role="button"
						tabindex="0"
						onclick={() => { if (pendingDeleteRecipeId !== recipe.id) goto('/rezepte/' + recipe.id); }}
						onkeydown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && pendingDeleteRecipeId !== recipe.id) goto('/rezepte/' + recipe.id); }}
					>
						{#if recipe.imageUrl}
							<img src={recipe.imageUrl} alt={recipe.title} class="recipe-card-img" />
						{:else}
							<div class="recipe-card-img-placeholder"></div>
						{/if}
						<div class="recipe-card-info">
							<span class="recipe-card-title">{recipe.title}</span>
							{#if recipe.prepTimeMin}
								<span class="recipe-card-time">{recipe.prepTimeMin} Min</span>
							{/if}
						</div>
						{#if pendingDeleteRecipeId === recipe.id}
							<div class="delete-confirm" role="presentation" onclick={(e) => e.stopPropagation()} onkeydown={(e) => e.stopPropagation()}>
								<span class="delete-label">Löschen?</span>
								<button class="btn-confirm" onclick={() => confirmDeleteRecipe(recipe.id)}>Ja</button>
								<button class="btn-cancel" onclick={() => { pendingDeleteRecipeId = null; }}>Nein</button>
							</div>
						{:else}
							<button class="trash-btn" aria-label="Rezept entfernen"
								onclick={(e) => { e.stopPropagation(); pendingDeleteRecipeId = recipe.id; }}>
								<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
									<polyline points="3 6 5 6 21 6"/>
									<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
									<path d="M10 11v6"/><path d="M14 11v6"/>
									<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
								</svg>
							</button>
						{/if}
					</div>
				{/each}
			</div>
		{/if}
	{/if}
</div>

<style>
	.page {
		padding: 24px 16px 100px;
		min-height: 100dvh;
	}

	/* ── Header ───────────────────────────────────────────────────────────── */
	.header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20px;
	}

	h1 {
		font-size: 1.375rem;
		font-weight: 700;
		color: var(--color-text);
		text-align: center;
		flex: 1;
	}

	.header-spacer {
		width: 32px;
	}

	.icon-btn {
		color: var(--color-primary);
		padding: 4px;
		border-radius: 8px;
		display: flex;
		align-items: center;
	}

	/* ── Tabs ─────────────────────────────────────────────────────────────── */
	.tab-bar {
		display: flex;
		gap: 4px;
		background: var(--color-primary-light);
		border-radius: 12px;
		padding: 4px;
		margin-bottom: 20px;
	}

	.tab-btn {
		flex: 1;
		padding: 8px 4px;
		border-radius: 9px;
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text-light);
		background: transparent;
		transition: background 0.15s ease, color 0.15s ease;
		text-align: center;
	}

	.tab-active {
		background: var(--color-bg);
		color: var(--color-primary);
		font-weight: 700;
		box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
	}

	/* ── Loading ──────────────────────────────────────────────────────────── */
	.loading-wrap {
		display: flex;
		justify-content: center;
		padding-top: 60px;
	}

	.spinner {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		border: 3px solid var(--color-primary-light);
		border-top-color: var(--color-primary);
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	/* ── Empty state ──────────────────────────────────────────────────────── */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		text-align: center;
		padding: 60px 24px;
		gap: 10px;
	}

	.empty-icon {
		color: #d1d5db;
		margin-bottom: 6px;
	}

	.empty-title {
		font-size: 1.0625rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.empty-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		max-width: 260px;
	}

	/* ── List cards ───────────────────────────────────────────────────────── */
	.list-cards {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.card {
		background: #ffffff;
		border-radius: 16px;
		padding: 16px;
		box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
		cursor: pointer;
		transition: box-shadow 0.15s ease;
	}

	.card:active {
		box-shadow: 0 1px 6px rgba(0, 0, 0, 0.1);
	}

	.card-main {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12px;
	}

	.card-info {
		display: flex;
		flex-direction: column;
		gap: 4px;
		min-width: 0;
		flex: 1;
	}

	.card-name {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.card-meta {
		display: flex;
		align-items: center;
		gap: 5px;
	}

	.card-supermarket {
		font-size: 0.6875rem;
		font-weight: 700;
		color: var(--color-primary);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.card-dot {
		font-size: 0.6875rem;
		color: var(--color-text-light);
	}

	.card-date {
		font-size: 0.6875rem;
		color: var(--color-text-light);
	}

	.card-right {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 8px;
		flex-shrink: 0;
	}

	.card-count {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		white-space: nowrap;
	}

	.trash-btn {
		color: #d1d5db;
		display: flex;
		align-items: center;
		padding: 2px;
		transition: color 0.12s ease;
	}

	.trash-btn:active {
		color: #ef4444;
	}

	/* ── Delete confirm ───────────────────────────────────────────────────── */
	.delete-confirm {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.delete-label {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		white-space: nowrap;
	}

	.btn-confirm {
		padding: 4px 10px;
		border-radius: 6px;
		background: #ef4444;
		color: #fff;
		font-size: 0.8125rem;
		font-weight: 600;
	}

	.btn-cancel {
		padding: 4px 10px;
		border-radius: 6px;
		background: var(--color-border);
		color: var(--color-text);
		font-size: 0.8125rem;
		font-weight: 600;
	}

	/* ── Product cards (Favoriten) ────────────────────────────────────────── */
	.prod-cards {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.prod-card {
		background: #ffffff;
		border-radius: 16px;
		padding: 12px 16px;
		box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
		cursor: pointer;
	}

	.prod-card-inner {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	.prod-img-wrapper {
		width: 60px;
		height: 60px;
		flex-shrink: 0;
		border-radius: 10px;
		overflow: hidden;
		background: #F9FAFB;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.prod-img {
		width: 100%;
		height: 100%;
		object-fit: contain;
	}

	.prod-img-placeholder {
		width: 100%;
		height: 100%;
		background: #F3F4F6;
	}

	.prod-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.prod-name {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.prod-brand {
		font-size: 0.8125rem;
		color: var(--color-text-light);
	}

	.prod-meta {
		display: flex;
		align-items: center;
		gap: 5px;
		margin-top: 2px;
	}

	.prod-supermarket {
		font-size: 0.6875rem;
		font-weight: 700;
		color: var(--color-primary);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.prod-price {
		font-size: 0.6875rem;
		color: var(--color-text-light);
	}

	.prod-right {
		flex-shrink: 0;
		display: flex;
		align-items: center;
	}

	/* ── Recipe cards (Gespeicherte Rezepte) ──────────────────────────────── */
	.recipe-cards {
		display: flex;
		flex-direction: column;
	}

	.recipe-card {
		display: flex;
		align-items: center;
		gap: 14px;
		width: 100%;
		padding: 14px 0;
		border: none;
		border-bottom: 1px solid var(--color-border);
		background: transparent;
		cursor: pointer;
		text-align: left;
	}

	.recipe-card:last-child {
		border-bottom: none;
	}

	.recipe-card-img {
		width: 90px;
		height: 70px;
		border-radius: 12px;
		object-fit: cover;
		flex-shrink: 0;
	}

	.recipe-card-img-placeholder {
		width: 90px;
		height: 70px;
		border-radius: 12px;
		background: var(--color-border);
		flex-shrink: 0;
	}

	.recipe-card-info {
		display: flex;
		flex-direction: column;
		gap: 6px;
		flex: 1;
		min-width: 0;
	}

	.recipe-card-title {
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
		line-height: 1.35;
	}

	.recipe-card-time {
		font-size: 0.75rem;
		color: var(--color-text-light);
	}
</style>
