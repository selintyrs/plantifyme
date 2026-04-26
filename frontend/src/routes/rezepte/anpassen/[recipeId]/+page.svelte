<script>
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { recipeCreationStore, resetRecipeCreation } from '$lib/stores/recipeCreation.js';
	import { authFetch } from '$lib/api.js';
	import { currentUser } from '$lib/stores/user.js';

	const ALLERGY_LABELS = {
		nuts: 'Nüsse',
		soy: 'Soja',
		gluten: 'Gluten',
		sesame: 'Sesam',
		mustard: 'Senf',
		celery: 'Sellerie',
		lupins: 'Lupinen',
	};

	let recipeId = $page.params.recipeId;
	let baseRecipe = $state(null);
	let recipeName = $state('');
	let isLoading = $state(true);
	let isGenerating = $state(false);

	let prepTimeMax = $state(null);
	let difficulty = $state(null);
	let budgetFriendly = $state(false);
	let simpleIngredients = $state(false);
	let highProtein = $state(false);
	let lowCarb = $state(false);
	let lowFat = $state(false);
	let wholeFood = $state(false);
	let sugarFree = $state(false);
	let glutenFree = $state(false);
	let soyFree = $state(false);
	let nutFree = $state(false);
	let allergies = $state('');
	let additionalNotes = $state('');
	let storeData = $state(null);

	let prefilledFromUser = $state(false);

	onMount(() => {
		loadData(recipeId);
	});

	$effect(() => {
		const user = $currentUser;
		if (!user || prefilledFromUser) return;

		const userAllergies = user.allergies ?? [];
		nutFree = userAllergies.includes('nuts');
		soyFree = userAllergies.includes('soy');
		glutenFree = userAllergies.includes('gluten');

		const fallbackLabels = userAllergies
			.filter(a => !['nuts', 'soy', 'gluten'].includes(a))
			.map(a => ALLERGY_LABELS[a])
			.filter(Boolean);
		const userCustom = (user.customAllergies ?? '').trim();
		const combined = [...fallbackLabels, ...(userCustom ? [userCustom] : [])].join(', ');
		if (combined) allergies = combined;

		if (user.dietaryPreferences?.length) {
			const prefs = user.dietaryPreferences;
			highProtein = prefs.includes('highProtein');
			lowCarb = prefs.includes('lowCarb');
			if (prefs.includes('glutenFree')) glutenFree = true;
		}

		prefilledFromUser = true;
	});

	async function loadData(id) {
		isLoading = true;
		if (id === 'neu') {
			const unsubscribe = recipeCreationStore.subscribe(value => { storeData = value; });
			unsubscribe();
			recipeName = storeData?.description
				? storeData.description.substring(0, 50) + (storeData.description.length > 50 ? '...' : '')
				: 'Eigenes Rezept';
			isLoading = false;
		} else {
			try {
				let res = await authFetch(`/api/base-recipes/${id}`);
				if (res.ok) {
					baseRecipe = await res.json();
					recipeName = baseRecipe.title;
				} else {
					res = await authFetch(`/api/saved-recipes/${id}`);
					if (res.ok) {
						baseRecipe = await res.json();
						recipeName = baseRecipe.title;
					}
				}
			} catch (e) {
				console.error('Rezept laden fehlgeschlagen:', e);
			} finally {
				isLoading = false;
			}
		}
	}

	async function generateRecipe() {
		isGenerating = true;
		try {
			const body = {
				title: baseRecipe?.title || storeData?.description || 'Rezept',
				description: storeData?.description || null,
				customIngredients: storeData?.customIngredients || [],
				preferences: {
					prepTimeMax: prepTimeMax !== null ? Number(prepTimeMax) : null,
					difficulty,
					highProtein,
					lowCarb,
					lowFat,
					wholeFood,
					sugarFree,
					budgetFriendly,
					simpleIngredients,
					glutenFree,
					soyFree,
					nutFree,
					allergies: allergies.trim() || null,
					additionalNotes: additionalNotes.trim() || null
				},
				baseRecipeId: recipeId !== 'neu' ? recipeId : null
			};

			const res = await authFetch('/api/recipes/generate', {
				method: 'POST',
				body: JSON.stringify(body)
			});

			if (res.ok) {
				const recipe = await res.json();
				resetRecipeCreation();
				goto(`/rezepte/${recipe.id}`);
			} else {
				console.error('Generierung fehlgeschlagen:', res.status);
			}
		} catch (e) {
			console.error('Generierung fehlgeschlagen:', e);
		} finally {
			isGenerating = false;
		}
	}
</script>

{#snippet toggle(value, onToggle)}
	<button
		class="toggle"
		class:toggle-active={value}
		onclick={onToggle}
		role="switch"
		aria-checked={value}
		aria-label={value ? 'Aktiviert' : 'Deaktiviert'}
		type="button"
	>
		<span class="toggle-thumb" class:toggle-thumb-active={value}></span>
	</button>
{/snippet}

{#if isGenerating}
	<div class="generating-overlay">
		<div class="spinner"></div>
		<p class="generating-text">Dein Rezept wird erstellt...</p>
		<p class="generating-sub">Das kann einige Sekunden dauern</p>
	</div>
{/if}

<div class="page">
	<!-- Header -->
	<div class="top-bar">
		<button class="back-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		<div class="top-bar-text">
			<h1>Rezept anpassen</h1>
			{#if recipeName}
				<p class="recipe-name">{recipeName}</p>
			{/if}
		</div>
	</div>

	{#if isLoading}
		<div class="spinner-wrapper">
			<div class="spinner"></div>
		</div>
	{:else}
		<!-- SETUP -->
		<p class="section-label">SETUP</p>
		<div class="settings-card">
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
					</svg>
					<span class="row-label">Zubereitungszeit</span>
				</div>
				<select class="dropdown" bind:value={prepTimeMax}>
					<option value={null}>Beliebig</option>
					<option value={15}>15 Min</option>
					<option value={30}>30 Min</option>
					<option value={45}>45 Min</option>
					<option value={60}>60 Min</option>
				</select>
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
					</svg>
					<span class="row-label">Schwierigkeit</span>
				</div>
				<select class="dropdown" bind:value={difficulty}>
					<option value={null}>Beliebig</option>
					<option value="easy">Einfach</option>
					<option value="medium">Mittel</option>
					<option value="hard">Schwer</option>
				</select>
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
					</svg>
					<span class="row-label">Budget-Friendly</span>
				</div>
				{@render toggle(budgetFriendly, () => budgetFriendly = !budgetFriendly)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
						<line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
					</svg>
					<span class="row-label">Einfache Zutaten</span>
				</div>
				{@render toggle(simpleIngredients, () => simpleIngredients = !simpleIngredients)}
			</div>
		</div>

		<!-- NÄHRWERTPROFIL -->
		<p class="section-label">NÄHRWERTPROFIL</p>
		<div class="settings-card">
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
					</svg>
					<span class="row-label">Proteinreich</span>
				</div>
				{@render toggle(highProtein, () => highProtein = !highProtein)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/><polyline points="17 18 23 18 23 12"/>
					</svg>
					<span class="row-label">Low Carb</span>
				</div>
				{@render toggle(lowCarb, () => lowCarb = !lowCarb)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
					</svg>
					<span class="row-label">Low Fat</span>
				</div>
				{@render toggle(lowFat, () => lowFat = !lowFat)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
						<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
					</svg>
					<span class="row-label">Whole Foods</span>
				</div>
				{@render toggle(wholeFood, () => wholeFood = !wholeFood)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
					</svg>
					<span class="row-label">Zuckerfrei</span>
				</div>
				{@render toggle(sugarFree, () => sugarFree = !sugarFree)}
			</div>
		</div>

		<!-- EINSCHRÄNKUNGEN -->
		<p class="section-label">EINSCHRÄNKUNGEN</p>
		<div class="settings-card">
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M2 22 16 8"/><path d="M3.47 12.53 5 11l1.53 1.53a3.5 3.5 0 0 1 0 4.94L5 19l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/>
						<path d="M7.47 8.53 9 7l1.53 1.53a3.5 3.5 0 0 1 0 4.94L9 15l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/>
						<path d="M11.47 4.53 13 3l1.53 1.53a3.5 3.5 0 0 1 0 4.94L13 11l-1.53-1.53a3.5 3.5 0 0 1 0-4.94z"/>
						<path d="M20 2h2v2a4 4 0 0 1-4 4h-2V6a4 4 0 0 1 4-4z"/>
					</svg>
					<span class="row-label">Glutenfrei</span>
				</div>
				{@render toggle(glutenFree, () => glutenFree = !glutenFree)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
					</svg>
					<span class="row-label">Sojafrei</span>
				</div>
				{@render toggle(soyFree, () => soyFree = !soyFree)}
			</div>
			<div class="settings-row">
				<div class="row-left">
					<svg class="row-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3z"/>
						<line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
					</svg>
					<span class="row-label">Nussfrei</span>
				</div>
				{@render toggle(nutFree, () => nutFree = !nutFree)}
			</div>
		</div>
		<div class="extra-field">
			<label class="extra-label" for="allergies">Weitere Allergien</label>
			<input
				id="allergies"
				class="text-input"
				type="text"
				placeholder="Z.B. Pilze, Sesam..."
				bind:value={allergies}
			/>
		</div>

		<!-- WEITERE SPEZIFIKATIONEN -->
		<p class="section-label">WEITERE SPEZIFIKATIONEN FÜR DEIN REZEPT</p>
		<div class="textarea-section">
			<textarea
				class="notes-textarea"
				placeholder="Gibt es noch etwas, worauf wir achten sollen? (Z.B. ohne Öl, proteinreich, etc.)"
				bind:value={additionalNotes}
			></textarea>
		</div>
	{/if}

	<!-- Generate Button -->
	<div class="generate-btn-wrapper">
		<button class="generate-btn" onclick={generateRecipe} disabled={isGenerating || isLoading}>
			<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.582a.5.5 0 0 1 0 .963L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/>
			</svg>
			Rezept generieren
		</button>
	</div>
</div>

<style>
	.page {
		padding: 0 0 160px;
	}

	/* Header */
	.top-bar {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 16px 16px 12px;
		background: white;
		position: sticky;
		top: 0;
		z-index: 10;
	}

	.top-bar-text {
		flex: 1;
	}

	.back-btn {
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
		flex-shrink: 0;
	}

	.back-btn:hover {
		background: var(--color-border);
	}

	h1 {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
	}

	.recipe-name {
		font-size: 0.875rem;
		color: var(--color-primary);
		margin: 2px 0 0;
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

	/* Generating overlay */
	.generating-overlay {
		position: fixed;
		inset: 0;
		background: rgba(255, 255, 255, 0.95);
		z-index: 50;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.generating-text {
		font-size: 1rem;
		color: var(--color-text-light);
		margin: 16px 0 4px;
	}

	.generating-sub {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		margin: 0;
	}

	/* Section label */
	.section-label {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--color-text-light);
		letter-spacing: 0.05em;
		text-transform: uppercase;
		padding: 0 16px;
		margin: 20px 0 8px;
	}

	/* Settings card */
	.settings-card {
		background: white;
		border-radius: 16px;
		border: 1.5px solid var(--color-border);
		margin: 0 16px;
		overflow: hidden;
	}

	.settings-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border);
	}

	.settings-row:last-child {
		border-bottom: none;
	}

	.row-left {
		display: flex;
		align-items: center;
	}

	.row-icon {
		color: var(--color-primary);
		margin-right: 12px;
		flex-shrink: 0;
	}

	.row-label {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text);
	}

	.dropdown {
		appearance: none;
		background: transparent;
		border: none;
		color: var(--color-text-light);
		font-size: 0.875rem;
		text-align: right;
		cursor: pointer;
		outline: none;
		font-family: inherit;
	}

	/* Toggle */
	.toggle {
		width: 44px;
		height: 26px;
		border-radius: 13px;
		border: none;
		background: #E5E7EB;
		cursor: pointer;
		padding: 0;
		position: relative;
		transition: background 0.2s;
		flex-shrink: 0;
	}

	.toggle-active {
		background: var(--color-primary);
	}

	.toggle-thumb {
		position: absolute;
		top: 2px;
		left: 0;
		width: 22px;
		height: 22px;
		border-radius: 50%;
		background: white;
		transform: translateX(2px);
		transition: transform 0.2s;
		box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
	}

	.toggle-thumb-active {
		transform: translateX(20px);
	}

	/* Extra fields */
	.extra-field {
		margin: 12px 16px 0;
	}

	.extra-label {
		display: block;
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text);
		margin-bottom: 8px;
	}

	.text-input {
		width: 100%;
		border-radius: 14px;
		border: 1.5px solid var(--color-border);
		padding: 12px 16px;
		font-size: 0.9375rem;
		font-family: inherit;
		color: var(--color-text);
		background: white;
		outline: none;
		box-sizing: border-box;
	}

	.text-input:focus {
		border-color: var(--color-primary);
	}

	.textarea-section {
		margin: 0 16px;
	}

	.notes-textarea {
		width: 100%;
		min-height: 100px;
		border-radius: 14px;
		border: 1.5px solid var(--color-border);
		padding: 14px 16px;
		font-size: 0.9375rem;
		font-family: inherit;
		color: var(--color-text);
		background: white;
		resize: vertical;
		outline: none;
		box-sizing: border-box;
	}

	.notes-textarea:focus {
		border-color: var(--color-primary);
	}

	/* Generate button */
	.generate-btn-wrapper {
		position: sticky;
		bottom: 80px;
		padding: 16px;
	}

	.generate-btn {
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

	.generate-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
</style>
