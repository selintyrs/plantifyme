<script>
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { categories, getArticlesByCategory } from '$lib/data/infoArticles.js';

	let categoryId = $derived($page.params.category);
	let category = $derived(categories.find(c => c.id === categoryId));
	let articles = $derived(getArticlesByCategory(categoryId));

	let openArticleId = $state($page.url.searchParams.get('openArticle'));

	function toggleArticle(articleId) {
		openArticleId = openArticleId === articleId ? null : articleId;
	}

	function formatContent(text) {
		const paragraphs = text.split('\n\n').filter(p => p.trim());
		return paragraphs.map(para => {
			let formatted = para.trim();
			// Quellen-Referenzen
			formatted = formatted.replace(/\[(\d+)\]/g, '<sup class="source-ref">$1</sup>');
			// Nummerierte Überschriften (z.B. "1. Der Ursprung")
			if (/^\d+\.\s/.test(formatted)) {
				return `<p><strong>${formatted}</strong></p>`;
			}
			// Kurze Absätze ohne Punkt am Ende = Zwischenüberschrift
			// (maximal 60 Zeichen und endet nicht mit . oder ] oder ))
			if (formatted.length <= 60 && !/[.\])]$/.test(formatted.replace(/<[^>]+>/g, '').trim())) {
				return `<p><strong>${formatted}</strong></p>`;
			}
			return `<p>${formatted}</p>`;
		}).join('');
	}
</script>

<div class="page">
	<header class="cat-header">
		<button class="back-btn" onclick={() => goto('/info')} aria-label="Zurück">
			<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<path d="M15 18l-6-6 6-6"/>
			</svg>
		</button>
		<h1>{category?.name ?? ''}</h1>
	</header>

	<div class="accordion-list">
		{#each articles as article}
			<div class="accordion-card">
				<button
					class="accordion-header"
					onclick={() => toggleArticle(article.id)}
					aria-expanded={openArticleId === article.id}
				>
					<span class="accordion-title">{article.title}</span>
					<svg
						class="chevron"
						class:chevron-open={openArticleId === article.id}
						xmlns="http://www.w3.org/2000/svg"
						width="20"
						height="20"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						<path d="M6 9l6 6 6-6"/>
					</svg>
				</button>

				{#if openArticleId === article.id}
					<div class="accordion-content">
						<div class="article-text">
							{@html formatContent(article.content)}
						</div>

						{#if article.sources && article.sources.length > 0}
							<hr class="sources-divider" />
							<p class="sources-label">Quellen</p>
							<ol class="sources-list">
								{#each article.sources as source, i}
									<li class="source-item">{source}</li>
								{/each}
							</ol>
						{/if}
					</div>
				{/if}
			</div>
		{/each}
	</div>
</div>

<style>
	.page {
		padding: 0 0 100px;
	}

	/* Header */
	.cat-header {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 20px 16px 16px;
		position: sticky;
		top: 0;
		background: var(--color-bg);
		z-index: 10;
		border-bottom: 1px solid var(--color-border);
	}

	.back-btn {
		flex-shrink: 0;
		width: 36px;
		height: 36px;
		border-radius: 50%;
		border: none;
		background: transparent;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		color: var(--color-text);
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

	/* Accordion list */
	.accordion-list {
		display: flex;
		flex-direction: column;
		gap: 10px;
		padding: 16px 16px 0;
	}

	.accordion-card {
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		background: var(--color-bg);
		overflow: hidden;
	}

	.accordion-header {
		width: 100%;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 18px 20px;
		background: transparent;
		border: none;
		cursor: pointer;
		text-align: left;
	}

	.accordion-header:hover {
		background: var(--color-primary-light);
	}

	.accordion-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
		line-height: 1.4;
		flex: 1;
	}

	.chevron {
		flex-shrink: 0;
		color: var(--color-text-light);
		transition: transform 0.2s ease;
	}

	.chevron-open {
		transform: rotate(180deg);
	}

	/* Accordion content */
	.accordion-content {
		padding: 0 20px 20px;
	}

	/* Article text */
	.article-text :global(p) {
		font-size: 0.9375rem;
		line-height: 1.7;
		color: var(--color-text);
		margin-bottom: 12px;
	}

	.article-text :global(p:last-child) {
		margin-bottom: 0;
	}

	.article-text :global(p strong) {
		display: block;
		margin-top: 16px;
		margin-bottom: 4px;
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.article-text :global(.source-ref) {
		color: var(--color-primary);
		font-weight: 600;
		font-size: 0.6875rem;
		cursor: default;
		vertical-align: super;
		line-height: 0;
	}

	/* Sources */
	.sources-divider {
		border: none;
		border-top: 1px solid var(--color-border);
		margin: 16px 0;
	}

	.sources-label {
		font-size: 0.75rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: var(--color-text-light);
		margin-bottom: 8px;
	}

	.sources-list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 6px;
		counter-reset: source-counter;
	}

	.source-item {
		font-size: 0.8125rem;
		color: var(--color-text-light);
		line-height: 1.5;
		counter-increment: source-counter;
		display: flex;
		gap: 6px;
	}

	.source-item::before {
		content: counter(source-counter) ".";
		flex-shrink: 0;
		font-weight: 600;
		color: var(--color-primary);
		min-width: 18px;
	}
</style>
