package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.service.ConflictException;
import ch.zhaw.plantifyme.service.OwnershipException;
import ch.zhaw.plantifyme.service.SavedProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // TODO: Replace userId query param with Auth0 JWT in Iteration 3
    @GetMapping("/saved-products")
    public ResponseEntity<List<SavedProduct>> getSavedProducts(
            @RequestParam String userId) {
        return ResponseEntity.ok(savedProductService.getByUser(userId));
    }

    // TODO: Replace X-User-Id header with Auth0 JWT token extraction in Iteration 3
    @DeleteMapping("/saved-products/{id}")
    public ResponseEntity<Void> unsaveProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        try {
            boolean found = savedProductService.unsaveProduct(id, userId);
            return found ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
