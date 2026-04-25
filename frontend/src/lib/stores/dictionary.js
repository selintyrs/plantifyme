import { writable, get } from 'svelte/store';
import Fuse from 'fuse.js';
import { authFetch } from '$lib/api.js';

export const dictionaryStore = writable([]);

export async function loadDictionary() {
	if (get(dictionaryStore).length > 0) return;

	const response = await authFetch('/api/frontend-dictionary');
	if (!response.ok) {
		console.error('Failed to load frontend dictionary:', response.status);
		return;
	}

	const data = await response.json();
	dictionaryStore.set(data);
}

export function groupByCategory(entries) {
	return entries.reduce((groups, entry) => {
		const cat = entry.uiCategory ?? 'Sonstiges';
		if (!groups[cat]) groups[cat] = [];
		groups[cat].push(entry);
		return groups;
	}, {});
}

export function searchDictionary(query, entries) {
	if (!query || query.trim() === '') return entries;
	if (!entries || entries.length === 0) return [];
	const fuse = new Fuse(entries, {
		keys: ['displayName'],
		threshold: 0.3
	});
	return fuse.search(query).map((r) => r.item);
}
