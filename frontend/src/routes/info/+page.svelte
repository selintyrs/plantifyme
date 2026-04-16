<script>
	import { goto } from '$app/navigation';
	import { categories } from '$lib/data/infoArticles.js';

	let imgErrors = $state({});

	function handleImgError(categoryId) {
		imgErrors = { ...imgErrors, [categoryId]: true };
	}
</script>

<div class="page">
	<h1>Fragen & Entdecken</h1>

	<!-- Chatbot Platzhalter -->
	<section class="chatbot-section">
		<div class="chatbot-row">
			<div class="bot-avatar" aria-hidden="true">
				<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
					<rect width="18" height="10" x="3" y="11" rx="2"/>
					<circle cx="12" cy="5" r="2"/>
					<path d="M12 7v4"/>
					<line x1="8" y1="16" x2="8" y2="16"/>
					<line x1="16" y1="16" x2="16" y2="16"/>
				</svg>
			</div>
			<div class="speech-bubble">
				Hey, ich bin dein Plantify Coach. Was möchtest du heute wissen?
			</div>
		</div>
		<div class="chat-input-row">
			<input
				class="chat-input"
				type="text"
				placeholder="Frag mich etwas..."
				disabled
				aria-label="Chatbot-Eingabe"
			/>
			<button class="chat-send" disabled aria-label="Senden">
				<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
					<path d="M22 2 11 13"/>
					<path d="M22 2 15 22 11 13 2 9l20-7z"/>
				</svg>
			</button>
		</div>
	</section>

	<!-- Themen -->
	<h2 class="section-title">Themen</h2>

	<div class="category-grid">
		{#each categories as cat}
			<button class="category-card" onclick={() => goto(`/info/${cat.id}`)}>
				<div class="cat-icon-wrapper">
					{#if imgErrors[cat.id]}
						<div class="cat-icon-fallback" aria-hidden="true">
							{cat.name.charAt(0)}
						</div>
					{:else}
						<img
							src="/images/categories/{cat.id}.png"
							alt={cat.name}
							width="48"
							height="48"
							class="cat-icon"
							onerror={() => handleImgError(cat.id)}
						/>
					{/if}
				</div>
				<span class="cat-name">{cat.name}</span>
			</button>
		{/each}
	</div>
</div>

<style>
	.page {
		padding: 24px 16px 100px;
	}

	h1 {
		font-size: 1.5rem;
		font-weight: 700;
		color: var(--color-text);
		margin-bottom: 24px;
	}

	/* Chatbot */
	.chatbot-section {
		margin-bottom: 28px;
	}

	.chatbot-row {
		display: flex;
		align-items: flex-start;
		gap: 12px;
		margin-bottom: 12px;
	}

	.bot-avatar {
		flex-shrink: 0;
		width: 44px;
		height: 44px;
		border-radius: 50%;
		background-color: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.speech-bubble {
		background: #F3F4F6;
		border-radius: 16px;
		padding: 14px 18px;
		font-size: 0.9375rem;
		color: var(--color-text);
		line-height: 1.5;
		flex: 1;
	}

	.chat-input-row {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.chat-input {
		flex: 1;
		height: 46px;
		border: 1.5px solid var(--color-border);
		border-radius: 23px;
		padding: 0 18px;
		font-size: 0.9375rem;
		color: var(--color-text);
		background: var(--color-bg);
		outline: none;
		opacity: 0.6;
		cursor: not-allowed;
	}

	.chat-input::placeholder {
		color: var(--color-text-light);
	}

	.chat-send {
		flex-shrink: 0;
		width: 46px;
		height: 46px;
		border-radius: 50%;
		border: none;
		background-color: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: not-allowed;
		opacity: 0.5;
	}

	/* Themen */
	.section-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
		margin-bottom: 14px;
	}

	.category-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 12px;
	}

	.category-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 24px 16px;
		background: var(--color-bg);
		border: 1.5px solid var(--color-border);
		border-radius: 16px;
		cursor: pointer;
		transition: background 0.15s ease;
		gap: 12px;
	}

	.category-card:hover,
	.category-card:active {
		background: var(--color-primary-light);
	}

	.cat-icon-wrapper {
		width: 48px;
		height: 48px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.cat-icon {
		width: 48px;
		height: 48px;
		object-fit: contain;
	}

	.cat-icon-fallback {
		width: 48px;
		height: 48px;
		border-radius: 50%;
		background-color: var(--color-primary);
		color: white;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 1.25rem;
		font-weight: 700;
	}

	.cat-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text);
		text-align: center;
		line-height: 1.3;
	}
</style>
