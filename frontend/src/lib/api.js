import { get } from 'svelte/store';
import { accessToken } from './stores/auth.js';

export async function authFetch(url, options = {}) {
	const token = get(accessToken);
	const headers = { ...(options.headers || {}) };
	if (token) {
		headers['Authorization'] = `Bearer ${token}`;
	}
	if (options.body && !headers['Content-Type']) {
		headers['Content-Type'] = 'application/json';
	}
	return fetch(url, { ...options, headers });
}
