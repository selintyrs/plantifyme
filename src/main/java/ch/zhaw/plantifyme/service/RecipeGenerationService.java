package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.model.RecipeIngredient;
import ch.zhaw.plantifyme.model.RecipeNutrition;
import ch.zhaw.plantifyme.model.RecipeStep;
import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.model.dto.RecipeGenerationRequest;
import ch.zhaw.plantifyme.model.dto.RecipePreferences;
import ch.zhaw.plantifyme.repository.BaseRecipeRepository;
import ch.zhaw.plantifyme.repository.SavedRecipeRepository;
import ch.zhaw.plantifyme.util.AuthUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeGenerationService.class);

    /** Package-private for AiComparisonTest re-use; not part of public API. */
    static final String SYSTEM_PROMPT = """
            Du bist ein veganer Koch-Experte und Ernährungsberater. Du erstellst kreative,\s
            alltagstaugliche vegane Rezepte für Menschen in der Schweiz.

            REGELN:
            - Alle Rezepte sind 100% vegan (keine tierischen Produkte)
            - Verwende Zutaten die in Schweizer Supermärkten (Migros, Coop, Aldi) erhältlich sind
            - Schreibe auf Deutsch (Schweizer Hochdeutsch)
            - Schreibe NIEMALS "vegan" in den Titel — die App ist per se vegan
            - Gib realistische Nährwert-Schätzungen pro Portion
            - Gib bei jeder Zutat den Typ an: "B" für verarbeitete vegane Produkte (Tofu, Seitan,\s
              Pflanzenmilch, etc.) oder "C" für natürliche Grundnahrungsmittel (Gemüse, Gewürze, Öle, etc.)
            - Bei Typ B Zutaten: gib matchingDisplayName und matchingSubcategory an für die Supermarkt-Suche
            - Bei Typ C Zutaten: setze matchingDisplayName und matchingSubcategory auf null

            Bekannte Typ B matchingDisplayName Werte und ihre Subcategories:
            - "Tofu" (Subcategories: "Nature", "Geräuchert")
            - "Milch" (Subcategories: "", "Kokos", "Soja", "Hafer")
            - "Sojasauce" (Subcategory: "")
            - "Senf" (Subcategory: "")
            - "Hülsenfrüchte" (Subcategories: "Linsen", "Kichererbsen", "Schwarze Bohnen")
            - "Tomaten (Konserve)" (Subcategories: "Stücke", "Tomatenmark")
            - "Bouillon" (Subcategory: "")
            - "Öl" (Subcategories: "Rapsöl", "Kokosöl")
            - "Essig" (Subcategory: "Apfelessig")
            - "Süssungsmittel" (Subcategory: "Ahornsirup")
            - "Seitan" (Subcategory: "")
            - "Brot" (Subcategories: "", "Vollkorn")
            - "Toast" (Subcategory: "")
            - "Burger Buns" (Subcategory: "")
            - "Bratwurst" (Subcategory: "Wiener")
            - "Nachos" (Subcategory: "")
            - "Tortilla" (Subcategory: "")
            - "Hörnli" (Subcategory: "")
            - "Spaghetti" (Subcategory: "")
            - "Penne" (Subcategory: "")
            - "Ketchup" (Subcategory: "")
            - "Asiatische Nudeln" (Subcategory: "Reisnudeln")
            - "Instant Nudeln" (Subcategory: "Ramen")
            - "Currysauce" (Subcategory: "Paste")
            - "Suppe" (Subcategory: "Miso")
            - "Pita" (Subcategory: "")
            - "Pasta Gericht" (Subcategory: "Lasagne")
            - "Gnocchi" (Subcategory: "")
            - "Granola" (Subcategory: "")

            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt. Kein Text davor oder danach.
            Kein Markdown. Keine Erklärungen. Nur das JSON-Objekt.

            JSON-Struktur:
            {
              "title": "Rezeptname",
              "description": "Kurze Beschreibung in 1-2 Sätzen",
              "category": "Kategorie (z.B. Pasta & Pizza, Asiatische Küche, etc.)",
              "prepTimeMin": 25,
              "difficulty": "easy|medium|hard",
              "servings": 2,
              "ingredients": [
                {
                  "name": "Zutatname",
                  "amount": 200,
                  "unit": "g|ml|EL|TL|Stk|Prise",
                  "type": "B|C",
                  "matchingDisplayName": "string oder null",
                  "matchingSubcategory": "string oder null"
                }
              ],
              "instructions": [
                { "step": 1, "text": "Anweisungstext" }
              ],
              "totalNutritionPerServing": {
                "kcal": 450,
                "fat": 18.0,
                "saturatedFat": 3.0,
                "carbs": 55.0,
                "sugar": 8.0,
                "protein": 22.0,
                "salt": 2.0
              }
            }
            """;

    private final ChatClient chatClient;
    private final BaseRecipeRepository baseRecipeRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final RecipeImageResolver imageResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecipeGenerationService(ChatClient.Builder chatClientBuilder,
                                   BaseRecipeRepository baseRecipeRepository,
                                   SavedRecipeRepository savedRecipeRepository,
                                   RecipeImageResolver imageResolver) {
        this.chatClient = chatClientBuilder.build();
        this.baseRecipeRepository = baseRecipeRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.imageResolver = imageResolver;
    }

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

    /** Package-private for AiComparisonTest re-use; not part of public API. */
    String buildUserPrompt(RecipeGenerationRequest request, BaseRecipe baseRecipe) {
        StringBuilder sb = new StringBuilder("Erstelle ein veganes Rezept");

        if (request.getTitle() != null) {
            sb.append(" für: ").append(request.getTitle());
        }
        if (request.getDescription() != null) {
            sb.append(". Beschreibung: ").append(request.getDescription());
        }

        if (baseRecipe != null) {
            String ingredientNames = baseRecipe.getIngredients().stream()
                    .map(RecipeIngredient::getName)
                    .collect(Collectors.joining(", "));
            sb.append(". Basiere auf folgendem Grundrezept und passe es an die Anforderungen an. BEHALTE den Originaltitel bei: ")
              .append(baseRecipe.getTitle())
              .append(". Zutaten: ").append(ingredientNames);
        }

        if (request.getCustomIngredients() != null && !request.getCustomIngredients().isEmpty()) {
            sb.append(". Verwende diese Zutaten: ")
              .append(String.join(", ", request.getCustomIngredients()));
        }

        RecipePreferences prefs = request.getPreferences();
        if (prefs != null) {
            List<String> prefParts = new ArrayList<>();
            if (prefs.getPrepTimeMax() != null) prefParts.add("Zubereitungszeit maximal " + prefs.getPrepTimeMax() + " Minuten");
            if (prefs.getDifficulty() != null) prefParts.add("Schwierigkeit: " + prefs.getDifficulty());
            if (prefs.isHighProtein()) prefParts.add("proteinreich");
            if (prefs.isLowCarb()) prefParts.add("kohlenhydratarm");
            if (prefs.isLowFat()) prefParts.add("fettarm");
            if (prefs.isWholeFood()) prefParts.add("Whole Food / vollwertig");
            if (prefs.isSugarFree()) prefParts.add("ohne Zucker");
            if (prefs.isBudgetFriendly()) prefParts.add("budget-freundlich");
            if (prefs.isSimpleIngredients()) prefParts.add("einfache Zutaten");
            if (prefs.isGlutenFree()) prefParts.add("glutenfrei");
            if (prefs.isSoyFree()) prefParts.add("sojafrei");
            if (prefs.isNutFree()) prefParts.add("nussfrei");
            if (prefs.getAllergies() != null && !prefs.getAllergies().isBlank()) prefParts.add("Allergien: " + prefs.getAllergies());
            if (prefs.getAdditionalNotes() != null && !prefs.getAdditionalNotes().isBlank()) prefParts.add(prefs.getAdditionalNotes());
            if (!prefParts.isEmpty()) {
                sb.append(". Anforderungen: ").append(String.join(", ", prefParts));
            }
        }

        return sb.toString();
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private SavedRecipe mapToSavedRecipe(JsonNode json, RecipeGenerationRequest request) {
        SavedRecipe recipe = new SavedRecipe();
        recipe.setUserId(AuthUtil.requireCurrentUserId());
        recipe.setAiGenerated(true);
        recipe.setSaved(false);
        String aiTitle = json.path("title").asText();
        if (aiTitle == null || aiTitle.isBlank()) {
            log.warn("LLM lieferte keinen Titel im JSON-Response (request.title={}, baseRecipeId={}). "
                    + "Recipe wird mit leerem Titel gespeichert.", request.getTitle(), request.getBaseRecipeId());
        }
        recipe.setTitle(aiTitle);
        // Override des KI-Titels NUR wenn ein Base-Recipe-Anchor mitgeschickt wurde —
        // dann soll der Original-Titel des Grundrezepts erhalten bleiben
        // (siehe SYSTEM_PROMPT "BEHALTE den Originaltitel bei").
        if (request.getBaseRecipeId() != null
                && request.getTitle() != null
                && !request.getTitle().isBlank()) {
            recipe.setTitle(request.getTitle());
        }
        recipe.setDescription(json.path("description").asText(null));
        recipe.setCategory(json.path("category").asText(null));
        recipe.setPrepTimeMin(json.path("prepTimeMin").asInt(0));
        recipe.setDifficulty(json.path("difficulty").asText(null));
        recipe.setServings(json.path("servings").asInt(2));
        recipe.setPreferences(request.getPreferences());
        recipe.setBaseRecipeId(request.getBaseRecipeId());
        recipe.setImageUrl(imageResolver.resolveImage(recipe.getTitle(), request.getBaseRecipeId(), recipe.getCategory()));
        recipe.setCreatedAt(new Date());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        recipe.setExpiresAt(cal.getTime());

        List<RecipeIngredient> ingredients = new ArrayList<>();
        JsonNode ingredientsNode = json.path("ingredients");
        if (ingredientsNode.isArray()) {
            for (JsonNode node : ingredientsNode) {
                RecipeIngredient ing = new RecipeIngredient();
                ing.setName(node.path("name").asText(null));
                ing.setAmount(node.path("amount").isNull() ? null : node.path("amount").asDouble());
                ing.setUnit(node.path("unit").asText(null));
                ing.setType(node.path("type").asText(null));
                ing.setMatchingDisplayName(node.path("matchingDisplayName").isNull() ? null : node.path("matchingDisplayName").asText(null));
                ing.setMatchingSubcategory(node.path("matchingSubcategory").isNull() ? null : node.path("matchingSubcategory").asText(null));
                ingredients.add(ing);
            }
        }
        recipe.setIngredients(ingredients);

        List<RecipeStep> steps = new ArrayList<>();
        JsonNode instructionsNode = json.path("instructions");
        if (instructionsNode.isArray()) {
            for (JsonNode node : instructionsNode) {
                RecipeStep step = new RecipeStep();
                step.setStep(node.path("step").asInt());
                step.setText(node.path("text").asText(null));
                steps.add(step);
            }
        }
        recipe.setInstructions(steps);

        JsonNode nutritionNode = json.path("totalNutritionPerServing");
        if (!nutritionNode.isMissingNode()) {
            RecipeNutrition nutrition = new RecipeNutrition();
            nutrition.setKcal(nutritionNode.path("kcal").asInt());
            nutrition.setFat(nutritionNode.path("fat").asDouble());
            nutrition.setSaturatedFat(nutritionNode.path("saturatedFat").asDouble());
            nutrition.setCarbs(nutritionNode.path("carbs").asDouble());
            nutrition.setSugar(nutritionNode.path("sugar").asDouble());
            nutrition.setProtein(nutritionNode.path("protein").asDouble());
            nutrition.setSalt(nutritionNode.path("salt").asDouble());
            recipe.setTotalNutritionPerServing(nutrition);
        }

        return recipe;
    }
}
