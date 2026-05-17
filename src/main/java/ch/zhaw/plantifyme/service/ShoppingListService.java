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
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.model.dto.RecipeIngredientItem;
import ch.zhaw.plantifyme.model.dto.ReplaceItemRequest;
import ch.zhaw.plantifyme.model.dto.ReplaceWithDiyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    @Autowired
    private DiyRecipeRepository diyRecipeRepository;

    @Autowired
    private FrontendDictionaryRepository frontendDictionaryRepository;

    @Autowired
    private GenericFoodService genericFoodService;

    @Autowired
    private PlantifyMatchService plantifyMatchService;

    @Autowired
    private UserRepository userRepository;

    private static final Map<String, String> ALLERGY_LABELS = Map.of(
            "nuts", "Nüsse",
            "soy", "Soja",
            "gluten", "Gluten",
            "sesame", "Sesam",
            "mustard", "Senf",
            "celery", "Sellerie",
            "lupins", "Lupinen"
    );

    private static final Map<String, String> DIET_MAP = Map.of(
            "highProtein", "high protein",
            "lowCarb", "low carb",
            "glutenFree", "gluten free",
            "lowCalorie", "low calorie"
    );

    /**
     * Changes the supermarket of a shopping list and re-matches all type A/B items
     * by delegating to PlantifyMatchService.match() — the single source of truth
     * for matching logic (allergen filter, preference sort, DIY fallback). Type C
     * items remain unchanged because they don't depend on the supermarket. Items
     * without a match keep their original type (no flip to "C") so they can be
     * re-matched correctly when switching supermarkets again.
     *
     * @throws OwnershipException if userId does not match the list owner
     */
    public Optional<ShoppingList> changeSupermarket(String listId, String userId, String newSupermarket) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();
        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        String normalized = newSupermarket.equalsIgnoreCase("Migros") ? "Migros" : newSupermarket.toLowerCase();
        list.setSupermarket(normalized);

        if (list.getItems() == null || list.getItems().isEmpty()) {
            return Optional.of(shoppingListRepository.save(list));
        }

        PlantifyMatchRequest matchRequest = buildMatchRequest(list, userId, normalized);
        PlantifyMatchResponse matchResponse = plantifyMatchService.match(matchRequest);

        Map<String, MatchResult> resultsByItemId = matchResponse.getMatches().stream()
                .collect(Collectors.toMap(MatchResult::getItemId, r -> r));

        for (ShoppingItem item : list.getItems()) {
            if ("C".equals(item.getType())) continue;

            MatchResult result = resultsByItemId.get(item.getItemId());
            if (result == null) continue;

            SupermarketProduct product = result.getSuggestedProduct();
            if (product != null) {
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setPrice(product.getPrice());
            } else {
                item.setProductId(null);
                item.setPrice(null);
            }
        }

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    private PlantifyMatchRequest buildMatchRequest(ShoppingList list, String userId, String supermarket) {
        PlantifyMatchRequest request = new PlantifyMatchRequest();
        request.setSupermarket(supermarket);

        List<MatchRequestItem> items = list.getItems().stream()
                .filter(item -> !"C".equals(item.getType()))
                .map(item -> {
                    MatchRequestItem ri = new MatchRequestItem();
                    ri.setItemId(item.getItemId());
                    ri.setDisplayName(item.getDisplayName() != null ? item.getDisplayName() : "");
                    ri.setSubcategory(item.getSubcategory() != null ? item.getSubcategory() : "");
                    ri.setType(item.getType());
                    return ri;
                })
                .toList();
        request.setItems(items);

        request.setPreferences(buildPreferences(userId));
        return request;
    }

    private MatchPreferences buildPreferences(String userId) {
        MatchPreferences prefs = new MatchPreferences();
        prefs.setBudget("medium");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return prefs;

        if (user.getAllergies() != null) {
            prefs.setAllergies(user.getAllergies().stream()
                    .map(ALLERGY_LABELS::get)
                    .filter(Objects::nonNull)
                    .toList());
        }
        if (user.getDietaryPreferences() != null) {
            prefs.setDiet(user.getDietaryPreferences().stream()
                    .map(DIET_MAP::get)
                    .filter(Objects::nonNull)
                    .toList());
        }
        return prefs;
    }

    /**
     * Adds a new item to an existing shopping list, matching it against
     * supermarket products (type A/B) or generic foods (type C).
     *
     * @throws OwnershipException if userId does not match the list owner
     */
    public Optional<ShoppingList> addItemToList(String listId, String userId, AddItemRequest request) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        ShoppingItem item = new ShoppingItem();
        item.setItemId(UUID.randomUUID().toString());
        item.setDisplayName(request.getDisplayName());
        item.setSubcategory(request.getSubcategory() != null ? request.getSubcategory() : "");
        item.setType(request.getType());
        item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        item.setIsChecked(false);

        if ("C".equals(request.getType())) {
            genericFoodRepository.findByFrontendDictionaryId(request.getFrontendDictionaryId())
                    .ifPresentOrElse(food -> {
                        item.setGenericFoodId(food.getId());
                        item.setName(food.getName());
                    }, () -> {
                        item.setName(request.getDisplayName());
                    });
        } else {
            String displayName = request.getDisplayName() != null ? request.getDisplayName() : "";
            String subcategory = request.getSubcategory() != null ? request.getSubcategory() : "";
            String supermarket = list.getSupermarket();
            String normalizedSupermarket = supermarket != null && supermarket.equalsIgnoreCase("Migros")
                    ? "Migros" : supermarket != null ? supermarket.toLowerCase() : "";
            List<SupermarketProduct> matches = supermarketProductRepository
                    .findByMatchingKeyAndSupermarket(displayName, subcategory, normalizedSupermarket);
            if (!matches.isEmpty()) {
                SupermarketProduct match = matches.get(0);
                item.setProductId(match.getId());
                item.setName(match.getName());
                item.setPrice(match.getPrice());
            } else {
                item.setProductId(null);
                item.setName(displayName);
                item.setPrice(null);
            }
        }

        List<ShoppingItem> items = new ArrayList<>(list.getItems() != null ? list.getItems() : List.of());
        items.add(item);
        list.setItems(items);

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    /**
     * Replaces the supermarket product of an existing item in a shopping list.
     *
     * @throws OwnershipException if userId does not match the list owner
     */
    public Optional<ShoppingList> removeItem(String listId, String userId, String itemId) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        if (list.getItems() == null) {
            return Optional.empty();
        }

        boolean removed = list.getItems().removeIf(i -> itemId.equals(i.getItemId()));
        if (!removed) {
            return Optional.empty();
        }

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    public Optional<ShoppingList> replaceItem(String listId, String userId, String itemId, String newProductId) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        ShoppingItem item = list.getItems() == null ? null :
                list.getItems().stream()
                        .filter(i -> itemId.equals(i.getItemId()))
                        .findFirst()
                        .orElse(null);

        if (item == null) {
            return Optional.empty();
        }

        Optional<SupermarketProduct> optionalProduct = supermarketProductRepository.findById(newProductId);
        if (optionalProduct.isEmpty()) {
            return Optional.empty();
        }

        SupermarketProduct product = optionalProduct.get();
        item.setProductId(product.getId());
        item.setName(product.getName());
        item.setPrice(product.getPrice());

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    /**
     * Toggles the isChecked flag of a single item in a shopping list.
     * Used by the Erledigt-Sektion in der veganisierten View — verschoben
     * sich gecheckte Items in einen separaten Bucket am Ende der Liste.
     *
     * @throws OwnershipException if userId does not match the list owner
     */
    public Optional<ShoppingList> setItemChecked(String listId, String userId, String itemId, boolean isChecked) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        if (list.getItems() == null) {
            return Optional.empty();
        }

        ShoppingItem item = list.getItems().stream()
                .filter(i -> itemId.equals(i.getItemId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            return Optional.empty();
        }

        item.setIsChecked(isChecked);

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    /**
     * Replaces an existing shopping list item with the selected ingredients of a DIY recipe.
     * The original item is removed and one new ShoppingItem is created per selected ingredient
     * (ingredients named "Wasser" are skipped).
     *
     * @throws OwnershipException if userId does not match the list owner
     */
    public Optional<ShoppingList> replaceWithDiy(String listId, String userId, String itemId, ReplaceWithDiyRequest request) {
        Optional<ShoppingList> optionalList = shoppingListRepository.findById(listId);
        if (optionalList.isEmpty()) {
            return Optional.empty();
        }

        ShoppingList list = optionalList.get();

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        Optional<DiyRecipe> optionalRecipe = diyRecipeRepository.findById(request.getDiyRecipeId());
        if (optionalRecipe.isEmpty()) {
            return Optional.empty();
        }
        DiyRecipe recipe = optionalRecipe.get();

        if (list.getItems() == null) {
            return Optional.empty();
        }

        ShoppingItem originalItem = list.getItems().stream()
                .filter(i -> itemId.equals(i.getItemId())
                        || request.getOriginalDisplayName().equals(i.getDisplayName()))
                .findFirst()
                .orElse(null);

        if (originalItem == null) {
            return Optional.empty();
        }

        List<ShoppingItem> items = new ArrayList<>(list.getItems());
        items.remove(originalItem);

        String supermarket = list.getSupermarket();
        String normalized = supermarket != null && supermarket.equalsIgnoreCase("Migros")
                ? "Migros" : supermarket != null ? supermarket.toLowerCase() : "";

        String diyGroupLabel = "Zutat für Plantified " + request.getOriginalDisplayName();

        List<DiyIngredient> ingredients = recipe.getIngredients();
        if (ingredients != null) {
            for (int i = 0; i < ingredients.size(); i++) {
                if (!request.getSelectedIndexes().contains(i)) {
                    continue;
                }
                DiyIngredient ingredient = ingredients.get(i);

                if ("Wasser".equals(ingredient.getName())) {
                    continue;
                }

                ShoppingItem newItem = new ShoppingItem();
                newItem.setItemId(UUID.randomUUID().toString());
                newItem.setQuantity(1);
                newItem.setIsChecked(false);
                newItem.setDiyGroupLabel(diyGroupLabel);

                if ("A".equals(ingredient.getType()) || "B".equals(ingredient.getType())) {
                    String displayName = ingredient.getMatchingDisplayName() != null
                            ? ingredient.getMatchingDisplayName() : "";
                    String subcategory = ingredient.getMatchingSubcategory() != null
                            ? ingredient.getMatchingSubcategory() : "";
                    newItem.setDisplayName(displayName);
                    newItem.setSubcategory(subcategory);
                    newItem.setType(ingredient.getType());

                    List<SupermarketProduct> matches = supermarketProductRepository
                            .findByMatchingKeyAndSupermarket(displayName, subcategory, normalized);
                    if (!matches.isEmpty()) {
                        SupermarketProduct match = matches.get(0);
                        newItem.setProductId(match.getId());
                        newItem.setName(match.getName());
                        newItem.setPrice(match.getPrice());
                    } else {
                        newItem.setProductId(null);
                        newItem.setName(ingredient.getName());
                        newItem.setPrice(null);
                    }
                } else {
                    newItem.setDisplayName(ingredient.getName());
                    newItem.setSubcategory("");
                    newItem.setType("C");

                    List<GenericFood> nameMatches = genericFoodRepository.findByName(ingredient.getName());
                    if (!nameMatches.isEmpty()) {
                        GenericFood food = nameMatches.get(0);
                        newItem.setGenericFoodId(food.getId());
                        newItem.setName(food.getName());
                    } else {
                        newItem.setGenericFoodId(null);
                        newItem.setName(ingredient.getName());
                    }
                }

                items.add(newItem);
            }
        }

        list.setItems(items);
        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
    }

    public ShoppingList addRecipeIngredients(String listId, AddRecipeIngredientsRequest request, String userId) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found: " + listId));

        if (!list.getUserId().equals(userId)) {
            throw new OwnershipException("User " + userId + " does not own list " + listId);
        }

        String supermarket = list.getSupermarket();
        String normalized = supermarket != null && supermarket.equalsIgnoreCase("Migros")
                ? "Migros" : supermarket != null ? supermarket.toLowerCase() : "";

        String diyGroupLabel = "Zutat für " + request.getRecipeTitle();

        List<ShoppingItem> items = new ArrayList<>(list.getItems() != null ? list.getItems() : List.of());

        if (request.getIngredients() != null) {
            for (RecipeIngredientItem ingredient : request.getIngredients()) {
                if ("Wasser".equals(ingredient.getName())) {
                    continue;
                }

                ShoppingItem newItem = new ShoppingItem();
                newItem.setItemId(UUID.randomUUID().toString());
                newItem.setQuantity(1);
                newItem.setIsChecked(false);
                newItem.setDiyGroupLabel(diyGroupLabel);

                String type = ingredient.getType() != null ? ingredient.getType() : "C";
                newItem.setType(type);

                if ("A".equals(type) || "B".equals(type)) {
                    String displayName = ingredient.getMatchingDisplayName() != null
                            ? ingredient.getMatchingDisplayName() : "";
                    String subcategory = ingredient.getMatchingSubcategory() != null
                            ? ingredient.getMatchingSubcategory() : "";
                    newItem.setDisplayName(displayName);
                    newItem.setSubcategory(subcategory);

                    List<SupermarketProduct> matches = supermarketProductRepository
                            .findByMatchingKeyAndSupermarket(displayName, subcategory, normalized);
                    if (!matches.isEmpty()) {
                        SupermarketProduct match = matches.get(0);
                        newItem.setProductId(match.getId());
                        newItem.setName(match.getName());
                        newItem.setPrice(match.getPrice());
                    } else {
                        newItem.setType("C");
                        newItem.setDisplayName(ingredient.getName());
                        newItem.setSubcategory("");
                        GenericFood fallbackFood = findGenericFoodFlexible(ingredient.getName());
                        if (fallbackFood != null) {
                            newItem.setGenericFoodId(fallbackFood.getId());
                            newItem.setName(fallbackFood.getName());
                        } else {
                            newItem.setGenericFoodId(null);
                            newItem.setName(ingredient.getName());
                        }
                    }
                } else {
                    newItem.setDisplayName(ingredient.getName());
                    newItem.setSubcategory("");

                    GenericFood genericFood = findGenericFoodFlexible(ingredient.getName());
                    if (genericFood != null) {
                        newItem.setGenericFoodId(genericFood.getId());
                        newItem.setName(genericFood.getName());
                    } else {
                        newItem.setGenericFoodId(null);
                        newItem.setName(ingredient.getName());
                    }
                }

                newItem.setUiCategory(resolveUiCategory(newItem.getType(), newItem.getDisplayName(), newItem.getGenericFoodId()));
                items.add(newItem);
            }
        }

        list.setItems(items);
        return shoppingListRepository.save(list);
    }

    public ShoppingList createFromRecipe(CreateListFromRecipeRequest request, String userId) {
        String supermarket = request.getSupermarket();
        String normalized = supermarket != null && supermarket.equalsIgnoreCase("Migros")
                ? "Migros" : supermarket != null ? supermarket.toLowerCase() : "";

        ShoppingList list = new ShoppingList();
        list.setUserId(userId);
        list.setName("Einkaufsliste für " + request.getRecipeTitle());
        list.setSupermarket(supermarket);
        list.setCreatedAt(java.time.LocalDateTime.now());

        String diyGroupLabel = "Zutat für " + request.getRecipeTitle();
        List<ShoppingItem> items = new ArrayList<>();

        if (request.getIngredients() != null) {
            for (RecipeIngredientItem ingredient : request.getIngredients()) {
                if ("Wasser".equals(ingredient.getName())) {
                    continue;
                }

                ShoppingItem item = new ShoppingItem();
                item.setItemId(UUID.randomUUID().toString());
                item.setQuantity(1);
                item.setIsChecked(false);
                item.setDiyGroupLabel(diyGroupLabel);

                String type = ingredient.getType() != null ? ingredient.getType() : "C";
                item.setType(type);

                if ("A".equals(type) || "B".equals(type)) {
                    String displayName = ingredient.getMatchingDisplayName() != null
                            ? ingredient.getMatchingDisplayName() : "";
                    String subcategory = ingredient.getMatchingSubcategory() != null
                            ? ingredient.getMatchingSubcategory() : "";
                    item.setDisplayName(displayName);
                    item.setSubcategory(subcategory);

                    List<SupermarketProduct> matches = supermarketProductRepository
                            .findByMatchingKeyAndSupermarket(displayName, subcategory, normalized);
                    if (!matches.isEmpty()) {
                        SupermarketProduct match = matches.get(0);
                        item.setProductId(match.getId());
                        item.setName(match.getName());
                        item.setPrice(match.getPrice());
                    } else {
                        item.setType("C");
                        item.setDisplayName(ingredient.getName());
                        item.setSubcategory("");
                        GenericFood fallbackFood = findGenericFoodFlexible(ingredient.getName());
                        if (fallbackFood != null) {
                            item.setGenericFoodId(fallbackFood.getId());
                            item.setName(fallbackFood.getName());
                        } else {
                            item.setName(ingredient.getName());
                        }
                    }
                } else {
                    item.setDisplayName(ingredient.getName());
                    item.setSubcategory("");

                    GenericFood genericFood = findGenericFoodFlexible(ingredient.getName());
                    if (genericFood != null) {
                        item.setGenericFoodId(genericFood.getId());
                        item.setName(genericFood.getName());
                    } else {
                        item.setName(ingredient.getName());
                    }
                }

                item.setUiCategory(resolveUiCategory(item.getType(), item.getDisplayName(), item.getGenericFoodId()));
                items.add(item);
            }
        }

        list.setItems(items);
        return shoppingListRepository.save(list);
    }

    private String resolveUiCategory(String type, String displayName, String genericFoodId) {
        if ("C".equals(type)) {
            if (genericFoodId != null) {
                return genericFoodRepository.findById(genericFoodId)
                        .map(GenericFood::getCategory)
                        .orElse("Sonstiges");
            }
            GenericFood food = findGenericFoodFlexible(displayName);
            return food != null ? food.getCategory() : "Sonstiges";
        }
        return frontendDictionaryRepository.findByDisplayName(displayName)
                .map(ch.zhaw.plantifyme.model.FrontendDictionary::getUiCategory)
                .orElse("Sonstiges");
    }

    private GenericFood findGenericFoodFlexible(String ingredientName) {
        return genericFoodService.findFlexible(ingredientName).orElse(null);
    }
}