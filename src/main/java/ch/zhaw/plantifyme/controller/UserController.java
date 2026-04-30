package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.UserUpdateRequest;
import ch.zhaw.plantifyme.service.UserService;
import ch.zhaw.plantifyme.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Benutzer", description = "Verwaltung von User-Profilen und Onboarding")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @NonNull
    private final UserService userService;

    @Operation(
            summary = "Eigenes Profil abrufen",
            description = "Liefert das User-Profil des authentifizierten Users. " +
                    "Falls noch kein Profil existiert, wird es aus den Auth0-JWT-Claims (email, given_name) automatisch erstellt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil zurückgegeben (oder neu erstellt)"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert")
    })
    @GetMapping("/me")
    public ResponseEntity<User> getMe() {
        String userId = AuthUtil.requireCurrentUserId();

        String email = null;
        String firstName = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getClaimAsString("email");
            firstName = jwt.getClaimAsString("given_name");
            if (firstName == null) firstName = jwt.getClaimAsString("name");
        }

        User user = userService.getOrCreateUser(userId, email, firstName);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Eigenes Profil aktualisieren",
            description = "Aktualisiert nur die im Request gesetzten Felder. " +
                    "firstName wird bei leerem/blankem String NICHT überschrieben (Pflichtfeld). " +
                    "lastName erlaubt leeren String. Setzt onboardingCompletedAt automatisch beim ersten Abschluss."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil aktualisiert"),
            @ApiResponse(responseCode = "401", description = "Nicht authentifiziert"),
            @ApiResponse(responseCode = "404", description = "User nicht gefunden")
    })
    @PatchMapping("/me")
    public ResponseEntity<User> updateMe(@RequestBody UserUpdateRequest update) {
        String userId = AuthUtil.requireCurrentUserId();
        User updated = userService.updateUser(userId, update);
        return ResponseEntity.ok(updated);
    }
}
