package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.model.dto.RecipeGenerationRequest;
import ch.zhaw.plantifyme.service.RecipeGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rezept-Generierung", description = "AI-basierte Erstellung individueller veganer Rezepte (Spring AI / Anthropic)")
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeGenerationController {

    @NonNull
    private final RecipeGenerationService recipeGenerationService;

    @Operation(
            summary = "Rezept generieren",
            description = "Generiert ein neues Rezept basierend auf User-Vorgaben (z.B. verfügbare Zutaten, Diät, Schwierigkeit). " +
                    "Speichert es automatisch als SavedRecipe (isAiGenerated=true, isSaved=false → 7-Tage-TTL falls nicht gespeichert)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rezept generiert und gespeichert"),
            @ApiResponse(responseCode = "500", description = "Generierung fehlgeschlagen (z.B. AI-API down oder ungültige Antwort)")
    })
    @PostMapping("/generate")
    public ResponseEntity<?> generateRecipe(@RequestBody RecipeGenerationRequest request) {
        try {
            SavedRecipe recipe = recipeGenerationService.generateRecipe(request);
            return ResponseEntity.ok(recipe);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Rezept konnte nicht generiert werden: " + e.getMessage());
        }
    }
}
