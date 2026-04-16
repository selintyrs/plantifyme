package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddItemRequest {
    private String displayName;
    private String subcategory = "";
    private String type;
    private String frontendDictionaryId;
    private Integer quantity;
}
