package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.ImpactTotalDto;
import ch.zhaw.plantifyme.service.ImpactService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
public class ImpactController {

    @NonNull
    private final ImpactService impactService;

    @GetMapping("/total")
    public ResponseEntity<ImpactTotalDto> getTotalImpact(@RequestParam String userId) {
        return ResponseEntity.ok(impactService.computeTotal(userId));
    }
}
