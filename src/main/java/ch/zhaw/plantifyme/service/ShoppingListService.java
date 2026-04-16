package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

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
}