package ch.zhaw.plantifyme.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NutritionDto {
    private Double kcal;
    private Double fat;
    private Double saturatedFat;
    private Double carbs;
    private Double sugar;
    private Double protein;
    private Double salt;
}
