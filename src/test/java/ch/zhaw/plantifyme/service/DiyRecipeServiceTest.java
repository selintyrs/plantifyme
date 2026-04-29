package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyIngredient;
import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.DiyRecipeDto;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.NutritionDto;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiyRecipeServiceTest {

    @Mock
    private DiyRecipeRepository diyRecipeRepository;

    @Mock
    private GenericFoodRepository genericFoodRepository;

    @Mock
    private SupermarketProductRepository supermarketProductRepository;

    @InjectMocks
    private DiyRecipeService service;

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static DiyIngredient ingredient(String name, Double amount, String unit) {
        DiyIngredient ing = mock(DiyIngredient.class);
        lenient().when(ing.getName()).thenReturn(name);
        lenient().when(ing.getAmount()).thenReturn(amount);
        lenient().when(ing.getUnit()).thenReturn(unit);
        return ing;
    }

    private static DiyIngredient ingredientWithMatching(String name, Double amount, String unit,
                                                         String matchingDn, String matchingSub) {
        DiyIngredient ing = ingredient(name, amount, unit);
        lenient().when(ing.getMatchingDisplayName()).thenReturn(matchingDn);
        lenient().when(ing.getMatchingSubcategory()).thenReturn(matchingSub);
        return ing;
    }

    private static DiyRecipe recipe(String id, List<DiyIngredient> ingredients) {
        DiyRecipe r = mock(DiyRecipe.class);
        lenient().when(r.getId()).thenReturn(id);
        lenient().when(r.getTitle()).thenReturn("Test Recipe " + id);
        lenient().when(r.getIngredients()).thenReturn(ingredients);
        return r;
    }

    private static GenericFood genericFood(String name, Double kcal, Double fat, Double satFat,
                                           Double carbs, Double sugar, Double protein, Double salt) {
        GenericFood gf = mock(GenericFood.class);
        lenient().when(gf.getName()).thenReturn(name);
        lenient().when(gf.getKcalPer100g()).thenReturn(kcal);
        lenient().when(gf.getFatPer100g()).thenReturn(fat);
        lenient().when(gf.getSaturatedFatPer100g()).thenReturn(satFat);
        lenient().when(gf.getCarbsPer100g()).thenReturn(carbs);
        lenient().when(gf.getSugarPer100g()).thenReturn(sugar);
        lenient().when(gf.getProteinPer100g()).thenReturn(protein);
        lenient().when(gf.getSaltPer100g()).thenReturn(salt);
        return gf;
    }

    private static SupermarketProduct supermarketProduct(Double kcal, Double fat, Double satFat,
                                                          Double carbs, Double sugar, Double protein, Double salt) {
        SupermarketProduct p = mock(SupermarketProduct.class);
        lenient().when(p.getEnergyKcal()).thenReturn(kcal);
        lenient().when(p.getFatG()).thenReturn(fat);
        lenient().when(p.getSaturatedFatG()).thenReturn(satFat);
        lenient().when(p.getCarbsG()).thenReturn(carbs);
        lenient().when(p.getSugarG()).thenReturn(sugar);
        lenient().when(p.getProteinG()).thenReturn(protein);
        lenient().when(p.getSaltG()).thenReturn(salt);
        return p;
    }

    private NutritionDto nutritionFor(DiyRecipe r) {
        when(diyRecipeRepository.findById(r.getId())).thenReturn(Optional.of(r));
        DiyRecipeDto dto = service.findById(r.getId());
        return dto.getNutritionPer100g();
    }

    // ── 1. calculateNutritionPer100g — Kern-Logik ──────────────────────────

    @Nested
    @DisplayName("calculateNutritionPer100g – Kern")
    class CoreCalculation {

        @Test
        @DisplayName("Eine Zutat 100g (200 kcal/100g) → kcal=200 (1:1 normalisiert)")
        void singleIngredient100g() {
            DiyIngredient ing = ingredient("Tomate", 100.0, "g");
            DiyRecipe r = recipe("r1", List.of(ing));
            GenericFood gf = genericFood("Tomate", 200.0, 5.0, 1.0, 20.0, 4.0, 8.0, 0.5);
            when(genericFoodRepository.findByName("Tomate")).thenReturn(List.of(gf));

            NutritionDto n = nutritionFor(r);

            assertThat(n.getKcal()).isEqualTo(200.0);
            assertThat(n.getFat()).isEqualTo(5.0);
            assertThat(n.getCarbs()).isEqualTo(20.0);
            assertThat(n.getProtein()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Zwei gleiche Mengen → gewichtetes Mittel: 100g(200kcal)+100g(100kcal) → 150 kcal/100g")
        void weightedAverage() {
            DiyIngredient a = ingredient("A", 100.0, "g");
            DiyIngredient b = ingredient("B", 100.0, "g");
            DiyRecipe r = recipe("r1", List.of(a, b));
            GenericFood gfA = genericFood("A", 200.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            GenericFood gfB = genericFood("B", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            when(genericFoodRepository.findByName("A")).thenReturn(List.of(gfA));
            when(genericFoodRepository.findByName("B")).thenReturn(List.of(gfB));

            NutritionDto n = nutritionFor(r);

            assertThat(n.getKcal()).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Normalisierung: 200g (200 kcal/100g) → 100 kcal/100g (Total 400 kcal in 200g)")
        void normalizationScaling() {
            DiyIngredient ing = ingredient("Reis", 200.0, "g");
            DiyRecipe r = recipe("r1", List.of(ing));
            GenericFood gf = genericFood("Reis", 200.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            when(genericFoodRepository.findByName("Reis")).thenReturn(List.of(gf));

            NutritionDto n = nutritionFor(r);

            // Total kcal = 200 * (200/100) = 400, Total grams = 200, → 400 * (100/200) = 200 (→ stays)
            // Wait: factor=2.0, totalKcal=200*2=400, scale=100/200=0.5, result=400*0.5=200
            assertThat(n.getKcal()).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Zutat mit nicht konvertierbarer Unit ('Stück') → übersprungen")
        void unconvertibleUnitSkipped() {
            DiyIngredient stk = ingredient("Apfel", 1.0, "Stück");
            DiyIngredient gram = ingredient("Reis", 100.0, "g");
            DiyRecipe r = recipe("r1", List.of(stk, gram));
            GenericFood gf = genericFood("Reis", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            when(genericFoodRepository.findByName("Reis")).thenReturn(List.of(gf));

            NutritionDto n = nutritionFor(r);

            // Only the rice contributes: 100g (100 kcal/100g) → 100 kcal/100g
            assertThat(n.getKcal()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Kein GenericFood-Match → Fallback auf SupermarketProduct via matchingDisplayName")
        void supermarketFallback() {
            DiyIngredient ing = ingredientWithMatching("Special", 100.0, "g", "Milch", "Soja");
            DiyRecipe r = recipe("r1", List.of(ing));
            when(genericFoodRepository.findByName("Special")).thenReturn(List.of());
            SupermarketProduct p = supermarketProduct(150.0, 5.0, 1.0, 10.0, 2.0, 8.0, 0.3);
            when(supermarketProductRepository.findByMatchingKey("Milch", "Soja")).thenReturn(List.of(p));

            NutritionDto n = nutritionFor(r);

            assertThat(n.getKcal()).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Weder GenericFood noch SupermarketProduct → Zutat wird übersprungen, andere zählen")
        void noMatchSkippedOthersStay() {
            DiyIngredient unknown = ingredientWithMatching("Unknown", 100.0, "g", "Foo", "Bar");
            DiyIngredient known = ingredient("Reis", 100.0, "g");
            DiyRecipe r = recipe("r1", List.of(unknown, known));
            GenericFood gfReis = genericFood("Reis", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            when(genericFoodRepository.findByName("Unknown")).thenReturn(List.of());
            when(supermarketProductRepository.findByMatchingKey("Foo", "Bar")).thenReturn(List.of());
            when(genericFoodRepository.findByName("Reis")).thenReturn(List.of(gfReis));

            NutritionDto n = nutritionFor(r);

            assertThat(n.getKcal()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Salt nutzt round2 statt round1 (zwei Dezimalstellen)")
        void saltUsesRound2() {
            DiyIngredient ing = ingredient("Reis", 100.0, "g");
            DiyRecipe r = recipe("r1", List.of(ing));
            GenericFood gf = genericFood("Reis", 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.255);
            when(genericFoodRepository.findByName("Reis")).thenReturn(List.of(gf));

            NutritionDto n = nutritionFor(r);

            // round2(0.255) = 0.26 (Math.round of 25.5 = 26)
            assertThat(n.getSalt()).isEqualTo(0.26);
        }
    }

    // ── 2. Default / Edge ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Defaults & Edge cases")
    class Defaults {

        @Test
        @DisplayName("ingredients null → nutritionPer100g null")
        void nullIngredients() {
            DiyRecipe r = recipe("r1", null);
            NutritionDto n = nutritionFor(r);
            assertThat(n).isNull();
        }

        @Test
        @DisplayName("ingredients empty → nutritionPer100g null")
        void emptyIngredients() {
            DiyRecipe r = recipe("r1", List.of());
            NutritionDto n = nutritionFor(r);
            assertThat(n).isNull();
        }

        @Test
        @DisplayName("Alle Zutaten ohne kcal-Match → nutritionPer100g null")
        void allIngredientsWithoutNutrition() {
            DiyIngredient a = ingredient("A", 100.0, "g");
            DiyIngredient b = ingredientWithMatching("B", 50.0, "g", "X", "Y");
            DiyRecipe r = recipe("r1", List.of(a, b));
            when(genericFoodRepository.findByName("A")).thenReturn(List.of());
            when(genericFoodRepository.findByName("B")).thenReturn(List.of());
            when(supermarketProductRepository.findByMatchingKey("X", "Y")).thenReturn(List.of());

            NutritionDto n = nutritionFor(r);

            assertThat(n).isNull();
        }
    }

    // ── 3. Public CRUD-Methoden ────────────────────────────────────────────

    @Nested
    @DisplayName("Public CRUD methods")
    class PublicMethods {

        @Test
        @DisplayName("findById unbekannt → ResponseStatusException 404")
        void findByIdUnknown() {
            when(diyRecipeRepository.findById("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById("ghost"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("DIY recipe not found");
        }

        @Test
        @DisplayName("findByMatching: subcategory null → leerer String an Repo")
        void findByMatchingNullSub() {
            when(diyRecipeRepository.findByDisplayNameAndSubcategory(any(), any())).thenReturn(List.of());

            service.findByMatching("Milch", null);

            verify(diyRecipeRepository).findByDisplayNameAndSubcategory("Milch", "");
        }

        @Test
        @DisplayName("findByMatching: subcategory blank → leerer String an Repo")
        void findByMatchingBlankSub() {
            when(diyRecipeRepository.findByDisplayNameAndSubcategory(any(), any())).thenReturn(List.of());

            service.findByMatching("Milch", "  ");

            verify(diyRecipeRepository).findByDisplayNameAndSubcategory("Milch", "");
        }

        @Test
        @DisplayName("findByMatching: subcategory non-empty → wird durchgereicht")
        void findByMatchingNonEmptySub() {
            when(diyRecipeRepository.findByDisplayNameAndSubcategory(eq("Milch"), eq("Soja")))
                    .thenReturn(List.of());

            service.findByMatching("Milch", "Soja");

            verify(diyRecipeRepository).findByDisplayNameAndSubcategory("Milch", "Soja");
        }
    }
}
