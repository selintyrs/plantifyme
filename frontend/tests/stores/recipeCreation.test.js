import { describe, it, expect } from 'vitest';
import { get } from 'svelte/store';
import { recipeCreationStore, resetRecipeCreation } from '$lib/stores/recipeCreation.js';

describe('recipeCreation store', () => {
    it('resetRecipeCreation setzt Store auf Default-Werte zurück', () => {
        recipeCreationStore.set({
            title: 'Modified Title',
            description: 'Some text',
            customIngredients: [{ name: 'Tomate' }],
            baseRecipeId: 'br-1'
        });

        resetRecipeCreation();

        expect(get(recipeCreationStore)).toEqual({
            title: '',
            description: '',
            customIngredients: [],
            baseRecipeId: null
        });
    });
});
