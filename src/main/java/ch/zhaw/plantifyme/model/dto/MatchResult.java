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
}
