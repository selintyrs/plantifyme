package ch.zhaw.plantifyme.coverage;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * FIX 1 — Müesli-Tagging.
 *
 * Ergänzt allen 66 Müesli-Produkten in supermarketProducts einen zusätzlichen
 * matching_keys-Eintrag {displayName: "Müesli", subcategory: "", type: "B"},
 * damit auch ohne Subcategory-Spezifizierung ein Match erfolgt.
 *
 * Idempotent via $addToSet (Re-Run fügt nichts Neues hinzu).
 *
 * Trigger: $env:RUN_FIX1 = "true"; .\mvnw.cmd test -Dtest=DataFix1Muesli
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX1", matches = "true")
class DataFix1Muesli {

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void applyFix1() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  FIX 1: Müesli matching_keys ergänzen\n");
        out.append("==================================================\n");

        // ─── PRE-CHECK ────────────────────────────────────────────
        Query queryWithEmptySub = Query.query(Criteria.where("matching_keys").elemMatch(
                Criteria.where("displayName").is("Müesli").and("subcategory").is("")
        ));
        Query queryAnyMuesli = Query.query(Criteria.where("matching_keys.displayName").is("Müesli"));

        long preCount_emptySub = mongoTemplate.count(queryWithEmptySub, "supermarketProducts");
        long preCount_anyMuesli = mongoTemplate.count(queryAnyMuesli, "supermarketProducts");

        out.append("\n  [PRE]  Docs mit subcategory=\"\":   ").append(preCount_emptySub).append("\n");
        out.append("  [PRE]  Docs mit irgendeinem Müesli-Key: ").append(preCount_anyMuesli).append("\n");

        // Sample BEFORE
        Document sampleBefore = mongoTemplate.findOne(queryAnyMuesli, Document.class, "supermarketProducts");
        if (sampleBefore != null) {
            out.append("\n  [PRE]  Sample (_id=").append(sampleBefore.get("_id")).append(") matching_keys:\n");
            out.append("         ").append(sampleBefore.get("matching_keys")).append("\n");
        }

        // ─── WRITE ────────────────────────────────────────────────
        Document newKey = new Document()
                .append("displayName", "Müesli")
                .append("subcategory", "")
                .append("type", "B");
        Update update = new Update().addToSet("matching_keys", newKey);

        out.append("\n  [WRITE] updateMulti mit $addToSet ");
        out.append("{displayName:\"Müesli\", subcategory:\"\", type:\"B\"}\n");

        UpdateResult result = mongoTemplate.updateMulti(queryAnyMuesli, update, "supermarketProducts");
        out.append("  [WRITE] matched=").append(result.getMatchedCount())
                .append(", modified=").append(result.getModifiedCount())
                .append(", acknowledged=").append(result.wasAcknowledged())
                .append("\n");

        // ─── POST-CHECK ───────────────────────────────────────────
        long postCount_emptySub = mongoTemplate.count(queryWithEmptySub, "supermarketProducts");
        long postCount_anyMuesli = mongoTemplate.count(queryAnyMuesli, "supermarketProducts");

        out.append("\n  [POST] Docs mit subcategory=\"\":   ").append(postCount_emptySub)
                .append(" (erwartet: ").append(preCount_anyMuesli).append(")\n");
        out.append("  [POST] Docs mit irgendeinem Müesli-Key: ").append(postCount_anyMuesli)
                .append(" (erwartet: ").append(preCount_anyMuesli).append(", unverändert)\n");

        // Sample AFTER (gleicher _id wie vorher)
        if (sampleBefore != null) {
            Document sampleAfter = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(sampleBefore.get("_id"))),
                    Document.class, "supermarketProducts");
            if (sampleAfter != null) {
                out.append("\n  [POST] Sample (_id=").append(sampleAfter.get("_id")).append(") matching_keys:\n");
                out.append("         ").append(sampleAfter.get("matching_keys")).append("\n");
            }
        }

        // ─── ZUSAMMENFASSUNG ──────────────────────────────────────
        out.append("\n  ─── ZUSAMMENFASSUNG ───\n");
        boolean countOk = postCount_emptySub == preCount_anyMuesli;
        boolean totalOk = postCount_anyMuesli == preCount_anyMuesli;
        out.append("  Subcategory-\"\" Count erreicht Ziel: ").append(countOk ? "JA ✓" : "NEIN ✗").append("\n");
        out.append("  Müesli-Total unverändert:            ").append(totalOk ? "JA ✓" : "NEIN ✗").append("\n");

        System.out.println(out);
    }
}
