package com.example.firestock.domain.manifest;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.RequiredQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ManifestEntry} demonstrating expected equipment definitions.
 *
 * <p>ManifestEntry defines what equipment should be present in a compartment.
 * During inventory checks and audits, actual equipment is verified against
 * the manifest.
 */
@DisplayName("ManifestEntry")
class ManifestEntryTest {

    private final ManifestEntryId id = ManifestEntryId.generate();
    private final ApparatusId apparatusId = ApparatusId.generate();
    private final CompartmentId compartmentId = CompartmentId.generate();
    private final EquipmentTypeId equipmentTypeId = EquipmentTypeId.generate();
    private final RequiredQuantity requiredQuantity = RequiredQuantity.of(2);

    @Nested
    @DisplayName("Creating critical entries")
    class CreatingCriticalEntries {

        @Test
        void critical_factory_creates_entry_marked_as_critical() {
            var entry = ManifestEntry.critical(
                    id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity
            );

            assertThat(entry.id()).isEqualTo(id);
            assertThat(entry.apparatusId()).isEqualTo(apparatusId);
            assertThat(entry.compartmentId()).isEqualTo(compartmentId);
            assertThat(entry.equipmentTypeId()).isEqualTo(equipmentTypeId);
            assertThat(entry.requiredQuantity()).isEqualTo(requiredQuantity);
            assertThat(entry.isCritical()).isTrue();
            assertThat(entry.displayOrder()).isZero();
            assertThat(entry.notes()).isNull();
        }

        @Test
        void critical_factory_accepts_integer_quantity() {
            var entry = ManifestEntry.critical(
                    id, apparatusId, compartmentId, equipmentTypeId, 4
            );

            assertThat(entry.requiredQuantity().value()).isEqualTo(4);
            assertThat(entry.isCritical()).isTrue();
        }
    }

    @Nested
    @DisplayName("Creating optional entries")
    class CreatingOptionalEntries {

        @Test
        void optional_factory_creates_entry_not_marked_as_critical() {
            var entry = ManifestEntry.optional(
                    id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity
            );

            assertThat(entry.id()).isEqualTo(id);
            assertThat(entry.apparatusId()).isEqualTo(apparatusId);
            assertThat(entry.compartmentId()).isEqualTo(compartmentId);
            assertThat(entry.equipmentTypeId()).isEqualTo(equipmentTypeId);
            assertThat(entry.requiredQuantity()).isEqualTo(requiredQuantity);
            assertThat(entry.isCritical()).isFalse();
            assertThat(entry.displayOrder()).isZero();
            assertThat(entry.notes()).isNull();
        }

        @Test
        void optional_factory_accepts_integer_quantity() {
            var entry = ManifestEntry.optional(
                    id, apparatusId, compartmentId, equipmentTypeId, 10
            );

            assertThat(entry.requiredQuantity().value()).isEqualTo(10);
            assertThat(entry.isCritical()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new ManifestEntry(
                    null, apparatusId, compartmentId, equipmentTypeId, requiredQuantity, true, 0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Manifest entry ID cannot be null");
        }

        @Test
        void rejects_null_apparatus_id() {
            assertThatThrownBy(() -> new ManifestEntry(
                    id, null, compartmentId, equipmentTypeId, requiredQuantity, true, 0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_compartment_id() {
            assertThatThrownBy(() -> new ManifestEntry(
                    id, apparatusId, null, equipmentTypeId, requiredQuantity, true, 0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Compartment ID cannot be null");
        }

        @Test
        void rejects_null_equipment_type_id() {
            assertThatThrownBy(() -> new ManifestEntry(
                    id, apparatusId, compartmentId, null, requiredQuantity, true, 0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Equipment type ID cannot be null");
        }

        @Test
        void rejects_null_required_quantity() {
            assertThatThrownBy(() -> new ManifestEntry(
                    id, apparatusId, compartmentId, equipmentTypeId, null, true, 0, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Required quantity cannot be null");
        }

        @Test
        void rejects_negative_display_order() {
            assertThatThrownBy(() -> new ManifestEntry(
                    id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity, true, -1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Display order must be non-negative");
        }

        @Test
        void accepts_zero_display_order() {
            var entry = new ManifestEntry(
                    id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity, true, 0, null
            );

            assertThat(entry.displayOrder()).isZero();
        }
    }

    @Nested
    @DisplayName("Update methods")
    class UpdateMethods {

        @Test
        void asCritical_marks_entry_as_critical() {
            var entry = ManifestEntry.optional(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var critical = entry.asCritical();

            assertThat(critical.isCritical()).isTrue();
            assertThat(critical).isNotSameAs(entry);
            assertThat(entry.isCritical()).isFalse(); // Original unchanged
        }

        @Test
        void asOptional_marks_entry_as_not_critical() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var optional = entry.asOptional();

            assertThat(optional.isCritical()).isFalse();
            assertThat(optional).isNotSameAs(entry);
            assertThat(entry.isCritical()).isTrue(); // Original unchanged
        }

        @Test
        void withRequiredQuantity_updates_quantity_using_RequiredQuantity() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);
            var newQuantity = RequiredQuantity.of(5);

            var updated = entry.withRequiredQuantity(newQuantity);

            assertThat(updated.requiredQuantity()).isEqualTo(newQuantity);
            assertThat(updated).isNotSameAs(entry);
        }

        @Test
        void withRequiredQuantity_updates_quantity_using_int() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var updated = entry.withRequiredQuantity(10);

            assertThat(updated.requiredQuantity().value()).isEqualTo(10);
            assertThat(updated).isNotSameAs(entry);
        }

        @Test
        void withDisplayOrder_updates_order() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var updated = entry.withDisplayOrder(5);

            assertThat(updated.displayOrder()).isEqualTo(5);
            assertThat(updated).isNotSameAs(entry);
        }

        @Test
        void withNotes_updates_notes() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var updated = entry.withNotes("Keep in waterproof bag");

            assertThat(updated.notes()).isEqualTo("Keep in waterproof bag");
            assertThat(updated).isNotSameAs(entry);
        }

        @Test
        void moveToCompartment_changes_compartment() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);
            var newCompartmentId = CompartmentId.generate();

            var moved = entry.moveToCompartment(newCompartmentId);

            assertThat(moved.compartmentId()).isEqualTo(newCompartmentId);
            assertThat(moved.apparatusId()).isEqualTo(apparatusId); // Apparatus unchanged
            assertThat(moved).isNotSameAs(entry);
        }
    }

    @Nested
    @DisplayName("Optional field accessors")
    class OptionalAccessors {

        @Test
        void notesOpt_returns_empty_when_null() {
            var entry = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            assertThat(entry.notesOpt()).isEmpty();
        }

        @Test
        void notesOpt_returns_value_when_present() {
            var entry = new ManifestEntry(
                    id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity, true, 0,
                    "Stored in protective case"
            );

            assertThat(entry.notesOpt()).contains("Stored in protective case");
        }
    }

    @Nested
    @DisplayName("Usage examples")
    class UsageExamples {

        @Test
        void typical_scba_manifest_entry() {
            var scbaTypeId = EquipmentTypeId.generate();
            var driverSideCompartmentId = CompartmentId.generate();

            // SCBA is critical equipment - must always be present and verified
            var scbaEntry = ManifestEntry.critical(
                    ManifestEntryId.generate(),
                    apparatusId,
                    driverSideCompartmentId,
                    scbaTypeId,
                    4  // 4 SCBA units required
            ).withDisplayOrder(1)
             .withNotes("Stored in SCBA brackets");

            assertThat(scbaEntry.isCritical()).isTrue();
            assertThat(scbaEntry.requiredQuantity().value()).isEqualTo(4);
            assertThat(scbaEntry.notesOpt()).isPresent();
        }

        @Test
        void typical_optional_equipment_entry() {
            var salvageCoverTypeId = EquipmentTypeId.generate();
            var rearCompartmentId = CompartmentId.generate();

            // Salvage covers are optional - nice to have but not mission-critical
            var salvageCoverEntry = ManifestEntry.optional(
                    ManifestEntryId.generate(),
                    apparatusId,
                    rearCompartmentId,
                    salvageCoverTypeId,
                    2
            ).withDisplayOrder(10);

            assertThat(salvageCoverEntry.isCritical()).isFalse();
        }

        @Test
        void manifest_for_multiple_compartments() {
            var scbaTypeId = EquipmentTypeId.generate();
            var halligan = EquipmentTypeId.generate();
            var driverSide = CompartmentId.generate();
            var passengerSide = CompartmentId.generate();

            var entry1 = ManifestEntry.critical(
                    ManifestEntryId.generate(), apparatusId, driverSide, scbaTypeId, 2
            ).withDisplayOrder(1);

            var entry2 = ManifestEntry.critical(
                    ManifestEntryId.generate(), apparatusId, passengerSide, scbaTypeId, 2
            ).withDisplayOrder(2);

            var entry3 = ManifestEntry.critical(
                    ManifestEntryId.generate(), apparatusId, driverSide, halligan, 1
            ).withDisplayOrder(3);

            // All entries belong to the same apparatus but different compartments
            assertThat(entry1.apparatusId()).isEqualTo(entry2.apparatusId());
            assertThat(entry1.compartmentId()).isNotEqualTo(entry2.compartmentId());
            assertThat(entry1.compartmentId()).isEqualTo(entry3.compartmentId());
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void update_methods_return_new_instances() {
            var original = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            var updated1 = original.withNotes("Notes");
            var updated2 = original.withDisplayOrder(5);
            var updated3 = original.asOptional();

            assertThat(updated1).isNotSameAs(original);
            assertThat(updated2).isNotSameAs(original);
            assertThat(updated3).isNotSameAs(original);
        }

        @Test
        void original_remains_unchanged_after_updates() {
            var original = ManifestEntry.critical(id, apparatusId, compartmentId, equipmentTypeId, requiredQuantity);

            original.withNotes("Notes");
            original.withDisplayOrder(5);
            original.asOptional();

            assertThat(original.notes()).isNull();
            assertThat(original.displayOrder()).isZero();
            assertThat(original.isCritical()).isTrue();
        }
    }
}
