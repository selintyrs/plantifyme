<script>
	import { goto } from '$app/navigation';
	import { signup } from '$lib/stores/auth.js';
</script>

<div class="page">
	<div class="logo-wrap">
		<div class="logo-circle">
			<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
				<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
			</svg>
		</div>
		<span class="brand">Plantify-Me</span>
	</div>

	<h1 class="title">Werde Teil von Plantify-Me</h1>
	<p class="subtitle">Starte deine pflanzliche Reise</p>

	<button class="btn-primary" onclick={() => signup()}>
		Mit E-Mail registrieren
	</button>

	<p class="footer-link">
		Schon ein Konto?
		<button class="link-btn" onclick={() => goto('/login')}>Anmelden</button>
	</p>
</div>

<style>
	.page {
		min-height: 100vh;
		padding: 60px 24px 32px;
		display: flex;
		flex-direction: column;
		align-items: center;
		max-width: 390px;
		margin: 0 auto;
	}

	.logo-wrap {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		margin-bottom: 40px;
	}

	.logo-circle {
		width: 80px;
		height: 80px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.brand {
		font-size: 1.5rem;
		font-weight: 800;
		color: var(--color-primary);
	}

	.title {
		font-size: 1.5rem;
		font-weight: 800;
		color: var(--color-text);
		margin: 0 0 8px;
		text-align: center;
	}

	.subtitle {
		font-size: 0.9375rem;
		color: var(--color-text-light);
		margin: 0 0 32px;
		text-align: center;
	}

	.btn-primary {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 14px 16px;
		border-radius: 14px;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		background: var(--color-primary);
		color: #fff;
		border: none;
		transition: background 0.15s ease;
	}

	.btn-primary:active {
		background: var(--color-primary-hover);
	}

	.footer-link {
		margin-top: 32px;
		font-size: 0.875rem;
		color: var(--color-text-light);
		text-align: center;
	}

	.link-btn {
		background: none;
		border: none;
		color: var(--color-primary);
		font-weight: 600;
		font-size: 0.875rem;
		cursor: pointer;
		padding: 0 0 0 4px;
	}
</style>
