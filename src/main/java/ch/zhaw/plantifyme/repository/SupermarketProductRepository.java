package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupermarketProductRepository extends MongoRepository<SupermarketProduct, String> {

    List<SupermarketProduct> findByPlantifyCatL4AndSupermarket(String plantifyCatL4, String supermarket);
}
