<script>
	import '../app.css';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { loadDictionary } from '$lib/stores/dictionary.js';
	import { initAuth, isAuthenticated } from '$lib/stores/auth.js';
	import { loadCurrentUser } from '$lib/stores/user.js';

	let { children } = $props();

	let initialized = $state(false);

	const PUBLIC_PREFIXES = ['/landing', '/login', '/signup'];
	const PUBLIC_EXACT = ['/info/ueber-uns', '/info/impressum', '/info/datenschutz'];

	function isPublicPath(path) {
		if (PUBLIC_EXACT.includes(path)) return true;
		return PUBLIC_PREFIXES.some(r => path.startsWith(r));
	}

	onMount(async () => {
		await initAuth();

		const path = $page.url.pathname;
		const isPublic = isPublicPath(path);
		const isOnboarding = path.startsWith('/onboarding');

		if (!$isAuthenticated && !isPublic) {
			goto('/landing');
			initialized = true;
			return;
		}
		if ($isAuthenticated && isPublic) {
			goto('/');
			initialized = true;
			return;
		}

		if ($isAuthenticated) {
			const user = await loadCurrentUser();
			if (user && !user.onboardingCompleted && !isOnboarding) {
				goto('/onboarding');
				initialized = true;
				return;
			}
			if (user && user.onboardingCompleted && isOnboarding) {
				goto('/');
				initialized = true;
				return;
			}
			loadDictionary();
		}

		initialized = true;
	});

	const navItems = [
		{
			href: '/',
			label: 'Home',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`
		},
		{
			href: '/liste',
			label: 'Liste',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/></svg>`
		},
		{
			href: '/rezepte',
			label: 'Rezepte',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`
		},
		{
			href: '/info',
			label: 'Info',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>`
		},
		{
			href: '/profil',
			label: 'Profil',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 1 0-16 0"/></svg>`
		}
	];

	function isActive(href, currentPath) {
		if (href === '/') return currentPath === '/';
		return currentPath.startsWith(href);
	}

	let showBottomNav = $derived(
		!$page.url.pathname.startsWith('/products') &&
		!$page.url.pathname.startsWith('/onboarding') &&
		!isPublicPath($page.url.pathname)
	);

	const TOP_BAR_ROOTS = ['/', '/liste', '/shoppinglists', '/rezepte', '/info', '/profil'];

	function showTopBarFor(path) {
		if (isPublicPath(path)) return false;
		if (path.startsWith('/onboarding')) return false;
		if (path.startsWith('/products')) return false;
		// Tab-Roots OK, aber Detail-Sub-Routen mit eigenem Back-Button NICHT
		if (path.startsWith('/rezepte/')) return false;
		if (path.startsWith('/info/')) return false;
		return TOP_BAR_ROOTS.includes(path);
	}

	let showTopBar = $derived(showTopBarFor($page.url.pathname));
</script>

<div id="app-shell">
	{#if initialized}
		{#if showTopBar}
			<header class="top-bar-global">
				<span class="top-bar-brand">Plantify-Me</span>
			</header>
		{/if}

		<main class:has-top-bar={showTopBar}>
			{@render children()}
		</main>

		{#if showBottomNav}
			<nav class="bottom-nav">
				{#each navItems as item}
					<a
						href={item.href}
						class="nav-item"
						class:active={isActive(item.href, $page.url.pathname)}
						aria-label={item.label}
					>
						{@html item.icon}
						<span>{item.label}</span>
					</a>
				{/each}
			</nav>
		{/if}
	{:else}
		<div class="loading-screen">
			<div class="spinner"></div>
		</div>
	{/if}
</div>

<style>
	.top-bar-global {
		position: sticky;
		top: 0;
		z-index: 50;
		height: var(--top-bar-height);
		display: flex;
		align-items: center;
		padding: 0 16px;
		background: var(--color-bg);
		border-bottom: 1px solid var(--color-border);
	}

	.top-bar-brand {
		font-size: 1.25rem;
		font-weight: 800;
		color: var(--color-primary);
		letter-spacing: -0.01em;
	}

	main.has-top-bar {
		padding-top: 0;
	}

	.bottom-nav {
		position: fixed;
		bottom: 0;
		left: 50%;
		transform: translateX(-50%);
		width: 100%;
		max-width: 390px;
		display: flex;
		justify-content: space-around;
		align-items: center;
		background-color: var(--color-bg);
		border-top: 1px solid var(--color-border);
		padding: 12px 24px 24px;
		z-index: 100;
	}

	.nav-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 4px;
		color: #9CA3AF;
		font-size: 10px;
		font-weight: 400;
		transition: color 0.15s ease;
	}

	.nav-item.active {
		color: var(--color-primary);
		font-weight: 500;
	}

	.nav-item :global(svg) {
		width: 24px;
		height: 24px;
	}

	.loading-screen {
		position: fixed;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		background: var(--color-bg);
	}

	.spinner {
		width: 40px;
		height: 40px;
		border: 3px solid var(--color-border);
		border-top-color: var(--color-primary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
