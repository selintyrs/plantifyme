<script>
	import { goto } from '$app/navigation';
	import PwaInstallBanner from '$lib/components/PwaInstallBanner.svelte';
</script>

<div class="page">
	<!-- 1. Hero ─────────────────────────────────────────────────────────── -->
	<section class="hero">
		<div class="logo-wrap">
			<div class="logo-circle">
				<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
					<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
				</svg>
			</div>
			<span class="brand">Plantify-Me</span>
		</div>

		<h1 class="hero-title">Veganer einkaufen war nie einfacher</h1>
		<p class="hero-sub">
			Finde pflanzliche Alternativen in Migros, Coop und Aldi und koche inspiriert.
		</p>

		<div class="phone-frame">
			<div class="phone-screen">
				<img src="/images/landing/home.jpg" alt="Plantify-Me Home-Screen" class="phone-screen-img" />
			</div>
		</div>

		<button class="btn-primary" onclick={() => goto('/signup')}>Jetzt kostenlos starten</button>
		<button class="link-btn" onclick={() => goto('/login')}>Schon ein Konto? Anmelden</button>
	</section>

	<!-- 2. Feature highlights ────────────────────────────────────────────── -->
	<section class="section">
		<h2 class="section-title">Was du mit Plantify-Me kannst</h2>
		<div class="feature-list">
			<div class="feature-card">
				<div class="feature-icon-wrap">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/>
						<path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/>
					</svg>
				</div>
				<div class="feature-text">
					<span class="feature-title">Einkaufsliste veganisieren</span>
					<span class="feature-sub">Füge deine Produkte hinzu und wir zeigen dir pflanzliche Alternativen.</span>
				</div>
			</div>

			<div class="feature-card">
				<div class="feature-icon-wrap">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3z"/>
						<path d="M5 3v4"/><path d="M19 17v4"/><path d="M3 5h4"/><path d="M17 19h4"/>
					</svg>
				</div>
				<div class="feature-text">
					<span class="feature-title">KI-gestützte Rezepte</span>
					<span class="feature-sub">Lass dir personalisierte Rezepte erstellen oder wähle aus 60+ Vorlagen.</span>
				</div>
			</div>

			<div class="feature-card">
				<div class="feature-icon-wrap">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<circle cx="12" cy="12" r="10"/>
						<path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
						<path d="M2 12h20"/>
					</svg>
				</div>
				<div class="feature-text">
					<span class="feature-title">Dein Impact sichtbar</span>
					<span class="feature-sub">Sieh wie viele Tiere, Wasser und CO₂ du sparst.</span>
				</div>
			</div>
		</div>
	</section>

	<!-- 3. How it works ──────────────────────────────────────────────────── -->
	<section class="section">
		<h2 class="section-title">So funktioniert es</h2>
		<div class="steps-row">
			<div class="step-card">
				<span class="step-number">1</span>
				<span class="step-title">Liste erstellen</span>
				<span class="step-sub">Füge deine Produkte hinzu</span>
			</div>
			<div class="step-card">
				<span class="step-number">2</span>
				<span class="step-title">Plantify-Me</span>
				<span class="step-sub">Veganisieren mit einem Klick</span>
			</div>
			<div class="step-card">
				<span class="step-number">3</span>
				<span class="step-title">Einkaufen</span>
				<span class="step-sub">Bei deinem Supermarkt</span>
			</div>
		</div>
	</section>

	<!-- 4. Visual transformation ─────────────────────────────────────────── -->
	<section class="section">
		<h2 class="section-title">Aus deiner normalen Einkaufsliste zu einer plantified Einkaufsliste</h2>
		<div class="transform-row">
			<div class="mini-phone">
				<img src="/images/landing/nicht-veganisiert.jpg" alt="Normale Einkaufsliste vor Veganisierung" class="mini-phone-img" />
			</div>
			<svg class="transform-arrow" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m9 18 6-6-6-6"/>
			</svg>
			<div class="mini-phone">
				<img src="/images/landing/veganisiert.jpg" alt="Veganisierte Einkaufsliste mit pflanzlichen Alternativen" class="mini-phone-img" />
			</div>
		</div>
	</section>

	<!-- 5. CTA repeat ────────────────────────────────────────────────────── -->
	<section class="section">
		<div class="cta-card">
			<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
				<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
			</svg>
			<h3 class="cta-title">Starte deine pflanzliche Reise</h3>
			<p class="cta-sub">Kostenlos und einfach</p>
			<button class="cta-btn" onclick={() => goto('/signup')}>Jetzt ausprobieren</button>
		</div>
	</section>

	<!-- 6. Footer ────────────────────────────────────────────────────────── -->
	<footer class="footer">
		<div class="footer-logo">
			<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
				<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
				<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
			</svg>
		</div>
		<div class="footer-links">
			<button class="footer-link" onclick={() => goto('/info/ueber-uns')}>Über uns</button>
			<button class="footer-link" onclick={() => goto('/info/impressum')}>Impressum</button>
			<button class="footer-link" onclick={() => goto('/info/datenschutz')}>Datenschutz</button>
		</div>
		<span class="copyright">© 2026 Plantify-Me</span>
	</footer>
</div>

<PwaInstallBanner />

<style>
	.page {
		max-width: 390px;
		margin: 0 auto;
		padding: 0 0 32px;
		background: var(--color-bg);
	}

	/* ── Hero ─────────────────────────────────────────────────────────── */
	.hero {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 14px;
		padding: 32px 20px 16px;
	}

	.logo-wrap {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8px;
		margin-bottom: 8px;
	}

	.logo-circle {
		width: 64px;
		height: 64px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8px 24px rgba(45, 125, 70, 0.25);
	}

	.brand {
		font-size: 1.125rem;
		font-weight: 800;
		color: var(--color-primary);
	}

	.hero-title {
		font-size: 1.875rem;
		font-weight: 700;
		line-height: 1.2;
		color: var(--color-text);
		text-align: center;
		margin: 4px 0 0;
	}

	.hero-sub {
		font-size: 1rem;
		line-height: 1.5;
		color: var(--color-text-light);
		text-align: center;
		margin: 0;
		max-width: 320px;
	}

	.phone-frame {
		margin: 16px 0 8px;
		background: #1a1a1a;
		padding: 8px;
		border-radius: 32px;
		box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
	}

	.phone-screen {
		width: 290px;
		border-radius: 24px;
		overflow: hidden;
		display: block;
	}

	.phone-screen-img {
		width: 100%;
		height: auto;
		display: block;
	}

	.btn-primary {
		width: 100%;
		padding: 16px;
		border-radius: 14px;
		background: var(--color-primary);
		color: #fff;
		border: none;
		font-size: 1rem;
		font-weight: 700;
		cursor: pointer;
		margin-top: 8px;
		transition: background 0.12s ease;
	}

	.btn-primary:active {
		background: var(--color-primary-hover);
	}

	.link-btn {
		background: none;
		border: none;
		color: var(--color-text-light);
		font-size: 0.875rem;
		cursor: pointer;
		padding: 4px 0;
	}

	.link-btn:hover {
		color: var(--color-primary);
	}

	/* ── Sections ─────────────────────────────────────────────────────── */
	.section {
		padding: 24px 20px 8px;
	}

	.section-title {
		font-size: 1.25rem;
		font-weight: 800;
		color: var(--color-text);
		margin: 0 0 14px;
		line-height: 1.3;
	}

	/* ── Feature cards ────────────────────────────────────────────────── */
	.feature-list {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.feature-card {
		display: flex;
		align-items: flex-start;
		gap: 14px;
		background: #fff;
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		padding: 16px;
	}

	.feature-icon-wrap {
		width: 40px;
		height: 40px;
		flex-shrink: 0;
		background: var(--color-primary-light);
		color: var(--color-primary);
		border-radius: 12px;
		padding: 8px;
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
	}

	.feature-text {
		display: flex;
		flex-direction: column;
		gap: 4px;
		min-width: 0;
	}

	.feature-title {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.feature-sub {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		line-height: 1.5;
	}

	/* ── Steps row ────────────────────────────────────────────────────── */
	.steps-row {
		display: flex;
		gap: 8px;
	}

	.step-card {
		flex: 1;
		min-width: 0;
		background: var(--color-primary-light);
		border-radius: 14px;
		padding: 14px 10px;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		text-align: center;
	}

	.step-number {
		font-size: 1.75rem;
		font-weight: 800;
		color: var(--color-primary);
		line-height: 1;
	}

	.step-title {
		font-size: 0.8125rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.step-sub {
		font-size: 0.75rem;
		color: var(--color-text-light);
		line-height: 1.3;
	}

	/* ── Transform row ────────────────────────────────────────────────── */
	.transform-row {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
	}

	.mini-phone {
		width: 160px;
		flex-shrink: 0;
		display: block;
	}

	.mini-phone-img {
		width: 100%;
		height: auto;
		display: block;
	}

	.transform-arrow {
		color: var(--color-primary);
		flex-shrink: 0;
	}

	/* ── CTA card ─────────────────────────────────────────────────────── */
	.cta-card {
		background: var(--color-primary);
		border-radius: 20px;
		padding: 32px 20px;
		text-align: center;
		color: #fff;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
	}

	.cta-title {
		font-size: 1.25rem;
		font-weight: 700;
		margin: 4px 0 0;
		line-height: 1.3;
	}

	.cta-sub {
		font-size: 0.9375rem;
		opacity: 0.9;
		margin: 0 0 6px;
	}

	.cta-btn {
		padding: 14px 28px;
		border-radius: 12px;
		background: #fff;
		color: var(--color-primary);
		border: none;
		font-size: 0.9375rem;
		font-weight: 700;
		cursor: pointer;
	}

	/* ── Footer ───────────────────────────────────────────────────────── */
	.footer {
		margin-top: 16px;
		padding: 24px 16px;
		border-top: 1px solid var(--color-border);
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
	}

	.footer-logo {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.footer-links {
		display: flex;
		gap: 16px;
	}

	.footer-link {
		background: none;
		border: none;
		color: var(--color-text-light);
		font-size: 0.8125rem;
		cursor: pointer;
		padding: 0;
	}

	.footer-link:hover {
		color: var(--color-primary);
	}

	.copyright {
		font-size: 0.75rem;
		color: var(--color-text-light);
	}
</style>
