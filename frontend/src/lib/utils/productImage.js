// Zentrale Strategie für Produkt-Bilder.
//
// Priorität:
//   1. product.imageUrl  — eindeutig pro Produkt, aus MongoDB
//   2. /images/products/{supermarket}/{articleId}.jpg — lokaler Fallback
//   3. null → die rendernde Komponente entscheidet selbst, was sie zeigt
//      (z.B. Inline-Placeholder "Kein Bild").
//
// Hintergrund: Bei Migros existieren Duplikate auf article_id (mehrere
// verschiedene Produkte teilen sich denselben Barcode). Lokale Dateinamen
// basieren auf article_id und sind dadurch nicht eindeutig. imageUrl aus
// MongoDB ist pro Produkt-Dokument eindeutig und daher die saubere Primary.

export function productImageUrl(product) {
	return product?.imageUrl ?? null;
}

export function productImageFallbackUrl(product) {
	const sm = product?.supermarket?.toLowerCase();
	const id = product?.articleId;
	if (!sm || !id) return null;
	return `/images/products/${sm}/${id}.jpg`;
}
