package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateListFromRecipeRequest {
    private String recipeTitle;
    private String supermarket;
    private List<RecipeIngredientItem> ingredients;
}
