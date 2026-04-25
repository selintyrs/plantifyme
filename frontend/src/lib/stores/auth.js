import { writable, get } from 'svelte/store';
import { createAuth0Client } from '@auth0/auth0-spa-js';
import { browser } from '$app/environment';
import {
	PUBLIC_AUTH0_DOMAIN,
	PUBLIC_AUTH0_CLIENT_ID,
	PUBLIC_AUTH0_AUDIENCE,
} from '$env/static/public';

export const authClient = writable(null);
export const isAuthenticated = writable(false);
export const user = writable(null);
export const accessToken = writable(null);
export const isLoading = writable(true);

export async function initAuth() {
	if (!browser) return;

	const client = await createAuth0Client({
		domain: PUBLIC_AUTH0_DOMAIN,
		clientId: PUBLIC_AUTH0_CLIENT_ID,
		authorizationParams: {
			redirect_uri: window.location.origin,
			audience: PUBLIC_AUTH0_AUDIENCE,
		},
		cacheLocation: 'localstorage',
		useRefreshTokens: true,
	});
	authClient.set(client);

	if (window.location.search.includes('code=')) {
		try {
			await client.handleRedirectCallback();
			window.history.replaceState({}, document.title, '/');
		} catch (e) {
			console.error('Auth0 callback error:', e);
		}
	}

	const isAuth = await client.isAuthenticated();
	isAuthenticated.set(isAuth);

	if (isAuth) {
		const u = await client.getUser();
		user.set(u);
		try {
			const token = await client.getTokenSilently();
			accessToken.set(token);
		} catch (e) {
			console.error('Token fetch failed:', e);
		}
	}

	isLoading.set(false);
}

export async function login(connection = null) {
	const client = get(authClient);
	if (!client) return;
	const params = {
		authorizationParams: {
			redirect_uri: window.location.origin,
			audience: PUBLIC_AUTH0_AUDIENCE,
		},
	};
	if (connection) {
		params.authorizationParams.connection = connection;
	}
	await client.loginWithRedirect(params);
}

export async function signup(connection = null) {
	const client = get(authClient);
	if (!client) return;
	const params = {
		authorizationParams: {
			redirect_uri: window.location.origin,
			audience: PUBLIC_AUTH0_AUDIENCE,
			screen_hint: 'signup',
		},
	};
	if (connection) {
		params.authorizationParams.connection = connection;
	}
	await client.loginWithRedirect(params);
}

export async function logout() {
	const client = get(authClient);
	if (!client) return;
	await client.logout({
		logoutParams: {
			returnTo: window.location.origin + '/landing',
		},
	});
}
