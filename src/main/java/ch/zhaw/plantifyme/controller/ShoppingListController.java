package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class ShoppingListController {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @PostMapping("/shoppinglists")
    public ResponseEntity<ShoppingList> createShoppingList(@RequestBody ShoppingList shoppingList) {
        try {
            shoppingList.setCreatedAt(LocalDateTime.now());
            ShoppingList saved = shoppingListRepository.save(shoppingList);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
