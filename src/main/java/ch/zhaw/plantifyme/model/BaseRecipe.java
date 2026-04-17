package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "baseRecipes")
@Getter
@NoArgsConstructor
public class BaseRecipe {
    @Id
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
