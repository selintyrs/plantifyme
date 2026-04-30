package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.service.ConflictException;
import ch.zhaw.plantifyme.service.OwnershipException;
import ch.zhaw.plantifyme.service.SavedProductService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Gespeicherte Produkte", description = "User-spezifische Produkt-Favoriten (Typ A & B aus Supermarkt-Katalog)")
@RestController
@RequestMapping("/api")
public class SavedProductController {

    @Autowired
    private SavedProductService savedProductService;

    @Operation(
            summary = "Produkt als Favorit speichern",
            description = "Speichert ein Supermarktprodukt als Favorit. Liefert 409 Conflict, falls das Produkt bereits gespeichert ist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produkt gespeichert"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert"),
            @ApiResponse(responseCode = "404", description = "Produkt nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Produkt bereits in den Favoriten")
    })
    @PostMapping("/saved-products")
    public ResponseEntity<SavedProduct> saveProduct(@RequestBody SaveProductRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return savedProductService.saveProduct(userId, request)
                    .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                    .orElse(ResponseEntity.notFound().build());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Eigene Produkt-Favoriten abrufen",
            description = "Liefert alle Produkte, die der authentifizierte User als Favorit gespeichert hat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favoriten zurückgegeben"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @GetMapping("/saved-products")
    public ResponseEntity<List<SavedProduct>> getSavedProducts() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(savedProductService.getByUser(userId));
    }

    @Operation(
            summary = "Produkt aus Favoriten entfernen",
            description = "Entfernt einen Favoriten. Owner-Check verhindert das Löschen fremder Favoriten."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorit entfernt"),
            @ApiResponse(responseCode = "403", description = "Favorit gehört einem anderen User"),
            @ApiResponse(responseCode = "404", description = "Favorit nicht gefunden")
    })
    @DeleteMapping("/saved-products/{id}")
    public ResponseEntity<Void> unsaveProduct(@PathVariable String id) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            boolean found = savedProductService.unsaveProduct(id, userId);
            return found ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
