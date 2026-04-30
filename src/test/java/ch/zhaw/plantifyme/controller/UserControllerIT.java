package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.config.SecurityConfig;
import ch.zhaw.plantifyme.model.User;
import ch.zhaw.plantifyme.model.dto.UserUpdateRequest;
import ch.zhaw.plantifyme.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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

    @Test
    @DisplayName("GET /api/users/me: User existiert → 200 + Body")
    void getMe_existingUser() throws Exception {
        when(userService.getOrCreateUser(eq(USER_ID), any(), any()))
                .thenReturn(existingUser());

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j
                                .subject(USER_ID)
                                .claim("email", "user@example.com")
                                .claim("given_name", "Selin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.firstName").value("Selin"));

        verify(userService).getOrCreateUser(eq(USER_ID), eq("user@example.com"), eq("Selin"));
    }

    @Test
    @DisplayName("GET /api/users/me: User existiert nicht → wird angelegt → 200")
    void getMe_createsIfMissing() throws Exception {
        User newlyCreated = existingUser();
        when(userService.getOrCreateUser(eq(USER_ID), any(), any()))
                .thenReturn(newlyCreated);

        // No email/given_name claims → service called with nulls
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).getOrCreateUser(eq(USER_ID), eq(null), eq(null));
    }

    @Test
    @DisplayName("GET /api/users/me ohne Auth → 401")
    void getMe_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getOrCreateUser(any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/users/me: firstName aktualisieren → 200")
    void updateMe_firstName() throws Exception {
        User updated = existingUser();
        updated.setFirstName("Anna");
        when(userService.updateUser(eq(USER_ID), any(UserUpdateRequest.class)))
                .thenReturn(updated);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Anna");

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));

        verify(userService).updateUser(eq(USER_ID), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/users/me ohne Auth → 401")
    void updateMe_unauthenticated() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Anna");

        mockMvc.perform(patch("/api/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUser(any(), any());
    }
}
