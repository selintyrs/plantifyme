package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.config.SecurityConfig;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlantifyMatchController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PlantifyMatchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PlantifyMatchService plantifyMatchService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static PlantifyMatchRequest validRequest(String supermarket) {
        PlantifyMatchRequest req = new PlantifyMatchRequest();
        req.setSupermarket(supermarket);
        MatchRequestItem item = new MatchRequestItem();
        item.setItemId("1");
        item.setDisplayName("Milch");
        item.setType("A");
        item.setSubcategory("");
        req.setItems(List.of(item));
        return req;
    }

    @Test
    @DisplayName("POST /api/plantify-match: erfolgreiches Match → 200 + matches im Body")
    void match_success() throws Exception {
        PlantifyMatchResponse response = new PlantifyMatchResponse();
        MatchResult result = new MatchResult();
        result.setItemId("1");
        result.setOriginalName("Milch");
        result.setType("A");
        response.setMatches(List.of(result));
        when(plantifyMatchService.match(any())).thenReturn(response);

        mockMvc.perform(post("/api/plantify-match")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Migros"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").isArray())
                .andExpect(jsonPath("$.matches[0].itemId").value("1"));

        verify(plantifyMatchService).match(any());
    }

    @Test
    @DisplayName("POST /api/plantify-match ohne Auth → 401")
    void match_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/plantify-match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Migros"))))
                .andExpect(status().isUnauthorized());

        verify(plantifyMatchService, never()).match(any());
    }

    @Test
    @DisplayName("POST mit supermarket=null → 400")
    void match_nullSupermarket() throws Exception {
        mockMvc.perform(post("/api/plantify-match")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(null))))
                .andExpect(status().isBadRequest());

        verify(plantifyMatchService, never()).match(any());
    }

    @Test
    @DisplayName("POST mit blank supermarket → 400")
    void match_blankSupermarket() throws Exception {
        mockMvc.perform(post("/api/plantify-match")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("   "))))
                .andExpect(status().isBadRequest());

        verify(plantifyMatchService, never()).match(any());
    }

    @Test
    @DisplayName("POST mit leerer Items-Liste → 200 mit leeren matches")
    void match_emptyItems() throws Exception {
        PlantifyMatchResponse empty = new PlantifyMatchResponse();
        empty.setMatches(List.of());
        when(plantifyMatchService.match(any())).thenReturn(empty);

        PlantifyMatchRequest req = new PlantifyMatchRequest();
        req.setSupermarket("Coop");
        req.setItems(List.of());

        mockMvc.perform(post("/api/plantify-match")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").isArray())
                .andExpect(jsonPath("$.matches.length()").value(0));
    }
}
