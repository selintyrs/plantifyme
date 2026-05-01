import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import {
    shoppingListStore,
    matchResults,
    isVeganized,
    savedListId,
    itemIdMap,
    addItem,
    removeItem,
    updateQuantity,
    resetAll
} from '$lib/stores/shoppingList.js';

// Hinweis: $app/environment ist via Test-Alias auf browser=false gemockt,
// das verhindert die sessionStorage-Subscription-Side-Effects komplett.

describe('shoppingList store', () => {
    beforeEach(() => {
        resetAll();
    });

    describe('addItem', () => {
        it('fügt ein neues Item mit fortlaufender ID, quantity=1 und uiCategory hinzu', () => {
            addItem({
                id: 'fd-1',
                displayName: 'Milch',
                type: 'A',
                uiCategory: 'Milchprodukte & Käse'
            });

            const items = get(shoppingListStore);
            expect(items).toHaveLength(1);
            expect(items[0]).toMatchObject({
                displayName: 'Milch',
                type: 'A',
                quantity: 1,
                subcategory: '',
                frontendDictionaryId: 'fd-1',
                uiCategory: 'Milchprodukte & Käse'
            });
            expect(items[0].id).toBeDefined();
        });

        it('erhöht quantity wenn das Item (gleiche FD-ID + subcategory) schon existiert', () => {
            const entry = { id: 'fd-1', displayName: 'Milch', type: 'A' };
            addItem(entry);
            addItem(entry);
            addItem(entry);

            const items = get(shoppingListStore);
            expect(items).toHaveLength(1);
            expect(items[0].quantity).toBe(3);
        });

        it('behandelt unterschiedliche subcategories als separate Items', () => {
            const entry = { id: 'fd-1', displayName: 'Milch', type: 'A' };
            addItem(entry, 'Soja');
            addItem(entry, 'Hafer');

            const items = get(shoppingListStore);
            expect(items).toHaveLength(2);
            expect(items.map((i) => i.subcategory).sort()).toEqual(['Hafer', 'Soja']);
        });

        it("setzt uiCategory auf 'Sonstiges' wenn nicht angegeben", () => {
            addItem({ id: 'fd-x', displayName: 'X', type: 'A' });
            expect(get(shoppingListStore)[0].uiCategory).toBe('Sonstiges');
        });
    });

    describe('removeItem', () => {
        it('entfernt das Item per id', () => {
            addItem({ id: 'fd-1', displayName: 'Milch', type: 'A' });
            addItem({ id: 'fd-2', displayName: 'Apfel', type: 'C' });

            const initial = get(shoppingListStore);
            const targetId = initial.find((i) => i.displayName === 'Milch').id;

            removeItem(targetId);

            const after = get(shoppingListStore);
            expect(after).toHaveLength(1);
            expect(after[0].displayName).toBe('Apfel');
        });
    });

    describe('updateQuantity', () => {
        it('inkrementiert um delta=+2', () => {
            addItem({ id: 'fd-1', displayName: 'Milch', type: 'A' });
            const id = get(shoppingListStore)[0].id;

            updateQuantity(id, 2);

            expect(get(shoppingListStore)[0].quantity).toBe(3);
        });
    });

    describe('resetAll', () => {
        it('leert alle assoziierten Stores', () => {
            addItem({ id: 'fd-1', displayName: 'Milch', type: 'A' });
            matchResults.set([{ itemId: '1' }]);
            isVeganized.set(true);
            savedListId.set('list-xyz');
            itemIdMap.set({ '1': 'backend-1' });

            resetAll();

            expect(get(shoppingListStore)).toEqual([]);
            expect(get(matchResults)).toEqual([]);
            expect(get(isVeganized)).toBe(false);
            expect(get(savedListId)).toBeNull();
            expect(get(itemIdMap)).toEqual({});
        });
    });
});
