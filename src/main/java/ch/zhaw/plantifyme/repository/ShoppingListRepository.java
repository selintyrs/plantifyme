package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.ShoppingList;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {

    List<ShoppingList> findByUserId(String userId);
}
