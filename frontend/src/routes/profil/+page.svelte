<script>
	import { currentUser, updateCurrentUser } from '$lib/stores/user.js';
	import { logout } from '$lib/stores/auth.js';

	const DIET_LABELS = {
		omnivore: 'Omnivore',
		flexitarian: 'Flexitarisch',
		vegetarian: 'Vegetarisch',
		pescetarian: 'Pescetarisch',
		vegan: 'Vegan',
		transitioning: 'Auf dem Weg zu vegan',
	};
	const GOAL_LABELS = {
		environment: 'Umwelt',
		animals: 'Tierwohl',
		health: 'Gesundheit',
		recipes: 'Rezepte',
		shopping: 'Bewusster einkaufen',
		trying: 'Ausprobieren',
	};
	const PREF_LABELS = {
		highProtein: 'Proteinreich',
		lowCarb: 'Low Carb',
		glutenFree: 'Glutenfrei',
		lowCalorie: 'Kalorienarm',
	};
	const ALLERGY_LABELS = {
		nuts: 'Nüsse',
		soy: 'Soja',
		gluten: 'Gluten',
		sesame: 'Sesam',
		mustard: 'Senf',
		celery: 'Sellerie',
		lupins: 'Lupinen',
	};
	const SUPERMARKET_LABELS = {
		Migros: 'Migros',
		coop: 'Coop',
		aldi: 'Aldi',
		none: 'Wechsle',
	};

	const DIET_OPTIONS = ['omnivore', 'flexitarian', 'vegetarian', 'pescetarian', 'vegan', 'transitioning'];
	const GOAL_OPTIONS = ['environment', 'animals', 'health', 'recipes', 'shopping', 'trying'];
	const PREF_OPTIONS = ['highProtein', 'lowCarb', 'glutenFree', 'lowCalorie'];
	const ALLERGY_OPTIONS = ['nuts', 'soy', 'gluten', 'sesame', 'mustard', 'celery', 'lupins'];
	const SUPERMARKET_OPTIONS = ['Migros', 'coop', 'aldi', 'none'];

	let editMode = $state(false);
	let isSaving = $state(false);
	let editValues = $state({
		firstName: '',
		lastName: '',
		currentDiet: null,
		goals: [],
		dietaryPreferences: [],
		allergies: [],
		customAllergies: '',
		defaultSupermarket: null,
	});

	let initial = $derived(($currentUser?.firstName?.[0] ?? '?').toUpperCase());
	let displayName = $derived.by(() => {
		const u = $currentUser;
		if (!u) return '';
		const ln = u.lastName?.trim();
		return ln ? `${u.firstName} ${ln}` : (u.firstName ?? '');
	});

	function enterEditMode() {
		const u = $currentUser;
		if (!u) return;
		editValues = {
			firstName: u.firstName ?? '',
			lastName: u.lastName ?? '',
			currentDiet: u.currentDiet ?? null,
			goals: [...(u.goals ?? [])],
			dietaryPreferences: [...(u.dietaryPreferences ?? [])],
			allergies: [...(u.allergies ?? [])],
			customAllergies: u.customAllergies ?? '',
			defaultSupermarket: u.defaultSupermarket ?? null,
		};
		editMode = true;
	}

	function cancelEdit() {
		editMode = false;
	}

	async function saveChanges() {
		if (isSaving) return;
		const trimmedFirstName = editValues.firstName.trim();
		if (!trimmedFirstName) return;
		isSaving = true;
		try {
			await updateCurrentUser({
				firstName: trimmedFirstName,
				lastName: editValues.lastName.trim(),
				currentDiet: editValues.currentDiet,
				goals: editValues.goals,
				dietaryPreferences: editValues.dietaryPreferences,
				allergies: editValues.allergies,
				customAllergies: editValues.customAllergies.trim(),
				defaultSupermarket: editValues.defaultSupermarket,
			});
			editMode = false;
		} catch (e) {
			console.error('Profil speichern fehlgeschlagen:', e);
		} finally {
			isSaving = false;
		}
	}

	function toggleMulti(field, value) {
		const arr = editValues[field];
		if (arr.includes(value)) {
			editValues[field] = arr.filter(v => v !== value);
		} else {
			editValues[field] = [...arr, value];
		}
	}

	function setSingle(field, value) {
		editValues[field] = value;
	}

	async function handleLogout() {
		await logout();
	}
</script>

<div class="page">
	<!-- 1. Top header ─────────────────────────────────────────────────────── -->
	<div class="top-bar">
		{#if editMode}
			<button class="icon-btn" onclick={cancelEdit} aria-label="Abbrechen">
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
			</button>
		{:else}
			<div class="icon-btn-placeholder"></div>
		{/if}

		<div></div>

		{#if editMode}
			<button class="icon-btn icon-btn-primary" onclick={saveChanges} disabled={isSaving || !editValues.firstName.trim()} aria-label="Speichern">
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
			</button>
		{:else}
			<button class="icon-btn icon-btn-primary" onclick={enterEditMode} aria-label="Bearbeiten">
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"/>
				</svg>
			</button>
		{/if}
	</div>

	{#if !$currentUser}
		<div class="loader-wrap"><div class="spinner"></div></div>
	{:else}
		<!-- 2. User info ───────────────────────────────────────────────────── -->
		<div class="user-block">
			<div class="avatar">{initial}</div>
			{#if editMode}
				<div class="user-edit-fields">
					<input
						class="text-input"
						type="text"
						placeholder="Vorname"
						bind:value={editValues.firstName}
						autocomplete="given-name"
					/>
					<input
						class="text-input"
						type="text"
						placeholder="Nachname (optional)"
						bind:value={editValues.lastName}
						autocomplete="family-name"
					/>
				</div>
			{:else}
				<span class="user-name">{displayName}</span>
				<span class="user-email">{$currentUser.email ?? ''}</span>
			{/if}
		</div>

		<!-- 3. Über dich ───────────────────────────────────────────────────── -->
		<h2 class="section-title">Über dich</h2>

		<!-- ── Aktuelle Ernährungsweise ─────────────────────────────────── -->
		<div class="info-row">
			<span class="info-label">Aktuelle Ernährungsweise</span>
			{#if editMode}
				<div class="pill-row">
					{#each DIET_OPTIONS as opt}
						<button
							class="pill"
							class:pill-active={editValues.currentDiet === opt}
							onclick={() => setSingle('currentDiet', opt)}
							type="button"
						>{DIET_LABELS[opt]}</button>
					{/each}
				</div>
			{:else if $currentUser.currentDiet}
				<div class="value-with-icon">
					<img class="value-icon" src="/images/onboarding/{$currentUser.currentDiet}.png" alt="" />
					<span class="info-value">{DIET_LABELS[$currentUser.currentDiet] ?? $currentUser.currentDiet}</span>
				</div>
			{:else}
				<span class="info-empty">Keine Angaben</span>
			{/if}
		</div>

		<!-- ── Deine Ziele ──────────────────────────────────────────────── -->
		<div class="info-row">
			<span class="info-label">Deine Ziele</span>
			{#if editMode}
				<div class="pill-row">
					{#each GOAL_OPTIONS as opt}
						<button
							class="pill"
							class:pill-active={editValues.goals.includes(opt)}
							onclick={() => toggleMulti('goals', opt)}
							type="button"
						>{GOAL_LABELS[opt]}</button>
					{/each}
				</div>
			{:else if ($currentUser.goals ?? []).length > 0}
				<div class="chip-row">
					{#each $currentUser.goals as g}
						<span class="chip">
							<img class="chip-icon" src="/images/onboarding/{g}.png" alt="" />
							{GOAL_LABELS[g] ?? g}
						</span>
					{/each}
				</div>
			{:else}
				<span class="info-empty">Keine Angaben</span>
			{/if}
		</div>

		<!-- ── Ernährungsform-Präferenzen ───────────────────────────────── -->
		<div class="info-row">
			<span class="info-label">Ernährungsform-Präferenzen</span>
			{#if editMode}
				<div class="pill-row">
					{#each PREF_OPTIONS as opt}
						<button
							class="pill"
							class:pill-active={editValues.dietaryPreferences.includes(opt)}
							onclick={() => toggleMulti('dietaryPreferences', opt)}
							type="button"
						>{PREF_LABELS[opt]}</button>
					{/each}
				</div>
			{:else if ($currentUser.dietaryPreferences ?? []).length > 0}
				<div class="chip-row">
					{#each $currentUser.dietaryPreferences as p}
						<span class="chip">
							<img class="chip-icon" src="/images/onboarding/{p}.png" alt="" />
							{PREF_LABELS[p] ?? p}
						</span>
					{/each}
				</div>
			{:else}
				<span class="info-empty">Keine Angaben</span>
			{/if}
		</div>

		<!-- ── Allergien ────────────────────────────────────────────────── -->
		<div class="info-row">
			<span class="info-label">Allergien</span>
			{#if editMode}
				<div class="pill-row">
					{#each ALLERGY_OPTIONS as opt}
						<button
							class="pill"
							class:pill-active={editValues.allergies.includes(opt)}
							onclick={() => toggleMulti('allergies', opt)}
							type="button"
						>{ALLERGY_LABELS[opt]}</button>
					{/each}
				</div>
				<input
					class="text-input"
					type="text"
					placeholder="Andere Allergien"
					bind:value={editValues.customAllergies}
				/>
			{:else if ($currentUser.allergies ?? []).length > 0 || ($currentUser.customAllergies ?? '').trim()}
				<div class="chip-row">
					{#each $currentUser.allergies ?? [] as a}
						<span class="chip">
							<img class="chip-icon" src="/images/onboarding/{a}.png" alt="" />
							{ALLERGY_LABELS[a] ?? a}
						</span>
					{/each}
					{#if ($currentUser.customAllergies ?? '').trim()}
						<span class="chip">{$currentUser.customAllergies}</span>
					{/if}
				</div>
			{:else}
				<span class="info-empty">Keine Angaben</span>
			{/if}
		</div>

		<!-- ── Standard-Supermarkt ──────────────────────────────────────── -->
		<div class="info-row">
			<span class="info-label">Standard-Supermarkt</span>
			{#if editMode}
				<div class="pill-row">
					{#each SUPERMARKET_OPTIONS as opt}
						<button
							class="pill"
							class:pill-active={editValues.defaultSupermarket === opt}
							onclick={() => setSingle('defaultSupermarket', opt)}
							type="button"
						>{SUPERMARKET_LABELS[opt]}</button>
					{/each}
				</div>
			{:else if $currentUser.defaultSupermarket}
				<div class="value-with-icon">
					<img class="value-icon-supermarket" src="/images/onboarding/{$currentUser.defaultSupermarket}.png" alt="" />
					<span class="info-value">{SUPERMARKET_LABELS[$currentUser.defaultSupermarket] ?? $currentUser.defaultSupermarket}</span>
				</div>
			{:else}
				<span class="info-empty">Keine Angaben</span>
			{/if}
		</div>

		<!-- 4. Account ─────────────────────────────────────────────────────── -->
		<h2 class="section-title">Account</h2>

		<button class="account-row" onclick={handleLogout}>
			<svg class="account-icon-svg account-icon-primary" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
				<polyline points="16 17 21 12 16 7"/>
				<line x1="21" y1="12" x2="9" y2="12"/>
			</svg>
			<div class="account-text">
				<span class="account-title">Abmelden</span>
				<span class="account-sub">Du wirst zur Anmeldung weitergeleitet</span>
			</div>
			<svg class="account-chevron" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="m9 18 6-6-6-6"/>
			</svg>
		</button>

		<div class="account-row account-row-disabled">
			<svg class="account-icon-svg account-icon-danger" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<polyline points="3 6 5 6 21 6"/>
				<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
				<path d="M10 11v6"/>
				<path d="M14 11v6"/>
				<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
			</svg>
			<div class="account-text">
				<span class="account-title account-title-danger">Konto löschen</span>
				<span class="account-sub">Auf Anfrage – kontaktiere uns für Hilfe</span>
			</div>
		</div>

		<p class="version">Plantify-Me v0.3</p>
	{/if}
</div>

<style>
	.page {
		max-width: 390px;
		margin: 0 auto;
		padding: 0 16px 100px;
		background: var(--color-bg);
	}

	/* ── Top bar ─────────────────────────────────────────────────────── */
	.top-bar {
		position: sticky;
		top: 0;
		z-index: 10;
		display: grid;
		grid-template-columns: 36px 1fr 36px;
		align-items: center;
		gap: 12px;
		padding: 16px 0;
		background: var(--color-bg);
	}


	.icon-btn {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		border: none;
		background: transparent;
		color: var(--color-text);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		padding: 0;
	}

	.icon-btn-primary {
		color: var(--color-primary);
	}

	.icon-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.icon-btn-placeholder {
		width: 36px;
		height: 36px;
	}

	/* ── Loader ──────────────────────────────────────────────────────── */
	.loader-wrap {
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

	/* ── User block ──────────────────────────────────────────────────── */
	.user-block {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8px;
		margin-bottom: 32px;
	}

	.avatar {
		width: 64px;
		height: 64px;
		border-radius: 50%;
		background: var(--color-primary);
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1.5rem;
		font-weight: 700;
		flex-shrink: 0;
		margin-bottom: 4px;
	}

	.user-name {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.user-email {
		font-size: 0.875rem;
		color: var(--color-text-light);
	}

	.user-edit-fields {
		display: flex;
		flex-direction: column;
		gap: 8px;
		width: 100%;
		max-width: 280px;
	}

	/* ── Section title ───────────────────────────────────────────────── */
	.section-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 24px 0 10px;
	}

	/* ── Info rows (minimalist list) ─────────────────────────────────── */
	.info-row {
		display: flex;
		flex-direction: column;
		gap: 8px;
		padding: 14px 0;
		border-bottom: 1px solid var(--color-border);
	}

	.info-row:last-of-type {
		border-bottom: none;
	}

	.info-label {
		font-size: 0.75rem;
		font-weight: 500;
		color: var(--color-text-light);
	}

	.info-value {
		font-size: 0.9375rem;
		color: var(--color-text);
	}

	.info-empty {
		font-size: 0.875rem;
		color: var(--color-text-light);
	}

	/* ── Chips (read-only) ───────────────────────────────────────────── */
	.chip-row {
		display: flex;
		flex-wrap: wrap;
		gap: 6px;
	}

	.chip {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		padding: 6px 12px;
		border-radius: 16px;
		background: transparent;
		border: 1px solid var(--color-border);
		color: var(--color-text);
		font-size: 0.8125rem;
		font-weight: 500;
	}

	.chip-icon {
		width: 16px;
		height: 16px;
		object-fit: contain;
		flex-shrink: 0;
	}

	.value-with-icon {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.value-icon {
		width: 20px;
		height: 20px;
		object-fit: contain;
		flex-shrink: 0;
	}

	.value-icon-supermarket {
		width: 24px;
		height: 18px;
		object-fit: contain;
		flex-shrink: 0;
	}

	/* ── Pills (edit mode) ───────────────────────────────────────────── */
	.pill-row {
		display: flex;
		flex-wrap: wrap;
		gap: 6px;
	}

	.pill {
		display: inline-flex;
		align-items: center;
		padding: 8px 14px;
		border: 1.5px solid var(--color-border);
		border-radius: 18px;
		background: #fff;
		color: var(--color-text);
		font-size: 0.8125rem;
		font-weight: 500;
		cursor: pointer;
		transition: background 0.12s ease, border-color 0.12s ease, color 0.12s ease;
	}

	.pill-active {
		border-color: var(--color-primary);
		background: var(--color-primary-light);
		color: var(--color-primary);
		font-weight: 600;
	}

	/* ── Inputs ──────────────────────────────────────────────────────── */
	.text-input {
		width: 100%;
		padding: 10px 12px;
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

	/* ── Account rows (minimalist) ───────────────────────────────────── */
	.account-row {
		display: flex;
		align-items: center;
		gap: 14px;
		width: 100%;
		padding: 16px 0;
		border: none;
		background: transparent;
		text-align: left;
		cursor: pointer;
		border-bottom: 1px solid var(--color-border);
	}

	.account-row:last-of-type {
		border-bottom: none;
	}

	.account-row-disabled {
		cursor: default;
	}

	.account-icon-svg {
		flex-shrink: 0;
	}

	.account-icon-primary {
		color: var(--color-primary);
	}

	.account-icon-danger {
		color: #ef4444;
	}

	.account-text {
		display: flex;
		flex-direction: column;
		gap: 2px;
		min-width: 0;
		flex: 1;
	}

	.account-title {
		font-size: 1rem;
		font-weight: 500;
		color: var(--color-text);
	}

	.account-title-danger {
		color: #ef4444;
	}

	.account-sub {
		font-size: 0.8125rem;
		color: var(--color-text-light);
	}

	.account-chevron {
		color: var(--color-text-light);
		flex-shrink: 0;
	}

	/* ── Version ─────────────────────────────────────────────────────── */
	.version {
		text-align: center;
		font-size: 0.75rem;
		color: var(--color-text-light);
		margin-top: 24px;
	}
</style>
