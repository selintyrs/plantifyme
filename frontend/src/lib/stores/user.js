import { writable, get } from 'svelte/store';
import { authFetch } from '$lib/api.js';
import { isAuthenticated } from './auth.js';
import { supermarketStore } from './supermarket.js';

export const currentUser = writable(null);
export const userLoading = writable(false);

const SUPERMARKET_DISPLAY = {
	Migros: 'Migros',
	coop: 'Coop',
	aldi: 'Aldi',
};

function applySupermarketDefault(user) {
	if (!user?.defaultSupermarket || user.defaultSupermarket === 'none') return;
	if (get(supermarketStore)) return;
	const display = SUPERMARKET_DISPLAY[user.defaultSupermarket] ?? user.defaultSupermarket;
	supermarketStore.set(display);
}

export async function loadCurrentUser() {
	if (!get(isAuthenticated)) return null;
	userLoading.set(true);
	try {
		const res = await authFetch('/api/users/me');
		if (res.ok) {
			const user = await res.json();
			currentUser.set(user);
			applySupermarketDefault(user);
			return user;
		}
	} catch (e) {
		console.error('Failed to load user:', e);
	} finally {
		userLoading.set(false);
	}
	return null;
}

export async function updateCurrentUser(data) {
	const res = await authFetch('/api/users/me', {
		method: 'PATCH',
		body: JSON.stringify(data),
	});
	if (res.ok) {
		const updated = await res.json();
		currentUser.set(updated);
		applySupermarketDefault(updated);
		return updated;
	}
	return null;
}
