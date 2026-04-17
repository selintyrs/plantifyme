package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.SavedRecipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SavedRecipeRepository extends MongoRepository<SavedRecipe, String> {
    List<SavedRecipe> findByUserIdAndIsSavedTrue(String userId);
    List<SavedRecipe> findByUserId(String userId);
}
