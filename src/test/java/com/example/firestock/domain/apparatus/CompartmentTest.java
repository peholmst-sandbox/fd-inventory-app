package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Compartment} demonstrating physical storage areas on apparatus.
 *
 * <p>Compartments provide organizational structure for equipment and enable
 * systematic inventory checks. Each compartment has a code, name, and location.
 */
@DisplayName("Compartment")
class CompartmentTest {

    private final CompartmentId id = CompartmentId.generate();
    private final ApparatusId apparatusId = ApparatusId.generate();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void creates_with_required_fields_via_factory() {
            var compartment = Compartment.create(
                    id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE
            );

            assertThat(compartment.id()).isEqualTo(id);
            assertThat(compartment.apparatusId()).isEqualTo(apparatusId);
            assertThat(compartment.code()).isEqualTo("D1");
            assertThat(compartment.name()).isEqualTo("Driver Side 1");
            assertThat(compartment.location()).isEqualTo(CompartmentLocation.DRIVER_SIDE);
            assertThat(compartment.description()).isNull();
            assertThat(compartment.displayOrder()).isZero();
        }

        @Test
        void creates_with_display_order_via_factory() {
            var compartment = Compartment.create(
                    id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, 5
            );

            assertThat(compartment.displayOrder()).isEqualTo(5);
        }

        @Test
        void creates_with_all_fields() {
            var compartment = new Compartment(
                    id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE,
                    "Contains SCBA equipment", 3
            );

            assertThat(compartment.description()).isEqualTo("Contains SCBA equipment");
            assertThat(compartment.displayOrder()).isEqualTo(3);
        }

        @Test
        void strips_whitespace_from_code() {
            var compartment = Compartment.create(
                    id, apparatusId, "  D1  ", "Driver Side 1", CompartmentLocation.DRIVER_SIDE
            );

            assertThat(compartment.code()).isEqualTo("D1");
        }

        @Test
        void strips_whitespace_from_name() {
            var compartment = Compartment.create(
                    id, apparatusId, "D1", "  Driver Side 1  ", CompartmentLocation.DRIVER_SIDE
            );

            assertThat(compartment.name()).isEqualTo("Driver Side 1");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new Compartment(
                    null, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Compartment ID cannot be null");
        }

        @Test
        void rejects_null_apparatus_id() {
            assertThatThrownBy(() -> new Compartment(
                    id, null, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_code() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, null, "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Code cannot be null");
        }

        @Test
        void rejects_null_name() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", null, CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Name cannot be null");
        }

        @Test
        void rejects_null_location() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", "Driver Side 1", null, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Location cannot be null");
        }

        @Test
        void rejects_empty_code() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code cannot be empty");
        }

        @Test
        void rejects_whitespace_only_code() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "   ", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code cannot be empty");
        }

        @Test
        void rejects_code_longer_than_20_characters() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "A".repeat(21), "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code must be 20 characters or less");
        }

        @Test
        void rejects_empty_name() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", "", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name cannot be empty");
        }

        @Test
        void rejects_whitespace_only_name() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", "   ", CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name cannot be empty");
        }

        @Test
        void rejects_name_longer_than_100_characters() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", "A".repeat(101), CompartmentLocation.DRIVER_SIDE, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name must be 100 characters or less");
        }

        @Test
        void rejects_negative_display_order() {
            assertThatThrownBy(() -> new Compartment(
                    id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, null, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Display order must be non-negative");
        }

        @Test
        void accepts_maximum_length_code() {
            var compartment = new Compartment(
                    id, apparatusId, "A".repeat(20), "Name", CompartmentLocation.DRIVER_SIDE, null, 0
            );

            assertThat(compartment.code()).hasSize(20);
        }

        @Test
        void accepts_maximum_length_name() {
            var compartment = new Compartment(
                    id, apparatusId, "D1", "A".repeat(100), CompartmentLocation.DRIVER_SIDE, null, 0
            );

            assertThat(compartment.name()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Update methods")
    class UpdateMethods {

        @Test
        void withDescription_updates_description() {
            var compartment = Compartment.create(id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE);

            var updated = compartment.withDescription("Contains SCBA equipment");

            assertThat(updated.description()).isEqualTo("Contains SCBA equipment");
            assertThat(updated).isNotSameAs(compartment);
        }

        @Test
        void withDisplayOrder_updates_order() {
            var compartment = Compartment.create(id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE);

            var updated = compartment.withDisplayOrder(10);

            assertThat(updated.displayOrder()).isEqualTo(10);
            assertThat(updated).isNotSameAs(compartment);
        }

        @Test
        void withName_updates_name() {
            var compartment = Compartment.create(id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE);

            var updated = compartment.withName("Driver Side Compartment 1");

            assertThat(updated.name()).isEqualTo("Driver Side Compartment 1");
            assertThat(updated).isNotSameAs(compartment);
        }

        @Test
        void withCode_updates_code() {
            var compartment = Compartment.create(id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE);

            var updated = compartment.withCode("DS1");

            assertThat(updated.code()).isEqualTo("DS1");
            assertThat(updated).isNotSameAs(compartment);
        }
    }

    @Nested
    @DisplayName("Optional field accessors")
    class OptionalAccessors {

        @Test
        void descriptionOpt_returns_empty_when_null() {
            var compartment = Compartment.create(id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE);

            assertThat(compartment.descriptionOpt()).isEmpty();
        }

        @Test
        void descriptionOpt_returns_value_when_present() {
            var compartment = new Compartment(
                    id, apparatusId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE,
                    "Contains SCBA", 0
            );

            assertThat(compartment.descriptionOpt()).contains("Contains SCBA");
        }
    }

    @Nested
    @DisplayName("Usage examples")
    class UsageExamples {

        @Test
        void typical_engine_compartments() {
            var engineId = ApparatusId.generate();

            var driverSide1 = Compartment.create(
                    CompartmentId.generate(), engineId, "D1", "Driver Side 1", CompartmentLocation.DRIVER_SIDE, 1
            );
            var passengerSide1 = Compartment.create(
                    CompartmentId.generate(), engineId, "P1", "Passenger Side 1", CompartmentLocation.PASSENGER_SIDE, 2
            );
            var crosslay = Compartment.create(
                    CompartmentId.generate(), engineId, "CL", "Crosslay", CompartmentLocation.CROSSLAY, 3
            );
            var rearCompartment = Compartment.create(
                    CompartmentId.generate(), engineId, "REAR", "Rear Compartment", CompartmentLocation.REAR, 4
            );

            assertThat(driverSide1.displayOrder()).isLessThan(passengerSide1.displayOrder());
            assertThat(passengerSide1.displayOrder()).isLessThan(crosslay.displayOrder());
            assertThat(crosslay.displayOrder()).isLessThan(rearCompartment.displayOrder());
        }
    }
}
