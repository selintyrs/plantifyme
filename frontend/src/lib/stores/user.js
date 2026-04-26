import { writable, get } from 'svelte/store';
import { authFetch } from '$lib/api.js';
import { isAuthenticated } from './auth.js';

export const currentUser = writable(null);
export const userLoading = writable(false);

export async function loadCurrentUser() {
	if (!get(isAuthenticated)) return null;
	userLoading.set(true);
	try {
		const res = await authFetch('/api/users/me');
		if (res.ok) {
			const user = await res.json();
			currentUser.set(user);
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
		return updated;
	}
	return null;
}
