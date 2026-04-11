<script>
	import { dictionaryStore, searchDictionary } from '$lib/stores/dictionary.js';

	let query = $state('');
	let results = $derived(searchDictionary(query, $dictionaryStore));
</script>

<div class="page">
	<h1>Einkaufsliste</h1>

	<p class="count">
		{$dictionaryStore.length} Einträge geladen
	</p>

	<div class="search-wrapper">
		<input
			type="search"
			placeholder="Produkt suchen…"
			bind:value={query}
			class="search-input"
		/>
	</div>

	{#if query.trim()}
		<ul class="results">
			{#each results as entry, i (i)}
				<li class="result-item">
					<span class="name">{entry.displayName}</span>
					<span class="category">{entry.uiCategory}</span>
				</li>
			{:else}
				<li class="empty">Keine Ergebnisse für „{query}"</li>
			{/each}
		</ul>
	{/if}
</div>

<style>
	.page {
		padding: 24px 16px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	h1 {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-text);
	}

	.count {
		font-size: 0.875rem;
		color: var(--color-text-light);
	}

	.search-wrapper {
		position: relative;
	}

	.search-input {
		width: 100%;
		padding: 10px 14px;
		border: 1px solid var(--color-border);
		border-radius: 8px;
		font-size: 1rem;
		font-family: inherit;
		color: var(--color-text);
		background-color: var(--color-bg);
		outline: none;
		transition: border-color 0.15s ease;
	}

	.search-input:focus {
		border-color: var(--color-primary);
	}

	.results {
		list-style: none;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.result-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 14px;
		background-color: var(--color-primary-light);
		border-radius: 8px;
	}

	.name {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text);
	}

	.category {
		font-size: 0.75rem;
		color: var(--color-text-light);
	}

	.empty {
		font-size: 0.875rem;
		color: var(--color-text-light);
		padding: 12px 0;
		text-align: center;
	}
</style>
