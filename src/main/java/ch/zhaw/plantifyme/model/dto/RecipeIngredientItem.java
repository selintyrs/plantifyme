package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecipeIngredientItem {
    private String name;
    private Double amount;
    private String unit;
    private String type;
    private String matchingDisplayName;
    private String matchingSubcategory;
}
