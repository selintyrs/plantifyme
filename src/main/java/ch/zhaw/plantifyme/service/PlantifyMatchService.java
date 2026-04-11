package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PlantifyMatchService {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    public PlantifyMatchResponse match(PlantifyMatchRequest request) {
        List<MatchResult> results = new ArrayList<>();

        for (MatchRequestItem item : request.getItems()) {
            MatchResult result = new MatchResult();
            result.setItemId(item.getItemId());
            result.setOriginalName(item.getDisplayName());
            result.setType(item.getType());

            if ("C".equals(item.getType())) {
                genericFoodRepository.findByFrontendDictionaryId(item.getFrontendDictionaryId())
                        .ifPresent(result::setGenericFood);
            } else {
                String subcategory = item.getSubcategory() != null ? item.getSubcategory() : "";
                List<SupermarketProduct> candidates = supermarketProductRepository
                        .findByMatchingKeyAndSupermarket(item.getDisplayName(), subcategory, request.getSupermarket());

                candidates = filterAllergens(candidates, request.getPreferences());
                candidates = sortByPreference(candidates, request.getPreferences());

                if (!candidates.isEmpty()) {
                    result.setSuggestedProduct(candidates.get(0));
                }
            }

            results.add(result);
        }

        PlantifyMatchResponse response = new PlantifyMatchResponse();
        response.setMatches(results);
        return response;
    }

    private List<SupermarketProduct> filterAllergens(List<SupermarketProduct> candidates, MatchPreferences preferences) {
        if (preferences == null || preferences.getAllergies() == null || preferences.getAllergies().isEmpty()) {
            return candidates;
        }
        return candidates.stream()
                .filter(p -> preferences.getAllergies().stream().noneMatch(a -> containsAllergen(p, a)))
                .toList();
    }

    private boolean containsAllergen(SupermarketProduct product, String allergen) {
        String term = allergen.toLowerCase();

        if (product.getAllergensContains() != null
                && product.getAllergensContains().toLowerCase().contains(term)) {
            return true;
        }

        if (product.getIngredients() != null) {
            return product.getIngredients().stream()
                    .anyMatch(ingredient -> ingredient.toLowerCase().contains(term));
        }

        return false;
    }

    private List<SupermarketProduct> sortByPreference(List<SupermarketProduct> candidates, MatchPreferences preferences) {
        if (preferences != null && preferences.getDiet() != null
                && preferences.getDiet().stream().anyMatch(d -> d.equalsIgnoreCase("high protein"))) {
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(
                            p -> p.getProteinG() != null ? -p.getProteinG() : 0.0))
                    .toList();
        }
        // default and budget=low: cheapest first
        return candidates.stream()
                .sorted(Comparator.comparingDouble(
                        p -> p.getPrice() != null ? p.getPrice() : Double.MAX_VALUE))
                .toList();
    }
}
