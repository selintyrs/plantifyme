package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GenericFoodController {

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    @GetMapping("/generic-foods/{foodId}")
    public ResponseEntity<GenericFood> getGenericFoodById(@PathVariable String foodId) {
        return genericFoodRepository.findById(foodId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
