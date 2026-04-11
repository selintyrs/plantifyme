package ch.zhaw.plantifyme.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingItem {
    private String itemId;
    private String productId;      // ref to supermarketProducts (type A/B)
    private String genericFoodId;  // ref to genericFoods (type C)
    private String type;           // A, B, or C
    private String name;
    private Double price;
    private Integer quantity;
    private Boolean isChecked;
}
