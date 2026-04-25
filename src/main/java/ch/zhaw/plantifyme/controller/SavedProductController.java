package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.SavedProduct;
import ch.zhaw.plantifyme.model.dto.SaveProductRequest;
import ch.zhaw.plantifyme.service.ConflictException;
import ch.zhaw.plantifyme.service.OwnershipException;
import ch.zhaw.plantifyme.service.SavedProductService;
import ch.zhaw.plantifyme.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SavedProductController {

    @Autowired
    private SavedProductService savedProductService;

    @PostMapping("/saved-products")
    public ResponseEntity<SavedProduct> saveProduct(@RequestBody SaveProductRequest request) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            return savedProductService.saveProduct(userId, request)
                    .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                    .orElse(ResponseEntity.notFound().build());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/saved-products")
    public ResponseEntity<List<SavedProduct>> getSavedProducts() {
        String userId = AuthUtil.requireCurrentUserId();
        return ResponseEntity.ok(savedProductService.getByUser(userId));
    }

    @DeleteMapping("/saved-products/{id}")
    public ResponseEntity<Void> unsaveProduct(@PathVariable String id) {
        String userId = AuthUtil.requireCurrentUserId();
        try {
            boolean found = savedProductService.unsaveProduct(id, userId);
            return found ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (OwnershipException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
