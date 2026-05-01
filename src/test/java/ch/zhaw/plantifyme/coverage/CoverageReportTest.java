package ch.zhaw.plantifyme.coverage;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coverage-Report-Test (NICHT Teil des Standard-Test-Runs).
 *
 * Triggert nur wenn die Umgebungsvariable RUN_COVERAGE=true gesetzt ist:
 *   Windows : $env:RUN_COVERAGE = "true"; .\mvnw.cmd test -Dtest=CoverageReportTest
 *   Bash    : RUN_COVERAGE=true ./mvnw test -Dtest=CoverageReportTest
 *
 * Generiert in /reports/:
 *   coverage_<yyyyMMdd_HHmmss>.csv           — pro FrontendDictionary-Item eine Zeile
 *   coverage_aggregate_<yyyyMMdd_HHmmss>.csv — Headline-Zahlen + Kategorie-Coverage
 *
 * Output ist in .gitignore — Git trackt nur den README in /reports/.
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_COVERAGE", matches = "true")
class CoverageReportTest {

    private static final List<String> SUPERMARKETS = List.of("Migros", "coop", "aldi");
    private static final Path REPORTS_DIR = Paths.get("reports");

    @Autowired private FrontendDictionaryRepository frontendDictionaryRepository;
    @Autowired private GenericFoodRepository genericFoodRepository;
    @Autowired private DiyRecipeRepository diyRecipeRepository;
    @Autowired private PlantifyMatchService plantifyMatchService;

    /** Stub damit SecurityConfig den JwtDecoder findet — wir brauchen keine echte Auth0-Anbindung. */
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void generateCoverageReport() throws IOException {
        Files.createDirectories(REPORTS_DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        List<FrontendDictionary> entries = frontendDictionaryRepository.findAll();
        List<ItemCoverage> rows = new ArrayList<>(entries.size());

        long startMs = System.currentTimeMillis();
        for (FrontendDictionary entry : entries) {
            rows.add(measure(entry));
        }
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;

        Path perItem = REPORTS_DIR.resolve("coverage_" + stamp + ".csv");
        writePerItemCsv(perItem, rows);

        Path aggregate = REPORTS_DIR.resolve("coverage_aggregate_" + stamp + ".csv");
        writeAggregateCsv(aggregate, rows);

        printConsoleSummary(rows, perItem, aggregate, elapsed);
    }

    // ─── Measurement ──────────────────────────────────────────────────────

    private ItemCoverage measure(FrontendDictionary entry) {
        ItemCoverage row = new ItemCoverage();
        row.id = entry.getId();
        row.displayName = entry.getDisplayName();
        row.uiCategory = entry.getUiCategory();
        row.type = entry.getType();

        // Match per supermarket via the real service (uses subcategory fallback).
        // We pass subcategory="" so this captures the BASE-MATCH coverage.
        for (String supermarket : SUPERMARKETS) {
            PlantifyMatchRequest req = buildBaseRequest(entry, supermarket);
            PlantifyMatchResponse resp = plantifyMatchService.match(req);
            MatchResult result = resp.getMatches().isEmpty() ? null : resp.getMatches().get(0);
            SupermarketCoverage sc = new SupermarketCoverage();
            if (result != null) {
                sc.specific = "specific".equals(result.getMatchLevel());
                sc.broader = "broader".equals(result.getMatchLevel());
            }
            row.bySupermarket.put(supermarket, sc);
        }

        // Type-C path: separate generic-food lookup
        row.typeC_genericFoodExists = !genericFoodRepository.findByName(entry.getDisplayName()).isEmpty();

        // DIY fallback: catch-all
        row.diyFallbackExists = !diyRecipeRepository.findByMatchingDisplayName(entry.getDisplayName()).isEmpty();

        return row;
    }

    private PlantifyMatchRequest buildBaseRequest(FrontendDictionary entry, String supermarket) {
        MatchRequestItem ri = new MatchRequestItem();
        ri.setItemId(entry.getId());
        ri.setDisplayName(entry.getDisplayName());
        ri.setSubcategory("");
        ri.setType(entry.getType());
        ri.setFrontendDictionaryId(entry.getId());

        PlantifyMatchRequest req = new PlantifyMatchRequest();
        req.setSupermarket(supermarket);
        req.setItems(List.of(ri));
        req.setPreferences(emptyPrefs());
        return req;
    }

    private MatchPreferences emptyPrefs() {
        MatchPreferences p = new MatchPreferences();
        p.setBudget("medium");
        p.setAllergies(List.of());
        p.setDiet(List.of());
        return p;
    }

    // ─── CSV writers ──────────────────────────────────────────────────────

    private void writePerItemCsv(Path path, List<ItemCoverage> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("frontendDictionaryId,displayName,uiCategory,type");
        for (String sm : SUPERMARKETS) {
            sb.append(",").append(sm).append("Specific");
            sb.append(",").append(sm).append("Broader");
        }
        sb.append(",typeC_genericFoodExists,diyFallbackExists\n");

        for (ItemCoverage r : rows) {
            sb.append(csvField(r.id)).append(",")
                    .append(csvField(r.displayName)).append(",")
                    .append(csvField(r.uiCategory)).append(",")
                    .append(csvField(r.type));
            for (String sm : SUPERMARKETS) {
                SupermarketCoverage sc = r.bySupermarket.get(sm);
                sb.append(",").append(sc.specific).append(",").append(sc.broader);
            }
            sb.append(",").append(r.typeC_genericFoodExists)
                    .append(",").append(r.diyFallbackExists)
                    .append("\n");
        }
        Files.writeString(path, sb.toString());
    }

    private void writeAggregateCsv(Path path, List<ItemCoverage> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("section,key,value\n");

        // Totals
        long total = rows.size();
        long typeA = rows.stream().filter(r -> "A".equals(r.type)).count();
        long typeB = rows.stream().filter(r -> "B".equals(r.type)).count();
        long typeC = rows.stream().filter(r -> "C".equals(r.type)).count();
        sb.append("totals,items_total,").append(total).append("\n");
        sb.append("totals,items_typeA,").append(typeA).append("\n");
        sb.append("totals,items_typeB,").append(typeB).append("\n");
        sb.append("totals,items_typeC,").append(typeC).append("\n");

        // Coverage per supermarket — only Type A & B items count for supermarket coverage
        long supermarketRelevant = typeA + typeB;
        for (String sm : SUPERMARKETS) {
            long anyMatch = rows.stream()
                    .filter(r -> !"C".equals(r.type))
                    .filter(r -> r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader)
                    .count();
            long specific = rows.stream()
                    .filter(r -> !"C".equals(r.type))
                    .filter(r -> r.bySupermarket.get(sm).specific)
                    .count();
            sb.append("coverage,").append(sm).append("_any_match_pct,")
                    .append(pct(anyMatch, supermarketRelevant)).append("\n");
            sb.append("coverage,").append(sm).append("_specific_pct,")
                    .append(pct(specific, supermarketRelevant)).append("\n");
        }

        // Type-C generic-food coverage
        long typeCwithFood = rows.stream()
                .filter(r -> "C".equals(r.type))
                .filter(r -> r.typeC_genericFoodExists)
                .count();
        sb.append("coverage,typeC_generic_food_pct,").append(pct(typeCwithFood, typeC)).append("\n");

        // DIY coverage
        long diyAvailable = rows.stream().filter(r -> r.diyFallbackExists).count();
        sb.append("coverage,diy_fallback_available_pct,").append(pct(diyAvailable, total)).append("\n");

        // Items with NO match anywhere (no SM, no DIY, no Type-C generic)
        long noMatchAnywhere = rows.stream()
                .filter(r -> !r.diyFallbackExists)
                .filter(r -> !r.typeC_genericFoodExists)
                .filter(r -> SUPERMARKETS.stream().noneMatch(sm ->
                        r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader))
                .count();
        sb.append("gap,items_no_match_anywhere,").append(noMatchAnywhere).append("\n");

        // Coverage per (uiCategory × supermarket) — Type A/B only
        Map<String, List<ItemCoverage>> byCategory = new LinkedHashMap<>();
        for (ItemCoverage r : rows) {
            if ("C".equals(r.type)) continue;
            byCategory.computeIfAbsent(
                    r.uiCategory != null ? r.uiCategory : "(none)",
                    k -> new ArrayList<>()).add(r);
        }
        List<Map.Entry<String, List<ItemCoverage>>> sortedCats = new ArrayList<>(byCategory.entrySet());
        sortedCats.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, List<ItemCoverage>> e : sortedCats) {
            String cat = e.getKey();
            int catTotal = e.getValue().size();
            for (String sm : SUPERMARKETS) {
                long matched = e.getValue().stream()
                        .filter(r -> r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader)
                        .count();
                sb.append("category_coverage,")
                        .append(csvField(cat + " | " + sm)).append(",")
                        .append(pct(matched, catTotal)).append("\n");
            }
        }

        // Top 10 categories with worst coverage (averaged across all 3 SMs)
        List<Map.Entry<String, Double>> categoryAvg = new ArrayList<>();
        for (Map.Entry<String, List<ItemCoverage>> e : byCategory.entrySet()) {
            int catTotal = e.getValue().size();
            if (catTotal == 0) continue;
            double sum = 0;
            for (String sm : SUPERMARKETS) {
                long matched = e.getValue().stream()
                        .filter(r -> r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader)
                        .count();
                sum += (matched * 100.0 / catTotal);
            }
            categoryAvg.add(Map.entry(e.getKey(), sum / SUPERMARKETS.size()));
        }
        categoryAvg.sort(Comparator.comparingDouble(Map.Entry::getValue));
        int worstLimit = Math.min(10, categoryAvg.size());
        for (int i = 0; i < worstLimit; i++) {
            Map.Entry<String, Double> e = categoryAvg.get(i);
            sb.append("worst_categories,").append(csvField(e.getKey())).append(",")
                    .append(String.format(java.util.Locale.US, "%.1f%%", e.getValue())).append("\n");
        }

        // Top 10 best
        List<Map.Entry<String, Double>> bestSorted = new ArrayList<>(categoryAvg);
        bestSorted.sort(Comparator.<Map.Entry<String, Double>, Double>comparing(Map.Entry::getValue).reversed());
        int bestLimit = Math.min(10, bestSorted.size());
        for (int i = 0; i < bestLimit; i++) {
            Map.Entry<String, Double> e = bestSorted.get(i);
            sb.append("best_categories,").append(csvField(e.getKey())).append(",")
                    .append(String.format(java.util.Locale.US, "%.1f%%", e.getValue())).append("\n");
        }

        Files.writeString(path, sb.toString());
    }

    // ─── Console summary ──────────────────────────────────────────────────

    private void printConsoleSummary(List<ItemCoverage> rows, Path perItem, Path aggregate, long elapsedSec) {
        long total = rows.size();
        long typeA = rows.stream().filter(r -> "A".equals(r.type)).count();
        long typeB = rows.stream().filter(r -> "B".equals(r.type)).count();
        long typeC = rows.stream().filter(r -> "C".equals(r.type)).count();
        long abTotal = typeA + typeB;

        Map<String, Long> anyByMarket = new HashMap<>();
        Map<String, Long> specificByMarket = new HashMap<>();
        for (String sm : SUPERMARKETS) {
            anyByMarket.put(sm, rows.stream()
                    .filter(r -> !"C".equals(r.type))
                    .filter(r -> r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader)
                    .count());
            specificByMarket.put(sm, rows.stream()
                    .filter(r -> !"C".equals(r.type))
                    .filter(r -> r.bySupermarket.get(sm).specific)
                    .count());
        }

        long noMatch = rows.stream()
                .filter(r -> !r.diyFallbackExists && !r.typeC_genericFoodExists)
                .filter(r -> SUPERMARKETS.stream().noneMatch(sm ->
                        r.bySupermarket.get(sm).specific || r.bySupermarket.get(sm).broader))
                .count();

        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("       PLANTIFY COVERAGE REPORT — HEADLINE");
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.printf("Items total: %d  (Typ A: %d, B: %d, C: %d)%n", total, typeA, typeB, typeC);
        System.out.printf("Coverage-relevant für Supermarkt-Match (A+B): %d%n", abTotal);
        System.out.println();
        System.out.printf("%-12s %-15s %-15s%n", "Supermarkt", "Specific", "Any Match");
        for (String sm : SUPERMARKETS) {
            System.out.printf("%-12s %-15s %-15s%n",
                    sm,
                    pct(specificByMarket.get(sm), abTotal),
                    pct(anyByMarket.get(sm), abTotal));
        }
        System.out.println();
        System.out.printf("Items ohne ANY Match (kein SM, kein Generic, kein DIY): %d%n", noMatch);
        System.out.printf("Laufzeit: %ds%n", elapsedSec);
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("Per-Item:    " + perItem.toAbsolutePath());
        System.out.println("Aggregate:   " + aggregate.toAbsolutePath());
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static String pct(long matched, long total) {
        if (total == 0) return "n/a";
        return String.format(java.util.Locale.US, "%.1f%%", matched * 100.0 / total);
    }

    private static String csvField(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static class ItemCoverage {
        String id;
        String displayName;
        String uiCategory;
        String type;
        Map<String, SupermarketCoverage> bySupermarket = new LinkedHashMap<>();
        boolean typeC_genericFoodExists;
        boolean diyFallbackExists;
    }

    private static class SupermarketCoverage {
        boolean specific;
        boolean broader;
    }
}
