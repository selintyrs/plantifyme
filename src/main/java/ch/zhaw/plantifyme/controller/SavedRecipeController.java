package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.service.SavedRecipeService;
import ch.zhaw.plantifyme.util.AuthUtil;
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

@RestController
@RequestMapping("/api/saved-recipes")
@RequiredArgsConstructor
public class SavedRecipeController {

    @NonNull
    private final SavedRecipeService savedRecipeService;

    @GetMapping
    public ResponseEntity<List<SavedRecipe>> getAll() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(savedRecipeService.findByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedRecipe> getById(@PathVariable String id) {
        return ResponseEntity.ok(savedRecipeService.findById(id));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<SavedRecipe> markAsSaved(@PathVariable String id) {
        return ResponseEntity.ok(savedRecipeService.markAsSaved(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        savedRecipeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
