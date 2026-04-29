package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.model.ShoppingItem;
import ch.zhaw.plantifyme.model.ShoppingList;
import ch.zhaw.plantifyme.model.SupermarketProduct;
import ch.zhaw.plantifyme.model.dto.ImpactTotalDto;
import ch.zhaw.plantifyme.repository.ShoppingListRepository;
import ch.zhaw.plantifyme.repository.SupermarketProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImpactServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private SupermarketProductRepository supermarketProductRepository;

    @InjectMocks
    private ImpactService service;

    private static final String USER_ID = "user-1";

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static ShoppingItem item(String productId, Integer quantity) {
        ShoppingItem i = new ShoppingItem();
        i.setItemId("it-" + (productId != null ? productId : "x"));
        i.setProductId(productId);
        i.setQuantity(quantity);
        return i;
    }

    private static ShoppingList list(List<ShoppingItem> items) {
        ShoppingList l = new ShoppingList();
        l.setId("list-1");
        l.setUserId(USER_ID);
        l.setItems(items);
        return l;
    }

    private static SupermarketProduct product(
            String id,
            String animalType,
            Double animalsAffectedPerUnit,
            Double co2,
            Double water,
            Double land
    ) {
        SupermarketProduct p = org.mockito.Mockito.mock(SupermarketProduct.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getAnimalType()).thenReturn(animalType);
        lenient().when(p.getAnimalsAffectedPerUnit()).thenReturn(animalsAffectedPerUnit);
        lenient().when(p.getImpactCo2SavedKg()).thenReturn(co2);
        lenient().when(p.getImpactWaterSavedL()).thenReturn(water);
        lenient().when(p.getImpactLandSavedM2()).thenReturn(land);
        return p;
    }

    private void stubProduct(SupermarketProduct p) {
        when(supermarketProductRepository.findById(p.getId())).thenReturn(Optional.of(p));
    }

    @BeforeEach
    void defaults() {
        // Empty by default; individual tests override
        lenient().when(shoppingListRepository.findByUserId(any())).thenReturn(List.of());
    }

    // ── 1. Tier-Aggregation ────────────────────────────────────────────────

    @Nested
    @DisplayName("Tier-Aggregation")
    class Animals {

        @Test
        @DisplayName("Single Item, qty=1, animalsAffectedPerUnit=0.05 → ceil = 1")
        void singleAnimal() {
            SupermarketProduct p = product("p1", "cow", 0.05, null, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p1", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getAnimals()).containsEntry("cow", 1);
            assertThat(dto.getAnimals()).containsEntry("pig", 0);
            assertThat(dto.getAnimals()).containsEntry("chicken", 0);
            assertThat(dto.getAnimals()).containsEntry("fish", 0);
        }

        @Test
        @DisplayName("Aggregation across items: cow=0.3*2=0.6→1, chicken=0.5*4=2.0→2")
        void aggregationAcrossItems() {
            SupermarketProduct cowProduct = product("cow1", "cow", 0.3, null, null, null);
            SupermarketProduct chickenProduct = product("chx1", "chicken", 0.5, null, null, null);
            stubProduct(cowProduct);
            stubProduct(chickenProduct);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(
                            item("cow1", 2),
                            item("chx1", 4)
                    ))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getAnimals()).containsEntry("cow", 1);
            assertThat(dto.getAnimals()).containsEntry("chicken", 2);
        }

        @Test
        @DisplayName("Math.ceil happens AFTER summing: 0.05+0.05=0.1 → ceil=1 (not 2)")
        void ceilAfterSum() {
            SupermarketProduct a = product("a", "cow", 0.05, null, null, null);
            SupermarketProduct b = product("b", "cow", 0.05, null, null, null);
            stubProduct(a);
            stubProduct(b);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("a", 1), item("b", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getAnimals()).containsEntry("cow", 1);
        }

        @Test
        @DisplayName("animalType null OR animalsAffectedPerUnit null → no animal contribution")
        void nullAnimalFields() {
            SupermarketProduct typeNull = product("p1", null, 0.5, null, null, null);
            SupermarketProduct unitNull = product("p2", "pig", null, null, null, null);
            stubProduct(typeNull);
            stubProduct(unitNull);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p1", 5), item("p2", 5)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getAnimals()).containsEntry("cow", 0);
            assertThat(dto.getAnimals()).containsEntry("pig", 0);
        }
    }

    // ── 2. CO2 / Wasser / Land Aggregation ─────────────────────────────────

    @Nested
    @DisplayName("CO2/Wasser/Land Aggregation")
    class Aggregation {

        @Test
        @DisplayName("CO2 summed across items, multiplied by quantity, rounded to 1 decimal")
        void co2Sum() {
            SupermarketProduct a = product("a", null, null, 1.234, null, null);
            SupermarketProduct b = product("b", null, null, 0.5, null, null);
            stubProduct(a);
            stubProduct(b);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("a", 2), item("b", 3)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            // 1.234*2 + 0.5*3 = 3.968 → rounded to 1 decimal = 4.0
            assertThat(dto.getCo2TotalKg()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Water summed and multiplied by quantity")
        void waterSum() {
            SupermarketProduct p = product("p", null, null, null, 30.0, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 4)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getWaterTotalL()).isEqualTo(120.0);
        }

        @Test
        @DisplayName("Land summed and multiplied by quantity")
        void landSum() {
            SupermarketProduct p = product("p", null, null, null, null, 7.5);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 2)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getLandTotalM2()).isEqualTo(15.0);
        }
    }

    // ── 3. CO2 Compare-Strings ─────────────────────────────────────────────

    @Nested
    @DisplayName("CO2 compare-string thresholds")
    class Co2Compare {

        @Test
        @DisplayName("totalCo2 ≤ 20 → 'km Autofahrt' (Divisor 0.1873)")
        void carCompare() {
            SupermarketProduct p = product("p", null, null, 5.0, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2Compare()).contains("km Autofahrt");
        }

        @Test
        @DisplayName("totalCo2 > 20 → 'km Flug' (Divisor 0.2630)")
        void flightCompare() {
            SupermarketProduct p = product("p", null, null, 25.0, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2Compare()).contains("km Flug");
        }
    }

    // ── 4. Wasser Compare-Strings ──────────────────────────────────────────

    @Nested
    @DisplayName("Water compare-string thresholds")
    class WaterCompare {

        @Test
        @DisplayName("totalWater < 60 → 'Minuten duschen' (/12)")
        void showerCompare() {
            SupermarketProduct p = product("p", null, null, null, 50.0, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getWaterCompare()).contains("Minuten duschen");
        }

        @Test
        @DisplayName("totalWater ≥ 60 → 'volle Badewannen' (/200)")
        void bathtubCompare() {
            SupermarketProduct p = product("p", null, null, null, 400.0, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getWaterCompare()).contains("Badewannen");
        }
    }

    // ── 5. Land Compare-Strings ────────────────────────────────────────────

    @Nested
    @DisplayName("Land compare-string thresholds")
    class LandCompare {

        @Test
        @DisplayName("totalLand < 25 → 'Parkplätzen' (/12.5)")
        void parkingCompare() {
            SupermarketProduct p = product("p", null, null, null, null, 10.0);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getLandCompare()).contains("Parkplätzen");
        }

        @Test
        @DisplayName("25 ≤ totalLand ≤ 500 → 'Tennisplätzen' (/195.7)")
        void tennisCompare() {
            SupermarketProduct p = product("p", null, null, null, null, 200.0);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getLandCompare()).contains("Tennisplätzen");
        }

        @Test
        @DisplayName("totalLand > 500 → 'Fussballfeldern' (/7140)")
        void footballCompare() {
            SupermarketProduct p = product("p", null, null, null, null, 800.0);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getLandCompare()).contains("Fussballfeldern");
        }
    }

    // ── 6. Edge Cases ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Keine Listen → alle Werte 0, animals zero, defaults populated")
        void noLists() {
            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(0.0);
            assertThat(dto.getWaterTotalL()).isEqualTo(0.0);
            assertThat(dto.getLandTotalM2()).isEqualTo(0.0);
            assertThat(dto.getAnimals()).containsEntry("cow", 0);
            assertThat(dto.getAnimals()).containsEntry("pig", 0);
            assertThat(dto.getAnimals()).containsEntry("chicken", 0);
            assertThat(dto.getAnimals()).containsEntry("fish", 0);
            assertThat(dto.getCo2Compare()).isNotNull();
            assertThat(dto.getWaterCompare()).isNotNull();
            assertThat(dto.getLandCompare()).isNotNull();
        }

        @Test
        @DisplayName("List.items=null wird übersprungen, andere Liste zählt")
        void itemsNullSkipped() {
            ShoppingList nullItems = list(null);
            SupermarketProduct p = product("p", null, null, 2.0, null, null);
            stubProduct(p);
            ShoppingList real = list(List.of(item("p", 1)));
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(nullItems, real));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Item ohne productId (z.B. Type C) wird übersprungen")
        void noProductIdSkipped() {
            SupermarketProduct p = product("p", null, null, 5.0, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(
                            item(null, 1),
                            item("", 1),
                            item("p", 1)
                    ))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("productId nicht in DB → Item übersprungen")
        void productIdMissingSkipped() {
            when(supermarketProductRepository.findById("ghost")).thenReturn(Optional.empty());
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("ghost", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Item ohne quantity → qty=1 default")
        void quantityNullDefaultsToOne() {
            SupermarketProduct p = product("p", null, null, 3.0, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", null)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Partial null impact fields: nur gesetzte Felder aggregieren")
        void partialNullImpactFields() {
            SupermarketProduct p = product("p", null, null, 4.0, null, null);
            stubProduct(p);
            when(shoppingListRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(list(List.of(item("p", 1)))));

            ImpactTotalDto dto = service.computeTotal(USER_ID);

            assertThat(dto.getCo2TotalKg()).isEqualTo(4.0);
            assertThat(dto.getWaterTotalL()).isEqualTo(0.0);
            assertThat(dto.getLandTotalM2()).isEqualTo(0.0);
        }
    }
}
