<!--
	PwaInstallBanner — plattform-spezifischer Install-Hinweis.

	Anzeige nur wenn:
	  - nicht authentifiziert
	  - nicht bereits standalone
	  - nicht in den letzten 30 Tagen dismissed
	  - Browser unterstützt Install (Mode A) ODER ist iOS-Safari (Mode B)
	  - 5 Sekunden seit Mount vergangen

	localStorage-Key 'plantify-pwa-dismissed' speichert ISO-Millisekunden
	als String. Beim Mount checken wir, ob der Wert < 30 Tage alt ist —
	dann bleibt der Banner unsichtbar.
-->
<script>
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { quintOut } from 'svelte/easing';
	import { isAuthenticated } from '$lib/stores/auth.js';

	const STORAGE_KEY = 'plantify-pwa-dismissed';
	const DISMISS_TTL_MS = 30 * 24 * 60 * 60 * 1000;
	const SHOW_DELAY_MS = 5000;

	let mode = $state('hidden');     // 'prompt' | 'ios' | 'hidden'
	let visible = $state(false);
	let deferredPrompt = null;       // BeforeInstallPromptEvent (non-reactive)
	let showTimer = null;

	function isStandalone() {
		if (typeof window === 'undefined') return false;
		return (
			window.matchMedia?.('(display-mode: standalone)').matches ||
			window.navigator.standalone === true
		);
	}

	function isIosSafari() {
		if (typeof navigator === 'undefined') return false;
		const ua = navigator.userAgent;
		const iOS = /iPad|iPhone|iPod/.test(ua);
		const safari = /Safari/.test(ua);
		const notOtherBrowser = !/CriOS|FxiOS|EdgiOS/.test(ua);
		return iOS && safari && notOtherBrowser;
	}

	function isRecentlyDismissed() {
		try {
			const stored = localStorage.getItem(STORAGE_KEY);
			if (!stored) return false;
			const ts = parseInt(stored, 10);
			if (isNaN(ts)) return false;
			return Date.now() - ts < DISMISS_TTL_MS;
		} catch {
			return false;
		}
	}

	function dismiss() {
		try {
			localStorage.setItem(STORAGE_KEY, String(Date.now()));
		} catch {
			// Wenn localStorage blockiert: einfach sliden, Banner kommt next visit wieder.
		}
		visible = false;
		mode = 'hidden';
	}

	async function install() {
		if (!deferredPrompt) return;
		try {
			deferredPrompt.prompt();
			const choice = await deferredPrompt.userChoice;
			// User entscheidet (accepted | dismissed) — entweder wird die App
			// installiert (dann sehen wir den Banner eh nicht mehr wegen standalone)
			// oder der User lehnt ab → Banner verschwindet, 30-Tage-Cooldown setzen.
			if (choice?.outcome === 'dismissed') {
				dismiss();
			} else {
				visible = false;
				mode = 'hidden';
			}
			deferredPrompt = null;
		} catch (e) {
			console.warn('PWA install prompt failed:', e);
			visible = false;
			mode = 'hidden';
		}
	}

	onMount(() => {
		// Frühe Exits — Banner muss gar nicht erst eingerichtet werden.
		if ($isAuthenticated) return;
		if (isStandalone()) return;
		if (isRecentlyDismissed()) return;

		// Listener für Mode A. Muss VOR dem 5-Sek-Timer registriert sein,
		// damit ein bereits gefeuertes Event (browser-internes Pre-Caching)
		// nicht verloren geht.
		const onBeforeInstall = (e) => {
			e.preventDefault();
			deferredPrompt = e;
			if (mode === 'hidden') mode = 'prompt';
		};
		window.addEventListener('beforeinstallprompt', onBeforeInstall);

		// Mode B — iOS Safari kann nicht via Event detected werden.
		if (isIosSafari() && mode === 'hidden') {
			mode = 'ios';
		}

		// 5-Sek-Delay — kein hektisches Banner direkt beim Page-Load.
		showTimer = setTimeout(() => {
			if (mode !== 'hidden') visible = true;
		}, SHOW_DELAY_MS);

		return () => {
			window.removeEventListener('beforeinstallprompt', onBeforeInstall);
			if (showTimer) clearTimeout(showTimer);
		};
	});
</script>

{#if visible && mode !== 'hidden'}
	<div
		class="pwa-banner"
		role="dialog"
		aria-label="App installieren"
		transition:fly={{ y: 100, duration: 350, easing: quintOut }}
	>
		<button class="close-btn" onclick={dismiss} aria-label="Schliessen">
			<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="M18 6 6 18M6 6l12 12"/>
			</svg>
		</button>

		<div class="banner-header">
			<div class="logo-circle">
				<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
					<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
				</svg>
			</div>
			<div class="banner-text">
				{#if mode === 'prompt'}
					<p class="headline">Plantify-Me als App</p>
					<p class="subline">Direkt vom Home-Bildschirm starten — kein Browser-Tab nötig</p>
				{:else}
					<p class="headline">Plantify-Me als App speichern</p>
					<p class="subline">
						Tippe auf
						<svg class="share-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
							<polyline points="16 6 12 2 8 6"/>
							<line x1="12" y1="2" x2="12" y2="15"/>
						</svg>
						und dann "Zum Home-Bildschirm"
					</p>
				{/if}
			</div>
		</div>

		{#if mode === 'prompt'}
			<button class="primary-btn" onclick={install}>
				Installieren
			</button>
		{:else}
			<button class="primary-btn" onclick={dismiss}>
				Verstanden
			</button>
		{/if}
	</div>
{/if}

<style>
	.pwa-banner {
		position: fixed;
		bottom: 0;
		left: 50%;
		transform: translateX(-50%);
		width: 100%;
		max-width: 420px;
		background: var(--color-bg);
		border-radius: 16px 16px 0 0;
		box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
		padding: 16px 20px calc(24px + env(safe-area-inset-bottom));
		z-index: 100;
		box-sizing: border-box;
	}

	.close-btn {
		position: absolute;
		top: 12px;
		right: 12px;
		background: none;
		border: none;
		padding: 6px;
		color: var(--color-text-light);
		cursor: pointer;
		display: flex;
		align-items: center;
		border-radius: 50%;
	}

	.close-btn:active {
		background: var(--color-border);
	}

	.banner-header {
		display: flex;
		align-items: flex-start;
		gap: 12px;
		margin-bottom: 16px;
		padding-right: 28px; /* Platz für Close-Button */
	}

	.logo-circle {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.banner-text {
		flex: 1;
		min-width: 0;
	}

	.headline {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text);
		margin: 0 0 4px;
		line-height: 1.3;
	}

	.subline {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		margin: 0;
		line-height: 1.4;
	}

	.share-icon {
		display: inline-block;
		vertical-align: middle;
		margin: 0 2px;
		color: var(--color-primary);
	}

	.primary-btn {
		width: 100%;
		padding: 12px 16px;
		border-radius: 14px;
		background: var(--color-primary);
		color: #fff;
		border: none;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		transition: background 0.15s ease;
	}

	.primary-btn:active {
		background: var(--color-primary-hover);
	}
</style>
