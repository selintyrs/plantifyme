import { writable } from 'svelte/store';

const stored = typeof window !== 'undefined'
    ? JSON.parse(localStorage.getItem('recentProducts') || '[]')
    : [];

export const recentProducts = writable(stored);

export function addRecentProduct(entry) {
    recentProducts.update(items => {
        const filtered = items.filter(i => i.id !== entry.id);
        const updated = [entry, ...filtered].slice(0, 20);
        if (typeof window !== 'undefined') {
            localStorage.setItem('recentProducts', JSON.stringify(updated));
        }
        return updated;
    });
}
