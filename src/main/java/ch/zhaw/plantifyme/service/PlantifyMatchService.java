package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PlantifyMatchService {

    @Autowired
    private SupermarketProductRepository supermarketProductRepository;

    @Autowired
    private GenericFoodRepository genericFoodRepository;

    @Autowired
    private DiyRecipeRepository diyRecipeRepository;

    public PlantifyMatchResponse match(PlantifyMatchRequest request) {
        List<MatchResult> results = new ArrayList<>();

        for (MatchRequestItem item : request.getItems()) {
            MatchResult result = new MatchResult();
            result.setItemId(item.getItemId());
            result.setOriginalName(item.getDisplayName());
            result.setType(item.getType());

            if ("C".equals(item.getType())) {
                // Stufe 1: Supermarkt-Produkt mit gepflegtem matching_key (subcategory-agnostisch).
                // Type-C-Items haben strukturell nur subcategory-spezifische Einträge
                // (z.B. Reis → Basmati/Risotto), daher broader-Match ohne subcategory-Filter.
                List<SupermarketProduct> typeCsupermarket = supermarketProductRepository
                        .findByMatchingDisplayNameAndSupermarket(item.getDisplayName(), request.getSupermarket());
                typeCsupermarket = filterAllergens(typeCsupermarket, request.getPreferences());
                if (!typeCsupermarket.isEmpty()) {
                    result.setSuggestedProduct(typeCsupermarket.get(0));
                    result.setMatchLevel("specific");
                } else {
                    // Stufe 2: GenericFood per frontendDictionaryId (Original-Pfad)
                    Optional<GenericFood> gfById = genericFoodRepository
                            .findByFrontendDictionaryId(item.getFrontendDictionaryId());
                    if (gfById.isPresent()) {
                        result.setGenericFood(gfById.get());
                    } else {
                        // Stufe 3: GenericFood per Name als Daten-Inkonsistenz-Fallback
                        List<GenericFood> gfByName = genericFoodRepository.findByName(item.getDisplayName());
                        if (!gfByName.isEmpty()) result.setGenericFood(gfByName.get(0));
                    }
                }
            } else {
                String subcategory = item.getSubcategory() != null ? item.getSubcategory() : "";

                // L5/specific: displayName + subcategory exact
                List<SupermarketProduct> candidates = supermarketProductRepository
                        .findByMatchingKeyAndSupermarket(item.getDisplayName(), subcategory, request.getSupermarket());
                candidates = filterAllergens(candidates, request.getPreferences());
                candidates = sortByPreference(candidates, request.getPreferences());
                String matchLevel = candidates.isEmpty() ? null : "specific";

                // Broader fallback: drop subcategory if specific search was empty and a subcategory was requested
                if (candidates.isEmpty() && !subcategory.isEmpty()) {
                    candidates = supermarketProductRepository
                            .findByMatchingKeyAndSupermarket(item.getDisplayName(), "", request.getSupermarket());
                    candidates = filterAllergens(candidates, request.getPreferences());
                    candidates = sortByPreference(candidates, request.getPreferences());
                    if (!candidates.isEmpty()) matchLevel = "broader";
                }

                if (!candidates.isEmpty()) {
                    result.setSuggestedProduct(candidates.get(0));
                    result.setMatchLevel(matchLevel);
                } else {
                    List<DiyRecipe> matchingDiys = diyRecipeRepository.findByMatchingDisplayName(item.getDisplayName());
                    if (!matchingDiys.isEmpty()) {
                        DiyRecipe diy = matchingDiys.get(0);
                        result.setFallbackDiyRecipeId(diy.getId());
                        result.setFallbackDiyRecipeTitle(diy.getTitle());
                    } else {
                        // No supermarket match and no DIY: try generic food as last resort.
                        // Type stays original — match status is expressed via genericFood field.
                        List<GenericFood> nameMatches = genericFoodRepository.findByName(item.getDisplayName());
                        if (!nameMatches.isEmpty()) result.setGenericFood(nameMatches.get(0));

                        // Variant-Fallback für Tiefkühlobst-Familie:
                        // Wenn Item Tiefkühlobst ist und kein direkter GenericFood-Treffer,
                        // mappe Subcategory (z.B. "Heidelbeeren") oder Default "Erdbeeren"
                        // auf den passenden Frisch-Obst-GenericFood.
                        if (result.getGenericFood() == null && "Tiefkühlobst".equals(item.getDisplayName())) {
                            String variant = subcategory.isBlank() ? "Erdbeeren" : subcategory;
                            List<GenericFood> variantMatches = genericFoodRepository.findByName(variant);
                            if (!variantMatches.isEmpty()) {
                                result.setGenericFood(variantMatches.get(0));
                                result.setVariantNote("Tiefkühl-" + variant);
                            }
                        }
                    }
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
