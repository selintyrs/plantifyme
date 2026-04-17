package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecipePreferences {
    private Integer prepTimeMax;
    private String difficulty;
    private boolean highProtein;
    private boolean lowCarb;
    private boolean lowFat;
    private boolean wholeFood;
    private boolean sugarFree;
    private boolean budgetFriendly;
    private boolean simpleIngredients;
    private boolean glutenFree;
    private boolean soyFree;
    private boolean nutFree;
    private String allergies;
    private String additionalNotes;
}
