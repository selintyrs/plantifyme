package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.BaseRecipeDto;
import ch.zhaw.plantifyme.service.BaseRecipeService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/base-recipes")
@RequiredArgsConstructor
public class BaseRecipeController {

    @NonNull
    private final BaseRecipeService baseRecipeService;

    @GetMapping
    public ResponseEntity<List<BaseRecipeDto>> getBaseRecipes(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(baseRecipeService.findByCategory(category));
        }
        return ResponseEntity.ok(baseRecipeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseRecipeDto> getBaseRecipeById(@PathVariable String id) {
        return ResponseEntity.ok(baseRecipeService.findById(id));
    }
}
