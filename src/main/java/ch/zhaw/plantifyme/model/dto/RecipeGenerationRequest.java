package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RecipeGenerationRequest {
    private String title;
    private String description;
    private List<String> customIngredients;
    private List<String> excludedIngredients;
    private RecipePreferences preferences;
    private String baseRecipeId;
}
