package ch.zhaw.plantifyme.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "savedProducts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedProduct {

    @Id
    private String id;

    private String userId;
    private String productId;
    private String type;
    private String name;
    private String brand;
    private Double price;
    private String supermarket;
    private String imageUrl;
    private LocalDateTime savedAt;
}
