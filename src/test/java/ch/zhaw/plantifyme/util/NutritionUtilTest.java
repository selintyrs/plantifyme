package ch.zhaw.plantifyme.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionUtilTest {

    @Nested
    @DisplayName("Mass and volume conversions")
    class MassAndVolume {

        @ParameterizedTest(name = "{0} {1} → {2} g")
        @CsvSource({
                "100,    g,   100.0",
                "0.5,    kg,  500.0",
                "250,    mg,    0.25",
                "200,    ml,  200.0",
                "1.5,    l,  1500.0",
                "2,      dl,  200.0"
        })
        void convertsMassAndVolume(double amount, String unit, double expected) {
            assertThat(NutritionUtil.toGrams(amount, unit)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Spoon and cup conversions")
    class SpoonAndCup {

        @ParameterizedTest(name = "{0} {1} → {2} g")
        @CsvSource({
                "1,    EL,         15.0",
                "2,    Esslöffel,  30.0",
                "1,    TL,          5.0",
                "3,    Teelöffel,  15.0",
                "1,    Tasse,     240.0",
                "0.5,  Tasse,     120.0"
        })
        void convertsSpoonAndCup(double amount, String unit, double expected) {
            assertThat(NutritionUtil.toGrams(amount, unit)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Unit normalization")
    class Normalization {

        @Test
        @DisplayName("is case-insensitive")
        void caseInsensitive() {
            assertThat(NutritionUtil.toGrams(2.0, "KG")).isEqualTo(2000.0);
            assertThat(NutritionUtil.toGrams(2.0, "Kg")).isEqualTo(2000.0);
            assertThat(NutritionUtil.toGrams(2.0, "kG")).isEqualTo(2000.0);
        }

        @Test
        @DisplayName("trims surrounding whitespace")
        void trimsWhitespace() {
            assertThat(NutritionUtil.toGrams(100.0, "  g  ")).isEqualTo(100.0);
            assertThat(NutritionUtil.toGrams(1.0, " EL ")).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("Non-convertible units")
    class NonConvertible {

        @ParameterizedTest(name = "unit \"{0}\" returns null")
        @ValueSource(strings = {"Stück", "Stk", "Prise", "", "Bund", "Pck"})
        void returnsNullForNonConvertible(String unit) {
            assertThat(NutritionUtil.toGrams(1.0, unit)).isNull();
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @ParameterizedTest(name = "amount=null, unit=\"{0}\" returns null")
        @ValueSource(strings = {"g", "kg", "EL"})
        void nullAmountReturnsNull(String unit) {
            assertThat(NutritionUtil.toGrams(null, unit)).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null or empty unit returns null")
        void nullOrEmptyUnitReturnsNull(String unit) {
            assertThat(NutritionUtil.toGrams(100.0, unit)).isNull();
        }

        @Test
        @DisplayName("both null returns null")
        void bothNullReturnsNull() {
            assertThat(NutritionUtil.toGrams(null, null)).isNull();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("zero amount returns zero, not null")
        void zeroAmount() {
            assertThat(NutritionUtil.toGrams(0.0, "g")).isEqualTo(0.0);
            assertThat(NutritionUtil.toGrams(0.0, "EL")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("negative amount is preserved (no validation)")
        void negativeAmount() {
            assertThat(NutritionUtil.toGrams(-5.0, "g")).isEqualTo(-5.0);
        }
    }
}
