<script>
	import { goto } from '$app/navigation';
	import { recipeCreationStore, resetRecipeCreation } from '$lib/stores/recipeCreation.js';

	let description = $state('');
	let customIngredients = $state([]);
	let ingredientInput = $state('');
	let excludedIngredients = $state([]);
	let excludedInput = $state('');

	function addIngredient() {
		const trimmed = ingredientInput.trim();
		if (trimmed && !customIngredients.includes(trimmed)) {
			customIngredients = [...customIngredients, trimmed];
			ingredientInput = '';
		}
	}

	function removeIngredient(index) {
		customIngredients = customIngredients.filter((_, i) => i !== index);
	}

	function handleKeydown(e) {
		if (e.key === 'Enter') {
			e.preventDefault();
			addIngredient();
		}
	}

	function addExcluded() {
		const trimmed = excludedInput.trim();
		if (trimmed && !excludedIngredients.includes(trimmed)) {
			excludedIngredients = [...excludedIngredients, trimmed];
			excludedInput = '';
		}
	}

	function removeExcluded(index) {
		excludedIngredients = excludedIngredients.filter((_, i) => i !== index);
	}

	function handleExcludedKeydown(e) {
		if (e.key === 'Enter') {
			e.preventDefault();
			addExcluded();
		}
	}

	function goToAnpassen() {
		recipeCreationStore.set({
			title: '',
			description,
			customIngredients,
			excludedIngredients,
			baseRecipeId: null
		});
		goto('/rezepte/anpassen/neu');
	}

	let canProceed = $derived(description.trim().length > 0 || customIngredients.length > 0);
</script>

<div class="page">
	<!-- Header -->
	<div class="top-bar">
		<button class="back-btn" onclick={() => history.back()} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		<h1>Rezept erstellen</h1>
	</div>

	<div class="step-indicator-wrapper">
		<p class="step-indicator">Schritt 1 von 3 · Deine Rezept-Idee</p>
		<div class="step-progress" aria-hidden="true">
			<span class="step-bar step-bar-active"></span>
			<span class="step-bar"></span>
			<span class="step-bar"></span>
		</div>
	</div>

	<div class="content">
		<!-- Beschreibung -->
		<section class="section">
			<textarea
				class="description-input"
				placeholder="Beschreibe dein Wunsch-Rezept, z. B. 'schnelles veganes Nudelgericht mit cremiger Sosse'"
				bind:value={description}
			></textarea>
		</section>

		<!-- Pflichtzutaten -->
		<section class="section">
			<h2 class="section-title">Diese Zutaten sollen drin sein (optional)</h2>
			<div class="ingredient-row">
				<input
					class="ingredient-input"
					type="text"
					placeholder="Füge eine Zutat hinzu"
					bind:value={ingredientInput}
					onkeydown={handleKeydown}
				/>
				<button
					class="add-btn"
					onclick={addIngredient}
					disabled={!ingredientInput.trim()}
				>
					Hinzufügen
				</button>
			</div>
			{#if customIngredients.length > 0}
				<div class="chips">
					{#each customIngredients as ingredient, i}
						<span class="chip">
							{ingredient}
							<button class="chip-remove" onclick={() => removeIngredient(i)} aria-label="Entfernen">
								<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
									<path d="M18 6 6 18M6 6l12 12"/>
								</svg>
							</button>
						</span>
					{/each}
				</div>
			{/if}
		</section>

		<!-- Ausschluss-Zutaten -->
		<section class="section">
			<h2 class="section-title">Diese Zutaten ausschliessen (optional)</h2>
			<div class="ingredient-row">
				<input
					class="ingredient-input"
					type="text"
					placeholder="z. B. Tofu, Sojamilch"
					bind:value={excludedInput}
					onkeydown={handleExcludedKeydown}
				/>
				<button
					class="add-btn"
					onclick={addExcluded}
					disabled={!excludedInput.trim()}
				>
					Hinzufügen
				</button>
			</div>
			{#if excludedIngredients.length > 0}
				<div class="chips">
					{#each excludedIngredients as ingredient, i}
						<span class="chip chip-excluded">
							{ingredient}
							<button class="chip-remove chip-remove-excluded" onclick={() => removeExcluded(i)} aria-label="Entfernen">
								<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
									<path d="M18 6 6 18M6 6l12 12"/>
								</svg>
							</button>
						</span>
					{/each}
				</div>
			{/if}
		</section>
	</div>

	<!-- Bottom Buttons -->
	<div class="bottom-actions">
		<p class="next-step-hint">
			<svg class="hint-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<circle cx="12" cy="12" r="10"/>
				<path d="M12 16v-4"/>
				<path d="M12 8h.01"/>
			</svg>
			<span>Nächster Schritt: Allergien, Zeitaufwand, Schwierigkeit</span>
		</p>
		<div class="bottom-btn-row">
			<button class="btn-cancel" onclick={() => { resetRecipeCreation(); goto('/rezepte'); }}>
				Abbrechen
			</button>
			<button class="btn-proceed" onclick={goToAnpassen} disabled={!canProceed}>
				Weiter
			</button>
		</div>
	</div>
</div>

<style>
	.page {
		padding: 0 0 100px;
	}

	.top-bar {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 16px 16px 12px;
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

	.step-indicator-wrapper {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		padding: 4px 16px 28px;
	}

	.step-indicator {
		font-size: 0.95rem;
		color: var(--color-primary-muted);
		margin: 0;
		letter-spacing: 0.02em;
	}

	.step-progress {
		display: flex;
		gap: 8px;
	}

	.step-bar {
		width: 32px;
		height: 4px;
		border-radius: 2px;
		background: var(--color-border);
	}

	.step-bar-active {
		background: var(--color-primary);
	}

	.content {
		padding: 0 16px;
		display: flex;
		flex-direction: column;
		gap: 32px;
	}

	.section {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.section-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
	}

	.description-input {
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
		box-sizing: border-box;
		outline: none;
	}

	.description-input:focus {
		border-color: var(--color-primary);
	}

	.ingredient-row {
		display: flex;
		gap: 10px;
	}

	.ingredient-input {
		flex: 1;
		min-width: 0;
		border-radius: 14px;
		border: 1.5px solid var(--color-border);
		padding: 12px 16px;
		font-size: 0.9375rem;
		font-family: inherit;
		color: var(--color-text);
		background: white;
		outline: none;
	}

	.ingredient-input:focus {
		border-color: var(--color-primary);
	}

	.add-btn {
		background: white;
		color: var(--color-primary);
		border: 1.5px solid var(--color-primary);
		border-radius: 14px;
		padding: 12px 20px;
		font-weight: 600;
		font-size: 0.875rem;
		cursor: pointer;
		white-space: nowrap;
		flex-shrink: 0;
		transition: background 0.12s ease;
	}

	.add-btn:hover {
		background: var(--color-primary-light);
	}

	.add-btn:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.chips {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
		margin-top: 4px;
	}

	.chip {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		background: var(--color-primary-light);
		color: var(--color-primary);
		padding: 6px 12px;
		border-radius: 20px;
		font-size: 0.8125rem;
		font-weight: 500;
	}

	.chip-excluded {
		background: var(--color-exclude-light);
		color: var(--color-exclude);
	}

	.chip-remove {
		background: none;
		border: none;
		padding: 0;
		color: var(--color-primary);
		cursor: pointer;
		display: flex;
		align-items: center;
		opacity: 0.7;
	}

	.chip-remove:hover {
		opacity: 1;
	}

	.chip-remove-excluded {
		color: var(--color-exclude);
	}

	/* Bottom Actions */
	.bottom-actions {
		position: sticky;
		bottom: 80px;
		display: flex;
		flex-direction: column;
		gap: 16px;
		padding: 16px 16px 28px;
		background: white;
	}

	.next-step-hint {
		font-size: 0.875rem;
		color: var(--color-text-light);
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		margin: 0;
	}

	.hint-icon {
		flex-shrink: 0;
	}

	.bottom-btn-row {
		display: flex;
		gap: 12px;
	}

	.btn-cancel {
		flex: 1;
		background: #F3F4F6;
		color: var(--color-text);
		border: none;
		border-radius: 14px;
		padding: 14px;
		font-weight: 600;
		font-size: 0.9375rem;
		cursor: pointer;
	}

	.btn-proceed {
		flex: 1;
		background: var(--color-primary);
		color: white;
		border: none;
		border-radius: 14px;
		padding: 14px;
		font-weight: 700;
		font-size: 0.9375rem;
		cursor: pointer;
	}

	.btn-proceed:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}
</style>
