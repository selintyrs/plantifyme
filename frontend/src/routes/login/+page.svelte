<script>
	import { goto } from '$app/navigation';
	import { login } from '$lib/stores/auth.js';
</script>

<div class="page">
	<div class="logo-wrap">
		<div class="logo-circle">
			<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
				<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
			</svg>
		</div>
		<span class="brand">Plantify</span>
	</div>

	<h1 class="title">Willkommen zurück</h1>
	<p class="subtitle">Melde dich an um weiterzumachen</p>

	<button class="btn-google" onclick={() => login('google-oauth2')}>
		<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24">
			<path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
			<path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
			<path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
			<path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84C6.71 7.31 9.14 5.38 12 5.38z"/>
		</svg>
		<span>Mit Google anmelden</span>
	</button>

	<div class="divider">
		<span>oder</span>
	</div>

	<button class="btn-primary" onclick={() => login()}>
		Mit E-Mail anmelden
	</button>

	<p class="footer-link">
		Noch kein Konto?
		<button class="link-btn" onclick={() => goto('/signup')}>Registrieren</button>
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

	.btn-google,
	.btn-primary {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 10px;
		padding: 14px 16px;
		border-radius: 14px;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.15s ease, border-color 0.15s ease;
	}

	.btn-google {
		background: var(--color-bg);
		color: var(--color-text);
		border: 1.5px solid var(--color-border);
	}

	.btn-google:active {
		border-color: var(--color-primary);
	}

	.btn-primary {
		background: var(--color-primary);
		color: #fff;
		border: none;
		margin-top: 4px;
	}

	.btn-primary:active {
		background: var(--color-primary-hover);
	}

	.divider {
		display: flex;
		align-items: center;
		width: 100%;
		margin: 18px 0;
		color: var(--color-text-light);
		font-size: 0.8125rem;
	}

	.divider::before,
	.divider::after {
		content: '';
		flex: 1;
		height: 1px;
		background: var(--color-border);
	}

	.divider span {
		padding: 0 12px;
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
