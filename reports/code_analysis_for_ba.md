# Plantify — Technische Code-Analyse für die BA

Dieses Dokument fasst die Architektur, Implementierung und Design-Entscheidungen
des Plantify-Codes zusammen. Es dient als Schreibgrundlage für Kapitel 5 der
Bachelor-Arbeit. Alle Angaben sind aus dem aktuellen Stand des Repositories
extrahiert; keine Wertungen.

---

## Sektion 1 — Tech-Stack-Übersicht

### Backend (Maven — `pom.xml`)

| Komponente | Version | Zweck |
|---|---|---|
| Java | 25 | Programmiersprache (Property `java.version`) |
| Spring Boot | 4.0.3 | Application-Framework (Parent-POM) |
| Spring AI Anthropic Starter | 2.0.0-M4 | Anthropic-Claude-Integration via `ChatClient` |
| Spring AI BOM | 2.0.0-M4 | Versions-Management für Spring AI |
| spring-boot-starter-data-mongodb | (von Boot 4.0.3) | MongoDB-Integration über Spring Data |
| spring-boot-starter-security | (von Boot 4.0.3) | Authentifizierung/Autorisierung |
| spring-boot-starter-oauth2-resource-server | (von Boot 4.0.3) | JWT-Resource-Server (Auth0) |
| spring-boot-starter-webmvc | (von Boot 4.0.3) | Servlet-basiertes REST-API |
| spring-dotenv | 4.0.0 | `.env`-File-Loader (paulschwarz) |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | Swagger-UI / OpenAPI-3-Generierung |
| Lombok | (transitive) | Boilerplate-Reduktion (`@Getter`, `@RequiredArgsConstructor`, …) |
| spring-boot-starter-data-mongodb-test | (Boot 4.0.3) | Test-Slice (transitiv: JUnit 5, Mockito) |
| spring-boot-starter-security-test | (Boot 4.0.3) | Spring Security Test-Helpers |
| spring-boot-starter-webmvc-test | (Boot 4.0.3) | MockMvc-Test-Slice |

Build-Plugins: `maven-compiler-plugin` (mit Lombok-Annotation-Processor),
`spring-boot-maven-plugin`, `maven-surefire-plugin` (mit erweitertem
Include-Pattern für `*IT.java`-Integrationstests).

### Frontend (npm — `frontend/package.json`)

| Komponente | Version | Zweck |
|---|---|---|
| Svelte | ^5.55.2 | UI-Framework (mit Runes — `$state`, `$derived`, `$effect`) |
| @sveltejs/kit | ^2.57.0 | Routing + Build-Pipeline |
| @sveltejs/vite-plugin-svelte | ^7.0.0 | Vite-Integration |
| @sveltejs/adapter-static | ^3.0.10 | Statisches Build (für Spring Boot Static Hosting) |
| @sveltejs/adapter-auto | ^7.0.1 | Adapter-Autodetection |
| Vite | ^8.0.7 | Dev-Server + Build |
| @auth0/auth0-spa-js | ^2.19.2 | Auth0-Login-Flow im Browser |
| fuse.js | ^7.3.0 | Fuzzy-Search im Frontend Dictionary |
| Vitest | ^4.1.5 | Test-Runner |
| happy-dom | ^20.9.0 | DOM-Stub für Tests |
| @testing-library/svelte | ^5.3.1 | Component-Test-API (vorbereitet, aktuell nur für Stores genutzt) |
| @testing-library/jest-dom | ^6.9.1 | DOM-Assertion-Helpers |
| @types/node | ^25.6.0 | Node-Typen für Vite-Config |

### Datenbank

- **MongoDB Atlas** (Cluster `cluster0.0neq3.mongodb.net`, Region `EU_CENTRAL_1`,
  AWS-Provider, Replica-Set `atlas-wqxq9v-shard-0`)
- Verbindung über `spring.mongodb.uri` (Spring Boot 4 hat das Property-Prefix
  von `spring.data.mongodb.*` auf `spring.mongodb.*` umgestellt — verifiziert
  via `MongoProperties`-Reflektion in `spring-boot-mongodb:4.0.3`)
- 9 Collections produktiv (siehe Sektion 4)
- MongoDB-Server-Version: nicht im Repo dokumentiert; Atlas-Standardversion

### Externe Dienste

| Dienst | Verwendung | Authentifizierung |
|---|---|---|
| **Anthropic** (Claude Sonnet 4) | AI-Recipe-Generation, Coach-Chatbot | `${ANTHROPIC_API_KEY}` aus `.env` |
| **Auth0** | User-Authentifizierung über JWT | `${AUTH0_DOMAIN}` + `${AUTH0_AUDIENCE}` |
| **Pexels** | Stock-Photos für AI-generierte Rezepte (Fallback) | `${PEXELS_API_KEY}` |

Anthropic-Modell-Konfiguration ([application.properties](src/main/resources/application.properties)):

```properties
spring.ai.anthropic.chat.options.model=claude-sonnet-4-20250514
spring.ai.anthropic.chat.options.temperature=0.7
spring.ai.anthropic.chat.options.max-tokens=2000
```

---

## Sektion 2 — Frontend-Architektur

### Verzeichnisstruktur (Top-Level)

```
frontend/
├── src/
│   ├── lib/
│   │   ├── api.js              authFetch() — fetch-Wrapper mit Bearer-Token
│   │   ├── data/
│   │   │   └── infoArticles.js Knowledge-Base für Info-Page (32 Artikel)
│   │   ├── stores/             7 Svelte-Stores (siehe unten)
│   │   ├── assets/             Statische Bilder
│   │   └── index.js            (Lib-Index, leer/exportiert nur)
│   └── routes/                 22 SvelteKit-Routes (alles Top-Level-Pages)
├── tests/
│   ├── stores/                 Vitest-Tests pro Store
│   ├── mocks/                  $app/environment + $lib/api Stubs
│   └── setup.js                Globales Vitest-Setup
├── package.json
├── vite.config.js              Build-Config (Output → src/main/resources/static)
└── vitest.config.js            Test-Config (separat von vite.config)
```

**Beobachtung:** Es gibt **keine `lib/components/`-Ebene**. Alle 22 `.svelte`-Files
liegen in `src/routes/` und sind Top-Level-Page-Routes. Wiederverwendbare UI-Logik
ist überwiegend inline in den Page-Routes definiert (z.B. `liste/+page.svelte`
hat ~1500 Zeilen mit Modal-Logik, Match-Anzeige, Item-Rendering, Toast-System).

### Routen-Übersicht

| Route | Zweck |
|---|---|
| `/` | Landing/Redirect je nach Auth-Status |
| `/landing` | Marketing-Page für unauthentifizierte User |
| `/login` | Auth0-Login-Trigger |
| `/signup` | Auth0-Signup-Trigger |
| `/onboarding` | Erste-Schritte-Flow (Diät, Allergien, Default-Supermarkt) |
| `/profil` | User-Profil verwalten |
| `/liste` | Live-Einkaufsliste mit Match (Hauptarbeitsfläche) |
| `/shoppinglists` | Übersicht gespeicherter Listen + Favoriten + Rezepte (Tab-Bar) |
| `/products/[productId]` | Produkt-Detail (Zutaten, Nährwerte, Impact, Alternativen) |
| `/products/eier` | Spezialseite für Eier (DIY-Ersatz statt Substitut) |
| `/rezepte` | Rezept-Übersicht (Slim-Liste + Saved-Recipes) |
| `/rezepte/[recipeId]` | Rezept-Detail (lädt aus `/api/saved-recipes`) |
| `/rezepte/anpassen/[recipeId]` | AI-Generierung mit Base-Recipe-Vorlage |
| `/rezepte/erstellen` | AI-Generierung von Grund auf (kein Base-Recipe) |
| `/rezepte/kategorie/[categoryId]` | Recipe-Liste pro Kategorie |
| `/rezepte/diy/[id]` | DIY-Recipe-Detail |
| `/info` | Coach-Chatbot + Kategorie-Übersicht |
| `/info/[category]` | Artikel-Liste pro Kategorie |
| `/info/ueber-uns` | Statische "Über uns"-Seite |
| `/info/impressum` | Statisches Impressum |
| `/info/datenschutz` | Statische Datenschutz-Seite |

### Stores (`src/lib/stores/`)

| Datei | Exporte | Funktionen |
|---|---|---|
| `auth.js` | `authClient`, `accessToken`, `user`, `isAuthenticated`, `userLoading` | Auth0-Wrapper; `login()`, `logout()`, `handleRedirect()` |
| `dictionary.js` | `dictionaryStore`, `loadDictionary()`, `groupByCategory()`, `searchDictionary()` | Frontend-Dictionary (Items für Listen-Add-Modal) + Fuzzy-Search via Fuse.js |
| `recentProducts.js` | `recentProducts`, `addRecentProduct()` | Per-User localStorage für zuletzt geöffnete Produkte (max 20) |
| `recipeCreation.js` | `recipeCreationStore`, `resetRecipeCreation()` | Trivialer State für Recipe-Generation-Form |
| `shoppingList.js` | `shoppingListStore`, `matchResults`, `isVeganized`, `savedListId`, `itemIdMap`, `addItem()`, `removeItem()`, `updateQuantity()`, `resetForLoad()`, `resetAll()` | Hauptstore der App; persistiert nach `sessionStorage` |
| `supermarket.js` | `supermarketStore` | Trivial: `writable(null)` |
| `user.js` | `currentUser`, `userLoading`, `loadCurrentUser()`, `updateCurrentUser()` | Backend-User-Profil (vom `/api/users/me`) |

#### Beispiel: reaktiver Store mit Aktions-Funktionen ([`shoppingList.js`](frontend/src/lib/stores/shoppingList.js))

```javascript
import { writable, get } from 'svelte/store';
import { browser } from '$app/environment';

export const shoppingListStore = writable(initial?.items ?? []);
export const matchResults     = writable(initial?.matchResults ?? []);

let nextId = initial?.nextId ?? 1;

export function addItem(entry, subcategory = '') {
    shoppingListStore.update((items) => {
        const exists = items.find(
            (i) => i.frontendDictionaryId === entry.id && i.subcategory === subcategory
        );
        if (exists) {
            return items.map((i) =>
                i.frontendDictionaryId === entry.id && i.subcategory === subcategory
                    ? { ...i, quantity: i.quantity + 1 } : i
            );
        }
        return [...items, { id: nextId++, displayName: entry.displayName, /* ... */ }];
    });
}
```

### Wichtige Components

Da es keine isolierten Components gibt, hier die **5 wichtigsten Page-Routes**
nach Logik-Komplexität:

| Page-Route | Zeilen (ca.) | Inhalte |
|---|---|---|
| `liste/+page.svelte` | ~1500 | Add-Modal, Subcat-Modal, Supermarket-Modal, Match-Anzeige, Impact-Karten, Save-Modal, Toast-System, `computeImpact()`-Logik (~70 Zeilen) |
| `products/[productId]/+page.svelte` | ~900 | Produkt-Header, Zutaten, Nährwerte, Impact-Block, Alternativ-Liste, "Zur Liste hinzufügen"-Modal |
| `rezepte/anpassen/[recipeId]/+page.svelte` | ~400 | Preferences-Form, Generate-Button, Loading-State |
| `info/+page.svelte` | ~350 | Coach-Chat-UI, Message-History, History-Truncation (`MAX_API_HISTORY=10`) |
| `shoppinglists/+page.svelte` | ~750 | Drei-Tab-Interface (Listen, Produkte, Rezepte), `loadListIntoStore()` |

#### Beispiel: Component-Snippet (Match-Card-Render aus `liste/+page.svelte`)

```svelte
{#each Object.entries(matchGrouped) as [cat, items] (cat)}
    <h3 class="cat-header">{cat}</h3>
    {#each items as result (result.itemId)}
        <div class="match-card">
            <span class="orig-name">{result.displayName}</span>
            {#if result.suggestedProduct}
                <span class="alt-name">→ {result.suggestedProduct.name}</span>
                <span class="price">CHF {result.suggestedProduct.price.toFixed(2)}</span>
            {:else if result.fallbackDiyRecipeId}
                <span class="diy-hint">DIY: {result.fallbackDiyRecipeTitle}</span>
            {:else if result.genericFood}
                <span class="generic">{result.genericFood.name}</span>
            {/if}
        </div>
    {/each}
{/each}
```

---

## Sektion 3 — Backend-Architektur

### Package-Struktur

```
ch.zhaw.plantifyme/
├── PlantifymeApplication.java   Spring Boot Bootstrap
├── config/                      4 Konfigurations-Klassen
├── controller/                  16 REST-Controller
├── data/                        InfoArticles (Coach-Wissensbasis, hardcoded)
├── model/                       18 Domain-Klassen + dto/ Subpackage
│   └── dto/                     25 Request/Response-DTOs
├── repository/                  9 Spring-Data-MongoDB-Repos
├── service/                     15 Service-Klassen (inkl. 2 Custom-Exceptions)
└── util/                        AuthUtil + NutritionUtil
```

### Controller-Übersicht (16)

| Controller | Base-Path | Endpoints | Zweck |
|---|---|---|---|
| `PlantifyMatchController` | `/api/plantify-match` | 1 | Kern-Match-Endpoint |
| `ShoppingListController` | `/api/shoppinglists` (über `/api`) | 11 | CRUD + Spezialaktionen für Einkaufslisten |
| `BaseRecipeController` | `/api/base-recipes` | 3 | Read-only Base-Recipes |
| `DiyRecipeController` | `/api/diy-recipes` | 3 | Read-only DIY-Recipes |
| `RecipeGenerationController` | `/api/recipes` | 1 | AI-Recipe-Generation |
| `SavedRecipeController` | `/api/saved-recipes` | 4 | User-spezifische Recipe-Speicherung |
| `SavedProductController` | `/api/saved-products` | 3 | Produkt-Favoriten |
| `CoachController` | `/api/coach` | 1 | AI-Chatbot |
| `ImpactController` | `/api/impact` | 1 | Impact-Aggregation |
| `UserController` | `/api/users` | 2 | User-Profil |
| `SupermarketProductController` | `/api/products` | 3 | Produkt-Details + Alternativen |
| `GenericFoodController` | `/api/generic-foods` | 2 | Generische Lebensmittel |
| `FrontendDictionaryController` | `/api/frontend-dictionary` | 1 | Dictionary-Lookup |
| `MongoTestController` | `/testmongodb` | 1 | DB-Connection-Smoke-Test |
| `ProductTestController` | `/api/test/*` | 6 | Diagnose-Endpoints |
| `AiTestController` | `/api/ai/test` | 1 | Spring-AI-Smoke-Test |

### Service-Übersicht (13 + 2 Exceptions)

| Service | Zweck (1-Satz) |
|---|---|
| `PlantifyMatchService` | Kern-Match-Algorithmus: Items → vegane Alternativen pro Supermarkt mit Subcategory-Fallback und DIY-/Generic-Food-Fallback |
| `ShoppingListService` | Einkaufslisten-Operationen (Supermarkt-Wechsel, Item-Add/Remove/Replace, Recipe-Ingredients-Add); delegiert Match-Logik an `PlantifyMatchService` |
| `BaseRecipeService` | Read-only Service für vordefinierte Rezepte (DTO-Mapping) |
| `DiyRecipeService` | DIY-Rezepte + `calculateNutritionPer100g`-Aggregation aus Zutaten via `NutritionUtil` |
| `RecipeGenerationService` | AI-Rezept-Generierung mit Anthropic Claude Sonnet 4; Base-Recipe-Vorlage als optionaler Anker |
| `RecipeImageResolver` | Hierarchische Bildauflösung: Base-Recipe → Smart-Match → Pexels-API → Kategorie-Default → Generic Fallback |
| `CoachService` | AI-Chatbot mit Stichwort-basiertem RAG (max 3 Artikel) aus `InfoArticles.ALL_ARTICLES` |
| `ImpactService` | Aggregation von CO₂/Wasser/Land/Tierwohl über alle User-Listen mit Vergleichswert-Strings |
| `UserService` | User-CRUD (`getOrCreateUser`, `updateUser`); `firstName` ist Pflichtfeld |
| `SavedRecipeService` | User-spezifische Recipe-Speicherung mit 7-Tage-TTL für nicht gespeicherte AI-Rezepte |
| `SavedProductService` | Produkt-Favoriten mit Conflict-Detection (Duplikate) und Owner-Check |
| `GenericFoodService` | `findFlexible()` mit Fuzzy-Lookup-Kette (exakt → Präfix-Stripping → Regex → Wort-Suche) |
| `SupermarketProductService` | Alternativ-Produkte in derselben L4-Kategorie |
| `OwnershipException` | RuntimeException für 403-Fälle (Liste/Produkt gehört einem anderen User) |
| `ConflictException` | RuntimeException für 409-Fälle (Favorit existiert bereits) |

### Repository-Übersicht (9)

Alle erweitern `MongoRepository<T, String>`:

| Repository | Custom-Methoden |
|---|---|
| `ShoppingListRepository` | `findByUserId(userId)` |
| `SupermarketProductRepository` | `findByPlantifyCatL4AndSupermarket`, `findByPlantifyCatL4AndSupermarketAndIdNot`, **`findByMatchingKeyAndSupermarket`** (`@Query` mit `$elemMatch`), `findByMatchingKey`, `findByMatchingDisplayNameWithImage` |
| `GenericFoodRepository` | `findByFrontendDictionaryId`, `findByName`, `findByNameContainingIgnoreCase` |
| `DiyRecipeRepository` | `findByDisplayNameAndSubcategory` (`@Query` mit `$elemMatch`), `findByMatchingDisplayName` |
| `BaseRecipeRepository` | `findByCategory` |
| `FrontendDictionaryRepository` | `findFirstByType`, `findByDisplayName` |
| `SavedRecipeRepository` | `findByUserId` |
| `SavedProductRepository` | `findByUserId`, `findByUserIdAndProductId` |
| `UserRepository` | (keine — Standard-CRUD reicht) |

### Konfigurations-Klassen

| Klasse | Zweck |
|---|---|
| `SecurityConfig` | `SecurityFilterChain`-Bean: `cors`, `csrf disabled`, statische Assets + `/landing/**`, `/login/**`, `/signup/**`, Swagger-Pfade `permitAll`, `/api/**` `authenticated`, JWT-Resource-Server-Config |
| `MongoConfig` | TTL-Index auf `savedRecipes.expiresAt` für 7-Tage-Auto-Cleanup |
| `OpenApiConfig` | OpenAPI-Bean mit Bearer-JWT-SecurityScheme + Plantify-Metadaten |
| `SpaConfiguration` | SvelteKit-Static-Hosting (Forwards für Client-Side-Routing) |

#### Beispiel-Snippet: PlantifyMatchService.match() (Kern-Match-Algorithmus)

```java
public PlantifyMatchResponse match(PlantifyMatchRequest request) {
    List<MatchResult> results = new ArrayList<>();
    for (MatchRequestItem item : request.getItems()) {
        MatchResult result = new MatchResult();
        result.setItemId(item.getItemId());
        result.setOriginalName(item.getDisplayName());
        result.setType(item.getType());

        if ("C".equals(item.getType())) {
            genericFoodRepository.findByFrontendDictionaryId(item.getFrontendDictionaryId())
                    .ifPresent(result::setGenericFood);
        } else {
            String subcategory = item.getSubcategory() != null ? item.getSubcategory() : "";
            // L5/specific match
            List<SupermarketProduct> candidates = supermarketProductRepository
                    .findByMatchingKeyAndSupermarket(item.getDisplayName(), subcategory, request.getSupermarket());
            candidates = filterAllergens(candidates, request.getPreferences());
            candidates = sortByPreference(candidates, request.getPreferences());
            String matchLevel = candidates.isEmpty() ? null : "specific";
            // L4/broader fallback if subcategory was non-empty
            if (candidates.isEmpty() && !subcategory.isEmpty()) {
                candidates = supermarketProductRepository
                        .findByMatchingKeyAndSupermarket(item.getDisplayName(), "", request.getSupermarket());
                candidates = filterAllergens(candidates, request.getPreferences());
                candidates = sortByPreference(candidates, request.getPreferences());
                if (!candidates.isEmpty()) matchLevel = "broader";
            }
            // ... DIY-Fallback + Generic-Food-Fallback
        }
        results.add(result);
    }
    PlantifyMatchResponse response = new PlantifyMatchResponse();
    response.setMatches(results);
    return response;
}
```

---

## Sektion 4 — Datenmodell

### Document-Klassen-Übersicht

| Java-Klasse | Collection | Wichtigste Felder | Beziehungen / Embedded |
|---|---|---|---|
| `User` | `users` | `id` (Auth0-sub), `email`, `firstName`, `lastName`, `currentDiet`, `goals[]`, `dietaryPreferences[]`, `allergies[]`, `defaultSupermarket`, `onboardingCompleted` | — |
| `ShoppingList` | `shoppinglists` | `id`, `userId`, `name`, `supermarket`, `createdAt`, `items[]` | **embedded:** `List<ShoppingItem>` |
| `SupermarketProduct` | `supermarketProducts` | `id`, `supermarket`, `articleId`, `name`, `brand`, `price`, `amount`, `qty`, `unit`, Nährwerte (8 Felder), Impact-Werte (12 Felder), `animalType`, `animalsAffectedPerUnit`, `plantifyCatL1..L5`, `matchingKeys[]` | **embedded:** `List<MatchingKey>` |
| `GenericFood` | `genericFoods` | `id`, `frontendDictionaryId`, `name`, `category`, `type` (immer "C"), `imageUrl`, `kcalPer100g`, `fatPer100g`, `proteinPer100g`, `saltPer100g`, etc. | — |
| `BaseRecipe` | `baseRecipes` | `id`, `title`, `description`, `category`, `prepTimeMin`, `difficulty`, `defaultServings`, `imageUrl`, `ingredients[]`, `instructions[]`, `totalNutritionPerServing` | **embedded:** `List<RecipeIngredient>`, `List<RecipeStep>`, `RecipeNutrition` |
| `DiyRecipe` | `diyRecipes` | `id`, `title`, `description`, `prepTimeMin`, `difficulty`, `imageUrl`, `useCase`, `matchingKeys[]`, `ingredients[]`, `instructions[]` | **embedded:** `List<MatchingKey>`, `List<DiyIngredient>`, `List<DiyStep>` |
| `FrontendDictionary` | `frontendDictionary` | `id`, `displayName`, `uiCategory`, `type`, `matchingKeys` (Map), `additionalOptions[]` | **embedded:** `List<AdditionalOption>` |
| `SavedRecipe` | `savedRecipes` | `id`, `userId`, `isAiGenerated`, `isSaved`, `title`, `category`, `prepTimeMin`, `servings`, `imageUrl`, `ingredients[]`, `instructions[]`, `totalNutritionPerServing`, `preferences`, `baseRecipeId`, `createdAt`, **`expiresAt`** (TTL für nicht gespeicherte) | **embedded:** Recipe-Komponenten + `RecipePreferences` |
| `SavedProduct` | `savedProducts` | `id`, `userId`, `productId`, `type`, `name`, `brand`, `price`, `supermarket`, `imageUrl`, `savedAt` | — (Referenz auf `supermarketProducts` via `productId`) |

### Type-A/B/C-System

Klassifikationssystem für Lebensmittel-Items:

| Type | Bedeutung | Wo gespeichert | Impact-Daten? | Beispiele |
|---|---|---|---|---|
| **A** | Vegane Substitut-Produkte mit dokumentierten Einsparungen ggü. tierischem Original | `supermarketProducts` (Type-A: 453 Produkte total) | ✅ Ja — `impact_co2_saved_*`, `animal_type`, `animals_affected_per_unit` | Hafer-Drink, veganes Hackfleisch, Tofu-Wurst |
| **B** | Bereits vegane Produkte ohne Substitut-Bezug (in DB mit Produktinfos für Einkaufsliste) | `supermarketProducts` | ❌ Nein | Linsen, Sojasauce, Hülsenfrüchte |
| **C** | Brand-lose, immer vegane Lebensmittel — kein Supermarkt-Bezug | `genericFoods` | ❌ Nein (nur Nährwerte pro 100g) | Apfel, Tomate, Karotte |

**Wichtige Klarstellung zum Code-Verhalten** (verifiziert in der ImpactValidationTest-Phase):
Type A im DB-Schema bezeichnet die **vegane Alternative** (nicht das tierische Original).
Das `animal_type`-Feld auf einem Type-A-Produkt sagt aus, _welche Tier-Industrie das
Produkt ersetzt_, nicht welches Tier das Produkt enthält.

### `matching_keys`-Struktur

Auf `SupermarketProduct` und `DiyRecipe` ist `matchingKeys` ein `List<MatchingKey>`,
wobei `MatchingKey` aus drei Strings besteht:

```java
public class MatchingKey {
    private String displayName;   // z.B. "Milch"
    private String subcategory;   // z.B. "Hafer" oder "" (leer = Base-Match)
    private String type;          // A, B oder C
}
```

Die Match-Query nutzt MongoDB's `$elemMatch`:

```
{'matching_keys': {$elemMatch: {'displayName': ?0, 'subcategory': ?1}}, 'supermarket': ?2}
```

Ein einzelnes Produkt kann mehrere `MatchingKey`-Einträge haben (Cross-Category-Matching).
Beispiel: Eine Betty-Bossi-Filetstreifen-Packung kann sowohl `{displayName="Poulet",
subcategory=""}` als auch `{displayName="Rind", subcategory=""}` als Matching-Keys
tragen, sodass sie für beide User-Wünsche als Substitut auftaucht.

### `plantify_cat_l1`–`plantify_cat_l5`-Hierarchie

Auf `SupermarketProduct` als 5-stufige Kategorie:

| Level | Beispiel-Wert |
|---|---|
| `plantify_cat_l1` | "Nahrungsmittel" |
| `plantify_cat_l2` | "Wurst & Aufschnitt" |
| `plantify_cat_l3` | "Aufschnitt" |
| `plantify_cat_l4` | "Pastrami" |
| `plantify_cat_l5` | "Vegan-Pastrami" (oft leer) |

Die Hierarchie wird im aktuellen Match-Code **nicht** zur Suche genutzt — nur die
flachen `matchingKeys` plus Subcategory-Fallback. Die Felder werden für andere
Zwecke verwendet (z.B. `findByPlantifyCatL4AndSupermarket` für die "Alternative
Produkte"-Anzeige auf der Produktdetail-Seite).

### Embedded vs. Referenzen

| Pattern | Wo |
|---|---|
| **Embedded** (Document mit eingebettetem `List<X>`) | `ShoppingList.items`, `SupermarketProduct.matchingKeys`, `BaseRecipe.ingredients/instructions`, `DiyRecipe.ingredients/instructions`, `FrontendDictionary.additionalOptions`, `SavedRecipe.ingredients/instructions/totalNutritionPerServing` |
| **Referenz per ID-String** (kein DBRef) | `ShoppingItem.productId` → `supermarketProducts._id`, `ShoppingItem.genericFoodId` → `genericFoods._id`, `SavedProduct.productId` → `supermarketProducts._id`, `SavedRecipe.baseRecipeId` → `baseRecipes._id`, `GenericFood.frontendDictionaryId` → `frontendDictionary._id` |

Spring Data MongoDB lädt Referenzen **nicht automatisch** — Code muss explizit per
Repository-Lookup nachladen (z.B. `supermarketProductRepository.findById(item.getProductId())`).

### Designer-Entscheidungen — Datenduplikation

- **Knowledge-Base zweifach:** `data/InfoArticles.java` (Java, 34 Artikel) und
  `frontend/src/lib/data/infoArticles.js` (JS, 32 Artikel). Der Coach-Service liest
  aus dem Backend-Datensatz; das Frontend rendert aus dem JS-Datensatz auf der
  Info-Page. Manuelle Synchronisation nötig.
- **Vergleichswert-Konstanten dupliziert:** Die Faktoren für "X km Auto",
  "Y Badewannen" etc. existieren als hardcoded Doubles sowohl in
  `ImpactService.java` als auch in `liste/+page.svelte` `computeImpact()`.

---

## Sektion 5 — Kern-Algorithmen

### 5a) Match-Algorithmus — `PlantifyMatchService.match()`

**Schritt-für-Schritt für jedes Item im Request:**

1. `MatchResult` initialisieren mit `itemId`, `originalName`, `type` aus dem Request-Item
2. **Type-C-Pfad:** Nutze `frontendDictionaryId` für direkten Lookup im `genericFoodRepository.findByFrontendDictionaryId()`
3. **Type-A/B-Pfad:**
   1. **Specific Match:** `findByMatchingKeyAndSupermarket(displayName, subcategory, supermarket)` → `matchLevel = "specific"`
   2. Allergen-Filter via `filterAllergens()` (ausschluss wenn Produkt einen Allergen-String enthält oder eine Zutat einen Allergen-String enthält)
   3. Preference-Sort via `sortByPreference()`: `high protein` → desc nach `proteinG`; default → asc nach `price`
   4. **Broader Fallback:** wenn Specific leer + Subcategory non-empty → retry mit `subcategory=""` → `matchLevel = "broader"`
   5. Wenn weiterhin leer: **DIY-Fallback** via `diyRecipeRepository.findByMatchingDisplayName()` — erstes DIY-Rezept gewinnt
   6. Wenn auch keine DIYs: **Generic-Food-Fallback** via `genericFoodRepository.findByName()` (Type bleibt original — wichtig für Reload-Pfade)

**Subcategory-Fallback** wurde in Phase 6 eingeführt, um Cases wie "Aufschnitt + Pastrami"
zu lösen: Wenn kein Pastrami-getaggtes Produkt existiert, wird auf alle Aufschnitte
ohne Subcategory-Spezifizierung erweitert. Das `matchLevel`-Feld kann vom Frontend
für Hinweise wie "Aus dem gleichen Sortiment" verwendet werden.

**Code-Snippet** (Kernlogik, ~25 Zeilen):

```java
public PlantifyMatchResponse match(PlantifyMatchRequest request) {
    List<MatchResult> results = new ArrayList<>();
    for (MatchRequestItem item : request.getItems()) {
        MatchResult result = new MatchResult();
        result.setItemId(item.getItemId());
        result.setType(item.getType());

        if ("C".equals(item.getType())) {
            genericFoodRepository.findByFrontendDictionaryId(item.getFrontendDictionaryId())
                    .ifPresent(result::setGenericFood);
        } else {
            String subcategory = item.getSubcategory() != null ? item.getSubcategory() : "";
            List<SupermarketProduct> candidates = supermarketProductRepository
                    .findByMatchingKeyAndSupermarket(item.getDisplayName(), subcategory, request.getSupermarket());
            candidates = filterAllergens(candidates, request.getPreferences());
            candidates = sortByPreference(candidates, request.getPreferences());
            String matchLevel = candidates.isEmpty() ? null : "specific";

            if (candidates.isEmpty() && !subcategory.isEmpty()) {
                candidates = supermarketProductRepository
                        .findByMatchingKeyAndSupermarket(item.getDisplayName(), "", request.getSupermarket());
                candidates = filterAllergens(candidates, request.getPreferences());
                candidates = sortByPreference(candidates, request.getPreferences());
                if (!candidates.isEmpty()) matchLevel = "broader";
            }

            if (!candidates.isEmpty()) {
                result.setSuggestedProduct(candidates.get(0));
                result.setMatchLevel(matchLevel);
            }
            // [DIY-Fallback + Generic-Fallback siehe Source]
        }
        results.add(result);
    }
    return new PlantifyMatchResponse(results);
}
```

### 5b) Recipe-Generation — `RecipeGenerationService.generateRecipe()`

**Logik:**

1. Wenn `request.getBaseRecipeId()` gesetzt: lade `BaseRecipe` aus DB
2. Baue User-Prompt via `buildUserPrompt(request, baseRecipe)`:
   - Title + Description aus Request
   - Wenn Base-Recipe vorhanden: "Basiere auf folgendem Grundrezept und passe es an die Anforderungen an. BEHALTE den Originaltitel bei: {title}. Zutaten: {ing1, ing2, …}"
   - Custom-Ingredients und Preferences hinzufügen
3. `chatClient.prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content()` — Anthropic-Aufruf
4. JSON aus der Response parsen (mit Markdown-Cleanup `removeBackticks()`)
5. Map zu `SavedRecipe` mit:
   - `userId` aus `AuthUtil.requireCurrentUserId()`
   - `isAiGenerated=true`, `isSaved=false`
   - `imageUrl` über `RecipeImageResolver.resolveImage(title, baseRecipeId, category)`
   - `expiresAt = jetzt + 7 Tage`
6. Persistieren in `savedRecipes`-Collection

**Wann Base-Recipe genutzt:** Genau dann, wenn `request.getBaseRecipeId() != null`.
Auch leere Preferences führen nicht zum Skip — der AI-Call läuft immer, **kein
Skip-Pfad**. Base-Recipe-Einbettung hat im Green-AI-Test zu praktisch keiner
Token-Einsparung geführt (-1.8% im A/B-Vergleich), wirkt eher als Konsistenz-Anker.

**System-Prompt** (~107 Zeilen): enthält Rollen-Definition ("veganer Koch-Experte"),
Schweizer-Supermarkt-Hinweis, JSON-Schema-Spezifikation für strukturierte Antwort,
und eine ~30-zeilige Tabelle mit gültigen `matchingDisplayName`/`matchingSubcategory`-Werten
zur Backend-Match-Kompatibilität.

**Code-Snippet:**

```java
public SavedRecipe generateRecipe(RecipeGenerationRequest request) throws Exception {
    BaseRecipe baseRecipe = null;
    if (request.getBaseRecipeId() != null) {
        baseRecipe = baseRecipeRepository.findById(request.getBaseRecipeId()).orElse(null);
    }

    String userPrompt = buildUserPrompt(request, baseRecipe);
    String raw = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .content();

    String cleaned = cleanJsonResponse(raw);
    JsonNode json = objectMapper.readTree(cleaned);

    SavedRecipe savedRecipe = mapToSavedRecipe(json, request);
    return savedRecipeRepository.save(savedRecipe);
}
```

### 5c) Coach mit minimal-RAG — `CoachService.chat()`

**Logik:**

1. `findRelevantArticles(userMessage, 3)` — Stichwort-basiertes Scoring über alle 34 Artikel
2. `buildSystemPrompt(relevantArticles)` — `SYSTEM_PROMPT_HEADER` + Title/Category/Content der ≤3 selektierten Artikel
3. `buildUserPrompt(message, history)` — bisheriger Verlauf + neue User-Nachricht
4. `chatClient.prompt().system(...).user(...).options(maxTokens=200).call().content()`
5. Zusätzlich `referencedArticles(query)` — liefert Top-2 Artikel mit Score ≥3 für UI-Hinweise

**Stichwort-Score-Berechnung** (`scoreArticle`):
- `extractKeywords(query)`: lowercase, Filter Stop-Words (~50 Begriffe), Mindestlänge 3 Zeichen
- Pro Keyword:
  - Match in `title` (lowercase): +3
  - Match in `category`: +2
  - Match in `content`: `+min(occurrences, 5)` (gedeckelt)
- Summe über alle Keywords = Score; Sort desc, Limit 3

**Token-Effekt** (verifiziert in AiComparisonTest):
- A (mit RAG): ~1500 Input-Tokens (3 Artikel + Header)
- B (ohne RAG, alle 34 Artikel): ~10'700 Input-Tokens
- → **84.7% Token-Einsparung**, **80.5% Kosten-Einsparung**

**Code-Snippet:**

```java
public String chat(String message, List<ChatMessage> history) {
    List<Article> relevant = findRelevantArticles(message, 3);
    String systemPrompt = buildSystemPrompt(relevant);
    String userPrompt = buildUserPrompt(message, history);

    return chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .options(AnthropicChatOptions.builder().maxTokens(200).build())
            .call()
            .content();
}

public List<ScoredArticle> findScoredArticles(String userQuery, int maxArticles) {
    if (userQuery == null || userQuery.isBlank()) return List.of();
    List<String> keywords = extractKeywords(userQuery);
    if (keywords.isEmpty()) return List.of();
    List<ScoredArticle> scored = new ArrayList<>();
    for (Article article : InfoArticles.ALL_ARTICLES) {
        int score = scoreArticle(article, keywords);
        if (score > 0) scored.add(new ScoredArticle(article, score));
    }
    return scored.stream()
            .sorted(Comparator.comparingInt(ScoredArticle::score).reversed())
            .limit(maxArticles).toList();
}
```

### 5d) Impact-Aggregation — `ImpactService.computeTotal()`

**Logik:**

1. Lade alle `ShoppingList`s des Users via `findByUserId(userId)`
2. Pro Item: skip wenn `productId == null/blank`
3. Lookup `SupermarketProduct`; skip wenn nicht gefunden
4. **Tierwohl:** Wenn `animalType != null && animalsAffectedPerUnit != null`:
   - `animals.merge(animalType, animalsAffectedPerUnit × qty, Double::sum)` — Aggregation **vor** Math.ceil
5. **CO₂/Wasser/Land:** Pro Indikator (independent): wenn nicht null, `total += value × qty`
6. **Math.ceil** auf jede Tierart (nach Aggregation, nicht pro Item) — verhindert Doppel-Aufrundung
7. Round CO₂/Wasser/Land auf 1 Dezimal (`Math.round × factor`)
8. Compare-Strings:
   - CO₂ > 20 kg → "X km Flug" (`/ 0.2630`); sonst "X km Autofahrt" (`/ 0.1873`)
   - Wasser ≥ 60 L → "X volle Badewannen" (`/ 200`); sonst "X Min duschen" (`/ 12`)
   - Land > 500 m² → "X Fussballfelder" (`/ 7140`); 25-500 → "X Tennisplätze" (`/ 195.7`); <25 → "X Parkplätze" (`/ 12.5`)

**Math.ceil-Logik für Tierwohl:** Aggregation **vor** ceil bedeutet z.B.
`0.05 + 0.05 = 0.1 → ceil = 1` (1 Tier total), **nicht** `ceil(0.05) + ceil(0.05) = 2`.
Dieses Verhalten wird durch `ImpactServiceTest.ceilAfterSum` explizit dokumentiert.

**Code-Snippet:**

```java
public ImpactTotalDto computeTotal(String userId) {
    Map<String, Double> animals = new HashMap<>();
    animals.put("cow", 0.0); animals.put("pig", 0.0);
    animals.put("chicken", 0.0); animals.put("fish", 0.0);
    double totalCo2 = 0, totalWater = 0, totalLand = 0;

    List<ShoppingList> lists = shoppingListRepository.findByUserId(userId);
    for (ShoppingList list : lists) {
        if (list.getItems() == null) continue;
        for (ShoppingItem item : list.getItems()) {
            if (item.getProductId() == null || item.getProductId().isBlank()) continue;
            SupermarketProduct product = supermarketProductRepository.findById(item.getProductId()).orElse(null);
            if (product == null) continue;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;

            if (product.getAnimalType() != null && product.getAnimalsAffectedPerUnit() != null) {
                animals.merge(product.getAnimalType(), product.getAnimalsAffectedPerUnit() * qty, Double::sum);
            }
            if (product.getImpactCo2SavedKg() != null)   totalCo2   += product.getImpactCo2SavedKg() * qty;
            if (product.getImpactWaterSavedL() != null)  totalWater += product.getImpactWaterSavedL() * qty;
            if (product.getImpactLandSavedM2() != null)  totalLand  += product.getImpactLandSavedM2() * qty;
        }
    }
    Map<String, Integer> animalsRounded = new HashMap<>();
    for (var e : animals.entrySet()) animalsRounded.put(e.getKey(), (int) Math.ceil(e.getValue()));
    // [DTO-Bau mit round() + buildXxxCompare() — siehe Source]
}
```

---

## Sektion 6 — API-Endpoints

Sortiert nach Funktionsbereich. Alle `/api/*`-Endpoints sind authentifiziert
(JWT-Resource-Server). `/testmongodb` ist offen.

### Auth / User

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/users/me` | `UserController` | Eigenes Profil; legt User automatisch an wenn nicht existent |
| PATCH | `/api/users/me` | `UserController` | Profil aktualisieren; firstName ist Pflichtfeld (leer → keine Änderung) |

### Match-Logik (Kernstück)

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| POST | `/api/plantify-match` | `PlantifyMatchController` | Items + Supermarkt + Preferences → vegane Alternativen |

### Einkaufslisten (CRUD + Spezialaktionen)

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/shoppinglists` | `ShoppingListController` | Eigene Listen abrufen |
| GET | `/api/shoppinglists/{listId}` | `ShoppingListController` | Einzelne Liste |
| POST | `/api/shoppinglists` | `ShoppingListController` | Neue Liste anlegen |
| PUT | `/api/shoppinglists/{listId}` | `ShoppingListController` | Komplett-Update (name, supermarket, items) |
| DELETE | `/api/shoppinglists/{listId}` | `ShoppingListController` | Liste löschen |
| PATCH | `/api/shoppinglists/{listId}/supermarket` | `ShoppingListController` | Supermarkt wechseln + Re-Match (delegiert an `PlantifyMatchService`) |
| POST | `/api/shoppinglists/{listId}/items` | `ShoppingListController` | Item hinzufügen mit Match |
| DELETE | `/api/shoppinglists/{listId}/items/{itemId}` | `ShoppingListController` | Item entfernen |
| PATCH | `/api/shoppinglists/{listId}/items/{itemId}` | `ShoppingListController` | Item-Produkt ersetzen |
| POST | `/api/shoppinglists/{listId}/items/{itemId}/replace-with-diy` | `ShoppingListController` | Item durch DIY-Zutaten ersetzen |
| POST | `/api/shoppinglists/{listId}/items/batch` | `ShoppingListController` | Rezept-Zutaten zur Liste hinzufügen |
| POST | `/api/shoppinglists/from-recipe` | `ShoppingListController` | Liste aus Rezept anlegen |

### Rezepte

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/base-recipes` | `BaseRecipeController` | Alle Base-Rezepte (optional `?category=...`) |
| GET | `/api/base-recipes/slim` | `BaseRecipeController` | Slim-DTOs für Listenansicht |
| GET | `/api/base-recipes/{id}` | `BaseRecipeController` | Einzelnes Base-Rezept |
| GET | `/api/diy-recipes` | `DiyRecipeController` | DIY-Rezepte (optional `?displayName=&subcategory=`) |
| GET | `/api/diy-recipes/slim` | `DiyRecipeController` | Slim-DTOs |
| GET | `/api/diy-recipes/{id}` | `DiyRecipeController` | Einzelnes DIY-Rezept mit `nutritionPer100g` |
| POST | `/api/recipes/generate` | `RecipeGenerationController` | AI-Rezept generieren (mit oder ohne baseRecipeId) |
| GET | `/api/saved-recipes` | `SavedRecipeController` | Eigene gespeicherte/generierte Rezepte |
| GET | `/api/saved-recipes/{id}` | `SavedRecipeController` | Einzelnes Saved-Recipe |
| POST | `/api/saved-recipes/{id}/save` | `SavedRecipeController` | Als gespeichert markieren (entzieht TTL) |
| DELETE | `/api/saved-recipes/{id}` | `SavedRecipeController` | Saved-Recipe löschen |

### Produkte

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/products/{productId}` | `SupermarketProductController` | Produkt-Details |
| GET | `/api/products` | `SupermarketProductController` | Suche per displayName mit Image |
| GET | `/api/products/{productId}/alternatives` | `SupermarketProductController` | Max 4 Alternativen in derselben L4-Kategorie |
| GET | `/api/generic-foods/{foodId}` | `GenericFoodController` | Generic Food per ID |
| GET | `/api/generic-foods/search` | `GenericFoodController` | Flexible Suche |

### Favoriten

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| POST | `/api/saved-products` | `SavedProductController` | Produkt als Favorit speichern (409 bei Duplikat) |
| GET | `/api/saved-products` | `SavedProductController` | Favoriten abrufen |
| DELETE | `/api/saved-products/{id}` | `SavedProductController` | Favorit entfernen (mit Owner-Check) |

### Match-Helpers

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/frontend-dictionary` | `FrontendDictionaryController` | Komplettes Dictionary für Frontend-Cache |

### Impact

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/api/impact/total` | `ImpactController` | Aggregat über alle Listen mit Vergleichswerten |

### Coach

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| POST | `/api/coach/chat` | `CoachController` | AI-Chat mit Stichwort-RAG (max 500 Zeichen Input, max 200 Output-Tokens) |

### Diagnose / Test

| Methode | Endpoint | Controller | Zweck |
|---|---|---|---|
| GET | `/testmongodb` | `MongoTestController` | DB-Connection-Smoke-Test |
| GET, POST | `/api/test/*` | `ProductTestController` | 6 Diagnose-Endpoints für DB-Inhalte |
| POST | `/api/ai/test` | `AiTestController` | Spring-AI-Smoke-Test |

---

## Sektion 7 — Test-Suite-Übersicht

### Test-Kategorien & Klassen

| Kategorie | Test-Klasse | Tests | Beschreibung |
|---|---|---|---|
| **Smoke** | `PlantifymeApplicationTests` | 1 | `@SpringBootTest contextLoads()` |
| **Unit (Util)** | `NutritionUtilTest` | 28 | Mass-/Volumen-Konvertierung, Spoon/Cup, Normalisierung, Edge-Cases (parametrisierte Tests) |
| **Unit (Service)** | `PlantifyMatchServiceTest` | 28 | Match-Algorithmus: Type A/B, Type C, Allergen-Filter, DIY-Fallback, Generic-Fallback, Subcategory-Fallback (4 Tests), Request-Level-Concerns |
| **Unit (Service)** | `ImpactServiceTest` | 20 | Tier-Aggregation, CO₂/Wasser/Land, Compare-String-Schwellwerte, Edge-Cases |
| **Unit (Service)** | `DiyRecipeServiceTest` | 14 | `calculateNutritionPer100g`-Aggregation aus `NutritionUtil` + Public-CRUD |
| **Unit (Service)** | `UserServiceTest` | 13 | `getOrCreateUser`, `updateUser` mit firstName-Schutzlogik |
| **Unit (Service)** | `RecipeImageResolverTest` | 11 | Hierarchie BaseRecipe → Smart-Match → Pexels → Default |
| **Unit (Service)** | `ShoppingListServiceTest` | 15 | Operationen aller Endpoints, inkl. Recipe-Driven (replaceWithDiy, addRecipeIngredients) |
| **Unit (Service)** | `BaseRecipeServiceTest` | 7 | findAll/findAllSlim/findByCategory/findById |
| **Integration (Controller)** | `PlantifyMatchControllerIT` | 5 | MockMvc + `@MockitoBean` für Service; JWT via `.with(jwt())` |
| **Integration (Controller)** | `ShoppingListControllerIT` | 5 | dito mit Owner-403 + 401-Tests |
| **Integration (Controller)** | `UserControllerIT` | 5 | dito mit JWT-Claims-Mapping |
| **Cross-Path Konsistenz** | `MatchConsistencyTest` | 5 | Direkter `match()` vs. `changeSupermarket()` (delegiert intern) |
| **Opt-In (Reporting)** | `CoverageReportTest` | 1 | Match-Coverage über alle FrontendDictionary-Items × 3 Supermärkte; CSV-Output |
| **Opt-In (Reporting)** | `ImpactValidationTest` | 1 | 8 Substitutions-Cases mit Methodik-Sollwert-Vergleich; CSV-Output |
| **Opt-In (Reporting)** | `AiComparisonTest` | 1 | Green-AI A/B-Vergleich mit Anthropic API; ~$0.50 Kosten pro Run |
| **Frontend (Stores)** | `tests/stores/dictionary.test.js` | 7 | `groupByCategory`, `searchDictionary` (Fuse.js Fuzzy) |
| **Frontend (Stores)** | `tests/stores/shoppingList.test.js` | 8 | `addItem`, `removeItem`, `updateQuantity`, `resetAll` |
| **Frontend (Stores)** | `tests/stores/recipeCreation.test.js` | 1 | `resetRecipeCreation` |
| **Frontend (Smoke)** | `tests/smoke.test.js` | 1 | Vitest-Setup-Verifikation |

### Aktuelle Zahlen

- **Backend regulär:** 157 Tests grün (`mvnw test` ohne Opt-In-Flags)
- **Backend Opt-In:** 3 Reporting-Tests (CoverageReportTest / ImpactValidationTest / AiComparisonTest), nicht im Standard-Lauf
- **Frontend:** 16 Tests grün (`npm run test:unit`)
- **Total dokumentiert:** 173 Tests + 3 Opt-In = 176

### Coverage-Tools

- **Backend**: keine JaCoCo-Konfiguration im `pom.xml`. Coverage wird nicht automatisch
  gemessen. Stattdessen existiert `CoverageReportTest`, das die *Match-Coverage* der
  DB-Datenbasis misst (welche FrontendDictionary-Items haben Treffer in welchen
  Supermärkten) — eine fachliche, keine code-bezogene Coverage-Metrik.
- **Frontend**: keine `c8`/`v8`-Coverage-Konfiguration in `vitest.config.js`. Vitest
  würde Coverage über `--coverage` unterstützen, ist aber nicht im Test-Script aktiviert.

### Cross-Path-Konsistenz-Tests

`MatchConsistencyTest` ist ein eigens hinzugefügter Test-Typ, der entstand, nachdem
in Phase 4 (Diagnose-Phase der Match-Inkonsistenz-Bugs) festgestellt wurde, dass
`PlantifyMatchService.match()` und `ShoppingListService.changeSupermarket()`
historisch zwei verschiedene Match-Algorithmen hatten (siehe Refactor in Phase 4).

Der Test ruft beide Code-Pfade mit **identischen Inputs** auf (gleiche Items,
gleicher Supermarkt, gleiche User-Preferences) und vergleicht die ausgewählten
`productId`s. Aktuell deckt der Test 5 Szenarien ab:

1. Default-Sortierung (cheapest first) — beide wählen denselben Produkt-ID
2. Allergen-Filter (User mit `soy`) — beide ignorieren Soja-Produkte
3. High-Protein-Diet — beide wählen das proteinreichste Produkt
4. Subcategory-Fallback (Pastrami → Aufschnitt) — beide wählen identisches Broader-Match
5. Kein Supermarkt-Match — beide haben `productId=null` (Type bleibt original)

**Begründung der Existenz:** Verhindert künftige Algorithmus-Drift zwischen
`match()` und `changeSupermarket()`. Letzterer delegiert seit Phase 4 intern an
`match()`, aber der Test sichert das langfristig ab — selbst wenn die Implementation
geändert wird, wird die Verhaltens-Konsistenz garantiert.

### Test-Konventionen

- **Standard-Naming:** `XxxTest.java` für Unit-Tests
- **Integration-Tests:** `XxxIT.java`-Suffix (Maven Surefire-Plugin im `pom.xml`
  mit erweitertem Include-Pattern `**/*IT.java`)
- **Opt-In-Tests:** `@EnabledIfEnvironmentVariable` mit explizitem Env-Var
  (`RUN_COVERAGE`, `RUN_IMPACT_VALIDATION`, `RUN_AI_COMPARISON`)
- **Mockito-Strategie:** `@MockitoSettings(strictness = Strictness.LENIENT)` bei
  Tests mit Multi-Getter-Mocks (Domain-Entities ohne `@Setter`); Mocks immer in
  lokale Variablen extrahieren bei verschachtelten `when().thenReturn()`-Calls
  (Lesson Learned aus Phase 1, dokumentiert in mehreren Test-Klassen)
- **Spring-Boot-4-Spezifika** (entdeckt in Phase 3): `@WebMvcTest` lebt in
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (umgezogen);
  `@MockBean` ist deprecated → `@MockitoBean` aus Spring Framework 6.2+;
  `ObjectMapper` wird in `@WebMvcTest`-Slice nicht mehr automatisch bereitgestellt
  (manuell instantiieren)

---

## Anhang — Versions-Snapshot

Stand des Repositories zum Zeitpunkt dieser Analyse:

| Komponente | Version | Quelle |
|---|---|---|
| Java | 25 | `pom.xml:30` |
| Spring Boot | 4.0.3 | `pom.xml:8` |
| Spring AI Anthropic Starter | 2.0.0-M4 | `pom.xml:36` |
| Spring AI BOM | 2.0.0-M4 | `pom.xml:99` |
| spring-dotenv | 4.0.0 | `pom.xml:53` |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | `pom.xml:63` |
| Svelte | ^5.55.2 | `frontend/package.json:23` |
| @sveltejs/kit | ^2.57.0 | `frontend/package.json:17` |
| Vite | ^8.0.7 | `frontend/package.json:24` |
| Vitest | ^4.1.5 | `frontend/package.json:25` |
| @auth0/auth0-spa-js | ^2.19.2 | `frontend/package.json:28` |
| fuse.js | ^7.3.0 | `frontend/package.json:30` |
| MongoDB-Server | (Atlas-managed; Version nicht im Repo dokumentiert) | — |
| Anthropic-Modell | claude-sonnet-4-20250514 | `application.properties:5` |

Anzahl Code-Artefakte:

| Kategorie | Anzahl |
|---|---|
| Java-Klassen Total | 90 (Production-Code, ohne Tests) |
| Controller | 16 |
| Service-Klassen | 13 (+ 2 Custom-Exceptions) |
| Repository-Interfaces | 9 |
| Document-Klassen | 9 |
| DTOs | 25 |
| MongoDB-Collections | 9 |
| REST-Endpoints | 45 |
| OpenAPI-Tags | 14 |
| Frontend-Routes | 22 (alle als `+page.svelte`) |
| Stores | 7 |
| Test-Klassen | 16 (Backend) + 4 (Frontend) |
| Tests grün | 173 + 3 Opt-In = 176 |

