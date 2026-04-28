<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { authFetch } from '$lib/api.js';

	const categoryMap = {
		'schweizer-kueche': 'Schweizer Küche',
		'pasta-pizza': 'Pasta & Pizza',
		'asiatische-kueche': 'Asiatische Küche',
		'fruehstueck-brunch': 'Frühstück & Brunch',
		'comfort-food': 'Comfort Food',
		'bowls-salate': 'Bowls & Salate',
		'suppen-eintoepfe': 'Suppen & Eintöpfe',
		'suesses-desserts': 'Süsses & Desserts'
	};

	let categoryId = $page.params.categoryId;
	let categoryName = $state(categoryMap[categoryId] || categoryId);

	let recipes = $state([]);
	let isLoading = $state(true);

	onMount(async () => {
		try {
			if (categoryId === 'pflanzliche-basics') {
				categoryName = 'Pflanzliche Basics';
				const res = await authFetch('/api/diy-recipes');
				if (res.ok) recipes = await res.json();
			} else {
				const res = await authFetch('/api/base-recipes?category=' + encodeURIComponent(categoryName));
				if (res.ok) recipes = await res.json();
			}
		} catch (e) {
			console.error('Fehler beim Laden:', e);
		} finally {
			isLoading = false;
		}
	});
</script>

<div class="page">
	<div class="top-bar">
		<button class="icon-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		<span class="top-title">{categoryName}</span>
		<div class="icon-btn-placeholder"></div>
	</div>

	{#if isLoading}
		<div class="spinner-wrapper">
			<div class="spinner"></div>
		</div>
	{:else if recipes.length === 0}
		<div class="empty-state">
			<p class="empty-text">Keine Rezepte in dieser Kategorie</p>
		</div>
	{:else}
		<div class="recipe-list">
			{#each recipes as recipe}
				<button class="recipe-card" onclick={() => {
					const id = recipe.id || recipe._id || '';
					console.log('Navigating to recipe:', id, recipe.title);
					if (categoryId === 'pflanzliche-basics') {
						goto('/rezepte/diy/' + id);
					} else {
						goto('/rezepte/anpassen/' + id);
					}
				}}>
					{#if recipe.imageUrl}
						<img src={recipe.imageUrl} alt={recipe.title} class="card-img" />
					{:else}
						<div class="card-img-placeholder"></div>
					{/if}
					<div class="card-info">
						<span class="card-title">{recipe.title}</span>
						{#if recipe.prepTimeMin}
							<span class="card-time">{recipe.prepTimeMin} Min</span>
						{/if}
					</div>
				</button>
			{/each}
		</div>
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

	.empty-state {
		display: flex;
		justify-content: center;
		padding: 60px 16px;
	}

	.empty-text {
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}

	.recipe-list {
		padding: 0 16px;
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

	.card-img {
		width: 90px;
		height: 70px;
		border-radius: 12px;
		object-fit: cover;
		flex-shrink: 0;
	}

	.card-img-placeholder {
		width: 90px;
		height: 70px;
		border-radius: 12px;
		background: var(--color-border);
		flex-shrink: 0;
	}

	.card-info {
		display: flex;
		flex-direction: column;
		gap: 6px;
		flex: 1;
		min-width: 0;
	}

	.card-title {
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

	.card-time {
		font-size: 0.75rem;
		color: var(--color-text-light);
	}
</style>
