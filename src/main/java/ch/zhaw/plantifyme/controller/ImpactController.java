package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.ImpactTotalDto;
import ch.zhaw.plantifyme.service.ImpactService;
import ch.zhaw.plantifyme.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Impact", description = "Berechnung des ökologischen Impacts (CO₂, Wasser, Land) und Tierwohl-Aggregation über alle Listen")
@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
public class ImpactController {

    @NonNull
    private final ImpactService impactService;

    @Operation(
            summary = "Gesamt-Impact des Users",
            description = "Aggregiert CO₂-, Wasser- und Land-Einsparungen sowie Anzahl betroffener Tiere (cow/pig/chicken/fish) " +
                    "über alle Listen des authentifizierten Users. Liefert zusätzlich anschauliche Vergleichs-Strings (z.B. 'X km Flug', 'Y volle Badewannen')."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Impact-Aggregat zurückgegeben"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @GetMapping("/total")
    public ResponseEntity<ImpactTotalDto> getTotalImpact() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(impactService.computeTotal(userId));
    }
}
