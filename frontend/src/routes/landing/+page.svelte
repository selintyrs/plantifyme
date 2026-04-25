<script>
	import { goto } from '$app/navigation';
</script>

<div class="page">
	<div class="hero">
		<div class="logo-circle">
			<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
				<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
			</svg>
		</div>
		<h1>Plantify</h1>
		<p class="tagline">Pflanzliche Alternativen, einfach gemacht.</p>
	</div>

	<div class="actions">
		<button class="btn-primary" onclick={() => goto('/signup')}>Kostenlos starten</button>
		<button class="btn-secondary" onclick={() => goto('/login')}>Ich habe schon ein Konto</button>
	</div>
</div>

<style>
	.page {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		padding: 80px 24px 40px;
		max-width: 390px;
		margin: 0 auto;
	}

	.hero {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16px;
	}

	.logo-circle {
		width: 96px;
		height: 96px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 12px 30px rgba(45, 125, 70, 0.25);
	}

	h1 {
		font-size: 2rem;
		font-weight: 800;
		color: var(--color-primary);
		margin: 8px 0 0;
	}

	.tagline {
		font-size: 1rem;
		color: var(--color-text-light);
		text-align: center;
		margin: 0;
	}

	.actions {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.btn-primary,
	.btn-secondary {
		width: 100%;
		padding: 14px 16px;
		border-radius: 14px;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.15s ease, border-color 0.15s ease;
	}

	.btn-primary {
		background: var(--color-primary);
		color: #fff;
		border: none;
	}

	.btn-primary:active {
		background: var(--color-primary-hover);
	}

	.btn-secondary {
		background: var(--color-bg);
		color: var(--color-primary);
		border: 1.5px solid var(--color-primary);
	}
</style>
