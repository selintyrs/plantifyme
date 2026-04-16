package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "diyRecipes")
@Getter
@NoArgsConstructor
public class DiyRecipe {
    @Id
    private String id;
    private String title;
    private String description;
    private Integer prepTimeMin;
    private String difficulty;
    private List<MatchingKey> matchingKeys;
    private List<DiyIngredient> ingredients;
    private List<DiyStep> instructions;
}
