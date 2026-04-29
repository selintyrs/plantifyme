package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.BaseRecipe;
import ch.zhaw.plantifyme.model.dto.BaseRecipeDto;
import ch.zhaw.plantifyme.model.dto.BaseRecipeListItemDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseRecipeServiceTest {

    @Mock
    private BaseRecipeRepository baseRecipeRepository;

    @InjectMocks
    private BaseRecipeService service;

    private static BaseRecipe baseRecipe(String id, String title, String category) {
        BaseRecipe br = mock(BaseRecipe.class);
        lenient().when(br.getId()).thenReturn(id);
        lenient().when(br.getTitle()).thenReturn(title);
        lenient().when(br.getDescription()).thenReturn("desc-" + id);
        lenient().when(br.getCategory()).thenReturn(category);
        lenient().when(br.getPrepTimeMin()).thenReturn(20);
        lenient().when(br.getDifficulty()).thenReturn("einfach");
        lenient().when(br.getDefaultServings()).thenReturn(2);
        lenient().when(br.getImageUrl()).thenReturn("https://img/" + id + ".jpg");
        return br;
    }

    // ── 1. findAll ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Leere DB → leere Liste")
        void emptyDb() {
            when(baseRecipeRepository.findAll()).thenReturn(List.of());

            assertThat(service.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Mit Recipes → DTO-Mapping setzt Kernfelder")
        void mapsToDto() {
            BaseRecipe br = baseRecipe("r1", "Risotto", "Schweizer Küche");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            List<BaseRecipeDto> result = service.findAll();

            assertThat(result).hasSize(1);
            BaseRecipeDto dto = result.get(0);
            assertThat(dto.getId()).isEqualTo("r1");
            assertThat(dto.getTitle()).isEqualTo("Risotto");
            assertThat(dto.getCategory()).isEqualTo("Schweizer Küche");
            assertThat(dto.getPrepTimeMin()).isEqualTo(20);
            assertThat(dto.getDifficulty()).isEqualTo("einfach");
            assertThat(dto.getDefaultServings()).isEqualTo(2);
            assertThat(dto.getImageUrl()).isEqualTo("https://img/r1.jpg");
            assertThat(dto.getDescription()).isEqualTo("desc-r1");
        }
    }

    // ── 2. findAllSlim ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllSlim")
    class FindAllSlim {

        @Test
        @DisplayName("Liefert BaseRecipeListItemDto mit nur den 6 Kernfeldern")
        void slimDto() {
            BaseRecipe br = baseRecipe("r1", "Pasta", "Pasta & Pizza");
            when(baseRecipeRepository.findAll()).thenReturn(List.of(br));

            List<BaseRecipeListItemDto> result = service.findAllSlim();

            assertThat(result).hasSize(1);
            BaseRecipeListItemDto slim = result.get(0);
            assertThat(slim.getId()).isEqualTo("r1");
            assertThat(slim.getTitle()).isEqualTo("Pasta");
            assertThat(slim.getCategory()).isEqualTo("Pasta & Pizza");
            assertThat(slim.getImageUrl()).isEqualTo("https://img/r1.jpg");
            assertThat(slim.getPrepTimeMin()).isEqualTo(20);
            assertThat(slim.getDifficulty()).isEqualTo("einfach");
        }
    }

    // ── 3. findByCategory ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCategory")
    class FindByCategory {

        @Test
        @DisplayName("Filter durchgeleitet, Resultate als DTOs")
        void filtersByCategory() {
            BaseRecipe br = baseRecipe("r1", "Spaghetti", "Pasta & Pizza");
            when(baseRecipeRepository.findByCategory("Pasta & Pizza")).thenReturn(List.of(br));

            List<BaseRecipeDto> result = service.findByCategory("Pasta & Pizza");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("Pasta & Pizza");
        }

        @Test
        @DisplayName("Repo gibt leer → leere DTO-Liste")
        void emptyResults() {
            when(baseRecipeRepository.findByCategory("Sonstiges")).thenReturn(List.of());

            assertThat(service.findByCategory("Sonstiges")).isEmpty();
        }
    }

    // ── 4. findById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Existing id → DTO mit Feldern")
        void existing() {
            BaseRecipe br = baseRecipe("r1", "Risotto", "Schweizer Küche");
            when(baseRecipeRepository.findById("r1")).thenReturn(Optional.of(br));

            BaseRecipeDto dto = service.findById("r1");

            assertThat(dto.getId()).isEqualTo("r1");
            assertThat(dto.getTitle()).isEqualTo("Risotto");
        }

        @Test
        @DisplayName("Unknown id → ResponseStatusException 404")
        void unknown() {
            when(baseRecipeRepository.findById("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById("ghost"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Base recipe not found");
        }
    }
}
