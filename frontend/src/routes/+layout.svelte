<script>
	import '../app.css';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { loadDictionary } from '$lib/stores/dictionary.js';

	let { children } = $props();

	onMount(() => {
		loadDictionary();
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
			href: '/impact',
			label: 'Impact',
			icon: `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/><path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/></svg>`
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
</script>

<div id="app-shell">
	<main>
		{@render children()}
	</main>

	{#if !$page.url.pathname.startsWith('/products')}
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
</div>

<style>
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
</style>
