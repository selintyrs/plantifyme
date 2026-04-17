import { writable } from 'svelte/store';

export const recipeCreationStore = writable({
    title: '',
    description: '',
    customIngredients: [],
    baseRecipeId: null
});

export function resetRecipeCreation() {
    recipeCreationStore.set({
        title: '',
        description: '',
        customIngredients: [],
        baseRecipeId: null
    });
}
