package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.DiyRecipeDto;
import ch.zhaw.plantifyme.model.dto.DiyRecipeListItemDto;
import ch.zhaw.plantifyme.service.DiyRecipeService;
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

@Tag(name = "DIY-Rezepte", description = "Pflanzliche Basics zum Selbstmachen (z.B. Hafermilch, veganer Frischkäse) mit per-100g Nährwerten")
@RestController
@RequestMapping("/api/diy-recipes")
@RequiredArgsConstructor
public class DiyRecipeController {

    @NonNull
    private final DiyRecipeService diyRecipeService;

    @Operation(
            summary = "DIY-Rezepte abrufen",
            description = "Liefert alle DIY-Rezepte. Mit Query-Params ?displayName= und optional ?subcategory= werden nur passende Rezepte für ein Frontend-Item zurückgegeben (z.B. Frischkäse-Alternativen)."
    )
    @ApiResponse(responseCode = "200", description = "Rezepte zurückgegeben")
    @GetMapping
    public ResponseEntity<List<DiyRecipeDto>> getDiyRecipes(
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String subcategory) {
        if (displayName == null || displayName.isBlank()) {
            return ResponseEntity.ok(diyRecipeService.findAll());
        }
        return ResponseEntity.ok(diyRecipeService.findByMatching(displayName, subcategory));
    }

    @Operation(
            summary = "Slim-Liste für DIY-Übersicht",
            description = "Liefert nur die Felder id, title, imageUrl, prepTimeMin, difficulty — optimiert für die Listenansicht."
    )
    @ApiResponse(responseCode = "200", description = "Slim-DTOs zurückgegeben")
    @GetMapping("/slim")
    public ResponseEntity<List<DiyRecipeListItemDto>> findAllSlim() {
        return ResponseEntity.ok(diyRecipeService.findAllSlim());
    }

    @Operation(
            summary = "DIY-Rezept-Details per ID",
            description = "Liefert ein einzelnes DIY-Rezept inklusive berechneter Nährwerte pro 100g (über NutritionUtil aggregiert aus den Zutaten)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezept gefunden"),
            @ApiResponse(responseCode = "404", description = "Rezept nicht gefunden")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DiyRecipeDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(diyRecipeService.findById(id));
    }
}
