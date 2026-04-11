package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SupermarketProductRepository extends MongoRepository<SupermarketProduct, String> {

    List<SupermarketProduct> findByPlantifyCatL4AndSupermarket(String plantifyCatL4, String supermarket);

    List<SupermarketProduct> findByPlantifyCatL4AndSupermarketAndIdNot(String plantifyCatL4, String supermarket, String id);

    @Query("{'matching_keys': {$elemMatch: {'displayName': ?0, 'subcategory': ?1}}, 'supermarket': ?2}")
    List<SupermarketProduct> findByMatchingKeyAndSupermarket(String displayName, String subcategory, String supermarket);
}
