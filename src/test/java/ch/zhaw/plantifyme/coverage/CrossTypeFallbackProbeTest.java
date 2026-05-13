package ch.zhaw.plantifyme.coverage;

import ch.zhaw.plantifyme.model.FrontendDictionary;
import ch.zhaw.plantifyme.model.GenericFood;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.MatchPreferences;
import ch.zhaw.plantifyme.model.dto.MatchRequestItem;
import ch.zhaw.plantifyme.model.dto.MatchResult;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchRequest;
import ch.zhaw.plantifyme.model.dto.PlantifyMatchResponse;
import ch.zhaw.plantifyme.repository.FrontendDictionaryRepository;
import ch.zhaw.plantifyme.repository.GenericFoodRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import ch.zhaw.plantifyme.service.PlantifyMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

/**
 * One-off probe for BA chapter 8.2.2: verifies cross-type-fallback coverage
 * for 4 specific items (Tiefkühlobst, Öl, Süssungsmittel, Reis) across
 * Migros/coop/aldi + genericFoods.
 *
 * Triggers only when RUN_PROBE=true.
 * PowerShell: $env:RUN_PROBE = "true"; .\mvnw.cmd test -Dtest=CrossTypeFallbackProbeTest
 */
@SpringBootTest
@ActiveProfiles("coverage")
@EnabledIfEnvironmentVariable(named = "RUN_PROBE", matches = "true")
class CrossTypeFallbackProbeTest {

    private static final List<String> ITEMS = List.of("Tiefkühlobst", "Öl", "Süssungsmittel", "Reis");
    private static final List<String> SUPERMARKETS = List.of("Migros", "coop", "aldi");
    private static final List<String> FRESH_FRUIT_PROBES = List.of(
            "Erdbeeren", "Heidelbeeren", "Himbeeren", "Brombeeren",
            "Mango", "Ananas", "Pfirsiche", "Aprikosen", "Kirschen"
    );

    @Autowired private FrontendDictionaryRepository fdRepo;
    @Autowired private SupermarketProductRepository spRepo;
    @Autowired private GenericFoodRepository gfRepo;
    @Autowired private PlantifyMatchService matchService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void probe() {
        StringBuilder out = new StringBuilder();
        out.append("\n==================================================\n");
        out.append("  CROSS-TYPE-FALLBACK-PROBE\n");
        out.append("==================================================\n");

        // ── Per-Item probe ────────────────────────────────────────
        for (String displayName : ITEMS) {
            out.append("\n### ").append(displayName).append(" ###\n");

            // 1. FrontendDictionary entry
            List<FrontendDictionary> fdMatches = fdRepo.findAll().stream()
                    .filter(e -> displayName.equals(e.getDisplayName()))
                    .toList();
            out.append("\n  [1] FrontendDictionary findByDisplayName: ").append(fdMatches.size()).append(" hit(s)\n");
            for (FrontendDictionary e : fdMatches) {
                out.append("       id=").append(e.getId())
                        .append(", type=").append(e.getType())
                        .append(", uiCategory=").append(e.getUiCategory())
                        .append("\n");
            }
            FrontendDictionary fd = fdMatches.isEmpty() ? null : fdMatches.get(0);

            // 2. supermarketProducts per supermarket
            out.append("\n  [2] supermarketProducts findByMatchingKeyAndSupermarket (subcategory=\"\"):\n");
            for (String sm : SUPERMARKETS) {
                List<SupermarketProduct> sp = spRepo.findByMatchingKeyAndSupermarket(displayName, "", sm);
                out.append("       ").append(sm).append(": ").append(sp.size()).append(" hit(s)");
                if (!sp.isEmpty()) {
                    out.append("  (first: ").append(sp.get(0).getName())
                            .append(" / type=").append(sp.get(0).getType())
                            .append(")");
                }
                out.append("\n");
            }

            // 3. genericFoods lookups: by name + by frontendDictionaryId
            List<GenericFood> gfByName = gfRepo.findByName(displayName);
            out.append("\n  [3a] genericFoods findByName: ").append(gfByName.size()).append(" hit(s)\n");
            for (GenericFood g : gfByName) {
                out.append("       id=").append(g.getId())
                        .append(", category=").append(g.getCategory())
                        .append(", frontendDictionaryId=").append(g.getFrontendDictionaryId())
                        .append("\n");
            }

            if (fd != null) {
                Optional<GenericFood> gfById = gfRepo.findByFrontendDictionaryId(fd.getId());
                out.append("\n  [3b] genericFoods findByFrontendDictionaryId(").append(fd.getId()).append("): ")
                        .append(gfById.isPresent() ? "FOUND" : "EMPTY").append("\n");
                gfById.ifPresent(g -> out.append("       name=").append(g.getName())
                        .append(", category=").append(g.getCategory()).append("\n"));
            }

            // 4. Simulated match() per supermarket
            out.append("\n  [4] Simulated PlantifyMatchService.match() per supermarket:\n");
            if (fd != null) {
                for (String sm : SUPERMARKETS) {
                    MatchResult mr = simulateMatch(fd, sm);
                    out.append("       ").append(sm).append(": ").append(describeResult(mr)).append("\n");
                }
            } else {
                out.append("       (no FrontendDictionary entry, skipped)\n");
            }
        }

        // ── Tiefkühlobst extra: fresh fruit in genericFoods ──────
        out.append("\n\n### Tiefkühlobst-Extra: Frisch-Obst in genericFoods? ###\n");
        for (String probe : FRESH_FRUIT_PROBES) {
            List<GenericFood> hits = gfRepo.findByName(probe);
            out.append("  ").append(probe).append(": ");
            if (hits.isEmpty()) {
                List<GenericFood> regex = gfRepo.findByNameContainingIgnoreCase(probe);
                if (regex.isEmpty()) {
                    out.append("none\n");
                } else {
                    out.append("regex-match → ");
                    for (GenericFood g : regex) out.append(g.getName()).append("; ");
                    out.append("\n");
                }
            } else {
                out.append("EXACT MATCH → ");
                for (GenericFood g : hits) out.append(g.getName()).append("; ");
                out.append("\n");
            }
        }

        System.out.println(out);
    }

    private MatchResult simulateMatch(FrontendDictionary fd, String supermarket) {
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

    private String describeResult(MatchResult r) {
        if (r == null) return "null";
        if (r.getSuggestedProduct() != null) return "suggestedProduct=" + r.getSuggestedProduct().getName();
        if (r.getGenericFood() != null) return "genericFood=" + r.getGenericFood().getName();
        if (r.getFallbackDiyRecipeId() != null) return "DIY=" + r.getFallbackDiyRecipeTitle();
        return "EMPTY (no match)";
    }
}
