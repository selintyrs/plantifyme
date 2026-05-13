package ch.zhaw.plantifyme.coverage;

import com.mongodb.client.result.DeleteResult;
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

/**
 * FIX 2 — Proteinpulver + Proteinriegel aus frontendDictionary entfernen.
 *
 * Beide Items sind Type-B, haben keinen Match in supermarketProducts und
 * keinen sinnvollen Cross-Type-Fallback. Anstatt fix-by-data zu reparieren,
 * werden sie aus dem Dictionary entfernt.
 *
 * Trigger: $env:RUN_FIX2 = "true"; .\mvnw.cmd test -Dtest=DataFix2Protein
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX2", matches = "true")
class DataFix2Protein {

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void applyFix2() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  FIX 2: Protein-Items aus frontendDictionary entfernen\n");
        out.append("==================================================\n");

        Query q = Query.query(Criteria.where("displayName").in("Proteinpulver", "Proteinriegel"));

        // ─── PRE-CHECK ────────────────────────────────────────────
        List<Document> preDocs = mongoTemplate.find(q, Document.class, "frontendDictionary");
        out.append("\n  [PRE]  Treffer: ").append(preDocs.size()).append(" (erwartet: 2)\n");
        for (Document d : preDocs) {
            out.append("         _id=").append(d.get("_id"))
                    .append(", displayName=").append(d.get("displayName"))
                    .append(", uiCategory=").append(d.get("uiCategory"))
                    .append(", type=").append(d.get("type")).append("\n");
        }

        if (preDocs.size() != 2) {
            out.append("\n  ✗ ABORT: Pre-Count != 2 — keine Schreib-Operation ausgeführt.\n");
            System.out.println(out);
            return;
        }

        // ─── WRITE ────────────────────────────────────────────────
        out.append("\n  [WRITE] deleteMany {displayName: {$in: [\"Proteinpulver\", \"Proteinriegel\"]}}\n");
        DeleteResult result = mongoTemplate.remove(q, "frontendDictionary");
        out.append("  [WRITE] deletedCount=").append(result.getDeletedCount())
                .append(", acknowledged=").append(result.wasAcknowledged()).append("\n");

        // ─── POST-CHECK ───────────────────────────────────────────
        List<Document> postDocs = mongoTemplate.find(q, Document.class, "frontendDictionary");
        out.append("\n  [POST] Treffer: ").append(postDocs.size()).append(" (erwartet: 0)\n");

        // Verifikation, dass die zwei spezifischen _ids weg sind
        for (String expectedId : List.of("92fa9d97-5453-460d-af9c-4ebdf9e2119e",
                                         "6596ba8e-dc47-4a9a-b9ce-51652becb895")) {
            Document gone = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(expectedId)),
                    Document.class, "frontendDictionary");
            out.append("  [POST] _id=").append(expectedId).append(": ")
                    .append(gone == null ? "GELÖSCHT ✓" : "NOCH DA ✗ — " + gone.get("displayName"))
                    .append("\n");
        }

        // ─── ZUSAMMENFASSUNG ──────────────────────────────────────
        out.append("\n  ─── ZUSAMMENFASSUNG ───\n");
        boolean deletedOk = result.getDeletedCount() == 2;
        boolean emptyOk = postDocs.isEmpty();
        out.append("  deletedCount == 2: ").append(deletedOk ? "JA ✓" : "NEIN ✗").append("\n");
        out.append("  Post-Find leer:    ").append(emptyOk ? "JA ✓" : "NEIN ✗").append("\n");

        System.out.println(out);
    }
}
