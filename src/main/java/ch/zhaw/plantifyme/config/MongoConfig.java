package ch.zhaw.plantifyme.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoConfig {

    @Bean
    public CommandLineRunner createTTLIndex(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations indexOps = mongoTemplate.indexOps("savedRecipes");
            indexOps.ensureIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(0));
        };
    }
}
