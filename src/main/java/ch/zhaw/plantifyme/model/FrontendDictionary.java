package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "frontendDictionary")
@Getter
@NoArgsConstructor
public class FrontendDictionary {

    @Id
    private String id;

    private String displayName;
    private String uiCategory;
    private String type; // A, B, or C

    // null for type C
    private Map<String, String> matchingKeys;

    private List<AdditionalOption> additionalOptions;
}
