package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecipeNutrition {
    private Integer kcal;
    private Double fat;
    private Double saturatedFat;
    private Double carbs;
    private Double sugar;
    private Double protein;
    private Double salt;
}
