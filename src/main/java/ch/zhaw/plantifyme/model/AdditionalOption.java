package ch.zhaw.plantifyme.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class AdditionalOption {
    private String optionName;
    private Map<String, String> additionalKeys;
}
