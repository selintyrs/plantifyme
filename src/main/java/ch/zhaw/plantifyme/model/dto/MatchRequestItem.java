package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchRequestItem {
    private String itemId;
    private String displayName;
    private String subcategory; // can be empty
    private String type;        // A, B, or C
    private String frontendDictionaryId; // only relevant for type C
}
