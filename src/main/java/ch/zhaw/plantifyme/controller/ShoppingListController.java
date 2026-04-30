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
import ch.zhaw.plantifyme.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Tag(name = "Einkaufslisten", description = "CRUD-Operationen und Spezialaktionen für User-Einkaufslisten")
@RestController
@RequestMapping("/api")
public class ShoppingListController {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ShoppingListService shoppingListService;

    @Operation(
            summary = "Eigene Listen abrufen",
            description = "Liefert alle Einkaufslisten des authentifizierten Users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listen zurückgegeben"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @GetMapping("/shoppinglists")
    public ResponseEntity<List<ShoppingList>> getShoppingLists() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(shoppingListRepository.findByUserId(userId));
    }

    @Operation(
            summary = "Liste per ID abrufen",
            description = "Liefert eine Liste, sofern sie dem authentifizierten User gehört."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste zurückgegeben"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @GetMapping("/shoppinglists/{listId}")
    public ResponseEntity<ShoppingList> getShoppingList(@PathVariable String listId) {
        String userId = AuthUtil.requireCurrentUserId();
        ShoppingList list = shoppingListRepository.findById(listId).orElse(null);
        if (list == null) {
            return ResponseEntity.notFound().build();
        }
        if (!list.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Neue Liste erstellen",
            description = "Erstellt eine neue Einkaufsliste. userId und createdAt werden serverseitig gesetzt — Werte im Request werden ignoriert."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Liste erstellt"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert"),
            @ApiResponse(responseCode = "500", description = "Persistierung fehlgeschlagen")
    })
    @PostMapping("/shoppinglists")
    public ResponseEntity<ShoppingList> createShoppingList(@RequestBody ShoppingList shoppingList) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            shoppingList.setUserId(userId);
            shoppingList.setCreatedAt(LocalDateTime.now());
            ShoppingList saved = shoppingListRepository.save(shoppingList);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Liste komplett ersetzen",
            description = "Aktualisiert Name, Supermarkt und Items der Liste in einem Schwung. " +
                    "Für gezielte Anpassungen bitte stattdessen die /items- und /supermarket-Endpoints nutzen."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste aktualisiert"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @PutMapping("/shoppinglists/{listId}")
    public ResponseEntity<ShoppingList> updateShoppingList(
            @PathVariable String listId,
            @RequestBody ShoppingList updatedList) {
        String userId = AuthUtil.requireCurrentUserId();
        ShoppingList existing = shoppingListRepository.findById(listId).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (!existing.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        existing.setName(updatedList.getName());
        existing.setSupermarket(updatedList.getSupermarket());
        existing.setItems(updatedList.getItems());

        ShoppingList saved = shoppingListRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @Operation(
            summary = "Liste löschen",
            description = "Löscht eine Liste, sofern sie dem authentifizierten User gehört. Inhalte werden mit gelöscht."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste gelöscht"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @DeleteMapping("/shoppinglists/{listId}")
    public ResponseEntity<Void> deleteShoppingList(@PathVariable String listId) {
        String userId = AuthUtil.requireCurrentUserId();
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

    @Operation(
            summary = "Supermarkt wechseln",
            description = "Wechselt den Supermarkt der Liste und re-matcht alle Typ-A/B-Items. " +
                    "Items ohne Match im neuen Supermarkt werden auf Typ C umgestellt (generisches Lebensmittel als Fallback). Typ-C-Items bleiben unverändert."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Supermarkt geändert, Items re-gematcht"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @PatchMapping("/shoppinglists/{listId}/supermarket")
    public ResponseEntity<ShoppingList> changeSupermarket(
            @PathVariable String listId,
            @RequestBody SupermarketChangeRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            Optional<ShoppingList> result = shoppingListService.changeSupermarket(listId, userId, request.getSupermarket());
            return result
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Item hinzufügen",
            description = "Fügt ein neues Item zur Liste hinzu. Bei Typ A/B wird gegen den Supermarkt gematcht; bei Typ C wird das generische Lebensmittel via frontendDictionaryId gesucht. Quantity defaultet auf 1."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item hinzugefügt"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @PostMapping("/shoppinglists/{listId}/items")
    public ResponseEntity<ShoppingList> addItemToList(
            @PathVariable String listId,
            @RequestBody AddItemRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return shoppingListService.addItemToList(listId, userId, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Item entfernen",
            description = "Entfernt ein einzelnes Item aus der Liste."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item entfernt"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste oder Item nicht gefunden")
    })
    @DeleteMapping("/shoppinglists/{listId}/items/{itemId}")
    public ResponseEntity<ShoppingList> removeItem(
            @PathVariable String listId,
            @PathVariable String itemId) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return shoppingListService.removeItem(listId, userId, itemId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Item ersetzen",
            description = "Ersetzt das Supermarktprodukt eines Items durch ein anderes (z.B. Alternative aus der Produktdetail-Seite)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item ersetzt"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste, Item oder Produkt nicht gefunden")
    })
    @PatchMapping("/shoppinglists/{listId}/items/{itemId}")
    public ResponseEntity<ShoppingList> replaceItem(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestBody ReplaceItemRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return shoppingListService.replaceItem(listId, userId, itemId, request.getProductId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Item durch DIY-Rezept ersetzen",
            description = "Ersetzt ein Item durch die ausgewählten Zutaten eines DIY-Rezepts (z.B. 'Frischkäse' → Cashew + Zitronensaft). " +
                    "Zutat 'Wasser' wird automatisch übersprungen. Neue Items erhalten ein diyGroupLabel zur Gruppierung im UI."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item ersetzt durch DIY-Zutaten"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste, Item oder DIY-Rezept nicht gefunden")
    })
    @PostMapping("/shoppinglists/{listId}/items/{itemId}/replace-with-diy")
    public ResponseEntity<ShoppingList> replaceWithDiy(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestBody ReplaceWithDiyRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return shoppingListService.replaceWithDiy(listId, userId, itemId, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Rezept-Zutaten hinzufügen (Batch)",
            description = "Fügt alle Zutaten eines Rezepts in einem Schwung zur Liste hinzu. " +
                    "'Wasser' wird übersprungen, alle anderen werden gegen den Supermarkt bzw. generische Lebensmittel gematcht."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zutaten hinzugefügt"),
            @ApiResponse(responseCode = "403", description = "Liste gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Liste nicht gefunden")
    })
    @PostMapping("/shoppinglists/{listId}/items/batch")
    public ResponseEntity<ShoppingList> addRecipeIngredients(
            @PathVariable String listId,
            @RequestBody AddRecipeIngredientsRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            ShoppingList updated = shoppingListService.addRecipeIngredients(listId, request, userId);
            return ResponseEntity.ok(updated);
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(
            summary = "Liste aus Rezept erstellen",
            description = "Erstellt eine neue Einkaufsliste basierend auf einem Rezept (Name 'Einkaufsliste für …'). " +
                    "Alle Rezept-Zutaten werden gematcht; 'Wasser' wird übersprungen."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Liste erstellt mit Zutaten"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @PostMapping("/shoppinglists/from-recipe")
    public ResponseEntity<ShoppingList> createFromRecipe(@RequestBody CreateListFromRecipeRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        ShoppingList list = shoppingListService.createFromRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }
}
