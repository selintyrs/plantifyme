# Plantify-Me

Web-App, die Einkaufslisten "plantifiziert": nicht-vegane Produkte werden automatisch durch vegane Alternativen aus Schweizer Supermärkten (Migros, Coop, Aldi) ersetzt. Die App zeigt den Umwelt-Impact (CO₂, Wasser, Land) und Tierwohl-Äquivalente an, generiert KI-Rezepte aus Schweizer Sortimenten und bietet einen Coach-Chatbot zu pflanzlicher Ernährung.

## Bachelorarbeit – Kontext

Dieses Repository entstand im Rahmen einer Bachelorarbeit an der **ZHAW (Zürcher Hochschule für Angewandte Wissenschaften)**. Plantify-Me ist eine end-to-end implementierte Web-App mit Backend, Frontend, Daten-Pipeline und Cloud-Deployment. Die ausführliche technische Code-Analyse für Kapitel 5 der Arbeit befindet sich unter [reports_final/code_analysis_for_ba.md](reports_final/code_analysis_for_ba.md).

## Tech-Stack

**Backend** (Java 21, Maven, `pom.xml`)
- Spring Boot 4.0.3 (`spring-boot-starter-webmvc`, `-data-mongodb`, `-security`, `-oauth2-resource-server`)
- Spring AI Anthropic Starter 2.0.0-M4 (Claude Sonnet 4 für Rezept-Generierung und Coach-Chatbot)
- springdoc-openapi 2.6.0 (Swagger-UI unter `/swagger-ui.html`)
- Lombok, spring-dotenv, JaCoCo 0.8.13

**Frontend** (`frontend/package.json`)
- Svelte 5 (Runes) + SvelteKit 2 + Vite 8
- Auth0 SPA SDK für JWT-Login
- Fuse.js für Fuzzy-Search im Frontend-Dictionary
- Vitest für Unit-Tests

**Datenbank**
- MongoDB Atlas (9 Collections, ~2'500 Produkte)

**Externe Dienste**
- Auth0 — Authentifizierung (Email/Passwort)
- Anthropic Claude — KI-Rezepte und Coach
- Pexels — Stock-Photos für KI-Rezepte (Fallback)

**Deployment**
- Docker Multi-Stage Build (Node-20 → Maven/Temurin-21 → JRE-Alpine)
- Render.com (Single-Container, Spring Boot served das gebaute Frontend statisch)
- GitHub Actions CI (Backend-Tests + JaCoCo, Frontend-Vitest)

## Projektstruktur

```
plantifyme/
├── src/main/java/ch/zhaw/plantifyme/
│   ├── controller/        16 REST-Controller (46 Endpoints)
│   ├── service/           13 Services (+ 2 Custom-Exceptions)
│   ├── repository/        9 MongoDB-Repositories
│   ├── model/             18 Domain-Klassen + 26 DTOs
│   ├── config/            Security, Mongo, OpenAPI, SPA-Hosting
│   ├── data/              Coach-Wissensbasis (34 Artikel, hardcoded)
│   └── util/              AuthUtil, NutritionUtil
├── src/main/resources/
│   ├── application.properties
│   └── static/            (gebaute Frontend-Assets nach `npm run build`)
├── src/test/java/         Backend-Tests (Unit, Integration, Opt-In)
├── frontend/
│   ├── src/
│   │   ├── routes/        21 SvelteKit-Routes
│   │   └── lib/           stores/, components/, utils/, data/, api.js
│   ├── static/            PWA-Assets (Manifest, Service Worker, Icons)
│   └── tests/             Vitest-Tests
├── .github/workflows/     CI-Pipeline
├── Dockerfile             Multi-Stage Build
├── reports/               Generierte CSVs (Coverage, Impact-Validation, AI-Comparison)
└── reports_final/         Finale Anhang-Versionen für die BA
```

## Lokales Setup

### Voraussetzungen
- Java 21 (Temurin empfohlen)
- Maven 3.9+ (oder `mvnw`-Wrapper im Repo)
- Node 20+ und npm
- MongoDB Atlas Cluster (oder lokales MongoDB) mit den 9 Collections
- Auth0 Account mit Single-Page-Application + API
- Anthropic API Key
- (Optional) Pexels API Key

### 1. Environment-Variablen

Backend (`.env` im Root, basierend auf [`.env.example`](.env.example)):
```
AUTH0_DOMAIN=dev-xxx.eu.auth0.com
AUTH0_CLIENT_ID=xxx
AUTH0_AUDIENCE=https://api.plantify-me.ch
ANTHROPIC_API_KEY=sk-ant-xxx
PEXELS_API_KEY=
MONGODB_URI=mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/?appName=Cluster0
```

Frontend (`frontend/.env`, basierend auf [`frontend/.env.example`](frontend/.env.example)):
```
PUBLIC_AUTH0_DOMAIN=dev-xxx.eu.auth0.com
PUBLIC_AUTH0_CLIENT_ID=xxx
PUBLIC_AUTH0_AUDIENCE=https://api.plantify-me.ch
```

### 2. Frontend bauen

```bash
cd frontend
npm install
npm run build    # baut nach ../src/main/resources/static/
```

Für Frontend-Dev-Server (mit Hot-Reload, Backend muss parallel laufen):
```bash
npm run dev      # http://localhost:5173
```

### 3. Backend starten

Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

Bash:
```bash
./mvnw spring-boot:run
```

App läuft auf `http://localhost:8080`, Swagger-UI auf `http://localhost:8080/swagger-ui.html`.

## Tests ausführen

**Backend** (Standard-Suite, ~165–180 Tests):
```bash
./mvnw test
```

**Backend mit JaCoCo-Coverage-Report:**
```bash
./mvnw verify
# Report unter target/site/jacoco/index.html
```

**Backend Opt-In-Tests** (Reporting / Probes — generieren CSVs unter `reports/`):
```bash
# Windows PowerShell
$env:RUN_COVERAGE = "true"; .\mvnw.cmd test -Dtest=CoverageReportTest
$env:RUN_IMPACT_VALIDATION = "true"; .\mvnw.cmd test -Dtest=ImpactValidationTest
$env:RUN_AI_COMPARISON = "true"; .\mvnw.cmd test -Dtest=AiComparisonTest   # kostet ~$0.50 Anthropic API

# Bash
RUN_COVERAGE=true ./mvnw test -Dtest=CoverageReportTest
```

**Frontend** (Vitest):
```bash
cd frontend
npm run test:unit
```

## Deployment

Production läuft auf **Render.com** als Docker-Service. Der `Dockerfile` ist Multi-Stage und produziert ein Single-Container-Image, das das gebaute Frontend statisch aus dem Spring-Boot-Jar serviert.

Build-Args (vom Render-Dashboard gesetzt):
- `PUBLIC_AUTH0_DOMAIN`, `PUBLIC_AUTH0_CLIENT_ID`, `PUBLIC_AUTH0_AUDIENCE`

Runtime-Env-Vars: dieselben wie für lokales Backend (siehe oben).

Lokales Docker-Build:
```bash
docker build \
  --build-arg PUBLIC_AUTH0_DOMAIN=dev-xxx.eu.auth0.com \
  --build-arg PUBLIC_AUTH0_CLIENT_ID=xxx \
  --build-arg PUBLIC_AUTH0_AUDIENCE=https://api.plantify-me.ch \
  -t plantifyme:local .

docker run -p 8080:8080 --env-file .env plantifyme:local
```

## API-Dokumentation

Swagger-UI unter `/swagger-ui.html` listet alle 46 REST-Endpoints mit Request/Response-Schemas. Eine vollständige Postman-Collection liegt im Root bei:
- [`Plantify API.postman_collection.json`](Plantify%20API.postman_collection.json)
- [`Plantify Local.postman_environment.json`](Plantify%20Local.postman_environment.json)

## Autorin

**Selin Soy** — ZHAW, Bachelorarbeit 2026
