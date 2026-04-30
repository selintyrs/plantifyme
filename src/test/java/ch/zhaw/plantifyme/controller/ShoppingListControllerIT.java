package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.config.SecurityConfig;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.service.ShoppingListService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShoppingListController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ShoppingListControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ShoppingListRepository shoppingListRepository;

    @MockitoBean
    private ShoppingListService shoppingListService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String OWNER = "user-1";
    private static final String OTHER = "user-2";
    private static final String LIST_ID = "list-1";

    private static ShoppingList list(String userId) {
        ShoppingList l = new ShoppingList();
        l.setId(LIST_ID);
        l.setUserId(userId);
        l.setName("Wocheneinkauf");
        l.setSupermarket("Migros");
        return l;
    }

    @Test
    @DisplayName("GET /api/shoppinglists: liefert nur eigene Listen")
    void getShoppingLists_success() throws Exception {
        when(shoppingListRepository.findByUserId(OWNER))
                .thenReturn(List.of(list(OWNER)));

        mockMvc.perform(get("/api/shoppinglists")
                        .with(jwt().jwt(j -> j.subject(OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(LIST_ID))
                .andExpect(jsonPath("$[0].userId").value(OWNER));

        verify(shoppingListRepository).findByUserId(OWNER);
    }

    @Test
    @DisplayName("GET /api/shoppinglists ohne Auth → 401")
    void getShoppingLists_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/shoppinglists"))
                .andExpect(status().isUnauthorized());

        verify(shoppingListRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("POST /api/shoppinglists: userId und createdAt werden serverseitig gesetzt → 201")
    void createList_success() throws Exception {
        // Request enthält fremde userId und null createdAt — beide sollen ignoriert/serverseitig gesetzt werden
        ShoppingList request = new ShoppingList();
        request.setName("Neue Liste");
        request.setSupermarket("Coop");
        request.setUserId("hacker-attempt");

        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenAnswer(inv -> {
                    ShoppingList arg = inv.getArgument(0);
                    arg.setId(LIST_ID);
                    return arg;
                });

        mockMvc.perform(post("/api/shoppinglists")
                        .with(jwt().jwt(j -> j.subject(OWNER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(LIST_ID))
                .andExpect(jsonPath("$.userId").value(OWNER))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("DELETE /api/shoppinglists/{id} eigene Liste → 200")
    void deleteOwnList_success() throws Exception {
        when(shoppingListRepository.findById(LIST_ID))
                .thenReturn(Optional.of(list(OWNER)));

        mockMvc.perform(delete("/api/shoppinglists/{id}", LIST_ID)
                        .with(jwt().jwt(j -> j.subject(OWNER)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(shoppingListRepository).deleteById(LIST_ID);
    }

    @Test
    @DisplayName("DELETE /api/shoppinglists/{id} fremde Liste → 403")
    void deleteForeignList_forbidden() throws Exception {
        when(shoppingListRepository.findById(LIST_ID))
                .thenReturn(Optional.of(list(OTHER)));

        mockMvc.perform(delete("/api/shoppinglists/{id}", LIST_ID)
                        .with(jwt().jwt(j -> j.subject(OWNER)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(shoppingListRepository, never()).deleteById(any());
    }
}
