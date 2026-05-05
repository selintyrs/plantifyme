package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.data.InfoArticles;
import ch.zhaw.plantifyme.data.InfoArticles.Article;
import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.model.dto.RecipeGenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
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
import java.util.List;
import java.util.Locale;

/**
 * Green-AI comparison report — A (with measure) vs B (without measure) per test input.
 * Costs ~$1 in real Anthropic API calls. Off by default; trigger with:
 *   RUN_AI_COMPARISON=true ./mvnw test -Dtest=AiComparisonTest
 */
@SpringBootTest
@ActiveProfiles("aicomparison")
@EnabledIfEnvironmentVariable(named = "RUN_AI_COMPARISON", matches = "true")
class AiComparisonTest {

    private static final Path REPORTS_DIR = Paths.get("reports");

    // Sonnet 4 pricing (USD per 1 M tokens), USD→CHF rate
    private static final double COST_INPUT_PER_MTOK_USD  = 3.00;
    private static final double COST_OUTPUT_PER_MTOK_USD = 15.00;
    private static final double USD_TO_CHF              = 0.88;

    @Autowired private CoachService coachService;
    @Autowired private ChatClient.Builder chatClientBuilder;
    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    private ChatClient chatClient;
    private String modelName = "claude-sonnet-4-20250514"; // mirror application.properties

    private record CoachInput(int idx, String label, String fullQuestion) {}
    private record RecipeInput(int idx, String label, String userInput, String baseRecipeRegex) {}

    private static final List<CoachInput> COACH_INPUTS = List.of(
            new CoachInput(1, "B12 für Veganerin",
                    "Wie viel Vitamin B12 muss ich als Veganerin täglich einnehmen?"),
            new CoachInput(2, "Eisen ohne Fleisch",
                    "Wie kann ich genug Eisen aufnehmen ohne Fleisch?"),
            new CoachInput(3, "Soja & Testosteron",
                    "Stimmt es dass Soja den Testosteronspiegel senkt?"),
            new CoachInput(4, "CO2 Rind vs pflanzlich",
                    "Wie hoch ist der CO2-Ausstoss von Rindfleisch im Vergleich zu pflanzlichem Hack?"),
            new CoachInput(5, "Vegan mit Kindern",
                    "Ich habe 2 Kinder und will mit Vegan anfangen — worauf muss ich achten?")
    );

    private static final List<RecipeInput> RECIPE_INPUTS = List.of(
            new RecipeInput(1, "Spaghetti Carbonara", "Spaghetti Carbonara", "(?i)spaghetti carbonara"),
            new RecipeInput(2, "Cordon Bleu",          "Cordon Bleu",          "(?i)cordon bleu"),
            new RecipeInput(3, "Thai Curry",           "Thai Curry",           "(?i)thai curry"),
            new RecipeInput(4, "Bibimbap",             "Bibimbap",             "(?i)bibimbap"),
            new RecipeInput(5, "Brownies",             "Brownies",             "(?i)brownies")
    );

    @Test
    void runComparison() throws IOException {
        chatClient = chatClientBuilder.build();
        Files.createDirectories(REPORTS_DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        List<Row> rows = new ArrayList<>();
        int runId = 0;

        // ─── A-CALLS FIRST (with measures) ────────────────────────────────
        System.out.println("\n=== Phase A: Calls WITH measures ===");
        for (CoachInput in : COACH_INPUTS) {
            rows.add(runCoach(++runId, in, "A_with_measure"));
        }
        for (RecipeInput in : RECIPE_INPUTS) {
            rows.add(runRecipe(++runId, in, "A_with_measure"));
        }

        // ─── B-CALLS SECOND (without measures) ───────────────────────────
        System.out.println("\n=== Phase B: Calls WITHOUT measures ===");
        for (CoachInput in : COACH_INPUTS) {
            rows.add(runCoach(++runId, in, "B_without_measure"));
        }
        for (RecipeInput in : RECIPE_INPUTS) {
            rows.add(runRecipe(++runId, in, "B_without_measure"));
        }

        Path csv = REPORTS_DIR.resolve("ai_comparison_" + stamp + ".csv");
        writeCsv(csv, rows);
        printConsoleSummary(rows, csv);
    }

    // ─── Coach calls ──────────────────────────────────────────────────────

    private Row runCoach(int runId, CoachInput in, String scenario) {
        Row row = baseRow(runId, "coach", in.label(), in.fullQuestion(), scenario);
        long start = System.currentTimeMillis();
        try {
            ChatResponse resp;
            if ("A_with_measure".equals(scenario)) {
                // RAG via CoachService selection (max 3 articles)
                List<Article> relevant = coachService.findRelevantArticles(in.fullQuestion(), 3);
                row.metaArticlesCount = relevant.size();
                String systemPrompt = coachService.buildSystemPrompt(relevant);
                resp = callAnthropic(systemPrompt, in.fullQuestion(), 2000);
            } else {
                // No RAG: ALL 34 articles in prompt
                row.metaArticlesCount = InfoArticles.ALL_ARTICLES.size();
                String systemPrompt = buildSystemPromptWithAllArticles();
                resp = callAnthropic(systemPrompt, in.fullQuestion(), 2000);
            }
            row.latencyMs = System.currentTimeMillis() - start;
            extractUsage(row, resp);
            row.maxTokens = 2000;
        } catch (Exception e) {
            row.latencyMs = System.currentTimeMillis() - start;
            row.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("Coach call failed (" + scenario + "/" + in.label() + "): " + row.error);
        }
        logRowToConsole(row);
        return row;
    }

    private String buildSystemPromptWithAllArticles() {
        StringBuilder sb = new StringBuilder(CoachService.SYSTEM_PROMPT_HEADER);
        sb.append("\nVERFÜGBARE ARTIKEL ALS WISSENSBASIS:\n");
        for (Article a : InfoArticles.ALL_ARTICLES) {
            sb.append("\nTitel: ").append(a.title()).append("\n");
            sb.append("Kategorie: ").append(a.category()).append("\n");
            sb.append("Inhalt: ").append(a.content()).append("\n");
        }
        return sb.toString();
    }

    // ─── Recipe calls ─────────────────────────────────────────────────────

    private Row runRecipe(int runId, RecipeInput in, String scenario) {
        Row row = baseRow(runId, "recipe", in.label(), in.userInput(), scenario);
        long start = System.currentTimeMillis();
        try {
            BaseRecipe baseRecipe = null;
            if ("A_with_measure".equals(scenario)) {
                baseRecipe = mongoTemplate.findOne(
                        Query.query(Criteria.where("title").regex(in.baseRecipeRegex())),
                        BaseRecipe.class);
                if (baseRecipe == null) {
                    row.error = "no base recipe found for regex: " + in.baseRecipeRegex();
                    System.err.println("Recipe NOT_FOUND for case " + in.label());
                    logRowToConsole(row);
                    return row;
                }
                row.metaBaseRecipeId = baseRecipe.getId();
                row.metaBaseRecipeUsed = true;
            }

            // Build RecipeGenerationRequest mimicking the production flow
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle(in.userInput());
            if (baseRecipe != null) req.setBaseRecipeId(baseRecipe.getId());

            // Reproduce buildUserPrompt logic via package-private accessor on a temp service
            // — we use chatClient directly so we can read tokens from chatResponse().
            String userPrompt = buildRecipeUserPrompt(req, baseRecipe);
            ChatResponse resp = chatClient.prompt()
                    .system(RecipeGenerationService.SYSTEM_PROMPT)
                    .user(userPrompt)
                    .options(AnthropicChatOptions.builder().maxTokens(2000).build())
                    .call()
                    .chatResponse();
            row.latencyMs = System.currentTimeMillis() - start;
            extractUsage(row, resp);
            row.maxTokens = 2000;
        } catch (Exception e) {
            row.latencyMs = System.currentTimeMillis() - start;
            row.error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("Recipe call failed (" + scenario + "/" + in.label() + "): " + row.error);
        }
        logRowToConsole(row);
        return row;
    }

    /** Mirror RecipeGenerationService.buildUserPrompt(...) behavior using its public package-private method. */
    private String buildRecipeUserPrompt(RecipeGenerationRequest req, BaseRecipe baseRecipe) {
        // We can't easily instantiate RecipeGenerationService standalone (needs ChatClient.Builder etc.),
        // and Spring AI's autowiring may inject the same instance — but the service's buildUserPrompt
        // is a pure function. Re-implementing 12 lines here keeps the test self-contained and
        // avoids accidental DB writes when the service tries to persist a SavedRecipe.
        StringBuilder sb = new StringBuilder("Erstelle ein veganes Rezept");
        if (req.getTitle() != null) sb.append(" für: ").append(req.getTitle());
        if (req.getDescription() != null) sb.append(". Beschreibung: ").append(req.getDescription());
        if (baseRecipe != null) {
            String ingredientNames = baseRecipe.getIngredients().stream()
                    .map(i -> i.getName())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            sb.append(". Basiere auf folgendem Grundrezept und passe es an die Anforderungen an. BEHALTE den Originaltitel bei: ")
              .append(baseRecipe.getTitle())
              .append(". Zutaten: ").append(ingredientNames);
        }
        return sb.toString();
    }

    // ─── Anthropic call helper ────────────────────────────────────────────

    private ChatResponse callAnthropic(String systemPrompt, String userPrompt, int maxTokens) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(AnthropicChatOptions.builder().maxTokens(maxTokens).build())
                .call()
                .chatResponse();
    }

    private void extractUsage(Row row, ChatResponse resp) {
        if (resp == null || resp.getMetadata() == null) return;
        Usage u = resp.getMetadata().getUsage();
        if (u == null) return;
        row.promptTokens = u.getPromptTokens();
        row.completionTokens = u.getCompletionTokens();
        row.totalTokens = u.getTotalTokens();
    }

    // ─── CSV ──────────────────────────────────────────────────────────────

    private Row baseRow(int runId, String block, String label, String full, String scenario) {
        Row r = new Row();
        r.runId = runId;
        r.timestampIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        r.block = block;
        r.testInput = label;
        r.testInputFull = full;
        r.scenario = scenario;
        r.model = modelName;
        r.temperature = 0.7;
        return r;
    }

    private void writeCsv(Path path, List<Row> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("run_id,timestamp_iso,block,test_input,test_input_full,scenario,")
          .append("model,temperature,max_tokens,prompt_tokens,completion_tokens,total_tokens,")
          .append("latency_ms,cost_input_usd,cost_output_usd,cost_total_usd,cost_total_chf,")
          .append("meta_articles_count,meta_base_recipe_id,meta_base_recipe_used,error\n");

        for (Row r : rows) {
            double costIn = r.promptTokens != null ? r.promptTokens * COST_INPUT_PER_MTOK_USD / 1_000_000.0 : 0;
            double costOut = r.completionTokens != null ? r.completionTokens * COST_OUTPUT_PER_MTOK_USD / 1_000_000.0 : 0;
            double costTotal = costIn + costOut;
            double costChf = costTotal * USD_TO_CHF;

            sb.append(r.runId).append(",")
              .append(r.timestampIso).append(",")
              .append(r.block).append(",")
              .append(csv(r.testInput)).append(",")
              .append(csv(r.testInputFull)).append(",")
              .append(r.scenario).append(",")
              .append(r.model).append(",")
              .append(num(r.temperature)).append(",")
              .append(intOrEmpty(r.maxTokens)).append(",")
              .append(intOrEmpty(r.promptTokens)).append(",")
              .append(intOrEmpty(r.completionTokens)).append(",")
              .append(intOrEmpty(r.totalTokens)).append(",")
              .append(longOrEmpty(r.latencyMs)).append(",")
              .append(money(costIn)).append(",")
              .append(money(costOut)).append(",")
              .append(money(costTotal)).append(",")
              .append(money(costChf)).append(",")
              .append(intOrEmpty(r.metaArticlesCount)).append(",")
              .append(csv(r.metaBaseRecipeId)).append(",")
              .append(r.metaBaseRecipeUsed ? "true" : "false").append(",")
              .append(csv(r.error))
              .append("\n");
        }
        Files.writeString(path, sb.toString());
    }

    // ─── Console summary ──────────────────────────────────────────────────

    private void logRowToConsole(Row r) {
        if (r.error != null) {
            System.out.printf("  [%d] %s/%s/%s: ERROR: %s (latency %d ms)%n",
                    r.runId, r.block, r.testInput, r.scenario, r.error,
                    r.latencyMs == null ? 0 : r.latencyMs);
        } else {
            System.out.printf("  [%d] %s/%s/%s: prompt=%s, completion=%s, latency=%dms%n",
                    r.runId, r.block, r.testInput, r.scenario,
                    r.promptTokens, r.completionTokens, r.latencyMs);
        }
    }

    private void printConsoleSummary(List<Row> rows, Path csv) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("       PLANTIFY GREEN-AI COMPARISON — HEADLINE");
        System.out.println("══════════════════════════════════════════════════════════");

        printBlock(rows, "coach", "BLOCK 1: COACH (RAG-Massnahme)");
        printBlock(rows, "recipe", "BLOCK 2: RECIPE (Base-Recipe-Massnahme)");

        printAggregates(rows);

        System.out.println();
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("CSV: " + csv.toAbsolutePath());
        System.out.println("══════════════════════════════════════════════════════════");
    }

    private void printBlock(List<Row> rows, String block, String header) {
        System.out.println();
        System.out.println(header);
        // Group by testInput, find A and B for each
        List<String> labels = rows.stream()
                .filter(r -> r.block.equals(block))
                .map(r -> r.testInput)
                .distinct()
                .toList();
        for (String label : labels) {
            Row a = rows.stream().filter(r -> r.block.equals(block) && r.testInput.equals(label)
                    && "A_with_measure".equals(r.scenario)).findFirst().orElse(null);
            Row b = rows.stream().filter(r -> r.block.equals(block) && r.testInput.equals(label)
                    && "B_without_measure".equals(r.scenario)).findFirst().orElse(null);
            System.out.printf("  %s%n", label);
            if (a != null) System.out.printf("    A (with measure):    prompt=%s, completion=%s, latency=%sms, cost=$%s%n",
                    a.promptTokens, a.completionTokens, a.latencyMs, money(costTotal(a)));
            if (b != null) System.out.printf("    B (without measure): prompt=%s, completion=%s, latency=%sms, cost=$%s%n",
                    b.promptTokens, b.completionTokens, b.latencyMs, money(costTotal(b)));
            if (a != null && b != null && a.promptTokens != null && b.promptTokens != null && b.promptTokens > 0) {
                double tokenSavingPct = (1.0 - (double) a.totalTokens / b.totalTokens) * 100;
                double costSavingPct = (1.0 - costTotal(a) / costTotal(b)) * 100;
                System.out.printf("    → Einsparung: %.1f%% Tokens, %.1f%% Kosten%n", tokenSavingPct, costSavingPct);
            }
        }
    }

    private void printAggregates(List<Row> rows) {
        for (String block : List.of("coach", "recipe")) {
            long sumA = rows.stream().filter(r -> r.block.equals(block) && "A_with_measure".equals(r.scenario))
                    .filter(r -> r.totalTokens != null).mapToLong(r -> r.totalTokens).sum();
            long sumB = rows.stream().filter(r -> r.block.equals(block) && "B_without_measure".equals(r.scenario))
                    .filter(r -> r.totalTokens != null).mapToLong(r -> r.totalTokens).sum();
            double costA = rows.stream().filter(r -> r.block.equals(block) && "A_with_measure".equals(r.scenario))
                    .mapToDouble(this::costTotal).sum();
            double costB = rows.stream().filter(r -> r.block.equals(block) && "B_without_measure".equals(r.scenario))
                    .mapToDouble(this::costTotal).sum();
            System.out.println();
            System.out.printf("  %s AGGREGATE:%n", block.toUpperCase(Locale.US));
            System.out.printf("    A (with measures):    Σ tokens=%d, Σ cost=$%.4f%n", sumA, costA);
            System.out.printf("    B (without measures): Σ tokens=%d, Σ cost=$%.4f%n", sumB, costB);
            if (sumB > 0) {
                System.out.printf("    Mittlere Einsparung:  %.1f%% Tokens, %.1f%% Kosten%n",
                        (1.0 - (double) sumA / sumB) * 100, (1.0 - costA / costB) * 100);
            }

            // Hochrechnung pro 1000 Anfragen (basierend auf Mittelwert pro Block)
            int n = (int) rows.stream().filter(r -> r.block.equals(block) && "A_with_measure".equals(r.scenario)).count();
            if (n > 0) {
                double avgCostA = costA / n;
                double avgCostB = costB / n;
                System.out.printf("    Hochrechnung pro 1000 Anfragen:%n");
                System.out.printf("      mit Massnahme:  ~$%.2f%n", avgCostA * 1000);
                System.out.printf("      ohne Massnahme: ~$%.2f%n", avgCostB * 1000);
                System.out.printf("      → Einsparung:   ~$%.2f%n", (avgCostB - avgCostA) * 1000);
            }
        }
    }

    private double costTotal(Row r) {
        if (r.promptTokens == null || r.completionTokens == null) return 0;
        return r.promptTokens * COST_INPUT_PER_MTOK_USD / 1_000_000.0
             + r.completionTokens * COST_OUTPUT_PER_MTOK_USD / 1_000_000.0;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
    private static String num(Double d) { return d == null ? "" : String.format(Locale.US, "%.2f", d); }
    private static String money(double d) { return String.format(Locale.US, "%.6f", d); }
    private static String intOrEmpty(Integer i) { return i == null ? "" : i.toString(); }
    private static String longOrEmpty(Long l) { return l == null ? "" : l.toString(); }

    private static class Row {
        int runId;
        String timestampIso;
        String block;
        String testInput;
        String testInputFull;
        String scenario;
        String model;
        Double temperature;
        Integer maxTokens;
        Integer promptTokens;
        Integer completionTokens;
        Integer totalTokens;
        Long latencyMs;
        Integer metaArticlesCount;
        String metaBaseRecipeId;
        boolean metaBaseRecipeUsed;
        String error;
    }
}
