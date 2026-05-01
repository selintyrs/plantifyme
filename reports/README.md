# /reports — Coverage & Analyse-Outputs

Generierte CSVs aus dem Coverage-Report-Test. Liefert quantitative
Aussagen für die BA: "Plantify deckt X% der FrontendDictionary-Items
in Coop / Migros / Aldi ab."

## Was hier landet

- `coverage_<yyyyMMdd_HHmmss>.csv` — pro FrontendDictionary-Item
  eine Zeile mit Match-Status (specific/broader) pro Supermarkt
- `coverage_aggregate_<yyyyMMdd_HHmmss>.csv` — Headline-Zahlen,
  Coverage pro Kategorie, Top-Gaps

CSVs sind in `.gitignore`, der `README.md` selbst ist getrackt.

## Generieren

```bash
# Windows
$env:RUN_COVERAGE = "true"
.\mvnw.cmd test -Dtest=CoverageReportTest

# Bash
RUN_COVERAGE=true ./mvnw test -Dtest=CoverageReportTest
```

Standard-Test-Run (`mvnw test` ohne env-var) lässt diesen Test
**übersprungen** — er läuft nur wenn explizit getriggert.

## Was wird gemessen

Pro FrontendDictionary-Item, pro Supermarkt:
- **specific match**: `findByMatchingKeyAndSupermarket(displayName, "", supermarket)` liefert ≥1 Treffer
- **broader fallback**: nur relevant wenn subcategory non-empty wäre — in dieser Phase iterieren wir nur Base-Match (subcategory=""), darum sind `*Broader`-Spalten faktisch nicht populiert

Plus:
- Type C: `genericFoodRepository.findByName(displayName)` Treffer?
- DIY: `diyRecipeRepository.findByMatchingDisplayName(displayName)` Treffer?

## Was NICHT gemessen wird

- additionalOptions-Iteration (würde N×M-fache Output-Größe erzeugen)
- Match-Quality (Anzahl Treffer, Preise)
- User-spezifische Filterung (Allergien etc.) — Run nutzt leere Preferences
