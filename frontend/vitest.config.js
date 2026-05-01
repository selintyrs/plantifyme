import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { resolve } from 'node:path';

// Standalone Vitest config (does NOT use sveltekit() plugin to avoid the
// SvelteKit dev/build pipeline). $app/environment and $lib/api are stubbed
// via aliases below — see tests/mocks/.
export default defineConfig({
    plugins: [svelte()],
    resolve: {
        alias: [
            // Order matters — most specific first
            { find: '$lib/api.js', replacement: resolve(__dirname, 'tests/mocks/api.js') },
            { find: '$app/environment', replacement: resolve(__dirname, 'tests/mocks/app-environment.js') },
            { find: '$lib', replacement: resolve(__dirname, 'src/lib') }
        ]
    },
    test: {
        environment: 'happy-dom',
        globals: true,
        setupFiles: ['tests/setup.js'],
        include: ['tests/**/*.test.js']
    }
});
