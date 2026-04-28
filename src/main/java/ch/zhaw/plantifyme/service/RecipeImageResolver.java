package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.repository.BaseRecipeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeImageResolver {

    private static final int SMART_MATCH_THRESHOLD = 2;

    private static final Set<String> STOPWORDS = Set.of(
            "der", "die", "das", "den", "dem", "des",
            "ein", "eine", "einen", "einem", "einer", "eines",
            "und", "oder", "mit", "auf", "für", "von", "im", "in",
            "zum", "zur", "an", "am", "vom",  "bei",
            "nicht", "kein", "keine"
    );

    private static final Map<String, String> CATEGORY_DEFAULTS = Map.of(
            "Schweizer Küche", "https://images.pexels.com/photos/4109111/pexels-photo-4109111.jpeg?w=800",
            "Pasta & Pizza", "https://images.pexels.com/photos/1438672/pexels-photo-1438672.jpeg?w=800",
            "Asiatische Küche", "https://images.pexels.com/photos/884600/pexels-photo-884600.jpeg?w=800",
            "Frühstück & Brunch", "https://images.pexels.com/photos/704569/pexels-photo-704569.jpeg?w=800",
            "Comfort Food", "https://images.pexels.com/photos/461198/pexels-photo-461198.jpeg?w=800",
            "Bowls & Salate", "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?w=800",
            "Suppen & Eintöpfe", "https://images.pexels.com/photos/539451/pexels-photo-539451.jpeg?w=800",
            "Süsses & Desserts", "https://images.pexels.com/photos/291528/pexels-photo-291528.jpeg?w=800"
    );

    private static final String FALLBACK_IMAGE = "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?w=800";

    private final BaseRecipeRepository baseRecipeRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pexels.api-key:}")
    private String pexelsApiKey;

    public String resolveImage(String aiTitle, String baseRecipeId, String aiCategory) {
        if (baseRecipeId != null && !baseRecipeId.isBlank()) {
            BaseRecipe base = baseRecipeRepository.findById(baseRecipeId).orElse(null);
            if (base != null && base.getImageUrl() != null && !base.getImageUrl().isBlank()) {
                log.info("Image resolver: using baseRecipeId={} image", baseRecipeId);
                return base.getImageUrl();
            }
        }

        String smartMatchUrl = smartMatchBaseRecipe(aiTitle, aiCategory);
        if (smartMatchUrl != null) {
            log.info("Image resolver: smart-match URL for AI title '{}'", aiTitle);
            return smartMatchUrl;
        }

        String pexelsImage = searchPexels(aiTitle);
        if (pexelsImage != null) {
            log.info("Image resolver: Pexels result for '{}'", aiTitle);
            return pexelsImage;
        }

        String categoryDefault = CATEGORY_DEFAULTS.get(aiCategory);
        if (categoryDefault != null) {
            log.info("Image resolver: category default for '{}'", aiCategory);
            return categoryDefault;
        }

        log.info("Image resolver: generic fallback");
        return FALLBACK_IMAGE;
    }

    private String smartMatchBaseRecipe(String title, String category) {
        if (title == null || title.isBlank()) return null;
        Set<String> titleTokens = tokenize(title);
        log.info("Smart-match input: title='{}' tokens={} category='{}'", title, titleTokens, category);
        if (titleTokens.isEmpty()) return null;

        BaseRecipe bestMatch = null;
        int bestScore = 0;

        for (BaseRecipe br : baseRecipeRepository.findAll()) {
            if (br.getImageUrl() == null || br.getImageUrl().isBlank()) continue;

            int score = 0;
            Set<String> brTitleTokens = tokenize(br.getTitle());

            for (String t : titleTokens) {
                if (brTitleTokens.contains(t)) score += 3;
            }

            if (category != null && category.equalsIgnoreCase(br.getCategory())) {
                score += 2;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = br;
            }
        }

        log.info("Smart-match result: bestMatch={} score={}",
                bestMatch != null ? bestMatch.getTitle() : "none", bestScore);

        if (bestMatch != null && bestScore >= SMART_MATCH_THRESHOLD) {
            return bestMatch.getImageUrl();
        }
        return null;
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        String normalized = text.toLowerCase().replaceAll("[^a-zäöüß0-9\\s-]", " ");
        Set<String> tokens = new HashSet<>();
        for (String token : normalized.split("\\s+")) {
            String t = token.trim();
            if (t.length() < 3) continue;
            if (STOPWORDS.contains(t)) continue;
            tokens.add(t);
        }
        return tokens;
    }

    private String searchPexels(String query) {
        if (pexelsApiKey == null || pexelsApiKey.isBlank()) {
            log.warn("Pexels API key not configured");
            return null;
        }
        if (query == null || query.isBlank()) return null;

        log.info("Pexels search starting: query='{}'", query);

        try {
            String url = "https://api.pexels.com/v1/search"
                    + "?query=" + URLEncoder.encode(query + " food vegan", StandardCharsets.UTF_8)
                    + "&per_page=5&orientation=landscape";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", pexelsApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.info("Pexels response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode photos = root.path("photos");
                log.info("Pexels found {} photos", photos.size());

                if (photos.isArray() && !photos.isEmpty()) {
                    JsonNode srcNode = photos.get(0).path("src");
                    String picked = srcNode.path("large").asText(null);
                    if (picked == null) picked = srcNode.path("medium").asText(null);
                    if (picked != null) {
                        log.info("Pexels picked URL: {}", picked);
                        return picked.contains("?")
                                ? picked.split("\\?")[0] + "?auto=compress&cs=tinysrgb&w=800"
                                : picked + "?w=800";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Pexels search FAILED for '{}': {}", query, e.getMessage(), e);
        }
        return null;
    }
}
