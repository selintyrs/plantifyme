package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.model.dto.RecipeGenerationRequest;
import ch.zhaw.plantifyme.service.RecipeGenerationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeGenerationController {

    @NonNull
    private final RecipeGenerationService recipeGenerationService;

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
