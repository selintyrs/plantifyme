package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ImpactTotalDto {
    private Map<String, Integer> animals;
    private Double co2TotalKg;
    private Double waterTotalL;
    private Double landTotalM2;
    private String co2Compare;
    private String waterCompare;
    private String landCompare;
}
