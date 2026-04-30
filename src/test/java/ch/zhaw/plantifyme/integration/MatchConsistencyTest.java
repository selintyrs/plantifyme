package ch.zhaw.plantifyme.integration;

import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.repository.UserRepository;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import ch.zhaw.plantifyme.service.ShoppingListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cross-path consistency test: PlantifyMatchService.match() (direct API path)
 * and ShoppingListService.changeSupermarket() (saved-list re-match path) must
 * select identical products for identical inputs. Phase 4 (BA) bug:
 * changeSupermarket previously had its own reduced match algorithm — missing
 * allergen filter, preference sort, and DIY fallback. This test prevents
 * regression to that divergence.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchConsistencyTest {

    @Mock private SupermarketProductRepository supermarketProductRepository;
    @Mock private GenericFoodRepository genericFoodRepository;
    @Mock private DiyRecipeRepository diyRecipeRepository;
    @Mock private ShoppingListRepository shoppingListRepository;
    @Mock private UserRepository userRepository;

    private PlantifyMatchService plantifyMatchService;
    private ShoppingListService shoppingListService;

    private static final String USER_ID = "user-1";
    private static final String LIST_ID = "list-1";
    private static final String SUPERMARKET = "coop";

    @BeforeEach
    void wireServices() {
        plantifyMatchService = new PlantifyMatchService();
        ReflectionTestUtils.setField(plantifyMatchService, "supermarketProductRepository", supermarketProductRepository);
        ReflectionTestUtils.setField(plantifyMatchService, "genericFoodRepository", genericFoodRepository);
        ReflectionTestUtils.setField(plantifyMatchService, "diyRecipeRepository", diyRecipeRepository);

        shoppingListService = new ShoppingListService();
        ReflectionTestUtils.setField(shoppingListService, "shoppingListRepository", shoppingListRepository);
        ReflectionTestUtils.setField(shoppingListService, "supermarketProductRepository", supermarketProductRepository);
        ReflectionTestUtils.setField(shoppingListService, "genericFoodRepository", genericFoodRepository);
        ReflectionTestUtils.setField(shoppingListService, "diyRecipeRepository", diyRecipeRepository);
        ReflectionTestUtils.setField(shoppingListService, "plantifyMatchService", plantifyMatchService);
        ReflectionTestUtils.setField(shoppingListService, "userRepository", userRepository);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static SupermarketProduct product(String id, Double price, Double proteinG,
                                              String allergensContains, List<String> ingredients) {
        SupermarketProduct p = mock(SupermarketProduct.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getPrice()).thenReturn(price);
        lenient().when(p.getProteinG()).thenReturn(proteinG);
        lenient().when(p.getAllergensContains()).thenReturn(allergensContains);
        lenient().when(p.getIngredients()).thenReturn(ingredients);
        lenient().when(p.getName()).thenReturn("Product-" + id);
        return p;
    }

    private static ShoppingItem item(String itemId, String displayName, String type) {
        ShoppingItem i = new ShoppingItem();
        i.setItemId(itemId);
        i.setDisplayName(displayName);
        i.setSubcategory("");
        i.setType(type);
        return i;
    }

    private static MatchRequestItem reqItem(String itemId, String displayName, String type) {
        MatchRequestItem r = new MatchRequestItem();
        r.setItemId(itemId);
        r.setDisplayName(displayName);
        r.setSubcategory("");
        r.setType(type);
        return r;
    }

    private void stubExistingList(List<ShoppingItem> items) {
        ShoppingList list = new ShoppingList();
        list.setId(LIST_ID);
        list.setUserId(USER_ID);
        list.setSupermarket("Migros");
        list.setItems(new ArrayList<>(items));
        when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUser(List<String> allergyKeys, List<String> dietKeys) {
        User user = User.builder()
                .id(USER_ID)
                .allergies(allergyKeys)
                .dietaryPreferences(dietKeys)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private PlantifyMatchRequest buildEquivalentRequest(MatchPreferences prefs, MatchRequestItem... items) {
        PlantifyMatchRequest req = new PlantifyMatchRequest();
        req.setSupermarket(SUPERMARKET);
        req.setItems(List.of(items));
        req.setPreferences(prefs);
        return req;
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Default-Sortierung (cheapest first) — beide Pfade wählen denselben Produkt-ID")
    void cheapestFirst_consistent() {
        SupermarketProduct cheap = product("p-cheap", 1.0, 5.0, null, null);
        SupermarketProduct mid = product("p-mid", 2.5, 5.0, null, null);
        SupermarketProduct expensive = product("p-exp", 4.0, 5.0, null, null);
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Milch", "", SUPERMARKET))
                .thenReturn(List.of(expensive, mid, cheap)); // unsortiert geliefert

        // Direct path
        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");
        prefs.setAllergies(List.of());
        prefs.setDiet(List.of());
        PlantifyMatchResponse direct = plantifyMatchService.match(
                buildEquivalentRequest(prefs, reqItem("it-1", "Milch", "A"))
        );

        // Indirect path
        stubExistingList(List.of(item("it-1", "Milch", "A")));
        stubUser(List.of(), List.of());
        ShoppingList saved = shoppingListService
                .changeSupermarket(LIST_ID, USER_ID, "Coop")
                .orElseThrow();

        // Beide Pfade müssen 'p-cheap' wählen
        assertThat(direct.getMatches().get(0).getSuggestedProduct().getId()).isEqualTo("p-cheap");
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo("p-cheap");
    }

    @Test
    @DisplayName("Allergen-Filter (User hat 'soy') — beide Pfade ignorieren Soja-Produkt")
    void allergenFilter_consistent() {
        SupermarketProduct soja = product("p-soja", 1.0, 8.0, "Enthält: Soja", null);
        SupermarketProduct hafer = product("p-hafer", 1.5, 4.0, "Enthält: Hafer", null);
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Milch", "", SUPERMARKET))
                .thenReturn(List.of(soja, hafer));

        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");
        prefs.setAllergies(List.of("Soja"));
        prefs.setDiet(List.of());
        PlantifyMatchResponse direct = plantifyMatchService.match(
                buildEquivalentRequest(prefs, reqItem("it-1", "Milch", "A"))
        );

        stubExistingList(List.of(item("it-1", "Milch", "A")));
        stubUser(List.of("soy"), List.of()); // 'soy' wird zu 'Soja' im Service gemapped

        ShoppingList saved = shoppingListService
                .changeSupermarket(LIST_ID, USER_ID, "Coop")
                .orElseThrow();

        assertThat(direct.getMatches().get(0).getSuggestedProduct().getId()).isEqualTo("p-hafer");
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo("p-hafer");
    }

    @Test
    @DisplayName("High-Protein-Diet — beide Pfade wählen das proteinreichste")
    void highProteinSort_consistent() {
        SupermarketProduct lowProt = product("p-low", 1.0, 2.0, null, null);
        SupermarketProduct highProt = product("p-high", 3.0, 12.0, null, null);
        SupermarketProduct midProt = product("p-mid", 2.0, 7.0, null, null);
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Tofu", "", SUPERMARKET))
                .thenReturn(List.of(lowProt, highProt, midProt));

        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");
        prefs.setAllergies(List.of());
        prefs.setDiet(List.of("high protein"));
        PlantifyMatchResponse direct = plantifyMatchService.match(
                buildEquivalentRequest(prefs, reqItem("it-1", "Tofu", "B"))
        );

        stubExistingList(List.of(item("it-1", "Tofu", "B")));
        stubUser(List.of(), List.of("highProtein")); // mapped to 'high protein' im Service

        ShoppingList saved = shoppingListService
                .changeSupermarket(LIST_ID, USER_ID, "Coop")
                .orElseThrow();

        assertThat(direct.getMatches().get(0).getSuggestedProduct().getId()).isEqualTo("p-high");
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo("p-high");
    }

    @Test
    @DisplayName("Subcategory-Fallback (Pastrami → Aufschnitt) — beide Pfade wählen das gleiche broader-Match")
    void subcategoryFallback_consistent() {
        // Specific Pastrami: leer → triggers broader fallback
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Aufschnitt", "Pastrami", SUPERMARKET))
                .thenReturn(List.of());
        SupermarketProduct salami = product("p-salami", 2.5, 8.0, null, null);
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Aufschnitt", "", SUPERMARKET))
                .thenReturn(List.of(salami));

        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");
        prefs.setAllergies(List.of());
        prefs.setDiet(List.of());
        MatchRequestItem ri = reqItem("it-1", "Aufschnitt", "A");
        ri.setSubcategory("Pastrami");
        PlantifyMatchResponse direct = plantifyMatchService.match(
                buildEquivalentRequest(prefs, ri)
        );

        ShoppingItem item = item("it-1", "Aufschnitt", "A");
        item.setSubcategory("Pastrami");
        stubExistingList(List.of(item));
        stubUser(List.of(), List.of());

        ShoppingList saved = shoppingListService
                .changeSupermarket(LIST_ID, USER_ID, "Coop")
                .orElseThrow();

        assertThat(direct.getMatches().get(0).getSuggestedProduct().getId()).isEqualTo("p-salami");
        assertThat(direct.getMatches().get(0).getMatchLevel()).isEqualTo("broader");
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo("p-salami");
    }

    @Test
    @DisplayName("Kein Supermarkt-Match — beide Pfade haben productId=null (kein Type-Flip im neuen Code)")
    void noMatch_consistent() {
        when(supermarketProductRepository.findByMatchingKeyAndSupermarket("Milch", "", SUPERMARKET))
                .thenReturn(List.of());
        when(diyRecipeRepository.findByMatchingDisplayName("Milch")).thenReturn(List.of());
        when(genericFoodRepository.findByName("Milch")).thenReturn(List.of());

        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");
        prefs.setAllergies(List.of());
        prefs.setDiet(List.of());
        PlantifyMatchResponse direct = plantifyMatchService.match(
                buildEquivalentRequest(prefs, reqItem("it-1", "Milch", "A"))
        );

        stubExistingList(List.of(item("it-1", "Milch", "A")));
        stubUser(List.of(), List.of());

        ShoppingList saved = shoppingListService
                .changeSupermarket(LIST_ID, USER_ID, "Coop")
                .orElseThrow();

        // Beide Pfade: kein suggestedProduct
        assertThat(direct.getMatches().get(0).getSuggestedProduct()).isNull();
        // ShoppingItem: type bleibt 'A' (kein Flip), productId null
        assertThat(saved.getItems().get(0).getType()).isEqualTo("A");
        assertThat(saved.getItems().get(0).getProductId()).isNull();
    }
}
