package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.DiyRecipe;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DiyRecipeRepository extends MongoRepository<DiyRecipe, String> {

    @Query("{ 'matchingKeys': { $elemMatch: { 'displayName': ?0, 'subcategory': ?1 } } }")
    List<DiyRecipe> findByDisplayNameAndSubcategory(String displayName, String subcategory);

    @Query("{ 'matchingKeys.displayName': ?0 }")
    List<DiyRecipe> findByMatchingDisplayName(String displayName);
}
