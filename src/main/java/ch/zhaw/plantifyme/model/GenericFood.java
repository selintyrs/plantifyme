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

    private String frontendDictionaryId;
    private String name;
    private String category;
    private String type; // always "C"
    private String imageUrl;
    private String imageCredit;

    @Field("kcal_per_100g")
    private Double kcalPer100g;

    @Field("fat_per_100g")
    private Double fatPer100g;

    @Field("saturated_fat_per_100g")
    private Double saturatedFatPer100g;

    @Field("carbs_per_100g")
    private Double carbsPer100g;

    @Field("sugar_per_100g")
    private Double sugarPer100g;

    @Field("protein_per_100g")
    private Double proteinPer100g;

    @Field("salt_per_100g")
    private Double saltPer100g;

    @Field("nutrition_source")
    private String nutritionSource;
}
