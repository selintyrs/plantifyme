<script>
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { infoArticles, categories } from '$lib/data/infoArticles.js';
	import { authFetch } from '$lib/api.js';
	import { currentUser } from '$lib/stores/user.js';

	let firstName = $derived($currentUser?.firstName ?? '');

	let shoppingLists = $state([]);
	let dictionary = $state([]);
	let baseRecipes = $state([]);
	let impactTotal = $state(null);
	let discoverImages = $state({});
	let isLoading = $state(true);

	onMount(async () => {
		await Promise.allSettled([
			loadShoppingLists(),
			loadDictionary(),
			loadBaseRecipes(),
			loadImpactTotal(),
		]);
		await loadDiscoverImages();
		isLoading = false;
	});

	async function loadDiscoverImages() {
		const items = discoverItems;
		if (!items.length) return;
		const results = await Promise.all(items.map(async (entry) => {
			try {
				const res = await authFetch(`/api/products?displayName=${encodeURIComponent(entry.displayName)}&limit=1`);
				if (!res.ok) return [entry.displayName, null];
				const products = await res.json();
				const p = products[0];
				if (!p) return [entry.displayName, null];
				return [entry.displayName, { id: p.id, imageUrl: p.imageUrl || null }];
			} catch {
				return [entry.displayName, null];
			}
		}));
		const map = {};
		for (const [name, info] of results) {
			if (info) map[name] = info;
		}
		discoverImages = map;
	}

	async function loadShoppingLists() {
		try {
			const res = await authFetch('/api/shoppinglists');
			if (res.ok) shoppingLists = await res.json();
		} catch (e) {
			console.error('shoppinglists load failed:', e);
		}
	}

	async function loadDictionary() {
		try {
			const res = await authFetch('/api/frontend-dictionary');
			if (res.ok) dictionary = await res.json();
		} catch (e) {
			console.error('dictionary load failed:', e);
		}
	}

	async function loadBaseRecipes() {
		try {
			const res = await authFetch('/api/base-recipes');
			if (res.ok) baseRecipes = await res.json();
		} catch (e) {
			console.error('base-recipes load failed:', e);
		}
	}

	async function loadImpactTotal() {
		try {
			const res = await authFetch('/api/impact/total');
			if (res.ok) impactTotal = await res.json();
		} catch (e) {
			console.error('impact load failed:', e);
		}
	}

	// ── Seed-based random ─────────────────────────────────────────────────
	function hashSeed(seed) {
		let hash = 0;
		for (let i = 0; i < seed.length; i++) {
			hash = ((hash << 5) - hash) + seed.charCodeAt(i);
			hash |= 0;
		}
		return hash;
	}

	function dayBasedRandom(seed, array) {
		if (!array?.length) return null;
		const idx = Math.abs(hashSeed(seed)) % array.length;
		return array[idx];
	}

	function dayBasedShuffle(seed, array, count) {
		if (!array?.length) return [];
		const shuffled = [...array];
		let hash = hashSeed(seed);
		for (let i = shuffled.length - 1; i > 0; i--) {
			hash = ((hash << 5) - hash) + i;
			const j = Math.abs(hash) % (i + 1);
			[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
		}
		return shuffled.slice(0, count);
	}

	function getISOWeek(d) {
		const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
		const dayNum = date.getUTCDay() || 7;
		date.setUTCDate(date.getUTCDate() + 4 - dayNum);
		const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1));
		return Math.ceil((((date - yearStart) / 86400000) + 1) / 7);
	}

	// ── Derived ───────────────────────────────────────────────────────────
	let greeting = $derived.by(() => {
		const h = new Date().getHours();
		let greet = 'Guten Tag';
		if (h < 12) greet = 'Guten Morgen';
		else if (h >= 18) greet = 'Guten Abend';
		return firstName ? `${greet}, ${firstName}` : greet;
	});

	let totalAnimals = $derived.by(() => {
		const a = impactTotal?.animals;
		if (!a) return 0;
		return (a.cow || 0) + (a.pig || 0) + (a.chicken || 0) + (a.fish || 0);
	});

	let latestList = $derived.by(() => {
		if (!shoppingLists?.length) return null;
		return [...shoppingLists].sort((a, b) => {
			const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
			const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
			return tb - ta;
		})[0];
	});

	let discoverItems = $derived.by(() => {
		const typeA = (dictionary || []).filter(e => e.type === 'A');
		const seed = new Date().toDateString();
		return dayBasedShuffle(seed, typeA, 6);
	});

	let recipeOfTheDay = $derived.by(() => {
		const seed = new Date().toDateString();
		return dayBasedRandom(seed, baseRecipes);
	});

	let tipOfTheWeek = $derived.by(() => {
		const now = new Date();
		const seed = `${now.getFullYear()}-${getISOWeek(now)}`;
		return dayBasedRandom(seed, infoArticles);
	});

	function tipCategoryId(article) {
		const match = categories.find(c => c.name === article?.category);
		return match?.id ?? null;
	}

	// ── Navigation ────────────────────────────────────────────────────────
	function openList() {
		goto('/shoppinglists');
	}
	function openRecipe(id) {
		goto('/rezepte/anpassen/' + id);
	}
	function openTip(article) {
		const catId = tipCategoryId(article);
		if (catId) {
			goto(`/info/${catId}?openArticle=${article.id}`);
		} else {
			goto('/info');
		}
	}
</script>

<div class="page">
	<!-- 1. Top Header ─────────────────────────────────────────────────────── -->
	<div class="top-bar">
		<span class="top-title">Plantify</span>
		<button class="icon-btn icon-btn-disabled" aria-label="Benachrichtigungen" disabled>
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
				<path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
			</svg>
		</button>
	</div>

	<!-- 2. Greeting ───────────────────────────────────────────────────────── -->
	<div class="hello">
		<h1 class="hello-title">{greeting}</h1>
	</div>

	<!-- 3. Active list quick-access ───────────────────────────────────────── -->
	{#if latestList}
		<button class="quick-list-card" onclick={openList}>
			<div class="quick-list-text">
				<span class="quick-list-label">Deine letzte Liste</span>
				<span class="quick-list-body">
					{latestList.supermarket ? latestList.supermarket.charAt(0).toUpperCase() + latestList.supermarket.slice(1) : '—'}
					· {latestList.items?.length ?? 0} Produkte
				</span>
			</div>
			<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="m9 18 6-6-6-6"/>
			</svg>
		</button>
	{/if}

	<!-- 4. Entdecke heute ─────────────────────────────────────────────────── -->
	{#if discoverItems.length > 0}
		<section class="section">
			<h2 class="section-title">Entdecke heute</h2>
			<p class="section-sub">Pflanzliche Alternativen, die du kennenlernen solltest</p>
			<div class="carousel">
				{#each discoverItems as entry (entry.id)}
					{@const info = discoverImages[entry.displayName]}
					<button class="discover-card" onclick={() => info?.id ? goto('/products/' + info.id) : goto('/liste')}>
						{#if info?.imageUrl}
							<img class="discover-img" src={info.imageUrl} alt={entry.displayName} loading="lazy" />
						{:else}
							<div class="discover-img discover-img-placeholder">
								<svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
									<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
									<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
								</svg>
							</div>
						{/if}
						<div class="discover-text">
							<span class="discover-name">{entry.displayName}</span>
							<span class="discover-sub">Pflanzliche Alternativen</span>
						</div>
					</button>
				{/each}
			</div>
		</section>
	{/if}

	<!-- 5. Rezept des Tages ───────────────────────────────────────────────── -->
	{#if recipeOfTheDay}
		<section class="section">
			<h2 class="section-title">Rezept des Tages</h2>
			<button class="recipe-card" onclick={() => openRecipe(recipeOfTheDay.id)}>
				{#if recipeOfTheDay.imageUrl}
					<img class="recipe-img" src={recipeOfTheDay.imageUrl} alt={recipeOfTheDay.title} />
				{:else}
					<div class="recipe-img recipe-img-placeholder"></div>
				{/if}
				<div class="recipe-body">
					{#if recipeOfTheDay.category}
						<span class="recipe-cat">{recipeOfTheDay.category}</span>
					{/if}
					<span class="recipe-title">{recipeOfTheDay.title}</span>
					<div class="recipe-badges">
						{#if recipeOfTheDay.prepTimeMin}
							<span class="recipe-badge">{recipeOfTheDay.prepTimeMin} min</span>
						{/if}
						{#if recipeOfTheDay.difficulty}
							<span class="recipe-badge">{recipeOfTheDay.difficulty}</span>
						{/if}
					</div>
				</div>
			</button>
		</section>
	{/if}

	<!-- 6. Tipp der Woche ─────────────────────────────────────────────────── -->
	{#if tipOfTheWeek}
		<section class="section">
			<h2 class="section-title">Tipp der Woche</h2>
			<button class="tip-card" onclick={() => openTip(tipOfTheWeek)}>
				<div class="tip-text">
					<span class="tip-cat">{tipOfTheWeek.category}</span>
					<span class="tip-title">{tipOfTheWeek.title}</span>
				</div>
				<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="m9 18 6-6-6-6"/>
				</svg>
			</button>
		</section>
	{/if}

	<!-- 7. Schnellzugriff ─────────────────────────────────────────────────── -->
	<section class="section">
		<h2 class="section-title">Schnellzugriff</h2>
		<div class="quick-row">
			<button class="quick-card" onclick={() => goto('/liste')}>
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/>
					<path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/>
				</svg>
				<span class="quick-label">Einkaufs&shy;liste</span>
			</button>
			<button class="quick-card" onclick={() => goto('/rezepte')}>
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
					<path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
				</svg>
				<span class="quick-label">Rezepte</span>
			</button>
			<button class="quick-card" onclick={() => goto('/info')}>
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M9 18h6"/><path d="M10 22h4"/>
					<path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5A4.61 4.61 0 0 1 8.91 14"/>
				</svg>
				<span class="quick-label">Plantified werden</span>
			</button>
			<button class="quick-card" onclick={() => goto('/shoppinglists')}>
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
				</svg>
				<span class="quick-label">Favoriten</span>
			</button>
		</div>
	</section>

	<!-- 8. Impact-Gesamtübersicht ─────────────────────────────────────────── -->
	<section class="section impact-section">
		<div class="impact-header">
			<h2 class="section-title">Dein Impact</h2>
			<p class="section-sub">Seit du Plantify nutzt</p>
		</div>

		{#if !impactTotal || (totalAnimals === 0 && (impactTotal.co2TotalKg ?? 0) === 0)}
			<div class="impact-empty">
				<p class="impact-empty-text">Erstelle deine erste Liste, um deinen Impact zu sehen.</p>
				<button class="impact-empty-btn" onclick={() => goto('/liste')}>Zur Einkaufsliste</button>
			</div>
		{:else}
			<div class="impact-card">
				<p class="impact-card-label">Anzahl Tiere, die dank dir nicht leiden mussten</p>
				<div class="animal-row">
					{#if impactTotal.animals?.cow > 0}
						<div class="animal-col">
							<img src="/images/icons/cow.png" alt="Kuh" width="44" height="44" />
							<span class="animal-count">{impactTotal.animals.cow}</span>
							<span class="animal-label">Kühe</span>
						</div>
					{/if}
					{#if impactTotal.animals?.pig > 0}
						<div class="animal-col">
							<img src="/images/icons/pig.png" alt="Schwein" width="44" height="44" />
							<span class="animal-count">{impactTotal.animals.pig}</span>
							<span class="animal-label">Schweine</span>
						</div>
					{/if}
					{#if impactTotal.animals?.chicken > 0}
						<div class="animal-col">
							<img src="/images/icons/chicken.png" alt="Huhn" width="44" height="44" />
							<span class="animal-count">{impactTotal.animals.chicken}</span>
							<span class="animal-label">Hühner</span>
						</div>
					{/if}
					{#if impactTotal.animals?.fish > 0}
						<div class="animal-col">
							<img src="/images/icons/fish.png" alt="Fisch" width="44" height="44" />
							<span class="animal-count">{impactTotal.animals.fish}</span>
							<span class="animal-label">Fische</span>
						</div>
					{/if}
					{#if totalAnimals === 0}
						<p class="animal-empty">Noch keine Tiere gerettet.</p>
					{/if}
				</div>
			</div>

			<div class="impact-card impact-card-white">
				<div class="impact-card-inner">
					<div class="impact-icon-wrap">
						<img src="/images/icons/co2.png" alt="CO₂" width="40" height="40" />
					</div>
					<p class="impact-card-text">So viel CO₂ hast du insgesamt gespart</p>
				</div>
				<p class="impact-value">{impactTotal.co2TotalKg?.toFixed(1) ?? '0.0'} kg</p>
				<p class="impact-compare">{impactTotal.co2Compare}</p>
			</div>

			<div class="impact-card impact-card-white">
				<div class="impact-card-inner">
					<div class="impact-icon-wrap">
						<img src="/images/icons/water.png" alt="Wasser" width="40" height="40" />
					</div>
					<p class="impact-card-text">So viel Wasser hast du insgesamt gespart</p>
				</div>
				<p class="impact-value">{impactTotal.waterTotalL?.toFixed(1) ?? '0.0'} L</p>
				<p class="impact-compare">{impactTotal.waterCompare}</p>
			</div>

			<div class="impact-card impact-card-white">
				<div class="impact-card-inner">
					<div class="impact-icon-wrap">
						<img src="/images/icons/land.png" alt="Land" width="40" height="40" />
					</div>
					<p class="impact-card-text">So viel Land hast du insgesamt gespart</p>
				</div>
				<p class="impact-value">{impactTotal.landTotalM2?.toFixed(1) ?? '0.0'} m²</p>
				<p class="impact-compare">{impactTotal.landCompare}</p>
			</div>
		{/if}
	</section>

	{#if isLoading}
		<div class="loading-overlay">
			<div class="spinner"></div>
		</div>
	{/if}
</div>

<style>
	.page {
		padding-bottom: 100px;
	}

	.top-bar {
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16px;
		background: var(--color-bg, white);
	}

	.top-title {
		font-size: 1.25rem;
		font-weight: 800;
		color: var(--color-primary);
		letter-spacing: -0.01em;
	}

	.icon-btn {
		width: 40px;
		height: 40px;
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

	.icon-btn-disabled {
		opacity: 0.45;
		cursor: default;
	}

	.icon-btn-disabled:hover {
		background: transparent;
	}

	/* ── Greeting ─────────────────────────────────────────────────────── */
	.hello {
		padding: 8px 16px 20px;
	}

	.hello-title {
		font-size: 1.5rem;
		font-weight: 800;
		color: var(--color-text);
		margin: 0;
		line-height: 1.2;
	}

	/* ── Quick list card ──────────────────────────────────────────────── */
	.quick-list-card {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: calc(100% - 32px);
		margin: 0 16px 16px;
		padding: 16px;
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		background: var(--color-bg);
		color: var(--color-text);
		text-align: left;
		cursor: pointer;
		transition: border-color 0.15s ease;
	}

	.quick-list-card:active {
		border-color: var(--color-primary);
	}

	.quick-list-text {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.quick-list-label {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--color-primary);
		letter-spacing: 0.06em;
		text-transform: uppercase;
	}

	.quick-list-body {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
	}

	.quick-list-card svg {
		color: var(--color-text-light);
	}

	/* ── Section ──────────────────────────────────────────────────────── */
	.section {
		margin-top: 24px;
	}

	.section-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
		padding: 0 16px;
		margin: 0 0 4px;
	}

	.section-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		padding: 0 16px;
		margin: 0 0 12px;
	}

	/* ── Carousel (Entdecke heute) ────────────────────────────────────── */
	.carousel {
		display: flex;
		gap: 12px;
		overflow-x: auto;
		padding: 0 16px 4px;
		scroll-snap-type: x mandatory;
	}

	.carousel::-webkit-scrollbar {
		display: none;
	}

	.discover-card {
		flex: 0 0 140px;
		display: flex;
		flex-direction: column;
		padding: 0;
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		background: var(--color-bg);
		color: var(--color-text);
		text-align: left;
		cursor: pointer;
		scroll-snap-align: start;
		overflow: hidden;
	}

	.discover-img {
		width: 100%;
		height: 100px;
		object-fit: cover;
		flex-shrink: 0;
	}

	.discover-img-placeholder {
		display: flex;
		align-items: center;
		justify-content: center;
		background: linear-gradient(150deg, var(--color-primary) 0%, var(--color-primary-hover, #1f5a30) 100%);
		color: #fff;
	}

	.discover-text {
		display: flex;
		flex-direction: column;
		gap: 2px;
		padding: 10px 12px 14px;
	}

	.discover-name {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.discover-sub {
		font-size: 0.75rem;
		color: var(--color-text-light);
	}

	/* ── Recipe of the day ────────────────────────────────────────────── */
	.recipe-card {
		display: flex;
		width: calc(100% - 32px);
		margin: 0 16px;
		height: 140px;
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		background: var(--color-bg);
		overflow: hidden;
		cursor: pointer;
		padding: 0;
		text-align: left;
	}

	.recipe-img {
		width: 120px;
		height: 100%;
		object-fit: cover;
		flex-shrink: 0;
	}

	.recipe-img-placeholder {
		background: var(--color-primary-light);
	}

	.recipe-body {
		flex: 1;
		min-width: 0;
		padding: 12px 14px;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		gap: 4px;
	}

	.recipe-cat {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-primary);
		letter-spacing: 0.06em;
		text-transform: uppercase;
	}

	.recipe-title {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
		line-height: 1.3;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.recipe-badges {
		display: flex;
		gap: 6px;
		flex-wrap: wrap;
	}

	.recipe-badge {
		font-size: 0.6875rem;
		padding: 3px 8px;
		border-radius: 999px;
		background: var(--color-primary-light);
		color: var(--color-primary);
		font-weight: 600;
	}

	/* ── Tipp der Woche ───────────────────────────────────────────────── */
	.tip-card {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		width: calc(100% - 32px);
		margin: 0 16px;
		padding: 16px;
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		background: var(--color-bg);
		color: var(--color-text);
		text-align: left;
		cursor: pointer;
		transition: border-color 0.15s ease;
	}

	.tip-card:active {
		border-color: var(--color-primary);
	}

	.tip-text {
		display: flex;
		flex-direction: column;
		gap: 4px;
		min-width: 0;
		flex: 1;
	}

	.tip-cat {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-primary);
		letter-spacing: 0.06em;
		text-transform: uppercase;
	}

	.tip-title {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
		line-height: 1.3;
	}

	.tip-card svg {
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	/* ── Quick row ────────────────────────────────────────────────────── */
	.quick-row {
		display: flex;
		gap: 8px;
		padding: 0 16px;
	}

	.quick-card {
		flex: 1;
		min-width: 0;
		height: 80px;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 6px;
		padding: 10px 4px;
		border: 1.5px solid var(--color-border);
		border-radius: 12px;
		background: var(--color-bg);
		color: var(--color-primary);
		text-align: center;
		cursor: pointer;
		transition: border-color 0.15s ease, background 0.15s ease;
	}

	.quick-card:active {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
	}

	.quick-label {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-text);
		line-height: 1.2;
		max-height: 2.4em;
		overflow: hidden;
	}

	/* ── Impact section (same as /liste) ──────────────────────────────── */
	.impact-section {
		padding: 0 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.impact-header {
		padding: 0;
	}

	.impact-section .section-title,
	.impact-section .section-sub {
		padding: 0;
	}

	.impact-card {
		border-radius: 16px;
		padding: 16px;
		background: #ffffff;
		box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
	}

	.impact-card-label {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-primary);
		margin: 0 0 16px;
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

	.animal-empty {
		width: 100%;
		text-align: center;
		font-size: 0.875rem;
		color: var(--color-text-light);
		margin: 0;
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
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.impact-card-text {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.4;
		margin: 0;
		padding-top: 2px;
	}

	.impact-value {
		font-size: 2.25rem;
		font-weight: 800;
		color: var(--color-primary);
		line-height: 1;
		margin: 0 0 6px;
	}

	.impact-compare {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text);
		line-height: 1.4;
		margin: 0;
	}

	.impact-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 24px 16px;
		border: 1.5px dashed var(--color-border);
		border-radius: 16px;
	}

	.impact-empty-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		text-align: center;
		margin: 0;
	}

	.impact-empty-btn {
		padding: 10px 20px;
		border: none;
		border-radius: 12px;
		background: var(--color-primary);
		color: #fff;
		font-size: 0.875rem;
		font-weight: 600;
		cursor: pointer;
	}

	/* ── Loading overlay ──────────────────────────────────────────────── */
	.loading-overlay {
		position: fixed;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.8);
		z-index: 100;
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
</style>
