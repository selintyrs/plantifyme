package ch.zhaw.plantifyme.coverage;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ImpactValidationTest (NICHT Teil des Standard-Test-Runs).
 *
 * Triggert nur wenn RUN_IMPACT_VALIDATION=true gesetzt ist:
 *   Bash    : RUN_IMPACT_VALIDATION=true ./mvnw test -Dtest=ImpactValidationTest
 *   PowerShell: $env:RUN_IMPACT_VALIDATION = "true"; .\mvnw.cmd test -Dtest=ImpactValidationTest
 *
 * Generiert: reports/impact_validation_<yyyyMMdd_HHmmss>.csv
 *   8 Substitutions-Cases × 3 Supermärkte = bis zu 24 Zeilen.
 *   Auswahl bei mehreren Matches: höchstes impact_co2_saved_pct.
 *
 * Mengen-Parsing-Strategie: amount/qty/unit-Felder → Regex aus name → NaN.
 * Quelle wird in Spalte size_source dokumentiert.
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_IMPACT_VALIDATION", matches = "true")
class ImpactValidationTest {

    private static final List<String> SUPERMARKETS = List.of("Migros", "coop", "aldi");
    private static final Path REPORTS_DIR = Paths.get("reports");

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    /**
     * Substitutions-Case-Spec — kategorie-basiert.
     * Suche per plantify_cat_l3 (echte Kategorie aus DB) + animal_type-Filter.
     * @param slaughterFactor kg pro Tier (Schlachtgewicht)
     */
    record CaseSpec(int nr, String label, String l3Category,
                    String expectedAnimalType, double slaughterFactor) {}

    private static final List<CaseSpec> CASES = List.of(
            new CaseSpec(1, "Vollmilch",         "Milch & Drinks",          "cow",     7100),
            new CaseSpec(2, "Hackfleisch Rind",  "Rind & Schwein",          "cow",     290),
            new CaseSpec(3, "Käse",              "Käse-Alternativen",       "cow",     7100),
            new CaseSpec(4, "Poulet",            "Poulet & Geflügel",       "chicken", 1.43),
            new CaseSpec(5, "Lachs",             "Fisch & Meeresfrüchte",   "fish",    1.5),
            new CaseSpec(6, "Joghurt",           "Joghurt",                 "cow",     7100),
            new CaseSpec(7, "Bratwurst",         "Wurst & Aufschnitt",      "pig",     90),
            new CaseSpec(8, "Rahm",              "Rahm & Sauerrahm",        "cow",     7100)
    );

    @Test
    void generateImpactValidationReport() throws IOException {
        Files.createDirectories(REPORTS_DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        List<Row> rows = new ArrayList<>();
        for (CaseSpec spec : CASES) {
            for (String sm : SUPERMARKETS) {
                rows.add(measure(spec, sm));
            }
        }

        Path out = REPORTS_DIR.resolve("impact_validation_" + stamp + ".csv");
        writeCsv(out, rows);

        List<MethodAggregate> globalMethodAgg = aggregateByMethod();
        List<MethodAnimalAggregate> methodAnimalAgg = aggregateByMethodAndAnimal();
        List<SupermarketAggregate> supermarketAgg = aggregateBySupermarket();
        List<Row> anomalies = detectAnomalies(rows);

        printConsoleSummary(rows, out, globalMethodAgg, methodAnimalAgg, supermarketAgg, anomalies);
    }

    // ─── Aggregations over the entire collection ──────────────────────────

    private record MethodAggregate(String method, long count) {}
    private record MethodAnimalAggregate(String method, String animal, long count) {}
    private record SupermarketAggregate(String supermarket, long count) {}

    private List<SupermarketAggregate> aggregateBySupermarket() {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                        Criteria.where("type").is("A")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("$supermarket")
                        .count().as("count")
        );
        var results = mongoTemplate.aggregate(agg, "supermarketProducts", org.bson.Document.class)
                .getMappedResults();
        List<SupermarketAggregate> out = new ArrayList<>();
        for (org.bson.Document d : results) {
            String sm = d.get("_id") == null ? "(null)" : d.get("_id").toString();
            long count = d.getInteger("count", 0);
            out.add(new SupermarketAggregate(sm, count));
        }
        out.sort(Comparator.comparingLong(SupermarketAggregate::count).reversed());
        return out;
    }

    private List<MethodAggregate> aggregateByMethod() {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                        Criteria.where("type").is("A")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("$impact_method")
                        .count().as("count")
        );
        var results = mongoTemplate.aggregate(agg, "supermarketProducts", org.bson.Document.class)
                .getMappedResults();
        List<MethodAggregate> out = new ArrayList<>();
        for (org.bson.Document d : results) {
            String method = d.get("_id") == null ? "(null)" : d.get("_id").toString();
            long count = d.getInteger("count", 0);
            out.add(new MethodAggregate(method, count));
        }
        out.sort(Comparator.comparingLong(MethodAggregate::count).reversed());
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<MethodAnimalAggregate> aggregateByMethodAndAnimal() {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                        Criteria.where("type").is("A")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("$impact_method", "$animal_type")
                        .count().as("count")
        );
        var results = mongoTemplate.aggregate(agg, "supermarketProducts", org.bson.Document.class)
                .getMappedResults();
        List<MethodAnimalAggregate> out = new ArrayList<>();
        for (org.bson.Document d : results) {
            Object idObj = d.get("_id");
            String method = "(null)";
            String animal = "(null)";
            if (idObj instanceof org.bson.Document idDoc) {
                method = idDoc.get("impact_method") == null ? "(null)" : idDoc.get("impact_method").toString();
                animal = idDoc.get("animal_type") == null ? "(null)" : idDoc.get("animal_type").toString();
            }
            long count = d.getInteger("count", 0);
            out.add(new MethodAnimalAggregate(method, animal, count));
        }
        out.sort(Comparator.<MethodAnimalAggregate, String>comparing(MethodAnimalAggregate::method)
                .thenComparing(MethodAnimalAggregate::animal));
        return out;
    }

    private List<Row> detectAnomalies(List<Row> rows) {
        List<Row> anomalies = new ArrayList<>();
        for (Row r : rows) {
            if (r.product == null) continue;
            // Negative water_saved means vegan alternative needs MORE water than animal original
            Double waterSaved = r.product.getImpactWaterSavedL();
            Double landSaved = r.product.getImpactLandSavedM2();
            Double co2Saved = r.product.getImpactCo2SavedKg();
            if ((waterSaved != null && waterSaved < 0)
                    || (landSaved != null && landSaved < 0)
                    || (co2Saved != null && co2Saved < 0)) {
                anomalies.add(r);
            }
        }
        return anomalies;
    }

    // ─── Measurement ──────────────────────────────────────────────────────

    private Row measure(CaseSpec spec, String supermarket) {
        Row row = new Row();
        row.caseNr = spec.nr();
        row.caseLabel = spec.label();
        row.supermarket = supermarket;
        row.expectedAnimalType = spec.expectedAnimalType();
        row.slaughterFactor = spec.slaughterFactor();

        // Query: Type A + supermarket + plantify_cat_l3 + animal_type
        Query q = new Query();
        q.addCriteria(Criteria.where("type").is("A"));
        q.addCriteria(Criteria.where("supermarket").is(supermarket));
        q.addCriteria(Criteria.where("plantify_cat_l3").is(spec.l3Category()));
        q.addCriteria(Criteria.where("animal_type").is(spec.expectedAnimalType()));
        List<SupermarketProduct> matches = mongoTemplate.find(q, SupermarketProduct.class);

        if (matches.isEmpty()) {
            row.status = "NOT_FOUND";
            return row;
        }

        // Auswahl: höchstes impact_co2_saved_pct (NaN-tolerant: nulls ans Ende)
        SupermarketProduct selected = matches.stream()
                .max(Comparator.comparingDouble(p ->
                        p.getImpactCo2SavedPct() != null ? p.getImpactCo2SavedPct() : Double.NEGATIVE_INFINITY))
                .orElse(matches.get(0));

        row.status = "OK";
        row.product = selected;
        row.candidateCount = matches.size();

        // Mengen-Parsing
        SizeResult sr = parseSize(selected, spec);
        row.sizeG = sr.sizeG;
        row.sizeSource = sr.source;
        row.parsedEggs = sr.eggs;

        // Sollwert
        row.expectedAnimalsPerUnit = computeExpectedAnimals(sr, spec);
        if (selected.getAnimalsAffectedPerUnit() != null
                && row.expectedAnimalsPerUnit != null
                && !row.expectedAnimalsPerUnit.isNaN()
                && row.expectedAnimalsPerUnit > 0) {
            double db = selected.getAnimalsAffectedPerUnit();
            row.deltaAnimals = Math.abs(db - row.expectedAnimalsPerUnit);
            row.deltaAnimalsPct = (row.deltaAnimals / row.expectedAnimalsPerUnit) * 100.0;
        }

        // Konsistenz-Check: (orig - saved) / orig × 100 ?= co2_saved_pct?
        if (selected.getImpactCo2OriginalKg() != null && selected.getImpactCo2SavedKg() != null
                && selected.getImpactCo2OriginalKg() > 0) {
            row.consistencyCheckCo2Pct = (selected.getImpactCo2SavedKg() / selected.getImpactCo2OriginalKg()) * 100.0;
        }

        return row;
    }

    private record SizeResult(Double sizeG, Integer eggs, String source) {}

    private SizeResult parseSize(SupermarketProduct p, CaseSpec spec) {

        // Versuch 1: amount + unit-Felder
        Double fromFields = sizeFromAmountUnit(p);
        if (fromFields != null) return new SizeResult(fromFields, null, "field");

        // Versuch 2: Regex aus name
        Double fromName = sizeFromName(p.getName());
        if (fromName != null) return new SizeResult(fromName, null, "regex");

        return new SizeResult(null, null, "none");
    }

    private Double sizeFromAmountUnit(SupermarketProduct p) {
        if (p.getAmount() == null || p.getUnit() == null) return null;
        double amount = p.getAmount();
        double qty = p.getQty() != null ? p.getQty() : 1.0;
        String unit = p.getUnit().toLowerCase().trim();
        double total = amount * qty;
        return switch (unit) {
            case "g" -> total;
            case "kg" -> total * 1000.0;
            case "ml" -> total;       // 1 ml ≈ 1 g für Milch / Joghurt-ähnliche
            case "l", "ltr" -> total * 1000.0;
            case "dl" -> total * 100.0;
            default -> null;
        };
    }

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|l|dl)\\b", Pattern.CASE_INSENSITIVE);

    private Double sizeFromName(String name) {
        if (name == null) return null;
        Matcher m = SIZE_PATTERN.matcher(name);
        if (!m.find()) return null;
        double value = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2).toLowerCase();
        return switch (unit) {
            case "g" -> value;
            case "kg" -> value * 1000.0;
            case "ml" -> value;
            case "l" -> value * 1000.0;
            case "dl" -> value * 100.0;
            default -> null;
        };
    }

    private Double computeExpectedAnimals(SizeResult sr, CaseSpec spec) {
        if (sr.sizeG() == null) return null;
        double sizeKg = sr.sizeG() / 1000.0;
        return sizeKg / spec.slaughterFactor();
    }

    // ─── CSV writer ───────────────────────────────────────────────────────

    private void writeCsv(Path path, List<Row> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("case_nr,case_label,case_status,supermarket,item_name,product_id,")
                .append("parsed_size_g,parsed_eggs,size_source,impact_method,")
                .append("co2_original_kg,co2_saved_kg,co2_saved_pct,")
                .append("water_original_L,water_saved_L,water_saved_pct,")
                .append("land_original_m2,land_saved_m2,land_saved_pct,")
                .append("animal_type,animals_affected_per_unit,expected_animals_per_unit,")
                .append("delta_animals,delta_animals_pct,consistency_check_co2_pct,candidate_count\n");

        for (Row r : rows) {
            sb.append(r.caseNr).append(",")
                    .append(csv(r.caseLabel)).append(",")
                    .append(r.status).append(",")
                    .append(r.supermarket).append(",");
            if (r.product != null) {
                sb.append(csv(r.product.getName())).append(",")
                        .append(csv(r.product.getId())).append(",")
                        .append(num(r.sizeG)).append(",")
                        .append(intOrEmpty(r.parsedEggs)).append(",")
                        .append(r.sizeSource).append(",")
                        .append(csv(r.product.getImpactMethod())).append(",")
                        .append(num(r.product.getImpactCo2OriginalKg())).append(",")
                        .append(num(r.product.getImpactCo2SavedKg())).append(",")
                        .append(num(r.product.getImpactCo2SavedPct())).append(",")
                        .append(num(r.product.getImpactWaterOriginalL())).append(",")
                        .append(num(r.product.getImpactWaterSavedL())).append(",")
                        .append(num(r.product.getImpactWaterSavedPct())).append(",")
                        .append(num(r.product.getImpactLandOriginalM2())).append(",")
                        .append(num(r.product.getImpactLandSavedM2())).append(",")
                        .append(num(r.product.getImpactLandSavedPct())).append(",")
                        .append(csv(r.product.getAnimalType())).append(",")
                        .append(num(r.product.getAnimalsAffectedPerUnit())).append(",")
                        .append(num(r.expectedAnimalsPerUnit)).append(",")
                        .append(num(r.deltaAnimals)).append(",")
                        .append(num(r.deltaAnimalsPct)).append(",")
                        .append(num(r.consistencyCheckCo2Pct)).append(",")
                        .append(r.candidateCount);
            } else {
                // 22 leere Spalten
                sb.append(",".repeat(22));
            }
            sb.append("\n");
        }
        Files.writeString(path, sb.toString());
    }

    // ─── Console summary ──────────────────────────────────────────────────

    private void printConsoleSummary(List<Row> rows, Path csv,
                                     List<MethodAggregate> globalMethodAgg,
                                     List<MethodAnimalAggregate> methodAnimalAgg,
                                     List<SupermarketAggregate> supermarketAgg,
                                     List<Row> anomalies) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("       PLANTIFY IMPACT VALIDATION — HEADLINE");
        System.out.println("══════════════════════════════════════════════════════════");
        int caseNr = -1;
        for (Row r : rows) {
            if (r.caseNr != caseNr) {
                caseNr = r.caseNr;
                System.out.printf("%nCase %d: %s%n", r.caseNr, r.caseLabel);
            }
            if ("NOT_FOUND".equals(r.status)) {
                System.out.printf("  %-7s : NOT_FOUND%n", r.supermarket);
                continue;
            }
            String method = r.product.getImpactMethod() == null ? "method=null" : "method=" + r.product.getImpactMethod();
            String aau = r.product.getAnimalsAffectedPerUnit() != null
                    ? String.format(Locale.US, "%.6f", r.product.getAnimalsAffectedPerUnit()) : "null";
            String exp = r.expectedAnimalsPerUnit != null
                    ? String.format(Locale.US, "%.6f", r.expectedAnimalsPerUnit) : "n/a";
            String delta = r.deltaAnimalsPct != null
                    ? String.format(Locale.US, "Δ=%.1f%%", r.deltaAnimalsPct) : "Δ=n/a";
            System.out.printf("  %-7s : %s | animal=%s | aau=%s | expected=%s | %s | %s | size=%s(%s)%n",
                    r.supermarket,
                    truncate(r.product.getName(), 38),
                    r.product.getAnimalType(),
                    aau,
                    exp,
                    delta,
                    method,
                    r.sizeG != null ? String.format(Locale.US, "%.0fg", r.sizeG)
                            : (r.parsedEggs != null ? r.parsedEggs + " eggs" : "n/a"),
                    r.sizeSource);
        }
        System.out.println();
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("TYPE=A PRODUCTS PER SUPERMARKET");
        System.out.println("──────────────────────────────────────────────────────────");
        for (SupermarketAggregate sa : supermarketAgg) {
            System.out.printf("  %-15s %6d%n", sa.supermarket(), sa.count());
        }

        System.out.println();
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("GLOBAL impact_method DISTRIBUTION (all type=A products)");
        System.out.println("──────────────────────────────────────────────────────────");
        long total = globalMethodAgg.stream().mapToLong(MethodAggregate::count).sum();
        for (MethodAggregate ma : globalMethodAgg) {
            double pct = total > 0 ? ma.count() * 100.0 / total : 0;
            System.out.printf("  %-25s %6d  (%5.1f%%)%n", ma.method(), ma.count(), pct);
        }
        System.out.printf("  %-25s %6d%n", "TOTAL", total);

        System.out.println();
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("impact_method × animal_type BREAKDOWN");
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.printf("  %-25s %-10s %6s%n", "method", "animal", "count");
        for (MethodAnimalAggregate ma : methodAnimalAgg) {
            System.out.printf("  %-25s %-10s %6d%n", ma.method(), ma.animal(), ma.count());
        }

        if (!anomalies.isEmpty()) {
            System.out.println();
            System.out.println("──────────────────────────────────────────────────────────");
            System.out.println("⚠ ANOMALIES — vegane Alternative spart NEGATIV bei einem Indikator");
            System.out.println("──────────────────────────────────────────────────────────");
            for (Row r : anomalies) {
                System.out.printf("  Case %d/%s: %s%n",
                        r.caseNr, r.supermarket, truncate(r.product.getName(), 50));
                Double w = r.product.getImpactWaterSavedL();
                Double l = r.product.getImpactLandSavedM2();
                Double c = r.product.getImpactCo2SavedKg();
                if (c != null && c < 0) System.out.printf("    co2_saved_kg   = %.3f (orig %.3f, pct %.1f%%)%n",
                        c, nz(r.product.getImpactCo2OriginalKg()), nz(r.product.getImpactCo2SavedPct()));
                if (w != null && w < 0) System.out.printf("    water_saved_L  = %.3f (orig %.3f, pct %.1f%%)%n",
                        w, nz(r.product.getImpactWaterOriginalL()), nz(r.product.getImpactWaterSavedPct()));
                if (l != null && l < 0) System.out.printf("    land_saved_m2  = %.3f (orig %.3f, pct %.1f%%)%n",
                        l, nz(r.product.getImpactLandOriginalM2()), nz(r.product.getImpactLandSavedPct()));
            }
        }

        System.out.println();
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("CSV: " + csv.toAbsolutePath());
        System.out.println("══════════════════════════════════════════════════════════");
    }

    private static double nz(Double d) { return d == null ? 0.0 : d; }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String num(Double d) {
        if (d == null || d.isNaN()) return "";
        return String.format(Locale.US, "%.6f", d);
    }

    private static String intOrEmpty(Integer i) {
        return i == null ? "" : i.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen - 1) + "…" : s;
    }

    private static class Row {
        int caseNr;
        String caseLabel;
        String supermarket;
        String status;
        SupermarketProduct product;
        Double sizeG;
        Integer parsedEggs; // unused since switching to category-based queries; kept for CSV column stability
        String sizeSource;
        String expectedAnimalType;
        double slaughterFactor;
        Double expectedAnimalsPerUnit;
        Double deltaAnimals;
        Double deltaAnimalsPct;
        Double consistencyCheckCo2Pct;
        int candidateCount;
    }
}
