package ch.zhaw.plantifyme.coverage;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

/**
 * End-to-End-Verifikation für Fix 4 (Type-C Cross-Fallback nach supermarketProducts).
 * Triggert: $env:RUN_FIX4_VERIFY = "true"; .\mvnw.cmd test -Dtest=Fix4TypeCCrossFallback
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX4_VERIFY", matches = "true")
class Fix4TypeCCrossFallback {

    private static final List<String> ITEMS = List.of("Öl", "Süssungsmittel", "Reis");
    private static final List<String> SUPERMARKETS = List.of("Migros", "coop", "aldi");

    @Autowired private PlantifyMatchService matchService;
    @Autowired private FrontendDictionaryRepository fdRepo;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void verify() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  FIX 4 VERIFIKATION — Type-C Cross-Fallback\n");
        out.append("==================================================\n");

        for (String displayName : ITEMS) {
            out.append("\n### ").append(displayName).append(" ###\n");

            FrontendDictionary fd = fdRepo.findAll().stream()
                    .filter(e -> displayName.equals(e.getDisplayName()))
                    .findFirst().orElse(null);

            if (fd == null) {
                out.append("  ✗ FrontendDictionary-Eintrag nicht gefunden — übersprungen\n");
                continue;
            }

            for (String sm : SUPERMARKETS) {
                MatchResult r = simulate(fd, sm);
                out.append("  ").append(sm).append(": ").append(describe(r)).append("\n");
            }
        }

        System.out.println(out);
    }

    private MatchResult simulate(FrontendDictionary fd, String supermarket) {
        MatchRequestItem ri = new MatchRequestItem();
        ri.setItemId(fd.getId());
        ri.setDisplayName(fd.getDisplayName());
        ri.setSubcategory("");
        ri.setType(fd.getType());
        ri.setFrontendDictionaryId(fd.getId());

        PlantifyMatchRequest req = new PlantifyMatchRequest();
        req.setSupermarket(supermarket);
        req.setItems(List.of(ri));
        MatchPreferences p = new MatchPreferences();
        p.setBudget("medium");
        p.setAllergies(List.of());
        p.setDiet(List.of());
        req.setPreferences(p);

        PlantifyMatchResponse resp = matchService.match(req);
        return resp.getMatches().isEmpty() ? null : resp.getMatches().get(0);
    }

    private String describe(MatchResult r) {
        if (r == null) return "null";
        if (r.getSuggestedProduct() != null) {
            return "[Stufe 1] suggestedProduct=" + r.getSuggestedProduct().getName();
        }
        if (r.getGenericFood() != null) {
            return "[Stufe 2/3] genericFood=" + r.getGenericFood().getName();
        }
        return "EMPTY (kein Match)";
    }
}
