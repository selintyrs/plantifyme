<script>
	import { goto } from '$app/navigation';
</script>

<div class="page">
	<header class="top-bar">
		<button class="back-btn" onclick={() => goto('/landing')} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="m15 18-6-6 6-6"/>
			</svg>
		</button>
		<h1>Datenschutz</h1>
	</header>

	<div class="content">
		<p>Inhalt folgt</p>
	</div>
</div>

<style>
	.page {
		max-width: 390px;
		margin: 0 auto;
		min-height: 100vh;
		background: var(--color-bg);
	}

	.top-bar {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 20px 16px 16px;
		position: sticky;
		top: 0;
		background: var(--color-bg);
		border-bottom: 1px solid var(--color-border);
	}

	.back-btn {
		flex-shrink: 0;
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
		padding: 24px 20px;
		font-size: 0.9375rem;
		color: var(--color-text-light);
	}
</style>
