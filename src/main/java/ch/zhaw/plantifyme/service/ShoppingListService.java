package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.AddItemRequest;
import ch.zhaw.plantifyme.model.dto.ReplaceItemRequest;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    /**
     * Changes the supermarket of a shopping list and re-matches all type A/B items
     * to products available at the new supermarket.
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

        list.setSupermarket(newSupermarket);

        List<ShoppingItem> updatedItems = list.getItems().stream().map(item -> {
            if ("C".equals(item.getType())) {
                return item;
            }

            String displayName = item.getDisplayName() != null ? item.getDisplayName() : "";
            String subcategory = item.getSubcategory() != null ? item.getSubcategory() : "";

            List<SupermarketProduct> matches = supermarketProductRepository
                    .findByMatchingKeyAndSupermarket(displayName, subcategory, newSupermarket);

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

            return item;
        }).toList();

        list.setItems(updatedItems);

        ShoppingList saved = shoppingListRepository.save(list);
        return Optional.of(saved);
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
            List<SupermarketProduct> matches = supermarketProductRepository
                    .findByMatchingKeyAndSupermarket(displayName, subcategory, list.getSupermarket());
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
}