package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.dto.AddItemRequest;
import ch.zhaw.plantifyme.model.dto.AddRecipeIngredientsRequest;
import ch.zhaw.plantifyme.model.dto.CreateListFromRecipeRequest;
import ch.zhaw.plantifyme.model.dto.ReplaceItemRequest;
import ch.zhaw.plantifyme.model.dto.ReplaceWithDiyRequest;
import ch.zhaw.plantifyme.model.dto.SupermarketChangeRequest;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.service.OwnershipException;
import ch.zhaw.plantifyme.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ShoppingListController {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingListService shoppingListService;

    // TODO: Replace userId query param with Auth0 JWT in Iteration 3
    @GetMapping("/shoppinglists")
    public ResponseEntity<List<ShoppingList>> getShoppingLists(@RequestParam String userId) {
        return ResponseEntity.ok(shoppingListRepository.findByUserId(userId));
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @GetMapping("/shoppinglists/{listId}")
    public ResponseEntity<ShoppingList> getShoppingList(
            @PathVariable String listId,
            @RequestHeader("X-User-Id") String userId) {
        ShoppingList list = shoppingListRepository.findById(listId).orElse(null);
        if (list == null) {
            return ResponseEntity.notFound().build();
        }
        if (!list.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(list);
    }

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

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @DeleteMapping("/shoppinglists/{listId}")
    public ResponseEntity<Void> deleteShoppingList(
            @PathVariable String listId,
            @RequestHeader("X-User-Id") String userId) {
        ShoppingList list = shoppingListRepository.findById(listId).orElse(null);
        if (list == null) {
            return ResponseEntity.notFound().build();
        }
        if (!list.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        shoppingListRepository.deleteById(listId);
        return ResponseEntity.ok().build();
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @PatchMapping("/shoppinglists/{listId}/supermarket")
    public ResponseEntity<ShoppingList> changeSupermarket(
            @PathVariable String listId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SupermarketChangeRequest request) {
        try {
            Optional<ShoppingList> result = shoppingListService.changeSupermarket(listId, userId, request.getSupermarket());
            return result
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @PostMapping("/shoppinglists/{listId}/items")
    public ResponseEntity<ShoppingList> addItemToList(
            @PathVariable String listId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AddItemRequest request) {
        try {
            return shoppingListService.addItemToList(listId, userId, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @PatchMapping("/shoppinglists/{listId}/items/{itemId}")
    public ResponseEntity<ShoppingList> replaceItem(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ReplaceItemRequest request) {
        try {
            return shoppingListService.replaceItem(listId, userId, itemId, request.getProductId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @PostMapping("/shoppinglists/{listId}/items/{itemId}/replace-with-diy")
    public ResponseEntity<ShoppingList> replaceWithDiy(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ReplaceWithDiyRequest request) {
        try {
            return shoppingListService.replaceWithDiy(listId, userId, itemId, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/shoppinglists/{listId}/items/batch")
    public ResponseEntity<ShoppingList> addRecipeIngredients(
            @PathVariable String listId,
            @RequestBody AddRecipeIngredientsRequest request,
            @RequestHeader("X-User-Id") String userId) {
        try {
            ShoppingList updated = shoppingListService.addRecipeIngredients(listId, request, userId);
            return ResponseEntity.ok(updated);
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/shoppinglists/from-recipe")
    public ResponseEntity<ShoppingList> createFromRecipe(
            @RequestBody CreateListFromRecipeRequest request,
            @RequestHeader("X-User-Id") String userId) {
        ShoppingList list = shoppingListService.createFromRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }
}
