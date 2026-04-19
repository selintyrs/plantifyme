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

        List<GenericFood> exactResults = genericFoodRepository.findByName(ingredientName);
        if (!exactResults.isEmpty()) return Optional.of(exactResults.get(0));

        String cleaned = ingredientName
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("(?i)^frische[sr]?\\s+", "")
                .replaceAll("(?i)^schwarze[sr]?\\s+", "")
                .replaceAll("(?i)zehen$", "")
                .replaceAll("(?i)saft$", "")
                .trim();

        if (!cleaned.equals(ingredientName)) {
            List<GenericFood> cleanedResults = genericFoodRepository.findByName(cleaned);
            if (!cleanedResults.isEmpty()) return Optional.of(cleanedResults.get(0));
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
