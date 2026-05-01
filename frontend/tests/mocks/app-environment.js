// Test stub for SvelteKit's $app/environment.
// Default: browser=false so stores skip session/localStorage subscription side-effects.
// Tests that need browser=true use vi.doMock or override directly.
export const browser = false;
export const dev = true;
export const building = false;
export const version = 'test';
