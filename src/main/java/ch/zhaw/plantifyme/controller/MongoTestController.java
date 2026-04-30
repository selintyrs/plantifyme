package ch.zhaw.plantifyme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

@Tag(name = "Test & Diagnostik", description = "Diagnose-Endpoints für Entwicklung — nicht für Produktivnutzung")
@RestController
public class MongoTestController {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Operation(
            summary = "MongoDB-Verbindung testen",
            description = "Schreibt ein Test-Dokument, liest es zurück und prüft den Inhalt. Diagnostik-Endpoint zur Verifikation der DB-Verbindung."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verbindung ok"),
            @ApiResponse(responseCode = "500", description = "DB-Verbindung fehlgeschlagen")
    })
    @GetMapping("/testmongodb")
    public ResponseEntity<String> testMongoDb() {
        String failure;
        try {
            // create test document
            Long time = System.currentTimeMillis();
            DBObject objectToSave = BasicDBObjectBuilder.start().add("time", time).get();
            // store test document
            DBObject saved = mongoTemplate.save(objectToSave, "Test");
            // retrive test document
            DBObject read = mongoTemplate.findById(saved.get("_id"), DBObject.class, "Test");
            // verify retrived document
            if (read != null && read.get("time").toString().equals(time.toString())) {
                return new ResponseEntity<>("Connection ok", HttpStatus.OK);
            } else {
                failure = "Retrived document does not match";
            }
        } catch (Exception e) {
            failure = e.toString();
        }
        return new ResponseEntity<>("Failed: " + failure, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}