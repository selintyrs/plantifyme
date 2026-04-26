package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.UserUpdateRequest;
import ch.zhaw.plantifyme.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    @NonNull
    private final UserRepository userRepository;

    public User getOrCreateUser(String userId, String email, String firstName) {
        return userRepository.findById(userId).orElseGet(() -> {
            User newUser = User.builder()
                    .id(userId)
                    .email(email)
                    .firstName(firstName)
                    .createdAt(LocalDateTime.now())
                    .onboardingCompleted(false)
                    .build();
            return userRepository.save(newUser);
        });
    }

    public User getUser(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User updateUser(String userId, UserUpdateRequest update) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (update.getFirstName() != null) user.setFirstName(update.getFirstName());
        if (update.getLastName() != null) user.setLastName(update.getLastName());
        if (update.getCurrentDiet() != null) user.setCurrentDiet(update.getCurrentDiet());
        if (update.getGoals() != null) user.setGoals(update.getGoals());
        if (update.getDietaryPreferences() != null) user.setDietaryPreferences(update.getDietaryPreferences());
        if (update.getAllergies() != null) user.setAllergies(update.getAllergies());
        if (update.getCustomAllergies() != null) user.setCustomAllergies(update.getCustomAllergies());
        if (update.getDefaultSupermarket() != null) user.setDefaultSupermarket(update.getDefaultSupermarket());

        if (Boolean.TRUE.equals(update.getOnboardingCompleted()) && !user.isOnboardingCompleted()) {
            user.setOnboardingCompleted(true);
            user.setOnboardingCompletedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }
}
