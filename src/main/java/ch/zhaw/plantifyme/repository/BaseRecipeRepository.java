package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.BaseRecipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BaseRecipeRepository extends MongoRepository<BaseRecipe, String> {
    List<BaseRecipe> findByCategory(String category);
}
