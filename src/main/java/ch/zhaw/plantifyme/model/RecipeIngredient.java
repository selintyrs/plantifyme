package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecipeIngredient {
    private String name;
    private Double amount;
    private String unit;
    private String type;
    private String matchingDisplayName;
    private String matchingSubcategory;
}
