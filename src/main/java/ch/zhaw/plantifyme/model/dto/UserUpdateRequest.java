package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String currentDiet;
    private List<String> goals;
    private List<String> dietaryPreferences;
    private List<String> allergies;
    private String customAllergies;
    private String defaultSupermarket;
    private Boolean onboardingCompleted;
}
