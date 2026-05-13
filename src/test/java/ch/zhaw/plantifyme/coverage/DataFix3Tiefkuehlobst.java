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

import java.util.ArrayList;
import java.util.List;

/**
 * FIX 3 — Tiefkühlobst additionalOptions.
 *
 * Setzt im frontendDictionary-Doc für Tiefkühlobst ein additionalOptions-Array
 * mit 5 Frisch-Obst-Varianten. Jede Option mappt im Backend-Service auf den
 * gleichnamigen GenericFood.
 *
 * Idempotent: $set überschreibt das ganze Array (also Re-Run = identisches Ergebnis).
 *
 * Trigger: $env:RUN_FIX3 = "true"; .\mvnw.cmd test -Dtest=DataFix3Tiefkuehlobst
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX3", matches = "true")
class DataFix3Tiefkuehlobst {

    private static final String TIEFKUEHLOBST_ID = "ef5ac9ca-9864-406e-8073-7ab809ef246d";
    private static final List<String> VARIANTS = List.of(
            "Erdbeeren", "Heidelbeeren", "Himbeeren", "Mango", "Ananas"
    );

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void applyFix3() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  FIX 3: Tiefkühlobst additionalOptions\n");
        out.append("==================================================\n");

        // ─── PRE-CHECK: Schema-Referenz aus existierendem Multi-Option-Item ─
        out.append("\n  [PRE]  Schema-Referenz aus existierendem Item:\n");
        Document refDoc = mongoTemplate.findOne(
                Query.query(Criteria.where("additionalOptions.0").exists(true)),
                Document.class, "frontendDictionary");
        if (refDoc != null) {
            out.append("         _id: ").append(refDoc.get("_id")).append("\n");
            out.append("         displayName: ").append(refDoc.get("displayName")).append("\n");
            Object addOpts = refDoc.get("additionalOptions");
            if (addOpts instanceof List<?> list && !list.isEmpty()) {
                out.append("         additionalOptions[0]: ").append(list.get(0)).append("\n");
            }
        } else {
            out.append("         (keine Referenz gefunden — Item-Insertion ohne Schema-Check)\n");
        }

        // ─── PRE-CHECK: Tiefkühlobst-Doc holen ────────────────────────────
        Document tiefkuehlobst = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(TIEFKUEHLOBST_ID)),
                Document.class, "frontendDictionary");

        if (tiefkuehlobst == null) {
            out.append("\n  ✗ ABORT: Tiefkühlobst-Doc nicht gefunden (_id=").append(TIEFKUEHLOBST_ID).append(")\n");
            System.out.println(out);
            return;
        }

        out.append("\n  [PRE]  Tiefkühlobst-Doc gefunden:\n");
        out.append("         displayName: ").append(tiefkuehlobst.get("displayName")).append("\n");
        out.append("         uiCategory: ").append(tiefkuehlobst.get("uiCategory")).append("\n");
        out.append("         type: ").append(tiefkuehlobst.get("type")).append("\n");
        out.append("         additionalOptions (vorher): ").append(tiefkuehlobst.get("additionalOptions")).append("\n");

        // ─── WRITE ────────────────────────────────────────────────────────
        List<Document> newOptions = new ArrayList<>();
        for (String variant : VARIANTS) {
            newOptions.add(new Document()
                    .append("optionName", variant)
                    .append("additionalKeys", new Document()));
        }

        Update update = new Update().set("additionalOptions", newOptions);
        Query targetQuery = Query.query(Criteria.where("_id").is(TIEFKUEHLOBST_ID));

        out.append("\n  [WRITE] updateFirst $set additionalOptions = 5 Optionen (Erdbeeren, Heidelbeeren, ...)\n");
        UpdateResult result = mongoTemplate.updateFirst(targetQuery, update, "frontendDictionary");
        out.append("  [WRITE] matched=").append(result.getMatchedCount())
                .append(", modified=").append(result.getModifiedCount())
                .append(", acknowledged=").append(result.wasAcknowledged()).append("\n");

        // ─── POST-CHECK ───────────────────────────────────────────────────
        Document afterDoc = mongoTemplate.findOne(targetQuery, Document.class, "frontendDictionary");
        out.append("\n  [POST] additionalOptions (nachher):\n");
        Object after = afterDoc != null ? afterDoc.get("additionalOptions") : null;
        if (after instanceof List<?> afterList) {
            for (Object opt : afterList) {
                out.append("         ").append(opt).append("\n");
            }
            out.append("         Anzahl: ").append(afterList.size()).append(" (erwartet: 5)\n");
        }

        // ─── ZUSAMMENFASSUNG ──────────────────────────────────────────────
        out.append("\n  ─── ZUSAMMENFASSUNG ───\n");
        boolean countOk = (after instanceof List<?>) && ((List<?>) after).size() == 5;
        out.append("  5 additionalOptions gesetzt: ").append(countOk ? "JA ✓" : "NEIN ✗").append("\n");

        System.out.println(out);
    }
}
