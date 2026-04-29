package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.UserUpdateRequest;
import ch.zhaw.plantifyme.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    private static final String USER_ID = "auth0|abc";

    private static User existingUser() {
        return User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .firstName("Selin")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .onboardingCompleted(false)
                .build();
    }

    // ── 1. getOrCreateUser ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreateUser")
    class GetOrCreateUser {

        @Test
        @DisplayName("User existiert → return existing, kein save")
        void existingUserReturned() {
            User existing = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

            User result = service.getOrCreateUser(USER_ID, "user@example.com", "Selin");

            assertThat(result).isSameAs(existing);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("User existiert nicht → save mit firstName, email, createdAt, onboardingCompleted=false")
        void newUserCreated() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = service.getOrCreateUser(USER_ID, "new@example.com", "Anna");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getId()).isEqualTo(USER_ID);
            assertThat(saved.getEmail()).isEqualTo("new@example.com");
            assertThat(saved.getFirstName()).isEqualTo("Anna");
            assertThat(saved.isOnboardingCompleted()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(result).isSameAs(saved);
        }
    }

    // ── 2. getUser ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("Unknown id → null")
        void unknownIdReturnsNull() {
            when(userRepository.findById("ghost")).thenReturn(Optional.empty());

            User result = service.getUser("ghost");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Bekannte id → User-Objekt")
        void knownIdReturnsUser() {
            User u = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThat(service.getUser(USER_ID)).isSameAs(u);
        }
    }

    // ── 3. updateUser ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("User nicht gefunden → ResponseStatusException 404")
        void notFoundThrows() {
            when(userRepository.findById("ghost")).thenReturn(Optional.empty());
            UserUpdateRequest req = new UserUpdateRequest();

            assertThatThrownBy(() -> service.updateUser("ghost", req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("firstName non-blank → wird gesetzt")
        void firstNameSetWhenNonBlank() {
            User u = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setFirstName("Neu");

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getFirstName()).isEqualTo("Neu");
        }

        @Test
        @DisplayName("firstName blank → NICHT überschrieben (Pflichtfeld, UAT-Fix)")
        void firstNameBlankNotOverwritten() {
            User u = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setFirstName("   ");

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getFirstName()).isEqualTo("Selin");
        }

        @Test
        @DisplayName("firstName empty string → NICHT überschrieben")
        void firstNameEmptyNotOverwritten() {
            User u = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setFirstName("");

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getFirstName()).isEqualTo("Selin");
        }

        @Test
        @DisplayName("lastName leerer String → WIRD überschrieben (optional)")
        void lastNameEmptyOverwritesValue() {
            User u = existingUser();
            u.setLastName("Soy");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setLastName("");

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getLastName()).isEmpty();
        }

        @Test
        @DisplayName("Allergies, defaultSupermarket, dietaryPreferences werden gesetzt")
        void multiFieldUpdate() {
            User u = existingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setAllergies(List.of("Soja", "Nüsse"));
            req.setDefaultSupermarket("Migros");
            req.setDietaryPreferences(List.of("vegan"));

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getAllergies()).containsExactly("Soja", "Nüsse");
            assertThat(result.getDefaultSupermarket()).isEqualTo("Migros");
            assertThat(result.getDietaryPreferences()).containsExactly("vegan");
        }

        @Test
        @DisplayName("onboardingCompleted=true bei vorher false → Timestamp wird gesetzt")
        void onboardingCompletedSetsTimestamp() {
            User u = existingUser(); // onboardingCompleted=false
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setOnboardingCompleted(true);

            LocalDateTime before = LocalDateTime.now();
            User result = service.updateUser(USER_ID, req);
            LocalDateTime after = LocalDateTime.now();

            assertThat(result.isOnboardingCompleted()).isTrue();
            assertThat(result.getOnboardingCompletedAt()).isNotNull();
            assertThat(result.getOnboardingCompletedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("onboardingCompleted=true bei bereits true → Timestamp NICHT überschrieben")
        void onboardingAlreadyTrueKeepsTimestamp() {
            User u = existingUser();
            u.setOnboardingCompleted(true);
            LocalDateTime original = LocalDateTime.of(2025, 6, 1, 10, 0);
            u.setOnboardingCompletedAt(original);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest();
            req.setOnboardingCompleted(true);

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getOnboardingCompletedAt()).isEqualTo(original);
        }

        @Test
        @DisplayName("Null Felder im Request → kein Update der entsprechenden User-Felder")
        void nullFieldsLeaveUserUntouched() {
            User u = existingUser();
            u.setLastName("Soy");
            u.setDefaultSupermarket("Coop");
            u.setAllergies(List.of("Soja"));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest(); // alles null

            User result = service.updateUser(USER_ID, req);

            assertThat(result.getFirstName()).isEqualTo("Selin");
            assertThat(result.getLastName()).isEqualTo("Soy");
            assertThat(result.getDefaultSupermarket()).isEqualTo("Coop");
            assertThat(result.getAllergies()).containsExactly("Soja");
        }
    }
}
