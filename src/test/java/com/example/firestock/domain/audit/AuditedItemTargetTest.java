package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AuditedItemTarget} and its implementations demonstrating
 * the XOR constraint between equipment and consumable audit targets.
 *
 * <p>An audited item targets either a piece of equipment OR a consumable stock entry,
 * but never both. This sealed interface hierarchy enforces that constraint at the type level.
 */
@DisplayName("AuditedItemTarget")
class AuditedItemTargetTest {

    @Nested
    @DisplayName("EquipmentTarget")
    class EquipmentTargetTests {

        @Test
        void creates_target_for_equipment_item() {
            var equipmentId = EquipmentItemId.generate();

            var target = new EquipmentTarget(equipmentId);

            assertThat(target.equipmentItemId()).isEqualTo(equipmentId);
        }

        @Test
        void of_factory_creates_target() {
            var equipmentId = EquipmentItemId.generate();

            var target = EquipmentTarget.of(equipmentId);

            assertThat(target.equipmentItemId()).isEqualTo(equipmentId);
        }

        @Test
        void rejects_null_equipment_id() {
            assertThatThrownBy(() -> new EquipmentTarget(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Equipment item ID cannot be null");
        }

        @Test
        void isEquipment_returns_true() {
            var target = EquipmentTarget.of(EquipmentItemId.generate());

            assertThat(target.isEquipment()).isTrue();
        }

        @Test
        void isConsumable_returns_false() {
            var target = EquipmentTarget.of(EquipmentItemId.generate());

            assertThat(target.isConsumable()).isFalse();
        }
    }

    @Nested
    @DisplayName("ConsumableTarget")
    class ConsumableTargetTests {

        @Test
        void creates_target_for_consumable_stock() {
            var stockId = ConsumableStockId.generate();

            var target = new ConsumableTarget(stockId);

            assertThat(target.consumableStockId()).isEqualTo(stockId);
        }

        @Test
        void of_factory_creates_target() {
            var stockId = ConsumableStockId.generate();

            var target = ConsumableTarget.of(stockId);

            assertThat(target.consumableStockId()).isEqualTo(stockId);
        }

        @Test
        void rejects_null_consumable_stock_id() {
            assertThatThrownBy(() -> new ConsumableTarget(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Consumable stock ID cannot be null");
        }

        @Test
        void isConsumable_returns_true() {
            var target = ConsumableTarget.of(ConsumableStockId.generate());

            assertThat(target.isConsumable()).isTrue();
        }

        @Test
        void isEquipment_returns_false() {
            var target = ConsumableTarget.of(ConsumableStockId.generate());

            assertThat(target.isEquipment()).isFalse();
        }
    }

    @Nested
    @DisplayName("Sealed interface pattern")
    class SealedInterfacePattern {

        @Test
        void can_use_pattern_matching_for_type_dispatch() {
            AuditedItemTarget equipmentTarget = EquipmentTarget.of(EquipmentItemId.generate());
            AuditedItemTarget consumableTarget = ConsumableTarget.of(ConsumableStockId.generate());

            // Pattern matching ensures exhaustive handling
            assertThat(describeTarget(equipmentTarget)).startsWith("Equipment:");
            assertThat(describeTarget(consumableTarget)).startsWith("Consumable:");
        }

        @Test
        void sealed_interface_has_exactly_two_implementations() {
            // The sealed interface permits only EquipmentTarget and ConsumableTarget
            var permittedSubclasses = AuditedItemTarget.class.getPermittedSubclasses();

            assertThat(permittedSubclasses).hasSize(2);
            assertThat(permittedSubclasses).containsExactlyInAnyOrder(
                    EquipmentTarget.class,
                    ConsumableTarget.class
            );
        }

        private String describeTarget(AuditedItemTarget target) {
            // Demonstrates exhaustive pattern matching (Java 21+)
            return switch (target) {
                case EquipmentTarget e -> "Equipment: " + e.equipmentItemId();
                case ConsumableTarget c -> "Consumable: " + c.consumableStockId();
            };
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void equipment_targets_with_same_id_are_equal() {
            var equipmentId = EquipmentItemId.generate();
            var target1 = EquipmentTarget.of(equipmentId);
            var target2 = EquipmentTarget.of(equipmentId);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void consumable_targets_with_same_id_are_equal() {
            var stockId = ConsumableStockId.generate();
            var target1 = ConsumableTarget.of(stockId);
            var target2 = ConsumableTarget.of(stockId);

            assertThat(target1).isEqualTo(target2);
        }

        @Test
        void equipment_and_consumable_targets_are_never_equal() {
            // Even conceptually "same" IDs would be different types
            AuditedItemTarget equipmentTarget = EquipmentTarget.of(EquipmentItemId.generate());
            AuditedItemTarget consumableTarget = ConsumableTarget.of(ConsumableStockId.generate());

            assertThat(equipmentTarget).isNotEqualTo(consumableTarget);
        }
    }
}
