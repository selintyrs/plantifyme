package ch.zhaw.plantifyme.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class AuthUtil {

    private AuthUtil() {}

    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    public static String requireCurrentUserId() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
