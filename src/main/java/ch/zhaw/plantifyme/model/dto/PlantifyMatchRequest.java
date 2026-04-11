package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PlantifyMatchRequest {
    private List<MatchRequestItem> items;
    private String supermarket;
    private MatchPreferences preferences;
}
