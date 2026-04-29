package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlantifyMatchServiceTest {

    @Mock
    private SupermarketProductRepository supermarketProductRepository;

    @Mock
    private GenericFoodRepository genericFoodRepository;

    @Mock
    private DiyRecipeRepository diyRecipeRepository;

    @InjectMocks
    private PlantifyMatchService service;

    // ── Fixtures ───────────────────────────────────────────────────────────

    private static MatchRequestItem item(String itemId, String displayName, String type, String subcategory, String fdId) {
        MatchRequestItem i = new MatchRequestItem();
        i.setItemId(itemId);
        i.setDisplayName(displayName);
        i.setType(type);
        i.setSubcategory(subcategory);
        i.setFrontendDictionaryId(fdId);
        return i;
    }

    private static PlantifyMatchRequest request(String supermarket, MatchPreferences prefs, MatchRequestItem... items) {
        PlantifyMatchRequest r = new PlantifyMatchRequest();
        r.setSupermarket(supermarket);
        r.setPreferences(prefs);
        r.setItems(Arrays.asList(items));
        return r;
    }

    private static MatchPreferences prefs(List<String> allergies, List<String> diet) {
        MatchPreferences p = new MatchPreferences();
        p.setAllergies(allergies);
        p.setDiet(diet);
        p.setBudget("medium");
        return p;
    }

    /** Builds a SupermarketProduct mock with lenient stubs for fields the service reads. */
    private static SupermarketProduct product(String id, Double price, Double proteinG,
                                              String allergensContains, List<String> ingredients) {
        SupermarketProduct p = mock(SupermarketProduct.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getPrice()).thenReturn(price);
        lenient().when(p.getProteinG()).thenReturn(proteinG);
        lenient().when(p.getAllergensContains()).thenReturn(allergensContains);
        lenient().when(p.getIngredients()).thenReturn(ingredients);
        return p;
    }

    private static DiyRecipe diyRecipe(String id, String title) {
        DiyRecipe d = mock(DiyRecipe.class);
        lenient().when(d.getId()).thenReturn(id);
        lenient().when(d.getTitle()).thenReturn(title);
        return d;
    }

    private static GenericFood genericFood(String id) {
        GenericFood gf = mock(GenericFood.class);
        lenient().when(gf.getId()).thenReturn(id);
        return gf;
    }

    private static MatchResult onlyResult(PlantifyMatchResponse response) {
        assertThat(response.getMatches()).hasSize(1);
        return response.getMatches().get(0);
    }

    // ── 1. Type A/B happy paths ────────────────────────────────────────────

    @Nested
    @DisplayName("Type A/B match")
    class TypeAB {

        @Test
        @DisplayName("single supermarket candidate → returned as suggestedProduct, type preserved")
        void singleCandidate() {
            SupermarketProduct candidate = product("p1", 3.50, 12.0, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Milch", "Hafer", "Migros"))
                    .thenReturn(List.of(candidate));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of(), List.of()),
                            item("1", "Milch", "A", "Hafer", null))
            );

            MatchResult r = onlyResult(response);
            assertThat(r.getType()).isEqualTo("A");
            assertThat(r.getSuggestedProduct()).isSameAs(candidate);
            assertThat(r.getGenericFood()).isNull();
            assertThat(r.getFallbackDiyRecipeId()).isNull();
        }

        @Test
        @DisplayName("multiple candidates default-sort cheapest first")
        void cheapestFirstByDefault() {
            SupermarketProduct expensive = product("p1", 9.90, null, null, null);
            SupermarketProduct cheap     = product("p2", 2.10, null, null, null);
            SupermarketProduct mid       = product("p3", 4.50, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(expensive, cheap, mid));

            PlantifyMatchResponse response = service.match(
                    request("Migros", null, item("1", "Tofu", "B", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(cheap);
        }

        @Test
        @DisplayName("'high protein' diet picks highest-protein product")
        void highProteinSorts() {
            SupermarketProduct lowProt  = product("p1", 2.00, 5.0, null, null);
            SupermarketProduct highProt = product("p2", 8.00, 25.0, null, null);
            SupermarketProduct midProt  = product("p3", 4.00, 15.0, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(lowProt, highProt, midProt));

            PlantifyMatchResponse response = service.match(
                    request("Coop", prefs(List.of(), List.of("high protein")),
                            item("1", "Tofu", "B", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(highProt);
        }

        @Test
        @DisplayName("subcategory is forwarded to repo verbatim")
        void subcategoryForwarded() {
            SupermarketProduct p = product("p1", 1.0, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(p));

            service.match(request("Migros", null,
                    item("1", "Milch", "A", "Soja", null)));

            verify(supermarketProductRepository)
                    .findByMatchingKeyAndSupermarket("Milch", "Soja", "Migros");
        }

        @Test
        @DisplayName("null subcategory becomes empty string for repo call")
        void nullSubcategoryNormalized() {
            SupermarketProduct p = product("p1", 1.0, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(p));

            service.match(request("Migros", null,
                    item("1", "Milch", "A", null, null)));

            verify(supermarketProductRepository)
                    .findByMatchingKeyAndSupermarket("Milch", "", "Migros");
        }
    }

    // ── 2. Allergen filtering ──────────────────────────────────────────────

    @Nested
    @DisplayName("Allergen filtering")
    class Allergens {

        @Test
        @DisplayName("hit on allergensContains excludes the product")
        void hitOnAllergensContains() {
            SupermarketProduct withSoy = product("p1", 1.0, null, "Enthält: Soja", null);
            SupermarketProduct safe    = product("p2", 2.0, null, "Enthält: Hafer", null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withSoy, safe));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of("Soja"), List.of()),
                            item("1", "Milch", "A", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(safe);
        }

        @Test
        @DisplayName("hit on ingredient substring excludes the product")
        void hitOnIngredient() {
            SupermarketProduct withNuts = product("p1", 1.0, null, null, List.of("Wasser", "Cashewnüsse", "Salz"));
            SupermarketProduct safe     = product("p2", 2.0, null, null, List.of("Wasser", "Hafer"));
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withNuts, safe));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of("Nüsse"), List.of()),
                            item("1", "Milch", "A", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(safe);
        }

        @Test
        @DisplayName("allergen match is case-insensitive")
        void caseInsensitive() {
            SupermarketProduct withSoy = product("p1", 1.0, null, "Enthält: SOJA", null);
            SupermarketProduct safe    = product("p2", 2.0, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withSoy, safe));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of("soja"), List.of()),
                            item("1", "Milch", "A", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(safe);
        }

        @Test
        @DisplayName("multi-allergy: product matching ANY allergy is excluded")
        void multipleAllergies() {
            SupermarketProduct withSoy   = product("p1", 1.0, null, "Soja", null);
            SupermarketProduct withNuts  = product("p2", 2.0, null, null, List.of("Haselnüsse"));
            SupermarketProduct safe      = product("p3", 3.0, null, null, List.of("Wasser"));
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withSoy, withNuts, safe));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of("Soja", "Nüsse"), List.of()),
                            item("1", "Milch", "A", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(safe);
        }

        @Test
        @DisplayName("empty allergies list disables filtering")
        void emptyAllergiesNoFilter() {
            SupermarketProduct withSoy = product("p1", 1.0, null, "Soja", null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withSoy));

            PlantifyMatchResponse response = service.match(
                    request("Migros", prefs(List.of(), List.of()),
                            item("1", "Milch", "A", "", null))
            );

            assertThat(onlyResult(response).getSuggestedProduct()).isSameAs(withSoy);
        }
    }

    // ── 3. DIY fallback ────────────────────────────────────────────────────

    @Nested
    @DisplayName("DIY fallback")
    class DiyFallback {

        @Test
        @DisplayName("no supermarket candidates + matching DIY → fallback fields populated, type preserved")
        void diyFallbackKeepsType() {
            DiyRecipe d = diyRecipe("d1", "DIY Veganer Frischkäse");
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of());
            when(diyRecipeRepository.findByMatchingDisplayName("Frischkäse"))
                    .thenReturn(List.of(d));

            PlantifyMatchResponse response = service.match(
                    request("Aldi", null, item("1", "Frischkäse", "A", "", null))
            );

            MatchResult r = onlyResult(response);
            assertThat(r.getType()).isEqualTo("A");
            assertThat(r.getSuggestedProduct()).isNull();
            assertThat(r.getGenericFood()).isNull();
            assertThat(r.getFallbackDiyRecipeId()).isEqualTo("d1");
            assertThat(r.getFallbackDiyRecipeTitle()).isEqualTo("DIY Veganer Frischkäse");
        }

        @Test
        @DisplayName("multiple DIY recipes → first one wins")
        void firstDiyWins() {
            DiyRecipe d1 = diyRecipe("d1", "First");
            DiyRecipe d2 = diyRecipe("d2", "Second");
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of());
            when(diyRecipeRepository.findByMatchingDisplayName(any()))
                    .thenReturn(List.of(d1, d2));

            MatchResult r = onlyResult(service.match(
                    request("Aldi", null, item("1", "Frischkäse", "A", "", null))
            ));

            assertThat(r.getFallbackDiyRecipeId()).isEqualTo("d1");
            assertThat(r.getFallbackDiyRecipeTitle()).isEqualTo("First");
        }

        @Test
        @DisplayName("Eier item with no supermarket match falls into DIY chain like any other ingredient")
        void eierIsNotSpecial() {
            DiyRecipe d = diyRecipe("eier-leinsamen", "Leinsamen-Ei");
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of());
            when(diyRecipeRepository.findByMatchingDisplayName("Eier"))
                    .thenReturn(List.of(d));

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Eier", "A", "", null))
            ));

            assertThat(r.getFallbackDiyRecipeId()).isEqualTo("eier-leinsamen");
            assertThat(r.getType()).isEqualTo("A");
        }
    }

    // ── 4. Generic-food fallback ───────────────────────────────────────────

    @Nested
    @DisplayName("Generic-food fallback")
    class GenericFoodFallback {

        @Test
        @DisplayName("no supermarket + no DIY → type flipped to C, generic food by name")
        void genericFoodFallback() {
            GenericFood gf = genericFood("gf1");
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of());
            when(diyRecipeRepository.findByMatchingDisplayName(any()))
                    .thenReturn(List.of());
            when(genericFoodRepository.findByName("Karotten"))
                    .thenReturn(List.of(gf));

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Karotten", "A", "", null))
            ));

            assertThat(r.getType()).isEqualTo("C");
            assertThat(r.getSuggestedProduct()).isNull();
            assertThat(r.getGenericFood()).isSameAs(gf);
        }

        @Test
        @DisplayName("no supermarket + no DIY + no generic-food → type C, genericFood null")
        void genericFoodEmpty() {
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of());
            when(diyRecipeRepository.findByMatchingDisplayName(any()))
                    .thenReturn(List.of());
            when(genericFoodRepository.findByName(any()))
                    .thenReturn(List.of());

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Unbekannt", "A", "", null))
            ));

            assertThat(r.getType()).isEqualTo("C");
            assertThat(r.getGenericFood()).isNull();
            assertThat(r.getSuggestedProduct()).isNull();
        }

        @Test
        @DisplayName("allergy filter empties candidates → falls into DIY → otherwise generic-food chain")
        void allergyEmptiedFallsThrough() {
            SupermarketProduct withSoy = product("p1", 1.0, null, "Soja", null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(withSoy));
            when(diyRecipeRepository.findByMatchingDisplayName(any()))
                    .thenReturn(List.of());
            GenericFood gf = genericFood("gf1");
            when(genericFoodRepository.findByName(any()))
                    .thenReturn(List.of(gf));

            MatchResult r = onlyResult(service.match(
                    request("Migros", prefs(List.of("Soja"), List.of()),
                            item("1", "Milch", "A", "", null))
            ));

            assertThat(r.getType()).isEqualTo("C");
            assertThat(r.getGenericFood()).isSameAs(gf);
        }
    }

    // ── 5. Type C item path ────────────────────────────────────────────────

    @Nested
    @DisplayName("Type C path")
    class TypeC {

        @Test
        @DisplayName("Type C → generic-food by frontendDictionaryId, supermarket repo never touched")
        void typeCDirectLookup() {
            GenericFood gf = genericFood("gf1");
            when(genericFoodRepository.findByFrontendDictionaryId("fd-99"))
                    .thenReturn(Optional.of(gf));

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Apfel", "C", "", "fd-99"))
            ));

            assertThat(r.getType()).isEqualTo("C");
            assertThat(r.getGenericFood()).isSameAs(gf);
            verify(supermarketProductRepository, never())
                    .findByMatchingKeyAndSupermarket(any(), any(), any());
            verify(diyRecipeRepository, never()).findByMatchingDisplayName(any());
        }

        @Test
        @DisplayName("Type C with unknown frontendDictionaryId → type C, genericFood null")
        void typeCUnknownId() {
            when(genericFoodRepository.findByFrontendDictionaryId(any()))
                    .thenReturn(Optional.empty());

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Was?", "C", "", "fd-x"))
            ));

            assertThat(r.getType()).isEqualTo("C");
            assertThat(r.getGenericFood()).isNull();
        }

        @Test
        @DisplayName("Type C does NOT trigger DIY-by-name or generic findByName fallbacks")
        void typeCNoFallbackChain() {
            when(genericFoodRepository.findByFrontendDictionaryId(any()))
                    .thenReturn(Optional.empty());

            service.match(request("Migros", null,
                    item("1", "Apfel", "C", "", "fd-99")));

            verify(diyRecipeRepository, never()).findByMatchingDisplayName(any());
            verify(genericFoodRepository, never()).findByName(any());
        }
    }

    // ── 6. Request-level concerns ──────────────────────────────────────────

    @Nested
    @DisplayName("Request-level concerns")
    class RequestLevel {

        @Test
        @DisplayName("mixed A+B+C items: every result returned in input order")
        void mixedItems() {
            SupermarketProduct prodA = product("a1", 2.0, null, null, null);
            SupermarketProduct prodB = product("b1", 3.0, null, null, null);
            GenericFood gf = genericFood("gf1");
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(eq("Milch"), any(), any()))
                    .thenReturn(List.of(prodA));
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(eq("Tofu"), any(), any()))
                    .thenReturn(List.of(prodB));
            when(genericFoodRepository.findByFrontendDictionaryId("fd-c"))
                    .thenReturn(Optional.of(gf));

            PlantifyMatchResponse response = service.match(request("Migros", null,
                    item("1", "Milch", "A", "", null),
                    item("2", "Tofu",  "B", "", null),
                    item("3", "Apfel", "C", "", "fd-c")
            ));

            assertThat(response.getMatches()).hasSize(3);
            assertThat(response.getMatches().get(0).getSuggestedProduct()).isSameAs(prodA);
            assertThat(response.getMatches().get(1).getSuggestedProduct()).isSameAs(prodB);
            assertThat(response.getMatches().get(2).getGenericFood()).isSameAs(gf);
        }

        @Test
        @DisplayName("itemId and originalName preserved on every result")
        void preservesItemIdAndOriginalName() {
            SupermarketProduct p = product("p1", 1.0, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(p));

            PlantifyMatchResponse response = service.match(request("Migros", null,
                    item("xyz", "Sojasauce", "B", "", null)
            ));

            MatchResult r = onlyResult(response);
            assertThat(r.getItemId()).isEqualTo("xyz");
            assertThat(r.getOriginalName()).isEqualTo("Sojasauce");
        }

        @Test
        @DisplayName("empty items list → empty matches list, no repo calls")
        void emptyItems() {
            PlantifyMatchResponse response = service.match(request("Migros", null));

            assertThat(response.getMatches()).isEmpty();
            verify(supermarketProductRepository, never())
                    .findByMatchingKeyAndSupermarket(any(), any(), any());
            verify(diyRecipeRepository, never()).findByMatchingDisplayName(any());
            verify(genericFoodRepository, never()).findByName(any());
            verify(genericFoodRepository, never()).findByFrontendDictionaryId(any());
        }

        @Test
        @DisplayName("null preferences → no allergy filter, default cheapest-first sort")
        void nullPreferences() {
            SupermarketProduct expensive = product("p1", 9.00, null, "Soja", null);
            SupermarketProduct cheap     = product("p2", 1.00, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(expensive, cheap));

            MatchResult r = onlyResult(service.match(
                    request("Migros", null, item("1", "Milch", "A", "", null))
            ));

            assertThat(r.getSuggestedProduct()).isSameAs(cheap);
        }

        @Test
        @DisplayName("preferences with null inner arrays → behaves as no preferences (cheapest first, no filter)")
        void emptyPreferenceArrays() {
            SupermarketProduct expensive = product("p1", 9.00, null, "Soja", null);
            SupermarketProduct cheap     = product("p2", 1.00, null, null, null);
            when(supermarketProductRepository.findByMatchingKeyAndSupermarket(any(), any(), any()))
                    .thenReturn(List.of(expensive, cheap));

            MatchPreferences emptyPrefs = new MatchPreferences();
            // allergies null, diet null, budget null

            MatchResult r = onlyResult(service.match(
                    request("Migros", emptyPrefs, item("1", "Milch", "A", "", null))
            ));

            assertThat(r.getSuggestedProduct()).isSameAs(cheap);

            // also check explicit empty lists, same outcome
            MatchPreferences emptyLists = prefs(Collections.emptyList(), Collections.emptyList());
            MatchResult r2 = onlyResult(service.match(
                    request("Migros", emptyLists, item("1", "Milch", "A", "", null))
            ));
            assertThat(r2.getSuggestedProduct()).isSameAs(cheap);
        }
    }
}
