package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "supermarketProducts")
@Getter
@NoArgsConstructor
public class SupermarketProduct {

    @Id
    private String id;

    // Identifikation
    private String supermarket;

    @Field("article_id")
    private String articleId;

    @Field("product_url")
    private String productUrl;

    @Field("image_url")
    private String imageUrl;

    // Produkt
    private String type; // A or B
    private String name;
    private String brand;
    private Double price;
    private Double amount;
    private Double qty;
    private String unit;

    @Field("price_per_unit")
    private Double pricePerUnit;

    @Field("price_per_unit_label")
    private String pricePerUnitLabel;

    // Flags
    @Field("is_bio")
    private Boolean isBio;

    @Field("is_gluten_free")
    private Boolean isGlutenFree;

    @Field("has_additives")
    private Boolean hasAdditives;

    // Inhalt
    private List<String> ingredients;

    @Field("allergens_contains")
    private String allergensContains;

    @Field("allergens_may_contain")
    private String allergensMayContain;

    // Nährwerte
    @Field("energy_kj")
    private Double energyKj;

    @Field("energy_kcal")
    private Double energyKcal;

    @Field("fat_g")
    private Double fatG;

    @Field("saturated_fat_g")
    private Double saturatedFatG;

    @Field("carbs_g")
    private Double carbsG;

    @Field("sugar_g")
    private Double sugarG;

    @Field("protein_g")
    private Double proteinG;

    @Field("salt_g")
    private Double saltG;

    // Kategorien
    @Field("plantify_cat_l1")
    private String plantifyCatL1;

    @Field("plantify_cat_l2")
    private String plantifyCatL2;

    @Field("plantify_cat_l3")
    private String plantifyCatL3;

    @Field("plantify_cat_l4")
    private String plantifyCatL4;

    @Field("plantify_cat_l5")
    private String plantifyCatL5;

    // Matching
    @Field("matching_keys")
    private List<MatchingKey> matchingKeys;

    // Impact (nur Typ A)
    @Field("impact_co2_original_kg")
    private Double impactCo2OriginalKg;

    @Field("impact_co2_saved_kg")
    private Double impactCo2SavedKg;

    @Field("impact_co2_saved_pct")
    private Double impactCo2SavedPct;

    @Field("impact_water_original_L")
    private Double impactWaterOriginalL;

    @Field("impact_water_saved_L")
    private Double impactWaterSavedL;

    @Field("impact_water_saved_pct")
    private Double impactWaterSavedPct;

    @Field("impact_land_original_m2")
    private Double impactLandOriginalM2;

    @Field("impact_land_saved_m2")
    private Double impactLandSavedM2;

    @Field("impact_land_saved_pct")
    private Double impactLandSavedPct;

    @Field("impact_method")
    private String impactMethod;

    // Tierwohl (nur Typ A)
    @Field("animal_type")
    private String animalType;

    @Field("animals_affected_per_unit")
    private Double animalsAffectedPerUnit;
}
