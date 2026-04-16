package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.SavedProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SavedProductRepository extends MongoRepository<SavedProduct, String> {

    List<SavedProduct> findByUserId(String userId);

    Optional<SavedProduct> findByUserIdAndProductId(String userId, String productId);
}
