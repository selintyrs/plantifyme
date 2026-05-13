package ch.zhaw.plantifyme.model.dto;

import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchResult {
    private String itemId;
    private String originalName;
    private String type;
    private SupermarketProduct suggestedProduct; // null for type C
    private GenericFood genericFood;             // null for type A/B
    private String fallbackDiyRecipeId;
    private String fallbackDiyRecipeTitle;
    private String matchLevel; // "specific" | "broader" | null (no match or non-supermarket result)
    private String variantNote; // optional Hinweis (z.B. "Tiefkühl-Erdbeeren") wenn Match über Variant-Fallback erfolgt
}
