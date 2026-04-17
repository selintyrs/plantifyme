package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SavedRecipe;
import ch.zhaw.plantifyme.repository.SavedRecipeRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedRecipeService {

    @NonNull
    private final SavedRecipeRepository savedRecipeRepository;

    public List<SavedRecipe> findByUser(String userId) {
        return savedRecipeRepository.findByUserIdAndIsSavedTrue(userId);
    }

    public SavedRecipe findById(String id) {
        return savedRecipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved recipe not found: " + id));
    }

    public SavedRecipe markAsSaved(String id) {
        SavedRecipe recipe = savedRecipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved recipe not found: " + id));
        recipe.setSaved(true);
        recipe.setExpiresAt(null);
        return savedRecipeRepository.save(recipe);
    }

    public void delete(String id) {
        if (!savedRecipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved recipe not found: " + id);
        }
        savedRecipeRepository.deleteById(id);
    }
}
