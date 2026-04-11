package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlantifyMatchController {

    @Autowired
    private PlantifyMatchService plantifyMatchService;

    @PostMapping("/plantify-match")
    public ResponseEntity<PlantifyMatchResponse> match(@RequestBody PlantifyMatchRequest request) {
        if (request.getSupermarket() == null || request.getSupermarket().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(plantifyMatchService.match(request));
    }
}
