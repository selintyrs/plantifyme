package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.BaseRecipeDto;
import ch.zhaw.plantifyme.model.dto.BaseRecipeListItemDto;
import ch.zhaw.plantifyme.service.BaseRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Base-Rezepte", description = "Vordefinierte vegane Rezepte mit Zutaten, Schritten und Nährwerten")
@RestController
@RequestMapping("/api/base-recipes")
@RequiredArgsConstructor
public class BaseRecipeController {

    @NonNull
    private final BaseRecipeService baseRecipeService;

    @Operation(
            summary = "Alle Base-Rezepte abrufen",
            description = "Liefert alle vordefinierten Rezepte. Optional via Query-Param ?category= filterbar (z.B. 'Pasta & Pizza')."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezepte zurückgegeben")
    })
    @GetMapping
    public ResponseEntity<List<BaseRecipeDto>> getBaseRecipes(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(baseRecipeService.findByCategory(category));
        }
        return ResponseEntity.ok(baseRecipeService.findAll());
    }

    @Operation(
            summary = "Slim-Liste für Übersicht",
            description = "Liefert nur die Felder id, title, category, imageUrl, prepTimeMin, difficulty — optimiert für die Listenansicht im Frontend."
    )
    @ApiResponse(responseCode = "200", description = "Slim-DTOs zurückgegeben")
    @GetMapping("/slim")
    public ResponseEntity<List<BaseRecipeListItemDto>> getBaseRecipesSlim() {
        return ResponseEntity.ok(baseRecipeService.findAllSlim());
    }

    @Operation(
            summary = "Rezept-Details per ID",
            description = "Liefert ein einzelnes Rezept inklusive Zutaten, Anleitung und Nährwerten pro Portion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezept gefunden"),
            @ApiResponse(responseCode = "404", description = "Rezept nicht gefunden")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BaseRecipeDto> getBaseRecipeById(@PathVariable String id) {
        return ResponseEntity.ok(baseRecipeService.findById(id));
    }
}
