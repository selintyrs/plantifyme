// Test stub for $lib/api.js — never makes a real HTTP call.
// Tests can vi.mocked(authFetch) it via dynamic mocks; default returns 200 empty.
export async function authFetch() {
    return new Response('[]', { status: 200, headers: { 'Content-Type': 'application/json' } });
}
