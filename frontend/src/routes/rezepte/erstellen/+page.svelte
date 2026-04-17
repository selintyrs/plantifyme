<script>
	import { goto } from '$app/navigation';
	import { recipeCreationStore, resetRecipeCreation } from '$lib/stores/recipeCreation.js';

	let description = $state('');
	let customIngredients = $state([]);
	let ingredientInput = $state('');

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

	function goToAnpassen() {
		recipeCreationStore.set({
			title: '',
			description,
			customIngredients,
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

	<div class="content">
		<!-- Beschreibe dein Rezept -->
		<section class="section">
			<h2 class="section-title">Beschreibe dein Rezept</h2>
			<p class="section-desc">Beschreibe uns, welche Art von Rezept du erstellen möchtest. Du kannst dabei so detailliert oder allgemein beschreiben, wie du möchtest.</p>
			<textarea
				class="description-input"
				placeholder="Zum Beispiel: Ein schnelles, veganes Nudelgericht mit viel Gemüse und einer cremigen Sosse..."
				bind:value={description}
			></textarea>
		</section>

		<!-- Spezifiere deine Zutaten -->
		<section class="section">
			<h2 class="section-title">Spezifiere deine Zutaten</h2>
			<p class="section-desc">Spezifiziere die Zutaten, die du in deinem Rezept verwenden möchtest. Dieser Schritt ist optional – füge so viele hinzu, wie du möchtest, oder überspringe es einfach.</p>
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
	</div>

	<!-- Bottom Buttons -->
	<div class="bottom-actions">
		<button class="btn-cancel" onclick={() => { resetRecipeCreation(); goto('/rezepte'); }}>
			Abbrechen
		</button>
		<button class="btn-proceed" onclick={goToAnpassen} disabled={!canProceed}>
			Weiter
		</button>
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

	.content {
		padding: 0 16px;
		display: flex;
		flex-direction: column;
		gap: 28px;
	}

	.section {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.section-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0;
	}

	.section-desc {
		font-size: 0.875rem;
		color: var(--color-text-light);
		line-height: 1.5;
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
		background: var(--color-primary);
		color: white;
		border: none;
		border-radius: 14px;
		padding: 12px 20px;
		font-weight: 600;
		font-size: 0.875rem;
		cursor: pointer;
		white-space: nowrap;
		flex-shrink: 0;
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

	/* Bottom Actions */
	.bottom-actions {
		position: sticky;
		bottom: 80px;
		display: flex;
		gap: 12px;
		padding: 16px;
		background: white;
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
