package ch.zhaw.plantifyme.model;

import ch.zhaw.plantifyme.model.dto.NutritionDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DiyRecipeDto {
    private String id;
    private String title;
    private String description;
    private Integer prepTimeMin;
    private String difficulty;
    private String imageUrl;
    private String useCase;
    private List<MatchingKey> matchingKeys;
    private List<DiyIngredient> ingredients;
    private List<DiyStep> instructions;
    private NutritionDto nutritionPer100g;
}
