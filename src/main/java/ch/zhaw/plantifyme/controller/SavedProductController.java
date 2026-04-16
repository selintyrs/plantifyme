package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.service.ConflictException;
import ch.zhaw.plantifyme.service.SavedProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SavedProductController {

    @Autowired
    private SavedProductService savedProductService;

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @PostMapping("/saved-products")
    public ResponseEntity<SavedProduct> saveProduct(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SaveProductRequest request) {
        try {
            return savedProductService.saveProduct(userId, request)
                    .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                    .orElse(ResponseEntity.notFound().build());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
