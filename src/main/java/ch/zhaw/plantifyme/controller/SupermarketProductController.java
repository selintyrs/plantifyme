package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SupermarketProductController {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @GetMapping("/products/{productId}")
    public ResponseEntity<SupermarketProduct> getProductById(@PathVariable String productId) {
        return supermarketProductRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
