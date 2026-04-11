import { writable, get } from 'svelte/store';

export const shoppingListStore = writable([]);

let nextId = 1;

export function addItem(entry, subcategory = '') {
	shoppingListStore.update((items) => {
		const exists = items.find(
			(i) => i.frontendDictionaryId === entry.id && i.subcategory === subcategory
		);
		if (exists) {
			return items.map((i) =>
				i.frontendDictionaryId === entry.id && i.subcategory === subcategory
					? { ...i, quantity: i.quantity + 1 }
					: i
			);
		}
		return [
			...items,
			{
				id: nextId++,
				displayName: entry.displayName,
				type: entry.type,
				subcategory,
				quantity: 1,
				frontendDictionaryId: entry.id,
				uiCategory: entry.uiCategory ?? 'Sonstiges'
			}
		];
	});
}

export function removeItem(id) {
	shoppingListStore.update((items) => items.filter((i) => i.id !== id));
}

export function updateQuantity(id, delta) {
	shoppingListStore.update((items) =>
		items.map((i) => (i.id === id ? { ...i, quantity: i.quantity + delta } : i))
	);
}

export function getItems() {
	return get(shoppingListStore);
}
