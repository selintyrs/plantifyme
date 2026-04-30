package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.service.SupermarketProductService;
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

import java.util.List;

@Tag(name = "Supermarkt-Produkte", description = "Produkt-Daten der unterstützten Supermärkte (Migros, Coop, Aldi) inkl. Impact und Alternativen")
@RestController
@RequestMapping("/api")
public class SupermarketProductController {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private SupermarketProductService supermarketProductService;

    @Operation(
            summary = "Produkt-Details per ID",
            description = "Liefert vollständige Produktinfos: Zutaten, Nährwerte, Impact-Daten (CO₂/Wasser/Land bei Typ A) und Tierwohl-Felder."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produkt gefunden"),
            @ApiResponse(responseCode = "404", description = "Produkt nicht gefunden")
    })
    @GetMapping("/products/{productId}")
    public ResponseEntity<SupermarketProduct> getProductById(@PathVariable String productId) {
        return supermarketProductRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Produkt-Suche per displayName",
            description = "Sucht Produkte über matching_keys.displayName, gefiltert auf Produkte mit Bild. limit defaultet auf 1 (z.B. für Recipe-Cover-Images)."
    )
    @ApiResponse(responseCode = "200", description = "Produkte zurückgegeben")
    @GetMapping("/products")
    public ResponseEntity<List<SupermarketProduct>> searchProducts(
            @RequestParam String displayName,
            @RequestParam(defaultValue = "1") int limit) {
        List<SupermarketProduct> products = supermarketProductRepository.findByMatchingDisplayNameWithImage(displayName);
        return ResponseEntity.ok(products.stream().limit(limit).toList());
    }

    @Operation(
            summary = "Produkt-Alternativen",
            description = "Liefert max. 4 Alternativen aus derselben L4-Kategorie und im selben Supermarkt. " +
                    "Optional via ?filter=cheaper auf günstigere Alternativen beschränken."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alternativen zurückgegeben"),
            @ApiResponse(responseCode = "404", description = "Ausgangsprodukt nicht gefunden")
    })
    @GetMapping("/products/{productId}/alternatives")
    public ResponseEntity<List<SupermarketProduct>> getAlternatives(
            @PathVariable String productId,
            @RequestParam(defaultValue = "") String filter) {
        return supermarketProductService.getAlternatives(productId, filter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
