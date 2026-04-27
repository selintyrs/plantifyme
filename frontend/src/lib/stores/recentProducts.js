import { writable, get } from 'svelte/store';
import { user } from './auth.js';

const LEGACY_KEY = 'recentProducts';

function getStorageKey() {
	const u = get(user);
	if (!u?.sub) return null;
	return `recentProducts_${u.sub}`;
}

function loadFromStorage(key) {
	if (typeof window === 'undefined' || !key) return [];
	try {
		const stored = localStorage.getItem(key);
		return stored ? JSON.parse(stored) : [];
	} catch {
		return [];
	}
}

function migrateLegacy(key) {
	if (typeof window === 'undefined') return;
	try {
		const legacy = localStorage.getItem(LEGACY_KEY);
		const current = localStorage.getItem(key);
		if (legacy && !current) {
			localStorage.setItem(key, legacy);
		}
		if (legacy) localStorage.removeItem(LEGACY_KEY);
	} catch {}
}

export const recentProducts = writable([]);

if (typeof window !== 'undefined') {
	user.subscribe((u) => {
		if (u?.sub) {
			const key = `recentProducts_${u.sub}`;
			migrateLegacy(key);
			recentProducts.set(loadFromStorage(key));
		} else {
			recentProducts.set([]);
		}
	});
}

export function addRecentProduct(entry) {
	const key = getStorageKey();
	recentProducts.update(items => {
		const filtered = items.filter(i => i.id !== entry.id);
		const updated = [entry, ...filtered].slice(0, 20);
		if (key && typeof window !== 'undefined') {
			try { localStorage.setItem(key, JSON.stringify(updated)); } catch {}
		}
		return updated;
	});
}
