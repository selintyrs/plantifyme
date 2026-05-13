package ch.zhaw.plantifyme.coverage;

import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
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
 * End-to-End-Verifikation für Fix 3 (Tiefkühlobst-Variant-Fallback).
 * Triggert: $env:RUN_FIX3_VERIFY = "true"; .\mvnw.cmd test -Dtest=Fix3TiefkuehlobstVerification
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_FIX3_VERIFY", matches = "true")
class Fix3TiefkuehlobstVerification {

    private static final String TIEFKUEHLOBST_ID = "ef5ac9ca-9864-406e-8073-7ab809ef246d";
    private static final List<String> VARIANTS = List.of(
            "Erdbeeren", "Heidelbeeren", "Himbeeren", "Mango", "Ananas"
    );
    private static final List<String> SUPERMARKETS = List.of("Migros", "coop", "aldi");

    @Autowired private PlantifyMatchService matchService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void verify() {
        StringBuilder out = new StringBuilder("\n==================================================\n");
        out.append("  FIX 3 VERIFIKATION — Tiefkühlobst-Variant-Fallback\n");
        out.append("==================================================\n");

        // ─── Default-Case (kein Long-Press, subcategory leer) ────────────
        out.append("\n  [Default] subcategory=\"\" (Click ohne Long-Press)\n");
        for (String sm : SUPERMARKETS) {
            MatchResult r = simulate("", sm);
            out.append("    ").append(sm).append(": ").append(describe(r)).append("\n");
        }

        // ─── Per-Variante (Long-Press) ────────────────────────────────────
        for (String variant : VARIANTS) {
            out.append("\n  [Long-Press] subcategory=\"").append(variant).append("\"\n");
            for (String sm : SUPERMARKETS) {
                MatchResult r = simulate(variant, sm);
                out.append("    ").append(sm).append(": ").append(describe(r)).append("\n");
            }
        }

        // ─── Negativ-Test: nicht-existierende Variant ────────────────────
        out.append("\n  [Negativ] subcategory=\"Brombeeren\" (nicht in genericFoods)\n");
        for (String sm : SUPERMARKETS) {
            MatchResult r = simulate("Brombeeren", sm);
            out.append("    ").append(sm).append(": ").append(describe(r)).append("\n");
        }

        System.out.println(out);
    }

    private MatchResult simulate(String subcategory, String supermarket) {
        MatchRequestItem ri = new MatchRequestItem();
        ri.setItemId(TIEFKUEHLOBST_ID);
        ri.setDisplayName("Tiefkühlobst");
        ri.setSubcategory(subcategory);
        ri.setType("B");
        ri.setFrontendDictionaryId(TIEFKUEHLOBST_ID);

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
        StringBuilder sb = new StringBuilder();
        if (r.getSuggestedProduct() != null) sb.append("suggestedProduct=").append(r.getSuggestedProduct().getName());
        else if (r.getGenericFood() != null) sb.append("genericFood=").append(r.getGenericFood().getName());
        else if (r.getFallbackDiyRecipeId() != null) sb.append("DIY=").append(r.getFallbackDiyRecipeTitle());
        else sb.append("EMPTY");
        if (r.getVariantNote() != null) sb.append(", variantNote=\"").append(r.getVariantNote()).append("\"");
        return sb.toString();
    }
}
