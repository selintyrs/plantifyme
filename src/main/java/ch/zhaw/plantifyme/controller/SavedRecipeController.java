package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.service.SavedRecipeService;
import ch.zhaw.plantifyme.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Gespeicherte Rezepte", description = "User-spezifisch gespeicherte und KI-generierte Rezepte (TTL: 7 Tage für nicht gespeicherte)")
@RestController
@RequestMapping("/api/saved-recipes")
@RequiredArgsConstructor
public class SavedRecipeController {

    @NonNull
    private final SavedRecipeService savedRecipeService;

    @Operation(
            summary = "Eigene gespeicherte Rezepte",
            description = "Liefert alle Rezepte, die der authentifizierte User gespeichert oder kürzlich generiert hat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezepte zurückgegeben"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @GetMapping
    public ResponseEntity<List<SavedRecipe>> getAll() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(savedRecipeService.findByUser(userId));
    }

    @Operation(
            summary = "Gespeichertes Rezept per ID",
            description = "Liefert ein einzelnes gespeichertes Rezept inkl. berechneter Nährwerte und Impact-Daten."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezept gefunden"),
            @ApiResponse(responseCode = "404", description = "Rezept nicht gefunden")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SavedRecipe> getById(@PathVariable String id) {
        return ResponseEntity.ok(savedRecipeService.findById(id));
    }

    @Operation(
            summary = "Rezept dauerhaft speichern",
            description = "Markiert ein KI-generiertes Rezept als gespeichert (isSaved=true), wodurch es vom 7-Tage-TTL ausgenommen wird."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezept als gespeichert markiert"),
            @ApiResponse(responseCode = "404", description = "Rezept nicht gefunden")
    })
    @PostMapping("/{id}/save")
    public ResponseEntity<SavedRecipe> markAsSaved(@PathVariable String id) {
        return ResponseEntity.ok(savedRecipeService.markAsSaved(id));
    }

    @Operation(
            summary = "Gespeichertes Rezept löschen",
            description = "Entfernt ein gespeichertes Rezept aus der DB."
    )
    @ApiResponse(responseCode = "200", description = "Rezept gelöscht")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        savedRecipeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
