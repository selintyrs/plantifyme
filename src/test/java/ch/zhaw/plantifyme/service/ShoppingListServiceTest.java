package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyIngredient;
import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.AddItemRequest;
import ch.zhaw.plantifyme.model.dto.AddRecipeIngredientsRequest;
import ch.zhaw.plantifyme.model.dto.CreateListFromRecipeRequest;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.model.dto.RecipeIngredientItem;
import ch.zhaw.plantifyme.model.dto.ReplaceWithDiyRequest;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.repository.UserRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;
    @Mock
    private SupermarketProductRepository supermarketProductRepository;
    @Mock
    private GenericFoodRepository genericFoodRepository;
    @Mock
    private DiyRecipeRepository diyRecipeRepository;
    @Mock
    private FrontendDictionaryRepository frontendDictionaryRepository;
    @Mock
    private GenericFoodService genericFoodService;
    @Mock
    private PlantifyMatchService plantifyMatchService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShoppingListService service;

    private static final String OWNER = "user-1";
    private static final String OTHER = "user-2";
    private static final String LIST_ID = "list-1";

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static ShoppingItem item(String itemId, String type) {
        ShoppingItem i = new ShoppingItem();
        i.setItemId(itemId);
        i.setType(type);
        i.setDisplayName("Milch");
        i.setSubcategory("");
        return i;
    }

    private static ShoppingList list(String userId, String supermarket, List<ShoppingItem> items) {
        ShoppingList l = new ShoppingList();
        l.setId(LIST_ID);
        l.setUserId(userId);
        l.setSupermarket(supermarket);
        l.setItems(items != null ? new ArrayList<>(items) : null);
        return l;
    }

    private static SupermarketProduct supermarketProduct(String id, String name, Double price) {
        SupermarketProduct p = mock(SupermarketProduct.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getName()).thenReturn(name);
        lenient().when(p.getPrice()).thenReturn(price);
        return p;
    }

    private static GenericFood genericFood(String id, String name, String category) {
        GenericFood gf = mock(GenericFood.class);
        lenient().when(gf.getId()).thenReturn(id);
        lenient().when(gf.getName()).thenReturn(name);
        lenient().when(gf.getCategory()).thenReturn(category);
        return gf;
    }

    private void stubSave() {
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PlantifyMatchResponse emptyResponse() {
        PlantifyMatchResponse r = new PlantifyMatchResponse();
        r.setMatches(List.of());
        return r;
    }

    private static PlantifyMatchResponse responseWith(MatchResult... results) {
        PlantifyMatchResponse r = new PlantifyMatchResponse();
        r.setMatches(List.of(results));
        return r;
    }

    private static MatchResult matchWithProduct(String itemId, String type, SupermarketProduct product) {
        MatchResult m = new MatchResult();
        m.setItemId(itemId);
        m.setType(type);
        m.setSuggestedProduct(product);
        return m;
    }

    private static MatchResult matchWithoutProduct(String itemId, String type) {
        MatchResult m = new MatchResult();
        m.setItemId(itemId);
        m.setType(type);
        return m;
    }

    // ── 1. changeSupermarket ───────────────────────────────────────────────

    @Nested
    @DisplayName("changeSupermarket")
    class ChangeSupermarket {

        @Test
        @DisplayName("Liste nicht gefunden → Optional.empty()")
        void listNotFound() {
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.empty());

            Optional<ShoppingList> result = service.changeSupermarket(LIST_ID, OWNER, "Coop");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("User-Mismatch → OwnershipException")
        void ownershipMismatch() {
            ShoppingList l = list(OWNER, "Migros", List.of());
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> service.changeSupermarket(LIST_ID, OTHER, "Coop"))
                    .isInstanceOf(OwnershipException.class);
        }

        @Test
        @DisplayName("'MIGROS' wird zu 'Migros' normalisiert; andere Supermärkte → lowercase")
        void migrosNormalization() {
            ShoppingList l = list(OWNER, "Migros", List.of());
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            stubSave();

            ShoppingList saved = service.changeSupermarket(LIST_ID, OWNER, "MIGROS").orElseThrow();
            assertThat(saved.getSupermarket()).isEqualTo("Migros");

            ShoppingList saved2 = service.changeSupermarket(LIST_ID, OWNER, "COOP").orElseThrow();
            assertThat(saved2.getSupermarket()).isEqualTo("coop");
        }

        @Test
        @DisplayName("Type-C-Item bleibt unverändert beim Re-Match (wird nicht an match() durchgereicht)")
        void typeCUntouched() {
            ShoppingItem c = item("it-c", "C");
            c.setName("Apfel");
            c.setGenericFoodId("gf-1");
            ShoppingList l = list(OWNER, "Migros", List.of(c));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            when(plantifyMatchService.match(any())).thenReturn(emptyResponse());
            stubSave();

            ShoppingList saved = service.changeSupermarket(LIST_ID, OWNER, "Coop").orElseThrow();

            ShoppingItem after = saved.getItems().get(0);
            assertThat(after.getType()).isEqualTo("C");
            assertThat(after.getName()).isEqualTo("Apfel");
            assertThat(after.getGenericFoodId()).isEqualTo("gf-1");
        }

        @Test
        @DisplayName("Type-A mit Match → productId/name/price gesetzt, type bleibt 'A'")
        void typeAWithMatchKeepsType() {
            ShoppingItem a = item("it-a", "A");
            a.setProductId("old-prod");
            a.setName("Alt");
            a.setPrice(1.0);
            ShoppingList l = list(OWNER, "Migros", List.of(a));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            SupermarketProduct newProduct = supermarketProduct("new-prod", "Hafermilch", 2.5);
            when(plantifyMatchService.match(any()))
                    .thenReturn(responseWith(matchWithProduct("it-a", "A", newProduct)));
            stubSave();

            ShoppingList saved = service.changeSupermarket(LIST_ID, OWNER, "Coop").orElseThrow();

            ShoppingItem after = saved.getItems().get(0);
            assertThat(after.getType()).isEqualTo("A");
            assertThat(after.getProductId()).isEqualTo("new-prod");
            assertThat(after.getName()).isEqualTo("Hafermilch");
            assertThat(after.getPrice()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("Type-A ohne Match → type bleibt 'A', productId/price auf null (kein Type-Flip mehr)")
        void typeANoMatchKeepsType() {
            ShoppingItem a = item("it-a", "A");
            a.setProductId("old-prod");
            a.setName("Hafermilch");
            a.setPrice(3.0);
            ShoppingList l = list(OWNER, "Migros", List.of(a));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            // match() liefert ein Result ohne suggestedProduct (z.B. weil kein Supermarkt-Match in neuem Markt)
            when(plantifyMatchService.match(any()))
                    .thenReturn(responseWith(matchWithoutProduct("it-a", "A")));
            stubSave();

            ShoppingList saved = service.changeSupermarket(LIST_ID, OWNER, "Coop").orElseThrow();

            ShoppingItem after = saved.getItems().get(0);
            assertThat(after.getType()).isEqualTo("A");
            assertThat(after.getProductId()).isNull();
            assertThat(after.getPrice()).isNull();
        }
    }

    // ── 2. addItemToList ───────────────────────────────────────────────────

    @Nested
    @DisplayName("addItemToList")
    class AddItem {

        @Test
        @DisplayName("Liste nicht gefunden → Optional.empty()")
        void listNotFound() {
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.empty());

            Optional<ShoppingList> result = service.addItemToList(LIST_ID, OWNER, new AddItemRequest());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("User-Mismatch → OwnershipException")
        void ownershipMismatch() {
            ShoppingList l = list(OWNER, "Migros", List.of());
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> service.addItemToList(LIST_ID, OTHER, new AddItemRequest()))
                    .isInstanceOf(OwnershipException.class);
        }

        @Test
        @DisplayName("Type-C mit gefundenem genericFood → genericFoodId/name gesetzt; quantity null → 1; isChecked=false")
        void typeCWithGenericFood() {
            ShoppingList l = list(OWNER, "Migros", new ArrayList<>());
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            GenericFood gf = genericFood("gf-1", "Apfel", "Obst");
            when(genericFoodRepository.findByFrontendDictionaryId("fd-apfel")).thenReturn(Optional.of(gf));
            stubSave();

            AddItemRequest req = new AddItemRequest();
            req.setType("C");
            req.setDisplayName("Apfel");
            req.setFrontendDictionaryId("fd-apfel");
            req.setQuantity(null);

            ShoppingList saved = service.addItemToList(LIST_ID, OWNER, req).orElseThrow();
            ShoppingItem added = saved.getItems().get(saved.getItems().size() - 1);

            assertThat(added.getType()).isEqualTo("C");
            assertThat(added.getGenericFoodId()).isEqualTo("gf-1");
            assertThat(added.getName()).isEqualTo("Apfel");
            assertThat(added.getQuantity()).isEqualTo(1);
            assertThat(added.getIsChecked()).isFalse();
            assertThat(added.getItemId()).isNotNull();
        }
    }

    // ── 3. removeItem & replaceItem ────────────────────────────────────────

    @Nested
    @DisplayName("removeItem & replaceItem")
    class RemoveAndReplace {

        @Test
        @DisplayName("removeItem mit unbekanntem itemId → Optional.empty()")
        void removeUnknownItem() {
            ShoppingItem a = item("it-1", "A");
            ShoppingList l = list(OWNER, "Migros", List.of(a));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));

            Optional<ShoppingList> result = service.removeItem(LIST_ID, OWNER, "ghost");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("removeItem User-Mismatch → OwnershipException")
        void removeOwnershipMismatch() {
            ShoppingList l = list(OWNER, "Migros", List.of(item("it-1", "A")));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> service.removeItem(LIST_ID, OTHER, "it-1"))
                    .isInstanceOf(OwnershipException.class);
        }

        @Test
        @DisplayName("replaceItem ersetzt productId/name/price")
        void replaceItemSuccess() {
            ShoppingItem a = item("it-1", "A");
            a.setProductId("old-prod");
            a.setName("Alt");
            a.setPrice(1.0);
            ShoppingList l = list(OWNER, "Migros", List.of(a));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));
            SupermarketProduct newProduct = supermarketProduct("new-prod", "Neu", 2.5);
            when(supermarketProductRepository.findById("new-prod")).thenReturn(Optional.of(newProduct));
            stubSave();

            ShoppingList saved = service.replaceItem(LIST_ID, OWNER, "it-1", "new-prod").orElseThrow();
            ShoppingItem after = saved.getItems().get(0);

            assertThat(after.getProductId()).isEqualTo("new-prod");
            assertThat(after.getName()).isEqualTo("Neu");
            assertThat(after.getPrice()).isEqualTo(2.5);
        }
    }

    // ── 4. replaceWithDiy / addRecipeIngredients / createFromRecipe ────────

    @Nested
    @DisplayName("Recipe-driven operations")
    class RecipeOps {

        @Test
        @DisplayName("replaceWithDiy: 'Wasser' wird übersprungen, nur selectedIndexes hinzugefügt")
        void replaceWithDiyWasserSkipped() {
            ShoppingItem original = item("it-1", "A");
            original.setDisplayName("Frischkäse");
            ShoppingList l = list(OWNER, "Migros", List.of(original));
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.of(l));

            DiyIngredient cashew = mock(DiyIngredient.class);
            lenient().when(cashew.getName()).thenReturn("Cashew");
            lenient().when(cashew.getType()).thenReturn("C");
            DiyIngredient wasser = mock(DiyIngredient.class);
            lenient().when(wasser.getName()).thenReturn("Wasser");
            lenient().when(wasser.getType()).thenReturn("C");
            DiyIngredient lemon = mock(DiyIngredient.class);
            lenient().when(lemon.getName()).thenReturn("Zitrone");
            lenient().when(lemon.getType()).thenReturn("C");

            DiyRecipe recipe = mock(DiyRecipe.class);
            lenient().when(recipe.getIngredients()).thenReturn(List.of(cashew, wasser, lemon));
            when(diyRecipeRepository.findById("diy-1")).thenReturn(Optional.of(recipe));
            when(genericFoodRepository.findByName(any())).thenReturn(List.of());
            stubSave();

            ReplaceWithDiyRequest req = mock(ReplaceWithDiyRequest.class);
            lenient().when(req.getDiyRecipeId()).thenReturn("diy-1");
            lenient().when(req.getSelectedIndexes()).thenReturn(List.of(0, 1, 2)); // alle ausgewählt
            lenient().when(req.getOriginalDisplayName()).thenReturn("Frischkäse");

            ShoppingList saved = service.replaceWithDiy(LIST_ID, OWNER, "it-1", req).orElseThrow();

            // Original entfernt, Wasser geskippt → 2 Items (Cashew + Zitrone)
            assertThat(saved.getItems()).hasSize(2);
            assertThat(saved.getItems()).extracting(ShoppingItem::getName)
                    .containsExactly("Cashew", "Zitrone");
            assertThat(saved.getItems()).allSatisfy(i ->
                    assertThat(i.getDiyGroupLabel()).isEqualTo("Zutat für Plantified Frischkäse"));
        }

        @Test
        @DisplayName("addRecipeIngredients: Liste nicht gefunden → ResponseStatusException 404")
        void addRecipeIngredientsListNotFound() {
            when(shoppingListRepository.findById(LIST_ID)).thenReturn(Optional.empty());

            AddRecipeIngredientsRequest req = new AddRecipeIngredientsRequest();
            req.setRecipeTitle("Pasta");
            req.setIngredients(List.of());

            assertThatThrownBy(() -> service.addRecipeIngredients(LIST_ID, req, OWNER))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("List not found");
        }

        @Test
        @DisplayName("createFromRecipe: 'Wasser' wird geskippt; diyGroupLabel + UUID gesetzt")
        void createFromRecipeWasserSkipped() {
            RecipeIngredientItem tomate = new RecipeIngredientItem();
            tomate.setName("Tomate");
            tomate.setType("C");
            RecipeIngredientItem wasser = new RecipeIngredientItem();
            wasser.setName("Wasser");
            wasser.setType("C");

            CreateListFromRecipeRequest req = new CreateListFromRecipeRequest();
            req.setRecipeTitle("Tomatensauce");
            req.setSupermarket("Coop");
            req.setIngredients(List.of(tomate, wasser));

            GenericFood gfTomate = genericFood("gf-tom", "Tomate", "Gemüse");
            when(genericFoodService.findFlexible(eq("Tomate")))
                    .thenReturn(Optional.of(gfTomate));
            stubSave();

            ShoppingList saved = service.createFromRecipe(req, OWNER);

            assertThat(saved.getName()).isEqualTo("Einkaufsliste für Tomatensauce");
            assertThat(saved.getSupermarket()).isEqualTo("Coop");
            assertThat(saved.getUserId()).isEqualTo(OWNER);
            assertThat(saved.getItems()).hasSize(1); // Wasser skipped
            ShoppingItem only = saved.getItems().get(0);
            assertThat(only.getName()).isEqualTo("Tomate");
            assertThat(only.getDiyGroupLabel()).isEqualTo("Zutat für Tomatensauce");
            assertThat(only.getItemId()).isNotNull();
        }
    }
}
