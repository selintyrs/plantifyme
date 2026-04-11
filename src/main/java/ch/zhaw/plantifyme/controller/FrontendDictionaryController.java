package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FrontendDictionaryController {

    @Autowired
    private FrontendDictionaryRepository frontendDictionaryRepository;

    @GetMapping("/frontend-dictionary")
    public ResponseEntity<List<FrontendDictionary>> getAllEntries() {
        return ResponseEntity.ok(frontendDictionaryRepository.findAll());
    }
}
