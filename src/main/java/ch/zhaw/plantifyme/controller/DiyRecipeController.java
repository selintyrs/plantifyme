package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.DiyRecipeDto;
import ch.zhaw.plantifyme.service.DiyRecipeService;
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
@RequestMapping("/api/diy-recipes")
@RequiredArgsConstructor
public class DiyRecipeController {

    @NonNull
    private final DiyRecipeService diyRecipeService;

    @GetMapping
    public ResponseEntity<List<DiyRecipeDto>> findByMatching(
            @RequestParam String displayName,
            @RequestParam(defaultValue = "") String subcategory) {
        return ResponseEntity.ok(diyRecipeService.findByMatching(displayName, subcategory));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiyRecipeDto> findById(@PathVariable String id) {
        return ResponseEntity.ok(diyRecipeService.findById(id));
    }
}
