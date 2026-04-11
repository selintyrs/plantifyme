package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class ProductTestController {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private FrontendDictionaryRepository frontendDictionaryRepository;

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @GetMapping("/products")
    public ResponseEntity<List<SupermarketProduct>> getFirst5Products() {
        List<SupermarketProduct> products = supermarketProductRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/frontend-dictionary")
    public ResponseEntity<List<FrontendDictionary>> getFirst5FrontendDictionary() {
        List<FrontendDictionary> entries = frontendDictionaryRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/frontend-dictionary/type-a")
    public ResponseEntity<FrontendDictionary> getFirstTypeAFrontendDictionary() {
        return frontendDictionaryRepository.findFirstByType("A")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/generic-foods")
    public ResponseEntity<List<GenericFood>> getFirst5GenericFoods() {
        List<GenericFood> foods = genericFoodRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(foods);
    }

    @PostMapping("/shoppinglists")
    public ResponseEntity<ShoppingList> createTestShoppingList() {
        ShoppingItem milch = ShoppingItem.builder()
                .itemId("item-1")
                .type("A")
                .name("Milch")
                .quantity(1)
                .isChecked(false)
                .build();

        ShoppingList list = ShoppingList.builder()
                .userId("test-user")
                .name("Meine Testliste")
                .supermarket("Migros")
                .createdAt(LocalDateTime.now())
                .items(List.of(milch))
                .build();

        ShoppingList saved = shoppingListRepository.save(list);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/shoppinglists")
    public ResponseEntity<List<ShoppingList>> getAllShoppingLists() {
        return ResponseEntity.ok(shoppingListRepository.findAll());
    }
}
