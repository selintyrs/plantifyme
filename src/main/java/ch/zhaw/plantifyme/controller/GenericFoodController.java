package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.service.GenericFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Generische Lebensmittel", description = "Lookup-Daten für brand-lose Lebensmittel (Typ C: Äpfel, Karotten, etc.) mit Basis-Nährwerten")
@RestController
@RequestMapping("/api")
public class GenericFoodController {

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    @Autowired
    private GenericFoodService genericFoodService;

    @Operation(
            summary = "Generisches Lebensmittel per ID",
            description = "Liefert ein einzelnes generisches Lebensmittel inkl. Nährwerten pro 100g."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lebensmittel gefunden"),
            @ApiResponse(responseCode = "404", description = "Lebensmittel nicht gefunden")
    })
    @GetMapping("/generic-foods/{foodId}")
    public ResponseEntity<GenericFood> getGenericFoodById(@PathVariable String foodId) {
        return genericFoodRepository.findById(foodId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Flexible Suche nach Name",
            description = "Sucht ein generisches Lebensmittel via flexiblem Matching (exakt → ohne Präfix → Regex → Wort-Match). " +
                    "Wird intern z.B. für DIY-Rezept-Zutaten genutzt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lebensmittel gefunden"),
            @ApiResponse(responseCode = "404", description = "Kein passendes Lebensmittel gefunden")
    })
    @GetMapping("/generic-foods/search")
    public ResponseEntity<GenericFood> searchGenericFood(@RequestParam String name) {
        return genericFoodService.findFlexible(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
