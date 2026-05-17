package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.dto.RecipeGenerationRequest;
import ch.zhaw.plantifyme.repository.BaseRecipeRepository;
import ch.zhaw.plantifyme.repository.SavedRecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-function-Tests für RecipeGenerationService.buildUserPrompt(...).
 *
 * Wir bauen den Service NICHT vollständig auf (keine LLM-Calls), sondern
 * instanziieren ihn mit Mocks der Repositories und einem Mock-ChatClient-Builder.
 * `buildUserPrompt` ist package-private und kann so direkt geprüft werden.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeGenerationServiceTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private BaseRecipeRepository baseRecipeRepository;
    @Mock private SavedRecipeRepository savedRecipeRepository;
    @Mock private RecipeImageResolver imageResolver;

    private RecipeGenerationService service;

    @BeforeEach
    void setUp() {
        // ChatClient wird im Konstruktor via builder.build() instanziiert.
        org.mockito.Mockito.when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new RecipeGenerationService(
                chatClientBuilder, baseRecipeRepository, savedRecipeRepository, imageResolver);
    }

    @Nested
    @DisplayName("excludedIngredients")
    class ExcludedIngredients {

        @Test
        @DisplayName("null → kein Excluded-Block im Prompt")
        void nullList_omitsBlock() {
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle("Pasta");
            req.setExcludedIngredients(null);

            String prompt = service.buildUserPrompt(req, null);

            assertThat(prompt).doesNotContain("WICHTIG");
            assertThat(prompt).doesNotContain("UNTER KEINEN UMSTÄNDEN");
        }

        @Test
        @DisplayName("leere Liste → kein Excluded-Block im Prompt")
        void emptyList_omitsBlock() {
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle("Pasta");
            req.setExcludedIngredients(List.of());

            String prompt = service.buildUserPrompt(req, null);

            assertThat(prompt).doesNotContain("WICHTIG");
        }

        @Test
        @DisplayName("ein Eintrag → Excluded-Block enthält genau diesen Eintrag")
        void singleEntry_appendedToPrompt() {
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle("Pasta");
            req.setExcludedIngredients(List.of("Tofu"));

            String prompt = service.buildUserPrompt(req, null);

            assertThat(prompt).contains("WICHTIG");
            assertThat(prompt).contains("UNTER KEINEN UMSTÄNDEN");
            assertThat(prompt).contains("Tofu");
        }

        @Test
        @DisplayName("mehrere Einträge → kommasepariert im Prompt")
        void multipleEntries_commaSeparated() {
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle("Pasta");
            req.setExcludedIngredients(List.of("Tofu", "Sojasauce", "Tempeh"));

            String prompt = service.buildUserPrompt(req, null);

            assertThat(prompt).contains("Tofu, Sojasauce, Tempeh");
        }

        @Test
        @DisplayName("Excluded-Block kommt NACH customIngredients-Block")
        void positionedAfterCustomIngredients() {
            RecipeGenerationRequest req = new RecipeGenerationRequest();
            req.setTitle("Pasta");
            req.setCustomIngredients(List.of("Spinat"));
            req.setExcludedIngredients(List.of("Tofu"));

            String prompt = service.buildUserPrompt(req, null);

            int customPos = prompt.indexOf("Verwende diese Zutaten");
            int excludedPos = prompt.indexOf("WICHTIG");
            assertThat(customPos).isGreaterThanOrEqualTo(0);
            assertThat(excludedPos).isGreaterThan(customPos);
        }
    }
}
