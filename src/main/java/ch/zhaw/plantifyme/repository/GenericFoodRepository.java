package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.GenericFood;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GenericFoodRepository extends MongoRepository<GenericFood, String> {

    Optional<GenericFood> findByFrontendDictionaryId(String frontendDictionaryId);

    Optional<GenericFood> findByName(String name);

    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<GenericFood> findByNameContainingIgnoreCase(String name);
}
