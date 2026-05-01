import { describe, it, expect } from 'vitest';
import { groupByCategory, searchDictionary } from '$lib/stores/dictionary.js';

describe('groupByCategory', () => {
    it('liefert leeres Objekt bei leerer Eingabe', () => {
        expect(groupByCategory([])).toEqual({});
    });

    it('gruppiert ein einzelnes Item in seine uiCategory', () => {
        const entries = [{ id: '1', displayName: 'Milch', uiCategory: 'Milchprodukte & Käse' }];
        const groups = groupByCategory(entries);
        expect(groups).toEqual({ 'Milchprodukte & Käse': entries });
    });

    it('gruppiert mehrere Items in ihre jeweiligen Kategorien', () => {
        const entries = [
            { id: '1', displayName: 'Milch', uiCategory: 'Milchprodukte & Käse' },
            { id: '2', displayName: 'Apfel', uiCategory: 'Obst & Gemüse' },
            { id: '3', displayName: 'Joghurt', uiCategory: 'Milchprodukte & Käse' }
        ];
        const groups = groupByCategory(entries);
        expect(Object.keys(groups)).toEqual(
            expect.arrayContaining(['Milchprodukte & Käse', 'Obst & Gemüse'])
        );
        expect(groups['Milchprodukte & Käse']).toHaveLength(2);
        expect(groups['Obst & Gemüse']).toHaveLength(1);
    });

    it("Items ohne uiCategory werden in 'Sonstiges' gruppiert", () => {
        const entries = [{ id: '1', displayName: 'X', uiCategory: undefined }];
        const groups = groupByCategory(entries);
        expect(groups).toEqual({ Sonstiges: entries });
    });
});

describe('searchDictionary', () => {
    const entries = [
        { id: '1', displayName: 'Milch', uiCategory: 'Milchprodukte & Käse' },
        { id: '2', displayName: 'Hafermilch', uiCategory: 'Milchprodukte & Käse' },
        { id: '3', displayName: 'Apfel', uiCategory: 'Obst & Gemüse' }
    ];

    it('leerer Query liefert alle Einträge unverändert zurück', () => {
        expect(searchDictionary('', entries)).toBe(entries);
        expect(searchDictionary('   ', entries)).toBe(entries);
    });

    it('Suche in leerer Liste liefert leere Liste', () => {
        expect(searchDictionary('Milch', [])).toEqual([]);
    });

    it('Fuzzy-Match findet relevante Einträge nach displayName', () => {
        const results = searchDictionary('Milch', entries);
        const names = results.map((r) => r.displayName);
        expect(names).toContain('Milch');
        expect(names).toContain('Hafermilch');
        expect(names).not.toContain('Apfel');
    });
});
