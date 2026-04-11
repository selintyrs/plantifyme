# CLAUDE.md – Plantify Kontext für Claude Code

## Was ist Plantify?

Plantify ist eine App (Bachelorarbeit ZHAW). User erstellen eine Einkaufsliste, das System erkennt nicht-vegane Produkte und ersetzt sie durch vegane Alternativen aus Schweizer Supermärkten (Migros, Coop, Aldi). Die App zeigt den Umwelt-Impact (CO₂, Wasser, Land) und Tierwohl-Äquivalente an.

## Tech Stack

- Backend: Java, Spring Boot 4.0.3, Maven
- Frontend: Svelte (SvelteKit) – wird separat entwickelt
- Datenbank: MongoDB Atlas
- Auth: Auth0 (JWT-basiert, Social Logins)
- KI: Spring AI (Rezept-Generierung, Chatbot)
- Testing: Postman (API), JUnit mit MockMvc (Backend)
- Lombok: @Getter, @NoArgsConstructor, @RequiredArgsConstructor, @NonNull

## Projektstruktur

```
src/main/java/ch/zhaw/plantifyme/
├── controller/        # REST Controller mit @RestController
├── model/             # DAOs (@Document) und DTOs
├── repository/        # MongoDB Repositories (extends MongoRepository)
├── service/           # Business Logic
└── PlantifymeApplication.java
```

## Coding-Konventionen

- DAOs: @Document(collection = "collectionName") + @Id + Lombok @Getter @NoArgsConstructor
- DTOs: @Getter @NoArgsConstructor, kein @Document
- Repositories: Interface extends MongoRepository<DAO, String>
- Controller: @RestController + @RequestMapping("/api")
- Kein passwordHash in DB – Auth0 übernimmt Authentifizierung
- Commit-Messages: "Closes #X" um GitHub Issues zu schliessen

## Produkt-Typen (KRITISCH)

| Typ | Beschreibung | Collection | Impact-Daten | Beispiele |
|-----|-------------|------------|--------------|-----------|
| A | Nicht-vegan, muss ersetzt werden | supermarketProducts | JA (CO₂, Wasser, Land, Tierwohl) | Milch, Fleisch, Käse, Eier |
| B | Bereits vegan, aber in DB mit Produktinfos | supermarketProducts | NEIN | Tofu, Hafermilch, vegane Burger |
| C | Generisch, immer vegan, kein Supermarkt-Bezug | genericFoods | NEIN | Äpfel, Tomaten, Karotten |

## Datenbank – 8 Collections

### 1. supermarketProducts (2437 Dokumente)

Alle Supermarkt-Produkte (Typ A und B).

Felder:
- Identifikation: `_id`, `supermarket`, `article_id`, `product_url`, `image_url`
- Produkt: `type` (A/B), `name`, `brand`, `price`, `amount`, `qty`, `unit`, `price_per_unit`, `price_per_unit_label`
- Flags: `is_bio`, `is_gluten_free`, `has_additives`
- Inhalt: `ingredients` (List<String>), `allergens_contains`, `allergens_may_contain`
- Nährwerte: `energy_kj`, `energy_kcal`, `fat_g`, `saturated_fat_g`, `carbs_g`, `sugar_g`, `protein_g`, `salt_g`
- Kategorien: `plantify_cat_l1` bis `plantify_cat_l5`
- Matching: `matching_keys` (List<MatchingKey>) – siehe Matching-Logik unten
- Impact (NUR Typ A): `impact_co2_original_kg`, `impact_co2_saved_kg`, `impact_co2_saved_pct`, `impact_water_original_L`, `impact_water_saved_L`, `impact_water_saved_pct`, `impact_land_original_m2`, `impact_land_saved_m2`, `impact_land_saved_pct`, `impact_method`
- Tierwohl (NUR Typ A): `animal_type` (cow/pig/chicken/fish), `animals_affected_per_unit`

### 2. frontendDictionary

UI-Einträge für die Einkaufslisten-Erstellung. Brücke zwischen Frontend-UI und Backend-Matching.

Felder:
- `_id`, `displayName` (z.B. "Joghurt"), `uiCategory` (z.B. "Milchprodukte & Käse")
- `type` (A/B/C)
- `matchingKeys` (Map mit plantify_cat_l2/l3/l4/l5-Werten) – Default-Kategorie für Matching
- `additionalOptions` (List<AdditionalOption>) – Spezifizierungen per Long-Press
  - Jede Option hat: `optionName` (z.B. "Schoko") + `additionalKeys` (Map mit l4/l5-Werten)
- Bei Typ C: `matchingKeys` = null, stattdessen `nutrition`-Felder

### 3. genericFoods

Generische, immer vegane Lebensmittel (Typ C).

Felder: `_id`, `name`, `category`, `type` ("C"), `imageUrl`, `matching_key`, `nutrition` (kcal, fat, carbs, protein, salt per 100g)

### 4. shoppinglists

User-Einkaufslisten mit eingebetteten Items.

Felder:
- `_id`, `userId`, `name`, `supermarket`, `createdAt`
- `items`: List<ShoppingItem>
  - `itemId`, `productId` (ref zu supermarketProducts), `genericFoodId` (ref zu genericFoods)
  - `type` (A/B/C), `name`, `price`, `quantity`, `isChecked`

Hinweis: `productId` und `genericFoodId` sind zwei separate Felder weil sie auf zwei verschiedene Collections referenzieren. `type` bestimmt welches Feld relevant ist (A/B → productId, C → genericFoodId).

### 5. users

User-Profile (kein passwordHash – Auth0).

Felder: `_id`, `email`, `name`, `preferences` (dietaryType, defaultSupermarket, allergies[], diet[], budget)

### 6. savedProducts

Favoriten für Typ A & B Produkte.

Felder: `_id`, `userId`, `productId`, `type`, `name`, `brand`, `supermarket`, `price`, `imageUrl`, `savedAt`

### 7. baseRecipes

Kuratierte Grundrezepte.

Felder: `_id`, `title`, `category`, `prepTime_min`, `defaultServings`, `ingredients[]`, `instructions[]`, `totalNutrition_perServing`, `totalImpact_perServing`

### 8. savedRecipes

User-Favoriten und KI-generierte Rezepte.

Felder: `_id`, `userId`, `isAiGenerated`, `isSaved`, `title`, `prepTime_min`, `servings`, `ingredients[]`, `instructions[]`, `calculatedNutrition`, `calculatedImpact`, `createdAt`
- TTL-Index: 7 Tage für nicht-gespeicherte Rezepte (isSaved=false)

## Matching-Logik (SEHR WICHTIG)

Das Matching zwischen Frontend-Auswahl und Supermarkt-Produkten funktioniert so:

### matching_keys in supermarketProducts

Jedes Produkt hat ein `matching_keys`-Feld. Das ist ein Array von Objekten:
```json
[
  {"displayName": "Milch", "subcategory": "Kokos", "type": "A"},
  {"displayName": "Milch", "subcategory": "", "type": "A"}
]
```

- `displayName` → entspricht `frontendDictionary.displayName`
- `subcategory` → entspricht `additionalOptions.optionName` (leer = Base-Match ohne Spezifizierung)
- Ein Produkt kann mehrere Einträge haben (Cross-Category Matching)

### Matching-Flow

1. User wählt im Frontend z.B. "Milch" (aus frontendDictionary)
2. Optional: User spezifiziert per Long-Press z.B. "Kokos"
3. Backend-Query auf supermarketProducts:
   - `matching_keys` enthält Objekt mit `displayName == "Milch"` AND `subcategory == ""` (oder "Kokos" falls spezifiziert)
   - AND `supermarket == gewählter Supermarkt`
4. Ergebnis: Alle passenden veganen Alternativen

### Warum matching_keys und nicht nur plantify_cat_l2-l5?

matching_keys ermöglicht Cross-Category-Matching. Beispiel: "Betty Bossi Filetstreifen" ist unter l3="Rind & Schwein" kategorisiert, matcht aber auch "Poulet Filet" über matching_keys. Rein über Kategorie-Levels würde man diese Zuordnung verlieren.

### MatchingKey Embedded Object

```java
@Getter
@NoArgsConstructor
public class MatchingKey {
    private String displayName;
    private String subcategory;
    private String type;
}
```

## API Endpoints – Iteration 1 (Einkaufsliste)

### Frontend Dictionary
- `GET /api/frontend-dictionary` → Einmaliger Load, danach Frontend-Cache mit Fuse.js Fuzzy Search

### Matching (Kernstück)
- `POST /api/plantify-match` → Nimmt User-Items + Supermarkt + Präferenzen (Allergien, Budget, Diät) im Body. Frontend schickt Präferenzen gecacht mit – KEIN extra DB-Call.

### Listen-Verwaltung
- `POST /api/shoppinglists` → Liste speichern
- `DELETE /api/shoppinglists/{listId}` → Löschen (userId aus JWT, Backend prüft Ownership)
- `PATCH /api/shoppinglists/{listId}/supermarket` → Supermarkt wechseln → Re-Match (nur Typ A/B, Typ C bleibt)
- `POST /api/shoppinglists/{listId}/items` → Item hinzufügen
- `PATCH /api/shoppinglists/{listId}/items/{itemId}` → Item ersetzen

### Produktdetails
- `GET /api/products/{productId}` → Vollständige Infos (Zutaten, Nährwerte, Impact)
- `GET /api/products/{productId}/alternatives?filter=cheaper` → Max 4 Alternativen in gleicher L4-Kategorie
- `GET /api/generic-foods/{foodId}` → Basis-Nährwerte für Typ C
- `POST /api/saved-products` → Favorit speichern

## Impact-Vergleiche (Backend-Konstanten)

### CO₂
- 1 km Autofahrt: 0.1873 kg CO₂eq
- 1 km Flug: 0.2630 kg CO₂eq
- Skalierung: <1 kg → km Auto, 1-20 kg → Auto mit Städtevergleich, >20 kg → Flug

### Wasser
- 1 Min duschen: 12 L
- 1 Badewanne: 200 L
- Skalierung: <60 L → Duschminuten, ≥60 L → Badewannen

### Land
- 1 Parkplatz: 12.5 m²
- 1 Tennisplatz: 195.7 m²
- 1 Fussballfeld: 7140 m²
- Skalierung: <25 m² → Parkplätze, 25-500 m² → Tennisplätze, >500 m² → Fussballfelder

### Tierwohl
- Nur auf Listenebene aggregiert anzeigen (nicht pro Produkt)
- Berechnung: Math.ceil(sum(animals_affected_per_unit × quantity)) pro Tierkategorie
- Icons: 🐄🐷🐔🐟

## Aktueller Stand

- ✅ supermarketProducts in MongoDB Atlas importiert (2437 Dokumente)
- ✅ Spring Boot Projekt aufgesetzt, MongoDB-Verbindung steht
- ✅ MongoTestController zum Testen der DB-Verbindung vorhanden
- 🔲 NÄCHSTER SCHRITT: Models (DAOs + DTOs), dann Repositories, dann Controller

## Reihenfolge der Implementierung

1. Models erstellen (DAO mit @Document, DTOs)
2. Repositories erstellen (MongoRepository interfaces)
3. Service Layer (Business Logic, Matching)
4. Controller erstellen (@RestController)
5. Testen mit Postman
6. Commit mit "Closes #X"
