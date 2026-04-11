package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.GenericFood;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenericFoodRepository extends MongoRepository<GenericFood, String> {
}
