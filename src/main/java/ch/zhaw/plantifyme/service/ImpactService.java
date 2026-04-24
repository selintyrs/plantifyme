package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.ImpactTotalDto;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImpactService {

    @NonNull
    private final ShoppingListRepository shoppingListRepository;

    @NonNull
    private final SupermarketProductRepository supermarketProductRepository;

    public ImpactTotalDto computeTotal(String userId) {
        Map<String, Double> animals = new HashMap<>();
        animals.put("cow", 0.0);
        animals.put("pig", 0.0);
        animals.put("chicken", 0.0);
        animals.put("fish", 0.0);

        double totalCo2 = 0;
        double totalWater = 0;
        double totalLand = 0;

        List<ShoppingList> lists = shoppingListRepository.findByUserId(userId);
        for (ShoppingList list : lists) {
            if (list.getItems() == null) continue;
            for (ShoppingItem item : list.getItems()) {
                if (item.getProductId() == null || item.getProductId().isBlank()) continue;
                SupermarketProduct product = supermarketProductRepository.findById(item.getProductId()).orElse(null);
                if (product == null) continue;

                int qty = item.getQuantity() != null ? item.getQuantity() : 1;

                if (product.getAnimalType() != null && product.getAnimalsAffectedPerUnit() != null) {
                    String key = product.getAnimalType();
                    animals.merge(key, product.getAnimalsAffectedPerUnit() * qty, Double::sum);
                }
                if (product.getImpactCo2SavedKg() != null) {
                    totalCo2 += product.getImpactCo2SavedKg() * qty;
                }
                if (product.getImpactWaterSavedL() != null) {
                    totalWater += product.getImpactWaterSavedL() * qty;
                }
                if (product.getImpactLandSavedM2() != null) {
                    totalLand += product.getImpactLandSavedM2() * qty;
                }
            }
        }

        Map<String, Integer> animalsRounded = new HashMap<>();
        for (Map.Entry<String, Double> e : animals.entrySet()) {
            animalsRounded.put(e.getKey(), (int) Math.ceil(e.getValue()));
        }

        ImpactTotalDto dto = new ImpactTotalDto();
        dto.setAnimals(animalsRounded);
        dto.setCo2TotalKg(round(totalCo2, 1));
        dto.setWaterTotalL(round(totalWater, 1));
        dto.setLandTotalM2(round(totalLand, 1));
        dto.setCo2Compare(buildCo2Compare(totalCo2));
        dto.setWaterCompare(buildWaterCompare(totalWater));
        dto.setLandCompare(buildLandCompare(totalLand));
        return dto;
    }

    private String buildCo2Compare(double totalCo2) {
        if (totalCo2 > 20) {
            return String.format("Das entspricht %.1f km Flug", totalCo2 / 0.2630);
        }
        return String.format("Das entspricht %.1f km Autofahrt", totalCo2 / 0.1873);
    }

    private String buildWaterCompare(double totalWater) {
        double badewannen = totalWater / 200.0;
        if (totalWater >= 60 && round(badewannen, 1) > 0) {
            return String.format("Das entspricht %.1f vollen Badewannen", badewannen);
        }
        return String.format("Das entspricht %.1f Minuten duschen", totalWater / 12.0);
    }

    private String buildLandCompare(double totalLand) {
        if (totalLand > 500) {
            double fussball = totalLand / 7140.0;
            if (round(fussball, 1) > 0) {
                return String.format("Das entspricht %.1f Fussballfeldern", fussball);
            }
            return String.format("Das entspricht %.1f Tennisplätzen", totalLand / 195.7);
        }
        if (totalLand >= 25) {
            double tennis = totalLand / 195.7;
            if (round(tennis, 1) > 0) {
                return String.format("Das entspricht %.1f Tennisplätzen", tennis);
            }
            return String.format("Das entspricht %.1f Parkplätzen", totalLand / 12.5);
        }
        return String.format("Das entspricht %.1f Parkplätzen", totalLand / 12.5);
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
