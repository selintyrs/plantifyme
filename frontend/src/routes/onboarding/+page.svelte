<script>
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { currentUser, updateCurrentUser } from '$lib/stores/user.js';

	const TOTAL_STEPS = 8;

	let currentStep = $state(1);
	let isSubmitting = $state(false);

	let answers = $state({
		firstName: '',
		lastName: '',
		currentDiet: null,
		goals: [],
		dietaryPreferences: [],
		allergies: [],
		customAllergies: '',
		defaultSupermarket: null,
	});

	onMount(() => {
		const user = $currentUser;
		if (user) {
			answers.firstName = user.firstName || '';
			answers.lastName = user.lastName || '';
		}
	});

	const dietOptions = [
		{ value: 'omnivore',      emoji: '🍖', label: 'Omnivore',                  sub: 'Ich esse alles' },
		{ value: 'flexitarian',   emoji: '🥗', label: 'Flexitarisch',              sub: 'Ich esse manchmal Fleisch' },
		{ value: 'vegetarian',    emoji: '🧀', label: 'Vegetarisch',               sub: 'Kein Fleisch, kein Fisch' },
		{ value: 'pescetarian',   emoji: '🐟', label: 'Pescetarisch',              sub: 'Kein Fleisch, aber Fisch' },
		{ value: 'vegan',         emoji: '🌱', label: 'Vegan',                     sub: 'Rein pflanzlich' },
		{ value: 'transitioning', emoji: '🌿', label: 'Auf dem Weg zu vegan',      sub: 'Ich reduziere tierische Produkte' },
	];

	const goalOptions = [
		{ value: 'environment', emoji: '🌍', label: 'Umwelt',              sub: 'Aus Umweltgründen' },
		{ value: 'animals',     emoji: '🐮', label: 'Tierwohl',            sub: 'Aus Tierschutz-Gründen' },
		{ value: 'health',      emoji: '💪', label: 'Gesundheit',          sub: 'Für meine Gesundheit' },
		{ value: 'recipes',     emoji: '🍳', label: 'Rezepte',             sub: 'Neue Rezepte entdecken' },
		{ value: 'shopping',    emoji: '🛒', label: 'Bewusster einkaufen', sub: 'Bewusster einkaufen' },
		{ value: 'trying',      emoji: '✨', label: 'Ausprobieren',        sub: 'Einfach mal ausprobieren' },
	];

	const dietaryPrefOptions = [
		{ value: 'highProtein', emoji: '💪', label: 'Proteinreich', sub: 'Viel Eiweiss' },
		{ value: 'lowCarb',     emoji: '🥗', label: 'Low Carb',     sub: 'Wenig Kohlenhydrate' },
		{ value: 'glutenFree',  emoji: '🌾', label: 'Glutenfrei',   sub: 'Ohne Gluten' },
		{ value: 'lowCalorie',  emoji: '🥬', label: 'Kalorienarm',  sub: 'Leichte Küche' },
	];

	const allergyOptions = [
		{ value: 'nuts',    emoji: '🥜', label: 'Nüsse' },
		{ value: 'soy',     emoji: '🌱', label: 'Soja' },
		{ value: 'gluten',  emoji: '🌾', label: 'Gluten' },
		{ value: 'sesame',  emoji: '🫘', label: 'Sesam' },
		{ value: 'mustard', emoji: '🌿', label: 'Senf' },
		{ value: 'celery',  emoji: '🥬', label: 'Sellerie' },
		{ value: 'lupins',  emoji: '🍃', label: 'Lupinen' },
	];

	const supermarketOptions = [
		{ value: 'Migros', emoji: '🟧', label: 'Migros' },
		{ value: 'coop',   emoji: '🔴', label: 'Coop' },
		{ value: 'aldi',   emoji: '🔵', label: 'Aldi' },
		{ value: 'none',   emoji: '🔄', label: 'Ich wechsle' },
	];

	let canProceed = $derived.by(() => {
		if (currentStep === 1 || currentStep === 8) return true;
		if (currentStep === 2) return answers.firstName.trim().length > 0;
		if (currentStep === 3) return answers.currentDiet !== null;
		if (currentStep === 4) return answers.goals.length > 0;
		if (currentStep === 5) return true;
		if (currentStep === 6) return true;
		if (currentStep === 7) return answers.defaultSupermarket !== null;
		return false;
	});

	let progressPct = $derived.by(() => {
		if (currentStep <= 1) return 0;
		if (currentStep >= 8) return 100;
		return ((currentStep - 1) / 6) * 100;
	});

	let showProgress = $derived(currentStep >= 2 && currentStep <= 7);
	let progressLabel = $derived(`Schritt ${currentStep - 1} von 6`);

	function selectDiet(value) {
		answers.currentDiet = value;
	}

	function toggleMulti(field, value) {
		const arr = answers[field];
		if (arr.includes(value)) {
			answers[field] = arr.filter(v => v !== value);
		} else {
			answers[field] = [...arr, value];
		}
	}

	function selectSupermarket(value) {
		answers.defaultSupermarket = value;
	}

	async function handleNext() {
		if (!canProceed || isSubmitting) return;

		if (currentStep === TOTAL_STEPS) {
			isSubmitting = true;
			try {
				await updateCurrentUser({
					...answers,
					firstName: answers.firstName.trim(),
					lastName: answers.lastName.trim() || null,
					onboardingCompleted: true,
				});
				goto('/');
			} catch (e) {
				console.error('Onboarding-Submit fehlgeschlagen:', e);
				isSubmitting = false;
			}
			return;
		}

		currentStep++;
	}

	function handleBack() {
		if (currentStep > 1) currentStep--;
	}
</script>

<div class="page">
	{#if showProgress}
		<div class="progress-bar-wrap">
			<div class="progress-track">
				<div class="progress-fill" style="width: {progressPct}%"></div>
			</div>
			<span class="progress-label">{progressLabel}</span>
		</div>
	{/if}

	<div class="content" class:content-no-progress={!showProgress}>
		{#if currentStep === 1}
			<div class="hero">
				<div class="logo-circle">
					<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
						<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
					</svg>
				</div>
				<h1>Willkommen bei Plantify-Me!</h1>
				<p class="hero-sub">
					Wir stellen dir ein paar Fragen, damit wir dir die besten Empfehlungen geben können.
					Dauert nur 2 Minuten.
				</p>
			</div>
		{:else if currentStep === 2}
			<div class="step">
				<h2>Wie heisst du?</h2>
				<p class="step-sub">Damit wir dich richtig begrüssen können</p>
				<div class="form-fields">
					<label class="field-label" for="first-name-input">Vorname</label>
					<input
						id="first-name-input"
						class="text-input"
						type="text"
						placeholder="Max"
						bind:value={answers.firstName}
						autocomplete="given-name"
					/>
					<label class="field-label" for="last-name-input">Nachname <span class="optional-tag">(optional)</span></label>
					<input
						id="last-name-input"
						class="text-input"
						type="text"
						placeholder="Mustermann"
						bind:value={answers.lastName}
						autocomplete="family-name"
					/>
				</div>
			</div>
		{:else if currentStep === 3}
			<div class="step">
				<h2>Wie ernährst du dich aktuell?</h2>
				<div class="card-list">
					{#each dietOptions as opt}
						<button
							class="opt-card"
							class:opt-selected={answers.currentDiet === opt.value}
							onclick={() => selectDiet(opt.value)}
							type="button"
						>
							<img
							src="/images/onboarding/{opt.value}.png"
							alt={opt.label}
							width="32"
							height="32"
							class="option-icon"
						/>
							<div class="opt-text">
								<span class="opt-name">{opt.label}</span>
								<span class="opt-sub">{opt.sub}</span>
							</div>
							<div class="opt-radio" class:opt-radio-checked={answers.currentDiet === opt.value}>
								{#if answers.currentDiet === opt.value}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
						</button>
					{/each}
				</div>
			</div>
		{:else if currentStep === 4}
			<div class="step">
				<h2>Warum bist du hier?</h2>
				<p class="step-sub">Wähle eine oder mehrere</p>
				<div class="card-list">
					{#each goalOptions as opt}
						{@const checked = answers.goals.includes(opt.value)}
						<button
							class="opt-card"
							class:opt-selected={checked}
							onclick={() => toggleMulti('goals', opt.value)}
							type="button"
						>
							<img
							src="/images/onboarding/{opt.value}.png"
							alt={opt.label}
							width="32"
							height="32"
							class="option-icon"
						/>
							<div class="opt-text">
								<span class="opt-name">{opt.label}</span>
								<span class="opt-sub">{opt.sub}</span>
							</div>
							<div class="opt-checkbox" class:opt-checkbox-checked={checked}>
								{#if checked}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
						</button>
					{/each}
				</div>
			</div>
		{:else if currentStep === 5}
			<div class="step">
				<h2>Was sind deine Ernährungs-Schwerpunkte?</h2>
				<p class="step-sub">Optional — wir nutzen das für Rezept-Empfehlungen</p>
				<div class="card-list">
					{#each dietaryPrefOptions as opt}
						{@const checked = answers.dietaryPreferences.includes(opt.value)}
						<button
							class="opt-card"
							class:opt-selected={checked}
							onclick={() => toggleMulti('dietaryPreferences', opt.value)}
							type="button"
						>
							<img
							src="/images/onboarding/{opt.value}.png"
							alt={opt.label}
							width="32"
							height="32"
							class="option-icon"
						/>
							<div class="opt-text">
								<span class="opt-name">{opt.label}</span>
								<span class="opt-sub">{opt.sub}</span>
							</div>
							<div class="opt-checkbox" class:opt-checkbox-checked={checked}>
								{#if checked}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
						</button>
					{/each}
				</div>
			</div>
		{:else if currentStep === 6}
			<div class="step">
				<h2>Hast du Allergien oder Unverträglichkeiten?</h2>
				<p class="step-sub">Wir filtern Rezepte und Produkte entsprechend</p>
				<div class="card-list">
					{#each allergyOptions as opt}
						{@const checked = answers.allergies.includes(opt.value)}
						<button
							class="opt-card"
							class:opt-selected={checked}
							onclick={() => toggleMulti('allergies', opt.value)}
							type="button"
						>
							<img
							src="/images/onboarding/{opt.value}.png"
							alt={opt.label}
							width="32"
							height="32"
							class="option-icon"
						/>
							<div class="opt-text">
								<span class="opt-name">{opt.label}</span>
							</div>
							<div class="opt-checkbox" class:opt-checkbox-checked={checked}>
								{#if checked}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
						</button>
					{/each}
				</div>
				<label class="custom-allergies-label" for="custom-allergies-input">Andere Allergien</label>
				<input
					id="custom-allergies-input"
					class="custom-allergies-input"
					type="text"
					placeholder="z.B. Erdbeeren, Pfirsiche"
					bind:value={answers.customAllergies}
				/>
			</div>
		{:else if currentStep === 7}
			<div class="step">
				<h2>Wo kaufst du am häufigsten ein?</h2>
				<p class="step-sub">Wir zeigen dir Alternativen aus diesem Supermarkt</p>
				<div class="card-list">
					{#each supermarketOptions as opt}
						<button
							class="opt-card"
							class:opt-selected={answers.defaultSupermarket === opt.value}
							onclick={() => selectSupermarket(opt.value)}
							type="button"
						>
							<img
								src="/images/onboarding/{opt.value}.png"
								alt={opt.label}
								width="48"
								height="32"
								class="supermarket-icon"
							/>
							<div class="opt-text">
								<span class="opt-name">{opt.label}</span>
							</div>
							<div class="opt-radio" class:opt-radio-checked={answers.defaultSupermarket === opt.value}>
								{#if answers.defaultSupermarket === opt.value}
									<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
								{/if}
							</div>
						</button>
					{/each}
				</div>
			</div>
		{:else if currentStep === 8}
			<div class="hero">
				<div class="success-circle">
					<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
						<polyline points="20 6 9 17 4 12"/>
					</svg>
				</div>
				<h1>Du bist startklar!</h1>
				<p class="hero-sub">Lass uns gemeinsam pflanzliche Alternativen entdecken.</p>
			</div>
		{/if}
	</div>

	<div class="actions">
		{#if currentStep === 1 || currentStep === 8}
			<button class="btn-primary btn-full" onclick={handleNext} disabled={isSubmitting}>
				{currentStep === 8 ? (isSubmitting ? 'Wird gespeichert…' : 'Los geht\'s') : 'Weiter'}
			</button>
		{:else}
			<button class="btn-secondary" onclick={handleBack}>Zurück</button>
			<button class="btn-primary" onclick={handleNext} disabled={!canProceed}>Weiter</button>
		{/if}
	</div>
</div>

<style>
	.page {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		max-width: 390px;
		margin: 0 auto;
		background: var(--color-bg);
	}

	/* ── Progress bar ──────────────────────────────────────────────────── */
	.progress-bar-wrap {
		position: sticky;
		top: 0;
		z-index: 10;
		background: var(--color-bg);
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 6px;
	}

	.progress-track {
		width: 100%;
		height: 6px;
		background: var(--color-border);
		border-radius: 3px;
		overflow: hidden;
	}

	.progress-fill {
		height: 100%;
		background: var(--color-primary);
		border-radius: 3px;
		transition: width 0.25s ease;
	}

	.progress-label {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--color-text-light);
		letter-spacing: 0.04em;
	}

	/* ── Content area ──────────────────────────────────────────────────── */
	.content {
		flex: 1;
		padding: 16px 20px 100px;
		overflow-y: auto;
	}

	.content-no-progress {
		padding-top: 40px;
	}

	/* ── Hero (Step 1 + 8) ─────────────────────────────────────────────── */
	.hero {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16px;
		padding-top: 60px;
	}

	.logo-circle,
	.success-circle {
		width: 96px;
		height: 96px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 12px 30px rgba(45, 125, 70, 0.25);
	}

	.hero h1 {
		font-size: 1.75rem;
		font-weight: 800;
		color: var(--color-text);
		text-align: center;
		margin: 8px 0 0;
	}

	.hero-sub {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		text-align: center;
		line-height: 1.5;
		margin: 0;
		max-width: 320px;
	}

	/* ── Step layout ───────────────────────────────────────────────────── */
	.step {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	.step h2 {
		font-size: 1.375rem;
		font-weight: 800;
		color: var(--color-text);
		margin: 0;
		line-height: 1.3;
	}

	.step-sub {
		font-size: 0.875rem;
		color: var(--color-text-light);
		margin: -8px 0 4px;
	}

	/* ── Name form ─────────────────────────────────────────────────────── */
	.form-fields {
		display: flex;
		flex-direction: column;
		gap: 6px;
	}

	.field-label {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-light);
		margin-top: 8px;
	}

	.optional-tag {
		font-weight: 400;
		color: var(--color-text-light);
		opacity: 0.7;
	}

	.text-input {
		width: 100%;
		padding: 12px 14px;
		border: 1.5px solid var(--color-border);
		border-radius: 12px;
		font-size: 0.9375rem;
		color: var(--color-text);
		background: var(--color-bg);
		outline: none;
	}

	.text-input:focus {
		border-color: var(--color-primary);
	}

	/* ── Option cards ──────────────────────────────────────────────────── */
	.card-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.opt-card {
		display: flex;
		align-items: center;
		gap: 14px;
		width: 100%;
		padding: 14px 16px;
		border: 1.5px solid var(--color-border);
		border-radius: 14px;
		background: var(--color-bg);
		text-align: left;
		cursor: pointer;
		transition: border-color 0.12s ease, background 0.12s ease;
	}

	.opt-card:hover {
		border-color: var(--color-primary);
	}

	.opt-selected {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
	}

	.option-icon {
		width: 32px;
		height: 32px;
		object-fit: contain;
		flex-shrink: 0;
	}

	.supermarket-icon {
		width: 48px;
		height: 32px;
		object-fit: contain;
		flex-shrink: 0;
	}

	.opt-text {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
	}

	.opt-name {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.opt-sub {
		font-size: 0.8125rem;
		color: var(--color-text-light);
	}

	.opt-checkbox,
	.opt-radio {
		width: 22px;
		height: 22px;
		border: 1.5px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		transition: background 0.12s ease, border-color 0.12s ease;
	}

	.opt-checkbox {
		border-radius: 6px;
	}

	.opt-radio {
		border-radius: 50%;
	}

	.opt-checkbox-checked,
	.opt-radio-checked {
		background: var(--color-primary);
		border-color: var(--color-primary);
	}

	/* ── Custom allergies input ────────────────────────────────────────── */
	.custom-allergies-label {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-light);
		margin-top: 8px;
	}

	.custom-allergies-input {
		width: 100%;
		padding: 12px 14px;
		border: 1.5px solid var(--color-border);
		border-radius: 12px;
		font-size: 0.9375rem;
		color: var(--color-text);
		background: var(--color-bg);
		outline: none;
	}

	.custom-allergies-input:focus {
		border-color: var(--color-primary);
	}

	/* ── Bottom actions ────────────────────────────────────────────────── */
	.actions {
		position: sticky;
		bottom: 0;
		display: flex;
		gap: 10px;
		padding: 12px 16px 24px;
		background: var(--color-bg);
		border-top: 1px solid var(--color-border);
	}

	.btn-primary,
	.btn-secondary {
		padding: 14px 16px;
		border-radius: 14px;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.12s ease, opacity 0.12s ease;
		flex: 1;
	}

	.btn-primary {
		background: var(--color-primary);
		color: #fff;
		border: none;
	}

	.btn-primary:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.btn-primary:not(:disabled):active {
		background: var(--color-primary-hover);
	}

	.btn-secondary {
		background: var(--color-bg);
		color: var(--color-primary);
		border: 1.5px solid var(--color-primary);
	}

	.btn-full {
		flex: 1 1 100%;
	}
</style>
