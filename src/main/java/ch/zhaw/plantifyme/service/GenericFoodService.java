package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenericFoodService {

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    public Optional<GenericFood> findFlexible(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) return Optional.empty();

        Optional<GenericFood> exact = genericFoodRepository.findByName(ingredientName);
        if (exact.isPresent()) return exact;

        String cleaned = ingredientName
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("(?i)^frische[sr]?\\s+", "")
                .replaceAll("(?i)^schwarze[sr]?\\s+", "")
                .replaceAll("(?i)zehen$", "")
                .replaceAll("(?i)saft$", "")
                .trim();

        if (!cleaned.equals(ingredientName)) {
            Optional<GenericFood> cleanedMatch = genericFoodRepository.findByName(cleaned);
            if (cleanedMatch.isPresent()) return cleanedMatch;
        }

        List<GenericFood> regexResults = genericFoodRepository.findByNameContainingIgnoreCase(cleaned);
        if (!regexResults.isEmpty()) return Optional.of(regexResults.get(0));

        String[] words = cleaned.split("\\s+");
        for (String word : words) {
            if (word.length() < 4) continue;
            regexResults = genericFoodRepository.findByNameContainingIgnoreCase(word);
            if (!regexResults.isEmpty()) return Optional.of(regexResults.get(0));
        }

        return Optional.empty();
    }
}
