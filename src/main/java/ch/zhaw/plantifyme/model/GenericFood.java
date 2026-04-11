package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "genericFoods")
@Getter
@NoArgsConstructor
public class GenericFood {

    @Id
    private String id;

    private String name;
    private String category;
    private String type; // always "C"

    @Field("imageUrl")
    private String imageUrl;

    @Field("matching_key")
    private String matchingKey;

    private Nutrition nutrition;

    @Getter
    @NoArgsConstructor
    public static class Nutrition {
        private Double kcal;
        private Double fat;
        private Double carbs;
        private Double protein;
        private Double salt;
    }
}
