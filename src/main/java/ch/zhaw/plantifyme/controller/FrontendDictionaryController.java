package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Frontend Dictionary", description = "Items für die Einkaufslisten-Auswahl im Frontend (Brücke zwischen UI-Anzeige und Backend-Matching)")
@RestController
@RequestMapping("/api")
public class FrontendDictionaryController {

    @Autowired
    private FrontendDictionaryRepository frontendDictionaryRepository;

    @Operation(
            summary = "Komplettes Dictionary abrufen",
            description = "Liefert alle Dictionary-Einträge in einem Schwung. Wird einmalig beim Frontend-Start geladen und gecacht (Fuse.js Fuzzy Search)."
    )
    @ApiResponse(responseCode = "200", description = "Dictionary zurückgegeben")
    @GetMapping("/frontend-dictionary")
    public ResponseEntity<List<FrontendDictionary>> getAllEntries() {
        return ResponseEntity.ok(frontendDictionaryRepository.findAll());
    }
}
