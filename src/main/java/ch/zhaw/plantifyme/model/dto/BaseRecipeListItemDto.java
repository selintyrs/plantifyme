package ch.zhaw.plantifyme.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseRecipeListItemDto {
    private String id;
    private String title;
    private String category;
    private String imageUrl;
    private Integer prepTimeMin;
    private String difficulty;
}
