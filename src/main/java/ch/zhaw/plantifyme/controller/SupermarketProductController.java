package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.service.SupermarketProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SupermarketProductController {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private SupermarketProductService supermarketProductService;

    @GetMapping("/products/{productId}")
    public ResponseEntity<SupermarketProduct> getProductById(@PathVariable String productId) {
        return supermarketProductRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    public ResponseEntity<List<SupermarketProduct>> searchProducts(
            @RequestParam String displayName,
            @RequestParam(defaultValue = "1") int limit) {
        List<SupermarketProduct> products = supermarketProductRepository.findByMatchingDisplayNameWithImage(displayName);
        return ResponseEntity.ok(products.stream().limit(limit).toList());
    }

    @GetMapping("/products/{productId}/alternatives")
    public ResponseEntity<List<SupermarketProduct>> getAlternatives(
            @PathVariable String productId,
            @RequestParam(defaultValue = "") String filter) {
        return supermarketProductService.getAlternatives(productId, filter)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
