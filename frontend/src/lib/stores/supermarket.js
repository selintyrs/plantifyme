import { writable } from 'svelte/store';

export const supermarketStore = writable('Migros');
export const isReadyToMatch = writable(false);
