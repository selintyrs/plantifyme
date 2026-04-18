package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AddRecipeIngredientsRequest {
    private String recipeTitle;
    private List<RecipeIngredientItem> ingredients;
}
