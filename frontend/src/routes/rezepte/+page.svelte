<script>
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { recipeCreationStore } from '$lib/stores/recipeCreation.js';

	let baseRecipes = $state([]);
	let savedRecipes = $state([]);
	let diyRecipes = $state([]);
	let isLoading = $state(true);
	let searchQuery = $state('');
	let searchActive = $state(false);

	onMount(async () => {
		try {
			const [baseRes, savedRes, diyRes] = await Promise.all([
				fetch('/api/base-recipes'),
				fetch('/api/saved-recipes?userId=temp-user'),
				fetch('/api/diy-recipes')
			]);
			if (baseRes.ok) baseRecipes = await baseRes.json();
			if (savedRes.ok) savedRecipes = await savedRes.json();
			if (diyRes.ok) diyRecipes = await diyRes.json();
		} catch (e) {
			console.error('Fehler beim Laden:', e);
		} finally {
			isLoading = false;
		}
	});

	let categories = $derived.by(() => {
		const order = ['Schweizer Küche', 'Pasta & Pizza', 'Asiatische Küche', 'Frühstück & Brunch', 'Comfort Food'];
		return order
			.map(cat => ({ name: cat, recipes: baseRecipes.filter(r => r.category === cat) }))
			.filter(cat => cat.recipes.length > 0);
	});

	let searchResults = $derived.by(() => {
		if (!searchQuery || searchQuery.trim().length < 2) return [];
		const q = searchQuery.toLowerCase().trim();
		return baseRecipes.filter(r =>
			r.title.toLowerCase().includes(q)
		).slice(0, 5);
	});

	function clearSearch() {
		searchQuery = '';
		searchActive = false;
	}

	function goToCreate() {
		recipeCreationStore.set({ title: '', description: searchQuery, customIngredients: [], baseRecipeId: null });
		searchActive = false;
		searchQuery = '';
		goto('/rezepte/erstellen');
	}

	function goToRecipe(recipe) {
		searchActive = false;
		searchQuery = '';
		goto('/rezepte/anpassen/' + recipe.id);
	}
</script>

<div class="page">
	<!-- Header -->
	<div class="page-header">
		<h1>Rezepte</h1>
		<button class="icon-btn" onclick={() => goto('/rezepte/gespeichert')} aria-label="Gespeicherte Rezepte">
			<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
			</svg>
		</button>
	</div>

	<!-- Suchleiste -->
	<div class="search-wrapper">
		<div class="search-bar">
			<svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
			</svg>
			<input
				class="search-input"
				type="text"
				placeholder="Rezept suchen oder erstellen..."
				bind:value={searchQuery}
				onfocus={() => searchActive = true}
				aria-label="Rezept suchen"
				autocomplete="off"
			/>
			{#if searchQuery}
				<button class="clear-btn" onclick={clearSearch} aria-label="Suche leeren">
					<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
						<path d="M18 6 6 18M6 6l12 12"/>
					</svg>
				</button>
			{:else}
				<svg class="ai-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.582a.5.5 0 0 1 0 .963L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/>
				</svg>
			{/if}
		</div>

		{#if searchActive && searchQuery.trim().length >= 2}
			<div class="search-dropdown">
				<button class="dropdown-item dropdown-create" onclick={goToCreate}>
					<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.582a.5.5 0 0 1 0 .963L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/>
					</svg>
					<span class="create-label">Eigenes Rezept erstellen</span>
				</button>
				{#each searchResults as recipe}
					<button class="dropdown-item" onclick={() => goToRecipe(recipe)}>
						<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
							<circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
						</svg>
						<span class="result-label">{recipe.title}</span>
					</button>
				{/each}
			</div>
		{/if}
	</div>

	{#if searchActive && searchQuery.trim().length >= 2}
		<div class="search-backdrop" role="presentation" onclick={clearSearch}></div>
	{/if}

	{#if isLoading}
		<div class="spinner-wrapper">
			<div class="spinner"></div>
		</div>
	{:else}
		<!-- Zuletzt gespeichert -->
		{#if savedRecipes.length > 0}
			<section class="section">
				<div class="section-header">
					<span class="section-title">Zuletzt gespeichert</span>
					<button class="see-all" onclick={() => goto('/rezepte/gespeichert')}>Alle ansehen ›</button>
				</div>
				<div class="scroll-row">
					{#each savedRecipes.slice(0, 10) as recipe}
						<button class="recipe-card" onclick={() => goto('/rezepte/' + recipe.id)}>
							<div class="card-img-wrapper">
								{#if recipe.imageUrl}
									<img src={recipe.imageUrl} alt={recipe.title} class="card-img" />
								{:else}
									<div class="card-img-placeholder">{recipe.title}</div>
								{/if}
								{#if recipe.prepTimeMin}
									<span class="time-badge">{recipe.prepTimeMin} Min</span>
								{/if}
							</div>
							<span class="card-title">{recipe.title}</span>
						</button>
					{/each}
				</div>
			</section>
		{/if}

		<!-- Pflanzliche Basics (DIY) -->
		<section class="section">
			<div class="section-header">
				<span class="section-title">Pflanzliche Basics</span>
				<button class="see-all" onclick={() => goto('/rezepte/diy')}>Alle ansehen ›</button>
			</div>
			<div class="scroll-row">
				{#each diyRecipes as recipe}
					<button class="recipe-card" onclick={() => goto('/rezepte/diy/' + recipe.id)}>
						<div class="card-img-wrapper">
							{#if recipe.imageUrl}
								<img src={recipe.imageUrl} alt={recipe.title} class="card-img" />
							{:else}
								<div class="card-img-diy">
									<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
										<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
										<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
									</svg>
								</div>
							{/if}
							{#if recipe.prepTimeMin}
								<span class="time-badge">{recipe.prepTimeMin} Min</span>
							{/if}
						</div>
						<span class="card-title">{recipe.title}</span>
					</button>
				{/each}
			</div>
		</section>

		<!-- Kategorie-Sektionen -->
		{#each categories as cat}
			<section class="section">
				<div class="section-header">
					<span class="section-title">{cat.name}</span>
					<button class="see-all" onclick={() => goto('/rezepte/kategorie/' + encodeURIComponent(cat.name))}>Alle ansehen ›</button>
				</div>
				<div class="scroll-row">
					{#each cat.recipes as recipe}
						<button class="recipe-card" onclick={() => goto('/rezepte/anpassen/' + recipe.id)}>
							<div class="card-img-wrapper">
								{#if recipe.imageUrl}
									<img src={recipe.imageUrl} alt={recipe.title} class="card-img" />
								{:else}
									<div class="card-img-placeholder">{recipe.title}</div>
								{/if}
								{#if recipe.prepTimeMin}
									<span class="time-badge">{recipe.prepTimeMin} Min</span>
								{/if}
							</div>
							<span class="card-title">{recipe.title}</span>
						</button>
					{/each}
				</div>
			</section>
		{/each}
	{/if}
</div>

<style>
	.page {
		padding: 0 0 100px;
	}

	/* Header */
	.page-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 24px 16px 8px;
	}

	h1 {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
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

	/* Suchleiste */
	.search-wrapper {
		position: relative;
		margin: 0 16px 20px;
		z-index: 50;
	}

	.search-bar {
		display: flex;
		align-items: center;
		gap: 10px;
		background: #F3F4F6;
		border-radius: 14px;
		padding: 12px 16px;
		width: 100%;
		box-sizing: border-box;
	}

	.search-icon {
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	.search-input {
		flex: 1;
		font-size: 0.9375rem;
		color: var(--color-text);
		background: transparent;
		border: none;
		outline: none;
		min-width: 0;
	}

	.search-input::placeholder {
		color: var(--color-text-light);
	}

	.ai-icon {
		color: var(--color-primary);
		flex-shrink: 0;
	}

	.clear-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		background: none;
		border: none;
		color: var(--color-text-light);
		cursor: pointer;
		padding: 0;
		flex-shrink: 0;
	}

	/* Dropdown */
	.search-dropdown {
		position: absolute;
		top: calc(100% + 6px);
		left: 0;
		right: 0;
		background: white;
		border: 1px solid var(--color-border);
		border-radius: 14px;
		box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
		overflow: hidden;
		z-index: 50;
	}

	.dropdown-item {
		display: flex;
		align-items: center;
		gap: 12px;
		width: 100%;
		padding: 12px 16px;
		border: none;
		border-bottom: 1px solid var(--color-border);
		background: white;
		cursor: pointer;
		text-align: left;
	}

	.dropdown-item:last-child {
		border-bottom: none;
	}

	.dropdown-item:active {
		background: var(--color-primary-light);
	}

	.dropdown-create svg {
		color: var(--color-primary);
		flex-shrink: 0;
	}

	.dropdown-item:not(.dropdown-create) svg {
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	.create-label {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-primary);
	}

	.result-label {
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	/* Backdrop */
	.search-backdrop {
		position: fixed;
		inset: 0;
		z-index: 40;
	}

	/* Loading */
	.spinner-wrapper {
		display: flex;
		justify-content: center;
		padding: 48px 0;
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

	/* Sektionen */
	.section {
		margin-bottom: 24px;
	}

	.section-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0 16px;
		margin-bottom: 8px;
	}

	.section-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.see-all {
		font-size: 0.8125rem;
		color: var(--color-primary);
		font-weight: 500;
		background: none;
		border: none;
		cursor: pointer;
		padding: 0;
	}

	/* Horizontal Scroll */
	.scroll-row {
		display: flex;
		gap: 12px;
		overflow-x: auto;
		padding: 0 16px 4px;
		scrollbar-width: none;
		scroll-snap-type: x mandatory;
	}

	.scroll-row::-webkit-scrollbar {
		display: none;
	}

	/* Recipe Card */
	.recipe-card {
		width: 150px;
		flex-shrink: 0;
		scroll-snap-align: start;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		text-align: left;
	}

	.card-img-wrapper {
		position: relative;
		width: 150px;
		height: 110px;
		border-radius: 12px;
		overflow: hidden;
	}

	.card-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.card-img-placeholder {
		width: 100%;
		height: 100%;
		background: var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 0.75rem;
		color: var(--color-text-light);
		padding: 8px;
		text-align: center;
		line-height: 1.3;
	}

	.card-img-diy {
		width: 100%;
		height: 100%;
		background: var(--color-primary-light);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.time-badge {
		position: absolute;
		top: 8px;
		right: 8px;
		background: rgba(0, 0, 0, 0.5);
		color: white;
		font-size: 0.6875rem;
		padding: 2px 8px;
		border-radius: 8px;
		font-weight: 500;
	}

	.card-title {
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text);
		margin-top: 6px;
		line-height: 1.35;
	}
</style>
