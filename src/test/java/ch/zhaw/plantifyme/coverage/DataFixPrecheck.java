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

/**
 * READ-ONLY Vorab-Check für drei BA-Data-Fixes (Müesli-Tagging, Protein-Items entfernen,
 * Tiefkühlobst genericFoods-Insert).
 *
 * Triggert nur mit RUN_PRECHECK=true. Schreibt NICHTS.
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_PRECHECK", matches = "true")
class DataFixPrecheck {

    @Autowired private MongoTemplate mongoTemplate;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void precheck() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  DATA-FIX VORAB-CHECKS (read-only)\n");
        out.append("==================================================\n");

        // ─── FIX 1 ────────────────────────────────────────────────
        out.append("\n### FIX 1: Müesli-Tagging ###\n");

        Query q1a = Query.query(Criteria.where("matching_keys").elemMatch(
                Criteria.where("displayName").is("Müesli").and("subcategory").is("")
        ));
        long count1a = mongoTemplate.count(q1a, "supermarketProducts");
        out.append("\n  1a) Docs mit matching_keys{displayName=\"Müesli\", subcategory=\"\"}: ")
                .append(count1a).append(" (erwartet: 0)\n");

        Query q1b = Query.query(Criteria.where("matching_keys.displayName").is("Müesli"));
        long count1b = mongoTemplate.count(q1b, "supermarketProducts");
        out.append("  1b) Docs mit matching_keys.displayName=\"Müesli\" (irgendeine subcategory): ")
                .append(count1b).append(" (erwartet: 66)\n");

        // Sample-Dokument zeigen, um die matching_keys-Struktur in der Realität zu sehen
        Document sample1 = mongoTemplate.findOne(q1b, Document.class, "supermarketProducts");
        if (sample1 != null) {
            out.append("\n  Sample-Doc (1 Müesli-Produkt):\n");
            out.append("    _id: ").append(sample1.get("_id")).append("\n");
            out.append("    name: ").append(sample1.get("name")).append("\n");
            out.append("    supermarket: ").append(sample1.get("supermarket")).append("\n");
            out.append("    matching_keys: ").append(sample1.get("matching_keys")).append("\n");
        }

        // ─── FIX 2 ────────────────────────────────────────────────
        out.append("\n\n### FIX 2: Proteinpulver + Proteinriegel entfernen ###\n");
        Query q2 = Query.query(Criteria.where("displayName").in("Proteinpulver", "Proteinriegel"));
        List<Document> proteinDocs = mongoTemplate.find(q2, Document.class, "frontendDictionary");
        out.append("\n  Treffer: ").append(proteinDocs.size()).append(" (erwartet: 2)\n");
        for (Document d : proteinDocs) {
            out.append("    _id=").append(d.get("_id"))
                    .append(", displayName=").append(d.get("displayName"))
                    .append(", uiCategory=").append(d.get("uiCategory"))
                    .append(", type=").append(d.get("type")).append("\n");
        }

        // ─── FIX 3 ────────────────────────────────────────────────
        out.append("\n\n### FIX 3: Tiefkühlobst genericFood-Eintrag ###\n");

        Query q3a = Query.query(Criteria.where("displayName").is("Tiefkühlobst"));
        Document fd = mongoTemplate.findOne(q3a, Document.class, "frontendDictionary");
        out.append("\n  3a) frontendDictionary findOne(displayName=\"Tiefkühlobst\"):\n");
        if (fd == null) {
            out.append("    NICHT GEFUNDEN — Fix kann nicht durchgeführt werden ohne frontendDictionaryId\n");
        } else {
            out.append("    _id: ").append(fd.get("_id")).append("\n");
            out.append("    displayName: ").append(fd.get("displayName")).append("\n");
            out.append("    uiCategory: ").append(fd.get("uiCategory")).append("\n");
            out.append("    type: ").append(fd.get("type")).append("\n");
        }

        Query q3b = Query.query(Criteria.where("name").is("Tiefkühlobst"));
        Document existingGf = mongoTemplate.findOne(q3b, Document.class, "genericFoods");
        out.append("\n  3b) genericFoods findOne(name=\"Tiefkühlobst\"): ")
                .append(existingGf == null ? "null (erwartet)" : "EXISTIERT BEREITS — Insert würde Duplikat erzeugen!").append("\n");
        if (existingGf != null) {
            out.append("    Konflikt-Doc: ").append(existingGf.toJson()).append("\n");
        }

        // ─── FIX 3 EXTRA: imageUrl-Schema aus existierendem Frisch-Obst ableiten ────
        out.append("\n  3c) Bestehendes Frisch-Obst als Schema-Referenz für imageUrl:\n");
        for (String probe : List.of("Erdbeeren", "Heidelbeeren", "Himbeeren", "Mango", "Ananas")) {
            Document gf = mongoTemplate.findOne(
                    Query.query(Criteria.where("name").is(probe)),
                    Document.class, "genericFoods");
            if (gf != null) {
                out.append("    ").append(probe)
                        .append(" → imageUrl=").append(gf.get("imageUrl"))
                        .append(", matching_key=").append(gf.get("matching_key"))
                        .append("\n");
                if (probe.equals("Erdbeeren")) {
                    out.append("       (Vollstruktur: ").append(gf.toJson()).append(")\n");
                }
            }
        }

        System.out.println(out);
    }
}
