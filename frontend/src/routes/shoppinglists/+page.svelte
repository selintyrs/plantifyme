<script>
	import { goto } from '$app/navigation';
	import {
		matchResults,
		isVeganized,
		savedListId,
		itemIdMap,
		resetForLoad,
	} from '$lib/stores/shoppingList.js';
	import { supermarketStore } from '$lib/stores/supermarket.js';

	let lists = $state([]);
	let isLoading = $state(true);
	let activeTab = $state('listen');
	let pendingDeleteId = $state(null);

	const tabs = [
		{ id: 'listen',   label: 'Listen' },
		{ id: 'produkte', label: 'Produkte' },
		{ id: 'rezepte',  label: 'Rezepte' },
	];

	$effect(() => {
		loadLists();
	});

	async function loadLists() {
		isLoading = true;
		try {
			const res = await fetch('/api/shoppinglists?userId=temp-user');
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

	async function confirmDelete(listId) {
		try {
			const res = await fetch(`/api/shoppinglists/${listId}`, {
				method: 'DELETE',
				headers: { 'X-User-Id': 'temp-user' },
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			lists = lists.filter((l) => l.id !== listId);
		} catch (e) {
			console.error('Liste löschen fehlgeschlagen:', e);
		} finally {
			pendingDeleteId = null;
		}
	}

	function loadListIntoStore(list) {
		const items = list.items || [];

		const storeItems = items.map((item, index) => ({
			id: index + 1,
			displayName: item.displayName || item.name,
			type: item.type,
			subcategory: item.subcategory || '',
			quantity: item.quantity,
			frontendDictionaryId: null,
			uiCategory: 'Sonstiges',
		}));

		const matches = items.map((item, index) => ({
			itemId: (index + 1).toString(),
			type: item.type,
			displayName: item.displayName || item.name,
			uiCategory: 'Sonstiges',
			suggestedProduct: item.type !== 'C' && item.productId
				? { id: item.productId, name: item.name, price: item.price }
				: null,
			genericFood: item.type === 'C' && item.genericFoodId
				? { id: item.genericFoodId }
				: null,
		}));

		const map = {};
		items.forEach((item, index) => {
			map[(index + 1).toString()] = item.itemId;
		});

		resetForLoad(storeItems);
		matchResults.set(matches);
		isVeganized.set(true);
		supermarketStore.set(list.supermarket);
		savedListId.set(list.id);
		itemIdMap.set(map);

		goto('/liste');
	}

	function formatDate(dateStr) {
		if (!dateStr) return '';
		const d = new Date(dateStr);
		return `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}.${d.getFullYear()}`;
	}
</script>

<svelte:head>
	<title>Gespeicherte Listen – Plantify</title>
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
				onclick={() => { activeTab = tab.id; }}
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

	<!-- Tab: Produkte / Rezepte -->
	{:else}
		<div class="placeholder">
			<p class="placeholder-text">Bald verfügbar</p>
		</div>
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

	/* ── Placeholder ──────────────────────────────────────────────────────── */
	.placeholder {
		display: flex;
		align-items: center;
		justify-content: center;
		padding-top: 80px;
	}

	.placeholder-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}
</style>
