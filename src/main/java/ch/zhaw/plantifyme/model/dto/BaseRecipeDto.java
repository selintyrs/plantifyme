package ch.zhaw.plantifyme.model.dto;

import ch.zhaw.plantifyme.model.RecipeIngredient;
import ch.zhaw.plantifyme.model.RecipeNutrition;
import ch.zhaw.plantifyme.model.RecipeStep;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BaseRecipeDto {
    private String id;
    private String title;
    private String description;
    private String category;
    private Integer prepTimeMin;
    private String difficulty;
    private Integer defaultServings;
    private String imageUrl;
    private List<RecipeIngredient> ingredients;
    private List<RecipeStep> instructions;
    private RecipeNutrition totalNutritionPerServing;
}
