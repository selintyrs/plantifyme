package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.DiyIngredient;
import ch.zhaw.plantifyme.model.DiyRecipe;
import ch.zhaw.plantifyme.model.DiyRecipeDto;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.NutritionDto;
import ch.zhaw.plantifyme.repository.DiyRecipeRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.util.NutritionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiyRecipeService {

    @NonNull
    private final DiyRecipeRepository diyRecipeRepository;

    @NonNull
    private final GenericFoodRepository genericFoodRepository;

    @NonNull
    private final SupermarketProductRepository supermarketProductRepository;

    public List<DiyRecipeDto> findAll() {
        return diyRecipeRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<DiyRecipeDto> findByMatching(String displayName, String subcategory) {
        String sub = (subcategory == null || subcategory.isBlank()) ? "" : subcategory;
        return diyRecipeRepository.findByDisplayNameAndSubcategory(displayName, sub)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public DiyRecipeDto findById(String id) {
        return diyRecipeRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DIY recipe not found: " + id));
    }

    private DiyRecipeDto toDto(DiyRecipe recipe) {
        DiyRecipeDto dto = new DiyRecipeDto();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setPrepTimeMin(recipe.getPrepTimeMin());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setUseCase(recipe.getUseCase());
        dto.setMatchingKeys(recipe.getMatchingKeys());
        dto.setIngredients(recipe.getIngredients());
        dto.setInstructions(recipe.getInstructions());
        dto.setNutritionPer100g(calculateNutritionPer100g(recipe));
        return dto;
    }

    private NutritionDto calculateNutritionPer100g(DiyRecipe recipe) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return null;
        }

        double totalKcal = 0, totalFat = 0, totalSatFat = 0;
        double totalCarbs = 0, totalSugar = 0, totalProtein = 0, totalSalt = 0;
        double totalGrams = 0;
        boolean anyContribution = false;

        for (DiyIngredient ing : recipe.getIngredients()) {
            Double grams = NutritionUtil.toGrams(ing.getAmount(), ing.getUnit());
            if (grams == null || grams <= 0) continue;

            Double kcalPer100 = null;
            Double fatPer100 = null;
            Double satFatPer100 = null;
            Double carbsPer100 = null;
            Double sugarPer100 = null;
            Double proteinPer100 = null;
            Double saltPer100 = null;

            List<GenericFood> foods = genericFoodRepository.findByName(ing.getName());
            if (!foods.isEmpty()) {
                GenericFood gf = foods.get(0);
                kcalPer100 = gf.getKcalPer100g();
                fatPer100 = gf.getFatPer100g();
                satFatPer100 = gf.getSaturatedFatPer100g();
                carbsPer100 = gf.getCarbsPer100g();
                sugarPer100 = gf.getSugarPer100g();
                proteinPer100 = gf.getProteinPer100g();
                saltPer100 = gf.getSaltPer100g();
            }

            if (kcalPer100 == null && ing.getMatchingDisplayName() != null) {
                String matchingDn = ing.getMatchingDisplayName();
                String matchingSub = ing.getMatchingSubcategory() != null ? ing.getMatchingSubcategory() : "";
                List<SupermarketProduct> products = supermarketProductRepository
                        .findByMatchingKey(matchingDn, matchingSub);
                if (!products.isEmpty()) {
                    SupermarketProduct p = products.get(0);
                    kcalPer100 = p.getEnergyKcal();
                    fatPer100 = p.getFatG();
                    satFatPer100 = p.getSaturatedFatG();
                    carbsPer100 = p.getCarbsG();
                    sugarPer100 = p.getSugarG();
                    proteinPer100 = p.getProteinG();
                    saltPer100 = p.getSaltG();
                }
            }

            if (kcalPer100 == null) continue;

            double factor = grams / 100.0;
            totalKcal += kcalPer100 * factor;
            totalFat += (fatPer100 != null ? fatPer100 : 0) * factor;
            totalSatFat += (satFatPer100 != null ? satFatPer100 : 0) * factor;
            totalCarbs += (carbsPer100 != null ? carbsPer100 : 0) * factor;
            totalSugar += (sugarPer100 != null ? sugarPer100 : 0) * factor;
            totalProtein += (proteinPer100 != null ? proteinPer100 : 0) * factor;
            totalSalt += (saltPer100 != null ? saltPer100 : 0) * factor;
            totalGrams += grams;
            anyContribution = true;
        }

        if (!anyContribution || totalGrams <= 0) return null;

        double scale = 100.0 / totalGrams;

        return new NutritionDto(
                round1(totalKcal * scale),
                round1(totalFat * scale),
                round1(totalSatFat * scale),
                round1(totalCarbs * scale),
                round1(totalSugar * scale),
                round1(totalProtein * scale),
                round2(totalSalt * scale)
        );
    }

    private Double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
