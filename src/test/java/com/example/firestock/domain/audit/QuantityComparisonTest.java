package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.numbers.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link QuantityComparison} demonstrating consumable quantity verification.
 *
 * <p>During formal audits, consumable stock levels are verified by comparing
 * the expected quantity (from the manifest) with the actual quantity found.
 * This value object encapsulates that comparison.
 */
@DisplayName("QuantityComparison")
class QuantityComparisonTest {

    @Nested
    @DisplayName("Creating comparisons")
    class CreatingComparisons {

        @Test
        void constructor_accepts_expected_and_found_quantities() {
            var expected = Quantity.of(100);
            var found = Quantity.of(95);

            var comparison = new QuantityComparison(expected, found);

            assertThat(comparison.expected()).isEqualTo(expected);
            assertThat(comparison.found()).isEqualTo(found);
        }

        @Test
        void matching_creates_comparison_with_equal_quantities() {
            // Use when expected equals found
            var quantity = Quantity.of(50);

            var comparison = QuantityComparison.matching(quantity);

            assertThat(comparison.expected()).isEqualTo(quantity);
            assertThat(comparison.found()).isEqualTo(quantity);
            assertThat(comparison.isMatch()).isTrue();
        }

        @Test
        void of_creates_comparison_from_integers() {
            // Convenience factory for simple cases
            var comparison = QuantityComparison.of(100, 95);

            assertThat(comparison.expected().value().intValue()).isEqualTo(100);
            assertThat(comparison.found().value().intValue()).isEqualTo(95);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_expected_quantity() {
            assertThatThrownBy(() -> new QuantityComparison(null, Quantity.of(10)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Expected quantity cannot be null");
        }

        @Test
        void rejects_null_found_quantity() {
            assertThatThrownBy(() -> new QuantityComparison(Quantity.of(10), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Found quantity cannot be null");
        }
    }

    @Nested
    @DisplayName("Detecting match, shortage, and surplus")
    class DetectingDiscrepancies {

        @Test
        void isMatch_returns_true_when_quantities_are_equal() {
            var comparison = QuantityComparison.of(100, 100);

            assertThat(comparison.isMatch()).isTrue();
            assertThat(comparison.isShortage()).isFalse();
            assertThat(comparison.isSurplus()).isFalse();
        }

        @Test
        void isShortage_returns_true_when_found_less_than_expected() {
            // Found 95 but expected 100 = shortage of 5
            var comparison = QuantityComparison.of(100, 95);

            assertThat(comparison.isShortage()).isTrue();
            assertThat(comparison.isMatch()).isFalse();
            assertThat(comparison.isSurplus()).isFalse();
        }

        @Test
        void isSurplus_returns_true_when_found_more_than_expected() {
            // Found 105 but expected 100 = surplus of 5
            var comparison = QuantityComparison.of(100, 105);

            assertThat(comparison.isSurplus()).isTrue();
            assertThat(comparison.isMatch()).isFalse();
            assertThat(comparison.isShortage()).isFalse();
        }
    }

    @Nested
    @DisplayName("Calculating differences")
    class CalculatingDifferences {

        @Test
        void shortageAmount_returns_missing_quantity() {
            var comparison = QuantityComparison.of(100, 95);

            assertThat(comparison.shortageAmount().value().intValue()).isEqualTo(5);
        }

        @Test
        void shortageAmount_returns_zero_when_no_shortage() {
            var comparison = QuantityComparison.of(100, 100);

            assertThat(comparison.shortageAmount()).isEqualTo(Quantity.zero());
        }

        @Test
        void shortageAmount_returns_zero_when_surplus() {
            var comparison = QuantityComparison.of(100, 105);

            assertThat(comparison.shortageAmount()).isEqualTo(Quantity.zero());
        }

        @Test
        void surplusAmount_returns_extra_quantity() {
            var comparison = QuantityComparison.of(100, 115);

            assertThat(comparison.surplusAmount().value().intValue()).isEqualTo(15);
        }

        @Test
        void surplusAmount_returns_zero_when_no_surplus() {
            var comparison = QuantityComparison.of(100, 100);

            assertThat(comparison.surplusAmount()).isEqualTo(Quantity.zero());
        }

        @Test
        void surplusAmount_returns_zero_when_shortage() {
            var comparison = QuantityComparison.of(100, 95);

            assertThat(comparison.surplusAmount()).isEqualTo(Quantity.zero());
        }

        @Test
        void difference_returns_absolute_difference_for_shortage() {
            var comparison = QuantityComparison.of(100, 95);

            assertThat(comparison.difference().value().intValue()).isEqualTo(5);
        }

        @Test
        void difference_returns_absolute_difference_for_surplus() {
            var comparison = QuantityComparison.of(100, 110);

            assertThat(comparison.difference().value().intValue()).isEqualTo(10);
        }

        @Test
        void difference_returns_zero_for_match() {
            var comparison = QuantityComparison.of(100, 100);

            assertThat(comparison.difference()).isEqualTo(Quantity.zero());
        }
    }

    @Nested
    @DisplayName("Working with decimal quantities")
    class DecimalQuantities {

        @Test
        void handles_decimal_quantities() {
            var expected = Quantity.of("10.50");
            var found = Quantity.of("10.25");

            var comparison = new QuantityComparison(expected, found);

            assertThat(comparison.isShortage()).isTrue();
            assertThat(comparison.shortageAmount().value()).isEqualByComparingTo("0.25");
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void equals_compares_both_quantities() {
            var comparison1 = QuantityComparison.of(100, 95);
            var comparison2 = QuantityComparison.of(100, 95);

            assertThat(comparison1).isEqualTo(comparison2);
        }

        @Test
        void different_quantities_are_not_equal() {
            var comparison1 = QuantityComparison.of(100, 95);
            var comparison2 = QuantityComparison.of(100, 90);

            assertThat(comparison1).isNotEqualTo(comparison2);
        }
    }
}
