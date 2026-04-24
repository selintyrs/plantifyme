<script>
	import { goto } from '$app/navigation';
	import { onMount, tick } from 'svelte';
	import { categories } from '$lib/data/infoArticles.js';

	const STORAGE_KEY = 'plantify_coach_chat';
	const MAX_STORED = 50;
	const MAX_API_HISTORY = 10;

	let imgErrors = $state({});
	let messages = $state([]);
	let inputText = $state('');
	let isLoading = $state(false);
	let chatContainerRef = $state(null);
	let inputRef = $state(null);

	const suggestedPrompts = [
		'Wie begegne ich dem Argument, Vegan sei Mangelernährung?',
		'Sind pflanzliche Alternativen wirklich besser?',
		'Ist der Mensch nicht von Natur aus Fleischfresser?',
	];

	onMount(() => {
		const stored = localStorage.getItem(STORAGE_KEY);
		if (stored) {
			try {
				const parsed = JSON.parse(stored);
				if (Array.isArray(parsed)) messages = parsed;
			} catch {}
		}
	});

	function handleImgError(categoryId) {
		imgErrors = { ...imgErrors, [categoryId]: true };
	}

	function saveMessages() {
		const toSave = messages.slice(-MAX_STORED);
		messages = toSave;
		try {
			localStorage.setItem(STORAGE_KEY, JSON.stringify(toSave));
		} catch {}
	}

	async function sendMessage(text) {
		const trimmed = text.trim();
		if (!trimmed || isLoading) return;

		messages = [...messages, { role: 'user', content: trimmed }];
		saveMessages();
		inputText = '';
		isLoading = true;

		await tick();
		scrollToBottom();

		try {
			const historyForApi = messages.slice(-MAX_API_HISTORY, -1).map(m => ({
				role: m.role === 'coach' ? 'assistant' : 'user',
				content: m.content,
			}));

			const res = await fetch('/api/coach/chat', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ message: trimmed, history: historyForApi }),
			});

			if (!res.ok) throw new Error(`HTTP ${res.status}`);
			const data = await res.json();

			messages = [...messages, {
				role: 'coach',
				content: data.reply,
				articles: Array.isArray(data.referencedArticles) ? data.referencedArticles : [],
			}];
			saveMessages();
		} catch (e) {
			console.error('Coach-Chat fehlgeschlagen:', e);
			messages = [...messages, {
				role: 'coach',
				content: 'Entschuldige, da ist etwas schiefgelaufen. Versuch es bitte nochmal.',
			}];
			saveMessages();
		} finally {
			isLoading = false;
			await tick();
			scrollToBottom();
		}
	}

	function scrollToBottom() {
		if (chatContainerRef) {
			chatContainerRef.scrollTop = chatContainerRef.scrollHeight;
		}
	}

	function clearChat() {
		messages = [];
		try {
			localStorage.removeItem(STORAGE_KEY);
		} catch {}
	}

	function handleKeydown(e) {
		if (e.key === 'Enter' && !e.shiftKey) {
			e.preventDefault();
			sendMessage(inputText);
		}
	}

	function openArticle(article) {
		const cat = categories.find(c => c.name === article.category);
		if (!cat) return;
		goto(`/info/${cat.id}?openArticle=${article.id}`);
	}
</script>

<div class="page">
	<h1>Fragen & Entdecken</h1>

	<!-- Plantify Coach ──────────────────────────────────────────────────── -->
	<section class="chatbot-section">
		{#if messages.length === 0}
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
					Hey, ich bin dein Plantify Coach. Was möchtest du wissen?
				</div>
			</div>

			<div class="suggested-list">
				{#each suggestedPrompts as prompt}
					<button class="suggested-chip" onclick={() => sendMessage(prompt)} disabled={isLoading}>
						{prompt}
					</button>
				{/each}
			</div>
		{:else}
			<div class="chat-container" bind:this={chatContainerRef}>
				{#each messages as msg}
					{#if msg.role === 'user'}
						<div class="user-row">
							<div class="user-bubble">{msg.content}</div>
						</div>
					{:else}
						<div class="chatbot-row">
							<div class="bot-avatar" aria-hidden="true">
								<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
									<rect width="18" height="10" x="3" y="11" rx="2"/>
									<circle cx="12" cy="5" r="2"/>
									<path d="M12 7v4"/>
									<line x1="8" y1="16" x2="8" y2="16"/>
									<line x1="16" y1="16" x2="16" y2="16"/>
								</svg>
							</div>
							<div class="coach-block">
								<div class="speech-bubble">{msg.content}</div>
								{#if msg.articles?.length > 0}
									<div class="article-chips">
										{#each msg.articles as article}
											<button class="article-chip" onclick={() => openArticle(article)}>
												<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z"/>
													<path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>
												</svg>
												<span class="article-chip-title">{article.title}</span>
												<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
													<path d="m9 18 6-6-6-6"/>
												</svg>
											</button>
										{/each}
									</div>
								{/if}
							</div>
						</div>
					{/if}
				{/each}

				{#if isLoading}
					<div class="chatbot-row">
						<div class="bot-avatar" aria-hidden="true">
							<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
								<rect width="18" height="10" x="3" y="11" rx="2"/>
								<circle cx="12" cy="5" r="2"/>
								<path d="M12 7v4"/>
								<line x1="8" y1="16" x2="8" y2="16"/>
								<line x1="16" y1="16" x2="16" y2="16"/>
							</svg>
						</div>
						<div class="speech-bubble typing">
							<span class="dot"></span>
							<span class="dot"></span>
							<span class="dot"></span>
						</div>
					</div>
				{/if}
			</div>
		{/if}

		<div class="chat-input-row">
			<input
				class="chat-input"
				type="text"
				placeholder="Frag mich etwas..."
				bind:value={inputText}
				onkeydown={handleKeydown}
				disabled={isLoading}
				bind:this={inputRef}
				aria-label="Chatbot-Eingabe"
			/>
			<button
				class="chat-send"
				onclick={() => sendMessage(inputText)}
				disabled={isLoading || !inputText.trim()}
				aria-label="Senden"
			>
				<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
					<path d="M22 2 11 13"/>
					<path d="M22 2 15 22 11 13 2 9l20-7z"/>
				</svg>
			</button>
		</div>

		{#if messages.length > 0}
			<button class="clear-chat-btn" onclick={clearChat}>Chat löschen</button>
		{/if}
	</section>

	<!-- Themen ──────────────────────────────────────────────────────────── -->
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

	/* ── Chatbot section ───────────────────────────────────────────────── */
	.chatbot-section {
		margin-bottom: 28px;
	}

	.chat-container {
		max-height: 400px;
		overflow-y: auto;
		padding: 12px;
		background: #F9FAFB;
		border-radius: 16px;
		display: flex;
		flex-direction: column;
		gap: 10px;
		margin-bottom: 12px;
	}

	.chatbot-row {
		display: flex;
		align-items: flex-start;
		gap: 12px;
		margin-bottom: 12px;
	}

	.bot-avatar {
		flex-shrink: 0;
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background-color: var(--color-primary);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.speech-bubble {
		background: #F3F4F6;
		border-radius: 16px;
		padding: 10px 14px;
		font-size: 0.9375rem;
		color: var(--color-text);
		line-height: 1.5;
		max-width: 80%;
	}

	.coach-block {
		display: flex;
		flex-direction: column;
		gap: 8px;
		flex: 1;
		min-width: 0;
	}

	.article-chips {
		display: flex;
		flex-wrap: wrap;
		gap: 6px;
	}

	.article-chip {
		display: inline-flex;
		align-items: center;
		gap: 4px;
		padding: 6px 12px;
		border: 1px solid var(--color-primary);
		border-radius: 20px;
		background: var(--color-bg);
		color: var(--color-primary);
		font-size: 0.75rem;
		font-weight: 500;
		cursor: pointer;
		max-width: 100%;
		transition: background 0.12s ease;
	}

	.article-chip:hover {
		background: var(--color-primary-light);
	}

	.article-chip-title {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		max-width: 180px;
	}

	.user-row {
		display: flex;
		justify-content: flex-end;
	}

	.user-bubble {
		background: var(--color-primary);
		color: #fff;
		border-radius: 16px;
		padding: 10px 14px;
		font-size: 0.9375rem;
		line-height: 1.5;
		max-width: 80%;
	}

	/* Typing indicator */
	.typing {
		display: inline-flex;
		gap: 4px;
		align-items: center;
	}

	.dot {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: var(--color-text-light);
		animation: pulse 1.2s infinite ease-in-out;
	}

	.dot:nth-child(2) { animation-delay: 0.2s; }
	.dot:nth-child(3) { animation-delay: 0.4s; }

	@keyframes pulse {
		0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
		40% { opacity: 1; transform: scale(1); }
	}

	/* Suggested prompts */
	.suggested-list {
		display: flex;
		flex-direction: column;
		gap: 8px;
		margin-bottom: 16px;
	}

	.suggested-chip {
		border: 1.5px solid var(--color-primary);
		background: var(--color-primary-light);
		color: var(--color-primary);
		font-size: 0.875rem;
		font-weight: 500;
		padding: 10px 14px;
		border-radius: 999px;
		text-align: left;
		cursor: pointer;
		transition: background 0.15s ease;
	}

	.suggested-chip:hover:not(:disabled) {
		background: var(--color-primary);
		color: #fff;
	}

	.suggested-chip:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	/* Input row */
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
	}

	.chat-input:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.chat-input::placeholder {
		color: var(--color-text-light);
	}

	.chat-input:focus {
		border-color: var(--color-primary);
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
		cursor: pointer;
		transition: opacity 0.15s ease;
	}

	.chat-send:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.clear-chat-btn {
		margin-top: 10px;
		font-size: 0.8125rem;
		color: var(--color-text-light);
		background: none;
		border: none;
		cursor: pointer;
		padding: 4px 0;
		text-decoration: underline;
	}

	.clear-chat-btn:hover {
		color: var(--color-text);
	}

	/* ── Themen ────────────────────────────────────────────────────────── */
	.section-title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text);
		margin-top: 28px;
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
