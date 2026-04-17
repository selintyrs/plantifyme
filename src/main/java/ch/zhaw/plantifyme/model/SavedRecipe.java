package ch.zhaw.plantifyme.model;

import ch.zhaw.plantifyme.model.dto.RecipePreferences;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "savedRecipes")
@Getter
@Setter
@NoArgsConstructor
public class SavedRecipe {
    @Id
    private String id;
    private String userId;
    private boolean isAiGenerated;
    private boolean isSaved;
    private String title;
    private String description;
    private String category;
    private Integer prepTimeMin;
    private String difficulty;
    private Integer servings;
    private String imageUrl;
    private List<RecipeIngredient> ingredients;
    private List<RecipeStep> instructions;
    private RecipeNutrition totalNutritionPerServing;
    private RecipePreferences preferences;
    private String baseRecipeId;
    private Date createdAt;
    private Date expiresAt;
}
