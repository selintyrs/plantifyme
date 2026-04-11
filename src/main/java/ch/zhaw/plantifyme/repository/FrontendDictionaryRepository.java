package ch.zhaw.plantifyme.repository;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FrontendDictionaryRepository extends MongoRepository<FrontendDictionary, String> {

    Optional<FrontendDictionary> findFirstByType(String type);
}
