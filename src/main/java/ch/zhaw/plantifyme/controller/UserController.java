package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.UserUpdateRequest;
import ch.zhaw.plantifyme.service.UserService;
import ch.zhaw.plantifyme.util.AuthUtil;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @NonNull
    private final UserService userService;

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

    @PatchMapping("/me")
    public ResponseEntity<User> updateMe(@RequestBody UserUpdateRequest update) {
        String userId = AuthUtil.requireCurrentUserId();
        User updated = userService.updateUser(userId, update);
        return ResponseEntity.ok(updated);
    }
}
