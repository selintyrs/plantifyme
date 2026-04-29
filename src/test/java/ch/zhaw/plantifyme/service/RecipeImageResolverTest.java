package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.repository.BaseRecipeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeImageResolverTest {

    @Mock
    private BaseRecipeRepository baseRecipeRepository;

    @InjectMocks
    private RecipeImageResolver resolver;

    private static final String FALLBACK = "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?w=800";
    private static final String PASTA_DEFAULT = "https://images.pexels.com/photos/1438672/pexels-photo-1438672.jpeg?w=800";

    // pexelsApiKey not set → searchPexels returns null (no real API call)

    private static BaseRecipe baseRecipe(String id, String title, String category, String imageUrl) {
        BaseRecipe br = mock(BaseRecipe.class);
        lenient().when(br.getId()).thenReturn(id);
        lenient().when(br.getTitle()).thenReturn(title);
        lenient().when(br.getCategory()).thenReturn(category);
        lenient().when(br.getImageUrl()).thenReturn(imageUrl);
        return br;
    }

    // ── 1. BaseRecipe-ID Match ─────────────────────────────────────────────

    @Nested
    @DisplayName("BaseRecipe-ID direct match")
    class BaseRecipeIdMatch {

        @Test
        @DisplayName("baseRecipeId mit gültigem Bild → liefert dieses Bild, kein smart-match")
        void directHit() {
            BaseRecipe br = baseRecipe("base-1", "Risotto", "Schweizer Küche", "https://example.com/risotto.jpg");
            when(baseRecipeRepository.findById("base-1")).thenReturn(Optional.of(br));

            String url = resolver.resolveImage("AI Risotto Variation", "base-1", "Schweizer Küche");

            assertThat(url).isEqualTo("https://example.com/risotto.jpg");
            verify(baseRecipeRepository, never()).findAll();
        }

        @Test
        @DisplayName("baseRecipeId nicht gefunden → fällt durch zu smart-match")
        void notFoundFallsThrough() {
            when(baseRecipeRepository.findById("missing")).thenReturn(Optional.empty());
            when(baseRecipeRepository.findAll()).thenReturn(List.of()); // smart-match leer

            String url = resolver.resolveImage("Title", "missing", "Pasta & Pizza");

            // No smart-match, no Pexels (key blank), category default → Pasta image
            assertThat(url).isEqualTo(PASTA_DEFAULT);
        }

        @Test
        @DisplayName("baseRecipeId gefunden aber imageUrl null → fällt durch")
        void imageUrlNullFallsThrough() {
            BaseRecipe br = baseRecipe("base-1", "Title", "Pasta & Pizza", null);
            when(baseRecipeRepository.findById("base-1")).thenReturn(Optional.of(br));
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            String url = resolver.resolveImage("Title", "base-1", "Pasta & Pizza");

            assertThat(url).isEqualTo(PASTA_DEFAULT);
        }

        @Test
        @DisplayName("baseRecipeId blank → kein Lookup, direkt smart-match-Phase")
        void blankIdSkipsLookup() {
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            String url = resolver.resolveImage("Title", "  ", "Pasta & Pizza");

            assertThat(url).isEqualTo(PASTA_DEFAULT);
            verify(baseRecipeRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        }
    }

    // ── 2. Smart-Match Score-System ────────────────────────────────────────

    @Nested
    @DisplayName("Smart-match score system")
    class SmartMatch {

        @Test
        @DisplayName("Token-Treffer (Score 3) → liefert Bild des matched BaseRecipe")
        void tokenMatchScore3() {
            BaseRecipe br = baseRecipe("br-1", "Joghurt Bowl", "Frühstück & Brunch", "https://img/bowl.jpg");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            // Title shares token "bowl" → score 3
            String url = resolver.resolveImage("Açaí Bowl mit Beeren", null, "Bowls & Salate");

            assertThat(url).isEqualTo("https://img/bowl.jpg");
        }

        @Test
        @DisplayName("Nur Kategorie-Match (Score 2) reicht für SMART_MATCH_THRESHOLD")
        void categoryOnlyScore2() {
            BaseRecipe br = baseRecipe("br-1", "Hörnli Auflauf", "Schweizer Küche", "https://img/swiss.jpg");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            // Title has zero overlap with "Hörnli Auflauf"; only category matches → score 2
            String url = resolver.resolveImage("Quinoa Pfanne", null, "Schweizer Küche");

            assertThat(url).isEqualTo("https://img/swiss.jpg");
        }

        @Test
        @DisplayName("Score 0 (kein Token, andere Kategorie) → kein smart-match, fällt durch")
        void scoreOneNoMatch() {
            BaseRecipe br = baseRecipe("br-1", "Hörnli Auflauf", "Schweizer Küche", "https://img/swiss.jpg");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            String url = resolver.resolveImage("Tofu Curry", null, "Asiatische Küche");

            // No token overlap, category mismatch → bestScore=0 → fall through to category default
            assertThat(url).contains("884600"); // Asiatische Küche default
        }

        @Test
        @DisplayName("Stoppwort-Filter: nur Stoppwörter im Titel → keine Tokens → kein Match")
        void stopwordOnlyTitle() {
            BaseRecipe br = baseRecipe("br-1", "Die Das Der", "Comfort Food", "https://img/cf.jpg");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            // Both titles tokenize to empty (stopwords + length<3) → smartMatch returns null
            String url = resolver.resolveImage("der die das", null, "Comfort Food");

            // category default for Comfort Food
            assertThat(url).contains("461198");
        }
    }

    // ── 3. Fallback chain ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Fallback chain")
    class Fallback {

        @Test
        @DisplayName("Smart-Match fail, bekannte Kategorie → category default URL")
        void categoryDefault() {
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            String url = resolver.resolveImage("Spaghetti Bolognese", null, "Pasta & Pizza");

            assertThat(url).isEqualTo(PASTA_DEFAULT);
        }

        @Test
        @DisplayName("Unbekannte Kategorie → generic FALLBACK_IMAGE")
        void unknownCategoryFallback() {
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            String url = resolver.resolveImage("Etwas", null, "Unbekannte Kategorie");

            assertThat(url).isEqualTo(FALLBACK);
        }

        @Test
        @DisplayName("aiTitle blank + unbekannte Kategorie → FALLBACK_IMAGE")
        void blankTitleFallback() {
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            // Note: aiCategory must be non-null because CATEGORY_DEFAULTS is Map.of(...)
            // which throws NPE on .get(null). Documented via this test's input.
            String url = resolver.resolveImage("", null, "Sonstiges");

            assertThat(url).isEqualTo(FALLBACK);
        }
    }
}
