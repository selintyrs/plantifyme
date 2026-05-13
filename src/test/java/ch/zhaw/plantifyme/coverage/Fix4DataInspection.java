package ch.zhaw.plantifyme.coverage;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX4_INSPECT", matches = "true")
class Fix4DataInspection {

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void inspect() {
        StringBuilder out = new StringBuilder("\n=== DB Inspection für Öl / Reis ===\n");

        for (String item : List.of("Öl", "Reis", "Süssungsmittel")) {
            out.append("\n--- ").append(item).append(" ---\n");
            for (String sm : List.of("Migros", "coop", "aldi")) {
                Query q = Query.query(Criteria.where("matching_keys.displayName").is(item)
                        .and("supermarket").is(sm));
                List<Document> docs = mongoTemplate.find(q, Document.class, "supermarketProducts");
                out.append("  ").append(sm).append(": ").append(docs.size()).append(" Produkte mit matching_keys.displayName=\"").append(item).append("\"\n");
                int show = Math.min(3, docs.size());
                for (int i = 0; i < show; i++) {
                    Document d = docs.get(i);
                    out.append("     ").append(d.get("name")).append("\n");
                    out.append("       matching_keys: ").append(d.get("matching_keys")).append("\n");
                }
            }
        }

        System.out.println(out);
    }
}
