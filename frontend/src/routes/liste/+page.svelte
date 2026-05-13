<script>
	import {
		dictionaryStore,
		searchDictionary,
		groupByCategory,
		loadDictionary,
	} from "$lib/stores/dictionary.js";
	import {
		shoppingListStore,
		addItem,
		removeItem,
		updateQuantity,
		isVeganized,
		matchResults,
		savedListId,
		itemIdMap,
		resetAll,
	} from "$lib/stores/shoppingList.js";
	import { supermarketStore } from "$lib/stores/supermarket.js";
	import { recentProducts, addRecentProduct } from "$lib/stores/recentProducts.js";
	import { goto } from "$app/navigation";
	import { onMount } from "svelte";
	import { authFetch } from "$lib/api.js";
	import { currentUser } from "$lib/stores/user.js";

	const ALLERGY_LABELS = {
		nuts: 'Nüsse',
		soy: 'Soja',
		gluten: 'Gluten',
		sesame: 'Sesam',
		mustard: 'Senf',
		celery: 'Sellerie',
		lupins: 'Lupinen',
	};

	const DIET_MAP = {
		highProtein: 'high protein',
		lowCarb: 'low carb',
		glutenFree: 'gluten free',
		lowCalorie: 'low calorie',
	};

	function buildPreferences() {
		const user = $currentUser;
		const allergies = (user?.allergies ?? [])
			.map(k => ALLERGY_LABELS[k])
			.filter(Boolean);
		const diet = (user?.dietaryPreferences ?? [])
			.map(k => DIET_MAP[k])
			.filter(Boolean);
		return { allergies, budget: 'medium', diet };
	}

	// ── Core state ────────────────────────────────────────────────────────────
	let isLoading = $state(false);

	// ── Save list modal ───────────────────────────────────────────────────────
	let saveModalOpen = $state(false);
	let saveListName = $state('');
	let isSaved = $state(false);
	let currentListName = $state('');
	let listNameEditing = $state(false);
	let listNameDraft = $state('');
	let pendingListDelete = $state(false);
	let saveToastVisible = $state(false);
	let saveToastMessage = $state('Liste gespeichert');
	let newListModalOpen = $state(false);
	let clearAfterSave = $state(false);

	// ── Modal state ───────────────────────────────────────────────────────────
	let modalOpen = $state(false);
	let query = $state("");
	let modalTab = $state('kategorien');
	let selectedCategory = $state(null);

	// ── Supermarkt Modal ──────────────────────────────────────────────────────
	let supermarketModalOpen = $state(false);
	let supermarketDraft = $state("Migros");
	const supermarkets = ["Migros", "Coop", "Aldi"];

	// ── Long-press / subcategory modal ────────────────────────────────────────
	let subcatModal = $state(false);
	let subcatEntry = $state(null);
	let subcatSelected = $state("");
	let longPressTimer = null;

	// ── Delete confirmation ───────────────────────────────────────────────────
	let pendingDeleteId = $state(null);

	// ── Derived ──────────────────────────────────────────────────────────────
	let searchResults = $derived(searchDictionary(query, $dictionaryStore));
	let grouped = $derived(groupByCategory(searchResults));

	let listGrouped = $derived(
		$shoppingListStore.reduce((acc, item) => {
			if (!acc[item.uiCategory]) acc[item.uiCategory] = [];
			acc[item.uiCategory].push(item);
			return acc;
		}, {}),
	);

	let matchGrouped = $derived(
		$matchResults.reduce((acc, r) => {
			const cat = r.uiCategory ?? "Sonstiges";
			if (!acc[cat]) acc[cat] = [];
			acc[cat].push(r);
			return acc;
		}, {}),
	);

	let impactData = $derived(computeImpact($matchResults, $shoppingListStore));

	function computeImpact(matchResults, shoppingList) {
		const animals = { cow: 0, pig: 0, chicken: 0, fish: 0 };
		let totalCo2 = 0, co2PctSum = 0, co2Count = 0;
		let totalWater = 0, waterPctSum = 0, waterCount = 0;
		let totalLand = 0, landPctSum = 0, landCount = 0;

		for (const r of matchResults) {
			if (r.type === 'C' || !r.suggestedProduct) continue;
			const p = r.suggestedProduct;
			const qty = shoppingList.find(i => i.id.toString() === r.itemId)?.quantity ?? 1;

			if (p.animalType && p.animalsAffectedPerUnit != null) {
				animals[p.animalType] = (animals[p.animalType] ?? 0) + p.animalsAffectedPerUnit * qty;
			}
			if (p.impactCo2SavedKg != null) {
				totalCo2 += p.impactCo2SavedKg * qty;
				if (p.impactCo2SavedPct != null) { co2PctSum += p.impactCo2SavedPct; co2Count++; }
			}
			if (p.impactWaterSavedL != null) {
				totalWater += p.impactWaterSavedL * qty;
				if (p.impactWaterSavedPct != null) { waterPctSum += p.impactWaterSavedPct; waterCount++; }
			}
			if (p.impactLandSavedM2 != null) {
				totalLand += p.impactLandSavedM2 * qty;
				if (p.impactLandSavedPct != null) { landPctSum += p.impactLandSavedPct; landCount++; }
			}
		}

		let co2Compare;
		if (totalCo2 > 20) {
			co2Compare = `Das entspricht ${(totalCo2 / 0.2630).toFixed(1)} km Flug`;
		} else {
			const co2Km = (totalCo2 / 0.1873).toFixed(1);
			co2Compare = `Das entspricht ${co2Km} km Autofahrt`;
		}

		let waterCompare;
		const waterBadewannen = (totalWater / 200).toFixed(1);
		if (totalWater >= 60 && waterBadewannen !== '0.0') {
			waterCompare = `Das entspricht ${waterBadewannen} vollen Badewannen`;
		} else {
			waterCompare = `Das entspricht ${(totalWater / 12).toFixed(1)} Minuten duschen`;
		}

		let landCompare;
		if (totalLand > 500) {
			const fussball = (totalLand / 7140).toFixed(1);
			if (fussball !== '0.0') {
				landCompare = `Das entspricht ${fussball} Fussballfeldern`;
			} else {
				landCompare = `Das entspricht ${(totalLand / 195.7).toFixed(1)} Tennisplätzen`;
			}
		} else if (totalLand >= 25) {
			const tennis = (totalLand / 195.7).toFixed(1);
			if (tennis !== '0.0') {
				landCompare = `Das entspricht ${tennis} Tennisplätzen`;
			} else {
				landCompare = `Das entspricht ${(totalLand / 12.5).toFixed(1)} Parkplätzen`;
			}
		} else {
			landCompare = `Das entspricht ${(totalLand / 12.5).toFixed(1)} Parkplätzen`;
		}

		return {
			animals: {
				cow: Math.ceil(animals.cow),
				pig: Math.ceil(animals.pig),
				chicken: Math.ceil(animals.chicken),
				fish: Math.ceil(animals.fish),
			},
			co2:   { total: totalCo2,   pct: co2Count   ? Math.round(co2PctSum   / co2Count)   : 0, compare: co2Compare },
			water: { total: totalWater, pct: waterCount ? Math.round(waterPctSum / waterCount) : 0, compare: waterCompare },
			land:  { total: totalLand,  pct: landCount  ? Math.round(landPctSum  / landCount)  : 0, compare: landCompare },
		};
	}

	function isAdded(entry) {
		return $shoppingListStore.some(
			(i) => i.frontendDictionaryId === entry.id,
		);
	}

	// ── Plantify Match API ────────────────────────────────────────────────────
	function buildRequestItems(items) {
		return items.map((item) => ({
			itemId: item.id.toString(),
			displayName: item.displayName,
			subcategory: item.subcategory || "",
			type: item.type,
			frontendDictionaryId: item.frontendDictionaryId,
		}));
	}

	onMount(() => {
		const params = new URLSearchParams(window.location.search);
		if (params.get('reload') === '1' && $shoppingListStore.length > 0) {
			plantifyMatch();
			// Note: deliberately NOT calling history.replaceState here.
			// Doing so corrupts the back-stack: a user navigating
			// /shoppinglists → /liste?reload=1 → /products/{id} would, after
			// pressing back, skip /liste and land on /shoppinglists instead.
			// The ?reload=1 param staying in the URL is harmless — plantifyMatch
			// is idempotent on re-mount (e.g. F5).
		}
	});

	$effect(() => {
		const id = $savedListId;
		if (id && !currentListName) {
			authFetch(`/api/shoppinglists/${id}`)
				.then(r => r.ok ? r.json() : null)
				.then(data => { if (data?.name) currentListName = data.name; })
				.catch(() => {});
		} else if (!id) {
			currentListName = '';
		}
	});

	async function plantifyMatch() {
		isLoading = true;
		supermarketModalOpen = false;
		try {
			const res = await authFetch("/api/plantify-match", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({
					items: buildRequestItems($shoppingListStore),
					supermarket: $supermarketStore === 'Coop' ? 'coop' : $supermarketStore === 'Aldi' ? 'aldi' : $supermarketStore,
					preferences: buildPreferences(),
				}),
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			const data = await res.json();
			const raw = data.matches || [];
			matchResults.set(raw.map(r => {
				const original = $shoppingListStore.find(i => i.id.toString() === r.itemId);
				return {
					...r,
					displayName: original?.displayName || r.originalName,
					uiCategory: original?.uiCategory || 'Sonstiges'
				};
			}));
			isVeganized.set(true);
			isSaved = false;
		} catch (e) {
			console.error("Plantify match failed:", e);
		} finally {
			isLoading = false;
		}
	}

	async function matchSingleItem(item) {
		try {
			const res = await authFetch("/api/plantify-match", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({
					items: buildRequestItems([item]),
					supermarket: $supermarketStore === 'Coop' ? 'coop' : $supermarketStore === 'Aldi' ? 'aldi' : $supermarketStore,
					preferences: buildPreferences(),
				}),
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			const data = await res.json();
			const matches = data.matches || [];
if (matches.length > 0) {
    const enriched = matches.map(r => {
        const original = $shoppingListStore.find(i => i.id.toString() === r.itemId);
        return {
            ...r,
            displayName: original?.displayName || r.originalName,
            uiCategory: original?.uiCategory || 'Sonstiges'
        };
    });
    matchResults.update(prev => [...prev, ...enriched]);
}
		} catch (e) {
			console.error("Single item match failed:", e);
		}
	}

	// ── Supermarkt Modal ──────────────────────────────────────────────────────
	function openSupermarketModal() {
		if ($supermarketStore && !$isVeganized) {
			plantifyMatch();
			return;
		}
		supermarketDraft = $supermarketStore;
		supermarketModalOpen = true;
	}

	async function changeSupermarket(newSupermarket) {
		supermarketStore.set(newSupermarket);
		supermarketModalOpen = false;

		if (!$savedListId) {
			await plantifyMatch();
			return;
		}

		isLoading = true;
		try {
			const res = await authFetch(`/api/shoppinglists/${$savedListId}/supermarket`, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ supermarket: newSupermarket }),
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			// Backend has persisted the supermarket change; refresh match data
			// via plantifyMatch() so suggestedProduct contains the full product
			// payload (impact fields, brand, imageUrl) — not just {id, name, price}.
			await plantifyMatch();
		} catch (e) {
			console.error('Supermarkt wechseln fehlgeschlagen:', e);
			await plantifyMatch();
		} finally {
			isLoading = false;
		}
	}

	function confirmSupermarket() {
		changeSupermarket(supermarketDraft);
	}

	async function syncItemToBackend(listId, entry, subcategory, quantity) {
		try {
			await authFetch(`/api/shoppinglists/${listId}/items`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({
					displayName: entry.displayName,
					subcategory: subcategory || '',
					type: entry.type,
					frontendDictionaryId: entry.id,
					quantity,
				}),
			});
		} catch (e) {
			console.error('Item sync fehlgeschlagen:', e);
			savedListId.set(null);
		}
	}

	// ── Add-Product Modal ─────────────────────────────────────────────────────
	async function openModal() {
		modalOpen = true;
		query = "";
		modalTab = 'kategorien';
		selectedCategory = null;
		if ($dictionaryStore.length === 0) {
			await loadDictionary();
		}
	}

	function closeModal() {
		modalOpen = false;
		query = "";
		selectedCategory = null;
	}

	async function handleEntryTap(entry) {
		isSaved = false;
		if (isAdded(entry)) {
			const items = $shoppingListStore.filter(
				(i) => i.frontendDictionaryId === entry.id,
			);
			items.forEach((i) => {
				removeItem(i.id);
				if ($isVeganized) {
					matchResults.update(prev => prev.filter(r => r.itemId !== i.id.toString()));
				}
			});
		} else {
			addRecentProduct(entry);
			const listIdToSync = $savedListId;
			addItem(entry, "");
			if ($isVeganized) {
				const added = $shoppingListStore.find(
					(i) => i.frontendDictionaryId === entry.id,
				);
				if (added) await matchSingleItem(added);
			}
			if (listIdToSync) {
				await syncItemToBackend(listIdToSync, entry, '', 1);
			}
		}
	}

	// ── Long press ────────────────────────────────────────────────────────────
	function startLongPress(entry) {
		if (!entry.additionalOptions || entry.additionalOptions.length === 0)
			return;
		longPressTimer = setTimeout(() => {
			subcatEntry = entry;
			subcatSelected = "";
			subcatModal = true;
		}, 500);
	}

	function cancelLongPress() {
		clearTimeout(longPressTimer);
	}

	async function confirmSubcat() {
		isSaved = false;
		if (subcatEntry) {
			addRecentProduct(subcatEntry);
			const listIdToSync = $savedListId;
			const entrySnapshot = subcatEntry;
			const subcatSnapshot = subcatSelected;
			addItem(subcatEntry, subcatSelected);
			if ($isVeganized) {
				const added = $shoppingListStore.find(
					(i) =>
						i.frontendDictionaryId === subcatEntry.id &&
						i.subcategory === subcatSelected,
				);
				if (added) await matchSingleItem(added);
			}
			if (listIdToSync) {
				await syncItemToBackend(listIdToSync, entrySnapshot, subcatSnapshot, 1);
			}
		}
		subcatModal = false;
		subcatEntry = null;
		subcatSelected = "";
	}

	// ── Quantity / delete ─────────────────────────────────────────────────────
	function handleMinus(item) {
		if (item.quantity === 1) {
			pendingDeleteId = item.id;
		} else {
			isSaved = false;
			updateQuantity(item.id, -1);
		}
	}

	function confirmDelete(id) {
		removeItem(id);
		if ($isVeganized) {
			matchResults.update(prev => prev.filter(r => r.itemId !== id.toString()));
		}
		pendingDeleteId = null;
		isSaved = false;
	}

	function cancelDelete() {
		pendingDeleteId = null;
	}

	// ── Quantity on match results ─────────────────────────────────────────────
	function matchQuantityFor(itemId) {
		const item = $shoppingListStore.find((i) => i.id.toString() === itemId);
		return item ? item.quantity : 1;
	}

	function updateMatchQuantity(itemId, delta) {
		const id = parseInt(itemId);
		const item = $shoppingListStore.find((i) => i.id === id);
		if (!item) return;
		if (item.quantity + delta < 1) {
			pendingDeleteId = id;
		} else {
			isSaved = false;
			updateQuantity(id, delta);
		}
	}

	// ── Save list ─────────────────────────────────────────────────────────────
	function focusOnMount(node) {
		node.focus();
	}

	function clearList() {
		// Nur den lokalen Store leeren. DB-Listen bleiben unverändert —
		// der User hat sie ja absichtlich gespeichert.
		resetAll();
		isSaved = false;
		currentListName = '';
	}

	function startEditName() {
		listNameDraft = currentListName;
		listNameEditing = true;
	}

	function cancelEditName() {
		listNameEditing = false;
		listNameDraft = '';
	}

	async function commitListName() {
		const newName = listNameDraft.trim();
		if (!newName || newName === currentListName) {
			listNameEditing = false;
			return;
		}

		if ($savedListId) {
			try {
				const res = await authFetch(`/api/shoppinglists/${$savedListId}`, {
					method: 'PUT',
					body: JSON.stringify({
						name: newName,
						supermarket: $supermarketStore,
						items: buildSaveItems(),
					}),
				});
				if (res.ok) {
					currentListName = newName;
					isSaved = true;
					saveToastMessage = 'Name aktualisiert';
					saveToastVisible = true;
					setTimeout(() => { saveToastVisible = false; }, 1500);
				}
			} catch (e) {
				console.error('Name speichern fehlgeschlagen:', e);
			}
		}
		listNameEditing = false;
	}

	async function confirmListDelete() {
		if (!$savedListId) return;
		try {
			const res = await authFetch(`/api/shoppinglists/${$savedListId}`, {
				method: 'DELETE',
			});
			if (res.ok) {
				resetAll();
				currentListName = '';
				pendingListDelete = false;
				isSaved = false;
			}
		} catch (e) {
			console.error('Liste löschen fehlgeschlagen:', e);
		}
	}

	async function handleSaveClick() {
		if ($savedListId) {
			saveListName = currentListName;
			await saveList();
		} else {
			openSaveModal();
		}
	}

	function openSaveModal() {
		const now = new Date();
		const dd = String(now.getDate()).padStart(2, '0');
		const mm = String(now.getMonth() + 1).padStart(2, '0');
		const yyyy = now.getFullYear();
		saveListName = `Einkaufsliste vom ${dd}.${mm}.${yyyy}`;
		saveModalOpen = true;
	}

	function buildSaveItems() {
		return $matchResults.map(r => {
			const listItem = $shoppingListStore.find(i => i.id.toString() === r.itemId);
			const qty = listItem?.quantity ?? 1;
			// Type stays the original from the FrontendDictionary (via shoppingList).
			// Match status is expressed orthogonally via productId / genericFoodId.
			const originalType = listItem?.type ?? r.type;
			return {
				itemId: crypto.randomUUID(),
				productId: r.suggestedProduct ? r.suggestedProduct.id : null,
				genericFoodId: r.genericFood ? r.genericFood.id : null,
				type: originalType,
				displayName: r.displayName ?? '',
				subcategory: listItem?.subcategory ?? '',
				name: r.suggestedProduct?.name ?? r.genericFood?.name ?? r.displayName,
				price: r.suggestedProduct?.price ?? null,
				quantity: qty,
				isChecked: false,
			};
		});
	}

	async function saveList() {
		const wasUpdate = $savedListId !== null;
		const url = wasUpdate ? `/api/shoppinglists/${$savedListId}` : '/api/shoppinglists';
		const method = wasUpdate ? 'PUT' : 'POST';

		try {
			const res = await authFetch(url, {
				method,
				body: JSON.stringify({
					name: saveListName,
					supermarket: $supermarketStore,
					items: buildSaveItems(),
				}),
			});
			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			const saved = await res.json();
			savedListId.set(saved.id);
			currentListName = saved.name ?? saveListName;
			const map = {};
			for (const backendItem of (saved.items || [])) {
				const localMatch = $matchResults.find(r => r.displayName === backendItem.displayName);
				if (localMatch) map[localMatch.itemId] = backendItem.itemId;
			}
			itemIdMap.set(map);
			saveModalOpen = false;
			isSaved = true;
			saveToastMessage = wasUpdate ? 'Liste aktualisiert' : 'Liste gespeichert';
			saveToastVisible = true;
			setTimeout(() => { saveToastVisible = false; }, 1500);
			if (clearAfterSave) { clearList(); clearAfterSave = false; }
		} catch (e) {
			console.error('Liste speichern fehlgeschlagen:', e);
		}
	}
</script>

<!-- ════════════════════════════════════════════════════ LOADING SCREEN ══════ -->
{#if isLoading}
	<div class="loading-screen">
		<div class="spinner"></div>
		<p class="loading-text">Deine Liste wird veganisiert…</p>
	</div>

	<!-- ═══════════════════════════════════════════════════════ MAIN PAGE ══════ -->
{:else}
	<div class="page">
		<!-- Header -->
		<div class="header">
			<div class="header-titles">
				<h1>Einkaufsliste</h1>
				{#if $savedListId && currentListName}
					<div class="list-name-row">
						{#if listNameEditing}
							<input
								class="list-name-input"
								type="text"
								bind:value={listNameDraft}
								onkeydown={(e) => { if (e.key === 'Enter') commitListName(); if (e.key === 'Escape') cancelEditName(); }}
								use:focusOnMount
							/>
							<button class="name-action-btn" onclick={commitListName} aria-label="Namen speichern">
								<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
									<polyline points="20 6 9 17 4 12"/>
								</svg>
							</button>
						{:else}
							<span class="list-name">{currentListName}</span>
							<button class="name-action-btn" onclick={startEditName} aria-label="Namen bearbeiten">
								<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
									<path d="M12 20h9"/>
									<path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
								</svg>
							</button>
						{/if}
					</div>
				{/if}
				{#if $isVeganized}
					<div class="supermarket-badge-row">
						<span class="supermarket-badge">{$supermarketStore}</span>
						<button class="badge-change-btn" onclick={openSupermarketModal} aria-label="Supermarkt ändern">Ändern</button>
					</div>
				{/if}
			</div>
			<div class="header-actions">
				{#if $savedListId}
					{#if pendingListDelete}
						<div class="delete-confirm">
							<span class="delete-label">Löschen?</span>
							<button class="btn-confirm" onclick={confirmListDelete}>Ja</button>
							<button class="btn-cancel" onclick={() => pendingListDelete = false}>Nein</button>
						</div>
					{:else}
						<button class="icon-btn icon-btn-trash" aria-label="Liste löschen" onclick={() => pendingListDelete = true}>
							<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
								<polyline points="3 6 5 6 21 6"/>
								<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
								<path d="M10 11v6"/><path d="M14 11v6"/>
								<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
							</svg>
						</button>
					{/if}
				{/if}
				<button class="icon-btn" aria-label="Gespeicherte Listen" onclick={() => goto('/shoppinglists')}>
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
					</svg>
				</button>
			</div>
		</div>

		<!-- Empty state -->
		{#if $shoppingListStore.length === 0}
			<div class="empty-state">
				<div class="empty-icon">
					<svg
						xmlns="http://www.w3.org/2000/svg"
						width="48"
						height="48"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="1.5"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						<circle cx="8" cy="21" r="1" /><circle
							cx="19"
							cy="21"
							r="1"
						/>
						<path
							d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"
						/>
					</svg>
				</div>
				<p class="empty-title">Deine Liste ist noch leer</p>
				<p class="empty-sub">
					Füge Produkte hinzu oder wähle eine deiner gespeicherten
					Listen.
				</p>
				<div class="empty-tip">
					<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/>
						<path d="M12 16v-4"/>
						<path d="M12 8h.01"/>
					</svg>
					<span>Tipp: Drücke lange auf ein Produkt, um z.B. die Sorte zu spezifizieren.</span>
				</div>
			</div>

			<!-- ── Veganized View ─────────────────────────────────────────────────── -->
		{:else if $isVeganized}
			<div class="item-list">
				{#each Object.entries(matchGrouped) as [category, results]}
					<div class="category-section">
						<div class="category-header">
							<svg
								xmlns="http://www.w3.org/2000/svg"
								width="14"
								height="14"
								viewBox="0 0 24 24"
								fill="none"
								stroke="currentColor"
								stroke-width="2"
								stroke-linecap="round"
								stroke-linejoin="round"
							>
								<path
									d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"
								/>
								<path
									d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"
								/>
							</svg>
							<span>{category}</span>
						</div>

						{#each results as result (result.itemId)}
							{@const qty = matchQuantityFor(result.itemId)}
							{@const itemId = parseInt(result.itemId)}
							<div class="item-row">
								<!-- Checkbox placeholder -->
								<div class="item-left">
									<span class="checkbox-circle"></span>
									<div class="item-info">
										{#if result.type === "C" && result.suggestedProduct}
											<span class="item-name"
												>{result.suggestedProduct
													.name}</span
											>
											<span class="item-plantified"
												>{result.diyGroupLabel || `Plantified ${result.displayName}`}</span
											>
										{:else if result.type === "C"}
											<span class="item-name"
												>{result.displayName}</span
											>
											{#if result.diyGroupLabel}
												<span class="item-plantified">{result.diyGroupLabel}</span>
											{/if}
										{:else if result.suggestedProduct}
											<span class="item-name"
												>{result.suggestedProduct
													.name}</span
											>
											<span class="item-plantified"
												>{result.diyGroupLabel || `Plantified ${result.displayName}`}</span
											>
										{:else if result.genericFood && result.variantNote}
											<span class="item-name">{result.genericFood.name}</span>
											<span class="item-plantified">{result.variantNote}</span>
										{:else if result.fallbackDiyRecipeId}
											<span class="item-name">{result.displayName}</span>
											<span class="item-diy-fallback">
												<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
												</svg>
												{result.displayName === 'Eier'
													? 'Plantfied Eier'
													: `Selbst machen: ${result.fallbackDiyRecipeTitle}`}
											</span>
										{:else}
											<span class="item-name"
												>{result.displayName}</span
											>
											<span class="item-no-match"
												>Keine Alternative gefunden</span
											>
										{/if}
									</div>
								</div>

								<div class="item-right">
									{#if pendingDeleteId === itemId}
										<div class="delete-confirm">
											<span class="delete-label"
												>Löschen?</span
											>
											<button
												class="btn-confirm"
												onclick={() =>
													confirmDelete(itemId)}
												>Ja</button
											>
											<button
												class="btn-cancel"
												onclick={cancelDelete}
												>Nein</button
											>
										</div>
									{:else}
										<div class="quantity-ctrl">
											<button
												class="qty-btn"
												aria-label="Weniger"
												onclick={() =>
													updateMatchQuantity(
														result.itemId,
														-1,
													)}
											>
												{#if qty === 1}
													<svg
														xmlns="http://www.w3.org/2000/svg"
														width="14"
														height="14"
														viewBox="0 0 24 24"
														fill="none"
														stroke="currentColor"
														stroke-width="2"
														stroke-linecap="round"
														stroke-linejoin="round"
														><polyline
															points="3 6 5 6 21 6"
														/><path
															d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"
														/><path
															d="M10 11v6"
														/><path
															d="M14 11v6"
														/><path
															d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"
														/></svg
													>
												{:else}
													<svg
														xmlns="http://www.w3.org/2000/svg"
														width="14"
														height="14"
														viewBox="0 0 24 24"
														fill="none"
														stroke="currentColor"
														stroke-width="2"
														stroke-linecap="round"
														stroke-linejoin="round"
														><line
															x1="5"
															y1="12"
															x2="19"
															y2="12"
														/></svg
													>
												{/if}
											</button>
											<span class="qty-num">{qty}</span>
											<button
												class="qty-btn"
												aria-label="Mehr"
												onclick={() =>
													updateMatchQuantity(
														result.itemId,
														1,
													)}
											>
												<svg
													xmlns="http://www.w3.org/2000/svg"
													width="14"
													height="14"
													viewBox="0 0 24 24"
													fill="none"
													stroke="currentColor"
													stroke-width="2"
													stroke-linecap="round"
													stroke-linejoin="round"
													><line
														x1="12"
														y1="5"
														x2="12"
														y2="19"
													/><line
														x1="5"
														y1="12"
														x2="19"
														y2="12"
													/></svg
												>
											</button>
										</div>
										{#if result.displayName === 'Eier' || result.fallbackDiyRecipeId || result.suggestedProduct || result.genericFood}
										<button
											class="chevron-btn"
											aria-label="Details"
											onclick={async () => {
												try {
													if (result.displayName === 'Eier') {
														goto('/products/eier');
													} else if (result.fallbackDiyRecipeId) {
														goto(`/rezepte/diy/${result.fallbackDiyRecipeId}`);
													} else if (result.suggestedProduct) {
														goto(`/products/${result.suggestedProduct.id}`);
													} else if (result.genericFood) {
														goto(`/products/${result.genericFood.id}?type=C`);
													} else {
														const res = await authFetch('/api/generic-foods/search?name=' + encodeURIComponent(result.displayName));
														if (res.ok) {
															const food = await res.json();
															goto(`/products/${food.id}?type=C`);
														}
													}
												} catch {}
											}}
										>
												<svg
													xmlns="http://www.w3.org/2000/svg"
													width="18"
													height="18"
													viewBox="0 0 24 24"
													fill="none"
													stroke="currentColor"
													stroke-width="2"
													stroke-linecap="round"
													stroke-linejoin="round"
													><polyline
														points="9 18 15 12 9 6"
													/></svg
												>
											</button>
										{/if}
								{/if}
								</div>
							</div>
						{/each}
					</div>
				{/each}
			</div>

			<!-- Bottom action row -->
			<div class="veganized-actions">
				<button
					class="btn-save"
					class:btn-save-done={isSaved}
					onclick={isSaved ? null : handleSaveClick}
					disabled={isSaved}
				>
					<svg
						xmlns="http://www.w3.org/2000/svg"
						width="18"
						height="18"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						{#if isSaved}
							<polyline points="20 6 9 17 4 12"/>
						{:else}
							<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
						{/if}
					</svg>
					{isSaved ? 'Gespeichert' : ($savedListId ? 'Aktualisieren' : 'Liste speichern')}
				</button>
				<button class="btn-new-list" onclick={() => { newListModalOpen = true; }}>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
					Neue Liste
				</button>
				<button
					class="btn-add-round"
					onclick={openModal}
					aria-label="Produkt hinzufügen"
				>
					<svg
						xmlns="http://www.w3.org/2000/svg"
						width="22"
						height="22"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2.5"
						stroke-linecap="round"
						stroke-linejoin="round"
						><line x1="12" y1="5" x2="12" y2="19" /><line
							x1="5"
							y1="12"
							x2="19"
							y2="12"
						/></svg
					>
				</button>
			</div>

			<!-- Impact section -->
			<div class="impact-section">
				<div class="impact-header">
					<h2 class="impact-title">Dein Impact</h2>
					<p class="impact-sub">Wenn du deine plantified Einkaufsliste verwendest</p>
				</div>

				<!-- Tierwohl -->
				<div class="impact-card">
					<p class="impact-card-label">Anzahl Tiere die dank dir nicht leiden müssen</p>
					<div class="animal-row">
						{#if impactData.animals.cow > 0}
							<div class="animal-col">
								<img src="/images/icons/cow.png" alt="Kuh" width="44" height="44" />
								<span class="animal-count">{impactData.animals.cow}</span>
								<span class="animal-label">{impactData.animals.cow === 1 ? 'Kuh' : 'Kühe'}</span>
							</div>
						{/if}
						{#if impactData.animals.pig > 0}
							<div class="animal-col">
								<img src="/images/icons/pig.png" alt="Schwein" width="44" height="44" />
								<span class="animal-count">{impactData.animals.pig}</span>
								<span class="animal-label">{impactData.animals.pig === 1 ? 'Schwein' : 'Schweine'}</span>
							</div>
						{/if}
						{#if impactData.animals.chicken > 0}
							<div class="animal-col">
								<img src="/images/icons/chicken.png" alt="Huhn" width="44" height="44" />
								<span class="animal-count">{impactData.animals.chicken}</span>
								<span class="animal-label">{impactData.animals.chicken === 1 ? 'Huhn' : 'Hühner'}</span>
							</div>
						{/if}
						{#if impactData.animals.fish > 0}
							<div class="animal-col">
								<img src="/images/icons/fish.png" alt="Fisch" width="44" height="44" />
								<span class="animal-count">{impactData.animals.fish}</span>
								<span class="animal-label">{impactData.animals.fish === 1 ? 'Fisch' : 'Fische'}</span>
							</div>
						{/if}
					</div>
				</div>

				<!-- CO₂ -->
				<div class="impact-card impact-card-white">
					<div class="impact-card-inner">
						<div class="impact-icon-wrap">
							<img src="/images/icons/co2.png" alt="CO₂" width="40" height="40" />
						</div>
						<p class="impact-card-text">So viel CO₂ sparst du durch diese Einkaufsliste</p>
					</div>
					<p class="impact-pct" class:impact-pct-negative={impactData.co2.pct < 0} class:impact-pct-neutral={impactData.co2.pct === 0}>{impactData.co2.pct}%</p>
					<p class="impact-compare">{impactData.co2.compare}</p>
				</div>

				<!-- Wasser -->
				<div class="impact-card impact-card-white">
					<div class="impact-card-inner">
						<div class="impact-icon-wrap">
							<img src="/images/icons/water.png" alt="Wasser" width="40" height="40" />
						</div>
						<p class="impact-card-text">So viel Wasser sparst du durch diese Einkaufsliste</p>
					</div>
					<p class="impact-pct" class:impact-pct-negative={impactData.water.pct < 0} class:impact-pct-neutral={impactData.water.pct === 0}>{impactData.water.pct}%</p>
					<p class="impact-compare">{impactData.water.compare}</p>
				</div>

				<!-- Land -->
				<div class="impact-card impact-card-white">
					<div class="impact-card-inner">
						<div class="impact-icon-wrap">
							<img src="/images/icons/land.png" alt="Land" width="40" height="40" />
						</div>
						<p class="impact-card-text">So viel Land sparst du durch diese Einkaufsliste</p>
					</div>
					<p class="impact-pct" class:impact-pct-negative={impactData.land.pct < 0} class:impact-pct-neutral={impactData.land.pct === 0}>{impactData.land.pct}%</p>
					<p class="impact-compare">{impactData.land.compare}</p>
				</div>
			</div>

			<!-- ── Normal (pre-match) View ────────────────────────────────────────── -->
		{:else}
			<div class="item-list">
				{#each Object.entries(listGrouped) as [category, items]}
					<div class="category-section">
						<div class="category-header">
							<svg
								xmlns="http://www.w3.org/2000/svg"
								width="14"
								height="14"
								viewBox="0 0 24 24"
								fill="none"
								stroke="currentColor"
								stroke-width="2"
								stroke-linecap="round"
								stroke-linejoin="round"
							>
								<path
									d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"
								/>
								<path
									d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"
								/>
							</svg>
							<span>{category}</span>
						</div>

						{#each items as item (item.id)}
							<div class="item-row">
								<div class="item-info">
									<span class="item-name"
										>{item.displayName}</span
									>
									{#if item.subcategory}
										<span class="item-subcat"
											>{item.subcategory}</span
										>
									{/if}
								</div>

								{#if pendingDeleteId === item.id}
									<div class="delete-confirm">
										<span class="delete-label"
											>Löschen?</span
										>
										<button
											class="btn-confirm"
											onclick={() =>
												confirmDelete(item.id)}
											>Ja</button
										>
										<button
											class="btn-cancel"
											onclick={cancelDelete}>Nein</button
										>
									</div>
								{:else}
									<div class="quantity-ctrl">
										<button
											class="qty-btn"
											aria-label="Weniger"
											onclick={() => handleMinus(item)}
										>
											{#if item.quantity === 1}
												<svg
													xmlns="http://www.w3.org/2000/svg"
													width="14"
													height="14"
													viewBox="0 0 24 24"
													fill="none"
													stroke="currentColor"
													stroke-width="2"
													stroke-linecap="round"
													stroke-linejoin="round"
													><polyline
														points="3 6 5 6 21 6"
													/><path
														d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"
													/><path d="M10 11v6" /><path
														d="M14 11v6"
													/><path
														d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"
													/></svg
												>
											{:else}
												<svg
													xmlns="http://www.w3.org/2000/svg"
													width="14"
													height="14"
													viewBox="0 0 24 24"
													fill="none"
													stroke="currentColor"
													stroke-width="2"
													stroke-linecap="round"
													stroke-linejoin="round"
													><line
														x1="5"
														y1="12"
														x2="19"
														y2="12"
													/></svg
												>
											{/if}
										</button>
										<span class="qty-num"
											>{item.quantity}</span
										>
										<button
											class="qty-btn"
											aria-label="Mehr"
											onclick={() =>
												updateQuantity(item.id, 1)}
										>
											<svg
												xmlns="http://www.w3.org/2000/svg"
												width="14"
												height="14"
												viewBox="0 0 24 24"
												fill="none"
												stroke="currentColor"
												stroke-width="2"
												stroke-linecap="round"
												stroke-linejoin="round"
												><line
													x1="12"
													y1="5"
													x2="12"
													y2="19"
												/><line
													x1="5"
													y1="12"
													x2="19"
													y2="12"
												/></svg
											>
										</button>
									</div>
								{/if}
							</div>
						{/each}
					</div>
				{/each}
			</div>

			<button class="plantify-btn" onclick={openSupermarketModal}>
				<svg
					xmlns="http://www.w3.org/2000/svg"
					width="20"
					height="20"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"
				>
					<path
						d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"
					/>
					<path
						d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"
					/>
				</svg>
				Plantify-Me
			</button>
		{/if}

		<!-- FAB — only when not veganized -->
		{#if !$isVeganized}
			<button
				class="fab"
				onclick={openModal}
				aria-label="Produkt hinzufügen"
			>
				<svg
					xmlns="http://www.w3.org/2000/svg"
					width="28"
					height="28"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2.5"
					stroke-linecap="round"
					stroke-linejoin="round"
					><line x1="12" y1="5" x2="12" y2="19" /><line
						x1="5"
						y1="12"
						x2="19"
						y2="12"
					/></svg
				>
			</button>
		{/if}
	</div>
{/if}

<!-- ═════════════════════════════════════════════════ ADD-PRODUCT MODAL ══════ -->
{#if modalOpen}
	<div class="backdrop" onclick={closeModal} role="presentation"></div>
	<div
		class="sheet"
		role="dialog"
		aria-modal="true"
		aria-label="Produkt hinzufügen"
	>
		<div class="drag-handle"></div>
		<div class="sheet-header">
			<h2>Produkt hinzufügen</h2>
		</div>

		<div class="sheet-search">
			<input
				type="search"
				placeholder="Suchen…"
				bind:value={query}
				class="search-input"
				autofocus
			/>
		</div>

		{#snippet entryItem(entry)}
			{@const added = isAdded(entry)}
			{@const hasOptions = entry.additionalOptions && entry.additionalOptions.length > 0}
			<button
				class="entry-row"
				class:entry-added={added}
				onclick={() => handleEntryTap(entry)}
				onmousedown={() => startLongPress(entry)}
				onmouseup={cancelLongPress}
				onmouseleave={cancelLongPress}
				ontouchstart={() => startLongPress(entry)}
				ontouchend={cancelLongPress}
			>
				<div class="entry-info">
					<span class="entry-name">{entry.displayName}</span>
					{#if hasOptions}
						<span class="entry-options-hint">optional spezifizieren</span>
					{/if}
				</div>
				<span class="entry-action">
					{#if added}
						<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
					{:else}
						<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
					{/if}
				</span>
			</button>
		{/snippet}

		{#if !query}
			<div class="sheet-tabs">
				<button class="sheet-tab" class:tab-active={modalTab === 'zuletzt'} onclick={() => { modalTab = 'zuletzt'; selectedCategory = null; }}>Zuletzt</button>
				<button class="sheet-tab" class:tab-active={modalTab === 'kategorien'} onclick={() => { modalTab = 'kategorien'; selectedCategory = null; }}>Kategorien</button>
			</div>
		{/if}

		<div class="sheet-body">
			{#if query}
				{#each Object.entries(grouped) as [category, entries]}
					<div class="modal-category">
						<p class="modal-cat-label">{category}</p>
						{#each entries as entry, i (i)}
							{@render entryItem(entry)}
						{/each}
					</div>
				{/each}
			{:else if modalTab === 'kategorien'}
				{#if selectedCategory}
					<div class="cat-breadcrumb">
						<button class="breadcrumb-back" onclick={() => selectedCategory = null}>Kategorien</button>
						<span class="breadcrumb-sep">›</span>
						<span class="breadcrumb-current">{selectedCategory === 'Milchprodukte & Käse' ? 'Milchprodukte & Eier' : selectedCategory}</span>
					</div>
					{#if $dictionaryStore.length === 0}
						<div class="empty-tab">
							<p class="empty-tab-text">Lade Produkte…</p>
						</div>
					{:else}
						{@const filtered = $dictionaryStore.filter(e => e.uiCategory === selectedCategory).sort((a, b) => a.displayName.localeCompare(b.displayName, 'de'))}
						{#if filtered.length === 0}
							<div class="empty-tab">
								<p class="empty-tab-text">Keine Produkte in dieser Kategorie</p>
							</div>
						{:else}
							{#each filtered as entry, i (i)}
								{@render entryItem(entry)}
							{/each}
						{/if}
					{/if}
				{:else}
					<div class="cat-grid">
						<button class="cat-card" onclick={() => selectedCategory = 'Obst & Gemüse'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/><path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/></svg>
							<span class="cat-card-name">Obst & Gemüse</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Brot & Backwaren'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11h18a1 1 0 0 1 1 1v3a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4v-3a1 1 0 0 1 1-1z"/><path d="M3 11a7 7 0 0 1 14 0"/></svg>
							<span class="cat-card-name">Brot & Backwaren</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Milchprodukte & Käse'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>
							<span class="cat-card-name">Milchprodukte & Eier</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Fleisch & Fisch'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M3 2v7c0 1.1.9 2 2 2s2-.9 2-2V2"/><line x1="4" y1="8" x2="4" y2="22"/><line x1="20" y1="5" x2="20" y2="22"/><path d="M20 5a2 2 0 0 0-2 2v3a2 2 0 0 0 4 0V7a2 2 0 0 0-2-2z"/></svg>
							<span class="cat-card-name">Fleisch & Fisch</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Saucen & Gewürze'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3h6"/><path d="M8 3c-2 1-3 3-3 6v10a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9c0-3-1-5-3-6"/><line x1="5" y1="13" x2="19" y2="13"/></svg>
							<span class="cat-card-name">Saucen & Gewürze</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Fertiggerichte'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="2" x2="12" y2="22"/><line x1="2" y1="12" x2="22" y2="12"/><path d="m20 16-4-4 4-4"/><path d="m4 8 4 4-4 4"/><path d="m16 4-4 4-4-4"/><path d="m8 20 4-4 4 4"/></svg>
							<span class="cat-card-name">Fertiggerichte</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Teigwaren & Getreide'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M2 22 16 8"/><path d="M3.47 12.53 5 11l1.53 1.53a3.5 3.5 0 0 1 0 4.94L5 19l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/><path d="M7.47 8.53 9 7l1.53 1.53a3.5 3.5 0 0 1 0 4.94L9 15l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/><path d="M11.47 4.53 13 3l1.53 1.53a3.5 3.5 0 0 1 0 4.94L13 11l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/></svg>
							<span class="cat-card-name">Teigwaren & Getreide</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Müesli & Cerealien'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12h20"/><path d="M5 12v3a4 4 0 0 0 4 4h6a4 4 0 0 0 4-4v-3"/><circle cx="8" cy="9" r="1"/><circle cx="12" cy="7" r="1"/><circle cx="16" cy="9" r="1"/></svg>
							<span class="cat-card-name">Müesli & Cerealien</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Süsses & Snacks'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="8" cy="9" r="1"/><circle cx="15" cy="9" r="1"/><circle cx="10" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></svg>
							<span class="cat-card-name">Süsses & Snacks</span>
						</button>
						<button class="cat-card" onclick={() => selectedCategory = 'Getränke'}>
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8z"/><line x1="6" y1="2" x2="6" y2="4"/><line x1="10" y1="2" x2="10" y2="4"/><line x1="14" y1="2" x2="14" y2="4"/></svg>
							<span class="cat-card-name">Getränke</span>
						</button>
					</div>
				{/if}
			{:else if modalTab === 'zuletzt'}
				{#if $recentProducts.length === 0}
					<div class="empty-tab">
						<p class="empty-tab-text">Noch keine kürzlich verwendeten Produkte</p>
					</div>
				{:else}
					{#each $recentProducts as entry, i (entry.id)}
						{@render entryItem(entry)}
					{/each}
				{/if}
			{/if}
		</div>

		<button class="sheet-fab" onclick={closeModal} aria-label="Schliessen">
			{#if $shoppingListStore.length > 0}
				<svg
					xmlns="http://www.w3.org/2000/svg"
					width="26"
					height="26"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2.5"
					stroke-linecap="round"
					stroke-linejoin="round"
					><polyline points="20 6 9 17 4 12" /></svg
				>
			{:else}
				<svg
					xmlns="http://www.w3.org/2000/svg"
					width="26"
					height="26"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2.5"
					stroke-linecap="round"
					stroke-linejoin="round"
					><line x1="18" y1="6" x2="6" y2="18" /><line
						x1="6"
						y1="6"
						x2="18"
						y2="18"
					/></svg
				>
			{/if}
		</button>
	</div>
{/if}

<!-- ══════════════════════════════════════════ SUPERMARKT MODAL ══════ -->
{#if supermarketModalOpen}
	<div
		class="backdrop backdrop-blur"
		onclick={() => {
			supermarketModalOpen = false;
		}}
		role="presentation"
	></div>
	<div class="supermarket-dialog" role="dialog" aria-modal="true">
		<h2 class="sm-title">Supermarkt wählen</h2>
		<p class="sm-sub">
			Wir suchen die besten veganen Alternativen für deinen Supermarkt
		</p>

		<div class="sm-options">
			{#each supermarkets as sm}
				<button
					class="sm-card"
					class:sm-card-selected={supermarketDraft === sm}
					onclick={() => {
						supermarketDraft = sm;
					}}
				>
					<span class="sm-name">{sm}</span>
					<span
						class="sm-radio"
						class:sm-radio-checked={supermarketDraft === sm}
					>
						{#if supermarketDraft === sm}
							<svg
								xmlns="http://www.w3.org/2000/svg"
								width="18"
								height="18"
								viewBox="0 0 24 24"
								fill="none"
								stroke="currentColor"
								stroke-width="2.5"
								stroke-linecap="round"
								stroke-linejoin="round"
								><polyline points="20 6 9 17 4 12" /></svg
							>
						{/if}
					</span>
				</button>
			{/each}
		</div>

		<button class="btn-primary" onclick={confirmSupermarket}>Weiter</button>
		<button
			class="btn-text"
			onclick={() => {
				supermarketModalOpen = false;
			}}>Abbrechen</button
		>
	</div>
{/if}

<!-- ══════════════════════════════════════════════ SUBCATEGORY MODAL ══════ -->
{#if subcatModal && subcatEntry}
	<div
		class="backdrop backdrop-top"
		onclick={() => {
			subcatModal = false;
		}}
		role="presentation"
	></div>
	<div class="subcat-dialog" role="dialog" aria-modal="true">
		<h3>{subcatEntry.displayName} spezifizieren</h3>
		<div class="subcat-options">
			{#each subcatEntry.additionalOptions as opt}
				<button
					class="subcat-btn"
					class:subcat-selected={subcatSelected === opt.optionName}
					onclick={() => {
						subcatSelected = opt.optionName;
					}}
				>
					{opt.optionName}
				</button>
			{/each}
		</div>
		<button class="btn-primary" onclick={confirmSubcat}>Hinzufügen</button>
	</div>
{/if}

<!-- ══════════════════════════════════════════════════ SAVE LIST MODAL ══════ -->
{#if newListModalOpen}
	<div class="backdrop backdrop-blur" onclick={() => { newListModalOpen = false; }} role="presentation"></div>
	<div class="supermarket-dialog" role="dialog" aria-modal="true">
		<h2 class="sm-title">Neue Liste starten</h2>
		<p class="sm-sub">Was möchtest du mit deiner aktuellen Liste tun?</p>
		{#if !isSaved}
			<button class="btn-primary new-list-btn" onclick={() => { newListModalOpen = false; clearAfterSave = true; openSaveModal(); }}>Liste speichern</button>
			<button class="new-list-delete-btn" onclick={() => { newListModalOpen = false; clearList(); }}>Liste löschen</button>
			<button class="btn-text" onclick={() => { newListModalOpen = false; }}>Abbrechen</button>
		{:else}
			<button class="btn-primary new-list-btn" onclick={() => { newListModalOpen = false; clearList(); }}>Neue Liste starten</button>
			<button class="btn-text" onclick={() => { newListModalOpen = false; }}>Abbrechen</button>
		{/if}
	</div>
{/if}

{#if saveModalOpen}
	<div class="backdrop backdrop-blur" onclick={() => { saveModalOpen = false; }} role="presentation"></div>
	<div class="supermarket-dialog" role="dialog" aria-modal="true">
		<h2 class="sm-title">{$savedListId ? 'Liste aktualisieren' : 'Liste speichern'}</h2>
		<p class="sm-sub">
			{$savedListId
				? 'Deine Änderungen werden in der bestehenden Liste gespeichert.'
				: 'Gib deiner Einkaufsliste einen Namen, um sie später wiederzufinden.'}
		</p>
		<input
			type="text"
			class="save-name-input"
			bind:value={saveListName}
			use:focusOnMount
		/>
		<div class="save-modal-actions">
			<button class="btn-save-cancel" onclick={() => { saveModalOpen = false; }}>Abbrechen</button>
			<button class="btn-primary save-modal-confirm" onclick={saveList}>{$savedListId ? 'Aktualisieren' : 'Speichern'}</button>
		</div>
	</div>
{/if}

<!-- ══════════════════════════════════════════════════════ SAVE TOAST ══════ -->
{#if saveToastVisible}
	<div class="save-toast">
		<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
			<polyline points="20 6 9 17 4 12"/>
		</svg>
		{saveToastMessage}
	</div>
{/if}

<style>
	/* ── Loading screen ───────────────────────────────────────────────────── */
	.loading-screen {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 100dvh;
		gap: 20px;
		background: var(--color-bg);
	}

	.spinner {
		width: 48px;
		height: 48px;
		border-radius: 50%;
		border: 4px solid var(--color-primary-light);
		border-top-color: var(--color-primary);
		animation: spin 0.8s linear infinite;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}

	.loading-text {
		font-size: 1rem;
		font-weight: 500;
		color: var(--color-text-light);
	}

	/* ── Page ─────────────────────────────────────────────────────────────── */
	.page {
		padding: 24px 16px 100px;
		min-height: 100dvh;
	}

	.header {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-bottom: 24px;
	}

	.header-titles {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	h1 {
		font-size: 1.875rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.list-name-row {
		display: flex;
		align-items: center;
		gap: 6px;
		margin-top: 2px;
	}

	.list-name {
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text);
	}

	.list-name-input {
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text);
		background: var(--color-bg);
		border: 1px solid var(--color-primary);
		border-radius: 6px;
		padding: 2px 6px;
		outline: none;
		font-family: inherit;
		max-width: 220px;
	}

	.name-action-btn {
		background: none;
		border: none;
		padding: 2px;
		color: var(--color-text-light);
		cursor: pointer;
		display: flex;
		align-items: center;
		transition: color 0.12s ease;
	}

	.name-action-btn:hover {
		color: var(--color-primary);
	}

	.header-actions {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.icon-btn-trash {
		color: var(--color-text-light);
	}

	.icon-btn-trash:active {
		color: #ef4444;
	}

	.supermarket-badge-row {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.supermarket-badge {
		font-size: 0.75rem;
		font-weight: 700;
		color: var(--color-primary);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.badge-change-btn {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-primary);
		text-decoration: underline;
		text-underline-offset: 2px;
		background: none;
		padding: 0;
	}

	.icon-btn {
		color: var(--color-primary);
		padding: 4px;
		border-radius: 8px;
	}

	/* ── Empty state ──────────────────────────────────────────────────────── */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		text-align: center;
		padding: 64px 24px;
		gap: 12px;
	}

	.empty-icon {
		color: #d1d5db;
		margin-bottom: 8px;
	}

	.empty-title {
		font-size: 1.125rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.empty-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
		max-width: 260px;
	}

	.empty-tip {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		margin-top: 16px;
		padding: 8px 14px;
		background: var(--color-primary-light);
		border-radius: 12px;
		font-size: 0.75rem;
		color: var(--color-primary);
		line-height: 1.4;
		text-align: left;
		max-width: 300px;
	}

	.empty-tip svg {
		flex-shrink: 0;
		margin-top: 1px;
	}

	.entry-info {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
		flex: 1;
	}

	.entry-options-hint {
		font-size: 0.6875rem;
		color: var(--color-text-light);
		line-height: 1.2;
	}

	/* ── Item list ────────────────────────────────────────────────────────── */
	.item-list {
		display: flex;
		flex-direction: column;
		gap: 20px;
	}

	.category-section {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.category-header {
		display: flex;
		align-items: center;
		gap: 6px;
		color: var(--color-primary);
		font-size: 0.6875rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		padding: 0 4px 6px;
	}

	.item-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 12px 4px;
		border-bottom: 1px solid var(--color-border);
		gap: 8px;
	}

	/* Veganized item layout */
	.item-left {
		display: flex;
		align-items: center;
		gap: 12px;
		flex: 1;
		min-width: 0;
	}

	.item-right {
		display: flex;
		align-items: center;
		gap: 4px;
		flex-shrink: 0;
	}

	.checkbox-circle {
		width: 22px;
		height: 22px;
		border-radius: 50%;
		border: 1.5px solid var(--color-border);
		flex-shrink: 0;
	}

	.chevron-btn {
		color: #d1d5db;
		display: flex;
		align-items: center;
		padding: 2px;
	}

	.item-info {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
	}

	.item-name {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.item-subcat {
		font-size: 0.75rem;
		color: var(--color-primary);
	}

	.item-plantified {
		font-size: 0.75rem;
		color: var(--color-primary);
		font-weight: 500;
	}

	.item-no-match {
		font-size: 0.75rem;
		color: #ef4444;
	}

	.item-diy-fallback {
		font-size: 0.75rem;
		color: var(--color-primary);
		font-weight: 500;
		display: inline-flex;
		align-items: center;
		gap: 4px;
	}

	/* ── Quantity controls ────────────────────────────────────────────────── */
	.quantity-ctrl {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.qty-btn {
		width: 28px;
		height: 28px;
		border-radius: 50%;
		border: 1px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--color-text);
		background: var(--color-bg);
	}

	.qty-num {
		min-width: 24px;
		text-align: center;
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
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

	/* ── Plantify Me Button ───────────────────────────────────────────────── */
	.plantify-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 10px;
		width: 100%;
		margin-top: 28px;
		padding: 16px;
		border-radius: 16px;
		background: var(--color-primary);
		color: #fff;
		font-size: 1.0625rem;
		font-weight: 700;
		box-shadow: 0 8px 30px rgba(45, 125, 70, 0.3);
		transition:
			background 0.15s ease,
			box-shadow 0.15s ease;
	}

	.plantify-btn:active {
		background: var(--color-primary-hover);
		box-shadow: 0 4px 16px rgba(45, 125, 70, 0.3);
	}

	/* ── Veganized actions row ────────────────────────────────────────────── */
	.veganized-actions {
		display: flex;
		gap: 12px;
		align-items: center;
		margin-top: 28px;
	}

	.btn-save,
	.btn-new-list {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		padding: 12px 16px;
		border-radius: 14px;
		border: 1.5px solid var(--color-primary);
		color: var(--color-primary);
		font-size: 0.875rem;
		font-weight: 600;
		background: var(--color-bg);
		white-space: nowrap;
		transition: background 0.12s ease;
	}

	.btn-save:active,
	.btn-new-list:active {
		background: var(--color-primary-light);
	}

	.btn-add-round {
		width: 52px;
		height: 52px;
		border-radius: 50%;
		background: var(--color-primary);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		box-shadow: 0 4px 12px rgba(45, 125, 70, 0.35);
	}

	/* ── FAB ──────────────────────────────────────────────────────────────── */
	.fab {
		position: fixed;
		bottom: calc(72px + 16px);
		right: max(16px, calc((100vw - 390px) / 2 + 16px));
		width: 56px;
		height: 56px;
		border-radius: 50%;
		background: var(--color-primary);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4px 12px rgba(45, 125, 70, 0.4);
		z-index: 50;
	}

	/* ── Backdrop ─────────────────────────────────────────────────────────── */
	.backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.4);
		z-index: 60;
	}

	.backdrop-top {
		z-index: 80;
	}

	.backdrop-blur {
		backdrop-filter: blur(4px);
		background: rgba(0, 0, 0, 0.35);
		z-index: 80;
	}

	/* ── Bottom sheet ─────────────────────────────────────────────────────── */
	.sheet {
		position: fixed;
		bottom: 0;
		left: 50%;
		transform: translateX(-50%);
		width: 100%;
		max-width: 390px;
		height: 85dvh;
		background: var(--color-bg);
		border-radius: 20px 20px 0 0;
		z-index: 70;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.drag-handle {
		width: 40px;
		height: 4px;
		background: #d1d5db;
		border-radius: 2px;
		margin: 12px auto 0;
		flex-shrink: 0;
	}

	.sheet-header {
		padding: 16px 20px 4px;
		flex-shrink: 0;
	}

	.sheet-header h2 {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.sheet-search {
		padding: 8px 16px;
		flex-shrink: 0;
	}

	.search-input {
		width: 100%;
		padding: 10px 14px;
		border: 1px solid var(--color-border);
		border-radius: 10px;
		font-size: 1rem;
		font-family: inherit;
		color: var(--color-text);
		background: var(--color-bg);
		outline: none;
		transition: border-color 0.15s ease;
	}

	.search-input:focus {
		border-color: var(--color-primary);
	}

	.sheet-body {
		flex: 1;
		overflow-y: auto;
		padding: 4px 16px 80px;
	}

	.modal-category {
		margin-bottom: 16px;
	}

	.modal-cat-label {
		font-size: 0.6875rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: var(--color-text-light);
		padding: 8px 0 4px;
	}

	.entry-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		padding: 11px 12px;
		border-radius: 10px;
		margin-bottom: 2px;
		background: transparent;
		text-align: left;
		transition: background 0.12s ease;
	}

	.entry-row:active {
		background: var(--color-primary-light);
	}
	.entry-added {
		background: var(--color-primary-light);
	}

	.entry-name {
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.entry-action {
		color: var(--color-primary);
		display: flex;
		align-items: center;
	}

	/* ── Modal tabs ───────────────────────────────────────────────────────── */
	.sheet-tabs {
		display: flex;
		border-bottom: 1px solid var(--color-border);
		padding: 0 16px;
		flex-shrink: 0;
	}

	.sheet-tab {
		flex: 1;
		padding: 10px 0;
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text-light);
		background: none;
		border: none;
		border-bottom: 2px solid transparent;
		cursor: pointer;
		transition: color 0.12s ease, border-color 0.12s ease;
	}

	.tab-active {
		font-weight: 700;
		color: var(--color-primary);
		border-bottom-color: var(--color-primary);
	}

	/* ── Category grid ────────────────────────────────────────────────────── */
	.cat-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 8px;
		padding: 12px 16px;
	}

	.cat-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 80px;
		padding: 10px 6px;
		border-radius: 14px;
		border: 1.5px solid var(--color-border);
		background: var(--color-bg);
		cursor: pointer;
		text-align: center;
		transition: border-color 0.12s ease, background 0.12s ease;
		color: var(--color-text-light);
	}

	.cat-card:active {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
	}

	.cat-card-name {
		font-size: 0.6875rem;
		color: var(--color-text);
		font-weight: 500;
		margin-top: 8px;
		line-height: 1.3;
		word-break: break-word;
	}

	/* ── Category breadcrumb ──────────────────────────────────────────────── */
	.cat-breadcrumb {
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 8px 0 12px;
		font-size: 0.8125rem;
	}

	.breadcrumb-back {
		color: var(--color-primary);
		font-size: 0.8125rem;
		font-weight: 500;
		background: none;
		border: none;
		cursor: pointer;
		padding: 0;
	}

	.breadcrumb-sep {
		color: var(--color-text-light);
	}

	.breadcrumb-current {
		color: var(--color-text);
		font-weight: 600;
	}

	/* ── Empty tab ────────────────────────────────────────────────────────── */
	.empty-tab {
		display: flex;
		justify-content: center;
		padding: 48px 16px;
	}

	.empty-tab-text {
		font-size: 0.875rem;
		color: var(--color-text-light);
		text-align: center;
	}

	.sheet-fab {
		position: absolute;
		bottom: 24px;
		right: 16px;
		width: 52px;
		height: 52px;
		border-radius: 50%;
		background: var(--color-primary);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4px 12px rgba(45, 125, 70, 0.4);
	}

	/* ── Supermarkt dialog ────────────────────────────────────────────────── */
	.supermarket-dialog {
		position: fixed;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		width: calc(100% - 48px);
		max-width: 342px;
		background: var(--color-bg);
		border-radius: 24px;
		padding: 32px 24px 24px;
		z-index: 90;
		box-shadow: 0 8px 40px rgba(0, 0, 0, 0.18);
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.sm-title {
		font-size: 1.375rem;
		font-weight: 700;
		color: var(--color-text);
		text-align: center;
	}

	.sm-sub {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		text-align: center;
		line-height: 1.5;
		margin-bottom: 4px;
	}

	.sm-options {
		display: flex;
		flex-direction: column;
		gap: 10px;
		margin: 4px 0;
	}

	.sm-card {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 14px 16px;
		border-radius: 16px;
		border: 1.5px solid var(--color-border);
		background: var(--color-bg);
		transition:
			border-color 0.12s ease,
			background 0.12s ease;
		width: 100%;
		text-align: left;
	}

	.sm-card-selected {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
	}

	.sm-name {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text);
	}

	.sm-radio {
		width: 24px;
		height: 24px;
		border-radius: 50%;
		border: 1.5px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--color-bg);
		flex-shrink: 0;
	}

	.sm-radio-checked {
		background: var(--color-primary);
		border-color: var(--color-primary);
	}

	.btn-primary {
		padding: 12px;
		border-radius: 10px;
		background: var(--color-primary);
		color: #fff;
		font-size: 0.9375rem;
		font-weight: 600;
		width: 100%;
		text-align: center;
		transition: background 0.15s ease;
	}

	.btn-primary:active {
		background: var(--color-primary-hover);
	}

	.btn-text {
		background: none;
		color: var(--color-text-light);
		font-size: 0.9375rem;
		font-weight: 500;
		padding: 8px;
		text-align: center;
		width: 100%;
	}

	.btn-text:active {
		color: var(--color-text);
	}

	.new-list-btn {
		margin-top: 4px;
	}

	.new-list-delete-btn {
		padding: 12px;
		border-radius: 10px;
		border: 1.5px solid #ef4444;
		color: #ef4444;
		font-size: 0.9375rem;
		font-weight: 600;
		width: 100%;
		text-align: center;
		background: transparent;
		transition: background 0.12s ease;
	}

	.new-list-delete-btn:active {
		background: #fef2f2;
	}

	/* ── Subcategory dialog ───────────────────────────────────────────────── */
	.subcat-dialog {
		position: fixed;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		width: calc(100% - 48px);
		max-width: 340px;
		background: var(--color-bg);
		border-radius: 16px;
		padding: 24px;
		z-index: 90;
		box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	.subcat-dialog h3 {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.subcat-options {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
	}

	.subcat-btn {
		padding: 8px 16px;
		border-radius: 20px;
		border: 1.5px solid var(--color-border);
		font-size: 0.875rem;
		color: var(--color-text);
		background: var(--color-bg);
		transition: all 0.12s ease;
	}

	.subcat-selected {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
		color: var(--color-primary);
		font-weight: 600;
	}

	/* ── Impact section ───────────────────────────────────────────────────── */
	.impact-section {
		padding: 40px 16px 32px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.impact-header {
		margin-bottom: 4px;
	}

	.impact-title {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.impact-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		margin-top: 4px;
		line-height: 1.4;
	}

	.impact-card {
		border-radius: 16px;
		padding: 16px;
		background: #ffffff;
		box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
	}

	.impact-card-white {
		background: #ffffff;
		box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
	}

	.impact-card-label {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-primary);
		margin-bottom: 16px;
		line-height: 1.4;
	}

	.animal-row {
		display: flex;
		justify-content: space-around;
	}

	.animal-col {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
	}

	.animal-count {
		font-size: 2rem;
		font-weight: 800;
		color: var(--color-primary);
		line-height: 1;
	}

	.animal-label {
		font-size: 0.75rem;
		color: var(--color-primary);
	}

	.impact-card-inner {
		display: flex;
		align-items: flex-start;
		gap: 12px;
		margin-bottom: 12px;
	}

	.impact-icon-wrap {
		width: 40px;
		height: 40px;
		border-radius: 12px;
		background: transparent;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.impact-card-text {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.4;
		padding-top: 2px;
	}

	.impact-pct {
		font-size: 3rem;
		font-weight: 800;
		color: var(--color-primary);
		line-height: 1;
		margin-bottom: 6px;
	}

	.impact-pct-negative {
		color: #DC2626;
	}

	.impact-pct-neutral {
		color: var(--color-text-light);
	}

	.impact-compare {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text);
		line-height: 1.4;
	}
</style>
