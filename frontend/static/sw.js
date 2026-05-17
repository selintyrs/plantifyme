// Minimaler Service Worker — Stufe 1 (installable PWA).
// Strategie: Network-First für /api/*, Cache-First für alles andere.
// Bump CACHE-Version bei Asset-Änderungen, damit alte Caches aufgeräumt werden.

const CACHE = 'plantify-v1';
const ASSETS = [
    '/',
    '/manifest.webmanifest',
    '/favicon.ico',
    '/favicon.svg',
    '/web-app-manifest-192x192.png',
    '/web-app-manifest-512x512.png',
    '/apple-touch-icon.png'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE).then((cache) => cache.addAll(ASSETS))
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    // Alte Caches aufräumen, falls CACHE-Version geändert wurde.
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
        ).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    if (url.pathname.startsWith('/api/')) {
        // Network-First für API-Calls — Daten müssen aktuell sein.
        event.respondWith(fetch(event.request));
        return;
    }

    // Cache-First für statische Assets / HTML.
    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request))
    );
});
