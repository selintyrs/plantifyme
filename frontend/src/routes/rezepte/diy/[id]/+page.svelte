<script>
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { authFetch } from '$lib/api.js';

	let id = $page.params.id;
	let recipe = $state(null);
	let isLoading = $state(true);
	let error = $state(null);
	let activeTab = $state('zutaten');

	onMount(async () => {
		try {
			const res = await authFetch(`/api/diy-recipes/${id}`);
			if (res.ok) {
				recipe = await res.json();
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

	function formatIngredientAmount(ing) {
		if (!ing.amount && ing.amount !== 0) return '';
		const amount = ing.amount % 1 === 0 ? ing.amount.toString() : ing.amount.toFixed(1).replace(/\.0$/, '');
		return `${amount}${ing.unit ? ' ' + ing.unit : ''}`;
	}

	function difficultyLabel(d) {
		if (d === 'easy') return 'Einfach';
		if (d === 'medium') return 'Mittel';
		if (d === 'hard') return 'Schwer';
		return d || '–';
	}
</script>

<div class="page">
	<!-- Top Bar -->
	<div class="top-bar">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
	</div>

	{#if isLoading}
		<div class="spinner-wrapper">
			<div class="spinner"></div>
		</div>
	{:else if error}
		<div class="error-wrapper">
			<p class="error-text">{error}</p>
		</div>
	{:else if recipe}
		<!-- Recipe Image -->
		{#if recipe.imageUrl}
			<img src={recipe.imageUrl} alt={recipe.title} class="recipe-img" />
		{:else}
			<div class="recipe-img-placeholder">
				<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
					<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
					<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
				</svg>
			</div>
		{/if}

		<!-- Recipe Info -->
		<div class="recipe-info">
			<span class="recipe-label">Pflanzliche Basics</span>
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
		</div>

		<!-- Tab Bar -->
		<div class="tab-bar">
			<button class="tab" class:tab-active={activeTab === 'zutaten'} onclick={() => activeTab = 'zutaten'}>
				Zutaten
			</button>
			<button class="tab" class:tab-active={activeTab === 'zubereitung'} onclick={() => activeTab = 'zubereitung'}>
				Zubereitung
			</button>
		</div>

		<!-- Tab: Zutaten -->
		{#if activeTab === 'zutaten'}
			<div class="ingredient-list">
				{#each (recipe.ingredients || []).filter(i => i.name !== 'Wasser') as ing}
					<div class="ingredient-row">
						<span class="ingredient-amount">{formatIngredientAmount(ing)}</span>
						<span class="ingredient-name">{ing.name}</span>
					</div>
				{/each}
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
	{/if}
</div>

<style>
	.page {
		padding: 0 0 100px;
	}

	.top-bar {
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		align-items: center;
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

	.recipe-img {
		width: 100%;
		height: 220px;
		object-fit: cover;
		display: block;
	}

	.recipe-img-placeholder {
		width: 100%;
		height: 220px;
		background: var(--color-primary-light);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.recipe-info {
		padding: 16px;
	}

	.recipe-label {
		font-size: 0.8125rem;
		color: var(--color-primary);
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

	.steps-list {
		padding: 0 16px;
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
		justify-content: center;
		padding: 80px 16px;
	}

	.error-text {
		font-size: 1rem;
		color: var(--color-text-light);
		margin: 0;
	}
</style>
