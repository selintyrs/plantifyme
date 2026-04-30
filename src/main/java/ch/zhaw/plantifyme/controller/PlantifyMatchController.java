package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plantify Match", description = "Matching-Logik für vegane Alternativen pro Supermarkt — Kernstück der Plantify-App")
@RestController
@RequestMapping("/api")
public class PlantifyMatchController {

    @Autowired
    private PlantifyMatchService plantifyMatchService;

    @Operation(
            summary = "Einkaufsliste matchen",
            description = "Nimmt eine Liste von Items entgegen und liefert vegane Alternativen für den gewählten Supermarkt. " +
                    "Berücksichtigt User-Allergien und Diät-Präferenzen. " +
                    "Fallback-Kette: Typ A/B Supermarkt-Match → DIY-Rezept → Typ C generisches Lebensmittel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match erfolgreich, Resultate in der Response"),
            @ApiResponse(responseCode = "400", description = "Supermarkt fehlt oder ungültig"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @PostMapping("/plantify-match")
    public ResponseEntity<PlantifyMatchResponse> match(@RequestBody PlantifyMatchRequest request) {
        if (request.getSupermarket() == null || request.getSupermarket().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(plantifyMatchService.match(request));
    }
}
