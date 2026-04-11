package ch.zhaw.plantifyme.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "shoppinglists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingList {

    @Id
    private String id;

    private String userId;
    private String name;
    private String supermarket;
    private LocalDateTime createdAt;
    private List<ShoppingItem> items;
}
