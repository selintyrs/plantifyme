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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import java.util.List;

@Tag(name = "Test & Diagnostik", description = "Diagnose-Endpoints für Entwicklung — nicht für Produktivnutzung")
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

    @Operation(summary = "Erste 5 Supermarkt-Produkte (Test)", description = "Smoke-Test für SupermarketProductRepository.")
    @GetMapping("/products")
    public ResponseEntity<List<SupermarketProduct>> getFirst5Products() {
        List<SupermarketProduct> products = supermarketProductRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Erste 5 Dictionary-Einträge (Test)", description = "Smoke-Test für FrontendDictionaryRepository.")
    @GetMapping("/frontend-dictionary")
    public ResponseEntity<List<FrontendDictionary>> getFirst5FrontendDictionary() {
        List<FrontendDictionary> entries = frontendDictionaryRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(entries);
    }

    @Operation(summary = "Erstes Typ-A-Dictionary-Item (Test)", description = "Smoke-Test für findFirstByType.")
    @GetMapping("/frontend-dictionary/type-a")
    public ResponseEntity<FrontendDictionary> getFirstTypeAFrontendDictionary() {
        return frontendDictionaryRepository.findFirstByType("A")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Erste 5 generische Lebensmittel (Test)", description = "Smoke-Test für GenericFoodRepository.")
    @GetMapping("/generic-foods")
    public ResponseEntity<List<GenericFood>> getFirst5GenericFoods() {
        List<GenericFood> foods = genericFoodRepository
                .findAll(PageRequest.of(0, 5))
                .getContent();
        return ResponseEntity.ok(foods);
    }

    @Operation(summary = "Testliste anlegen", description = "Erstellt eine Hardcoded-Testliste für 'test-user' mit einem 'Milch'-Item.")
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

    @Operation(summary = "Alle Listen (ohne Auth-Filter)", description = "Diagnostik: liefert alle Listen aus der DB ohne User-Isolation.")
    @GetMapping("/shoppinglists")
    public ResponseEntity<List<ShoppingList>> getAllShoppingLists() {
        return ResponseEntity.ok(shoppingListRepository.findAll());
    }
}
