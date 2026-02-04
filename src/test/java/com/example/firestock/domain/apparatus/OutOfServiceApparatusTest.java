package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;
import java.time.Year;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link OutOfServiceApparatus} demonstrating temporarily unavailable apparatus.
 *
 * <p>OutOfServiceApparatus represents apparatus that is temporarily unavailable
 * for incident response due to maintenance, repair, or equipment issues.
 */
@DisplayName("OutOfServiceApparatus")
class OutOfServiceApparatusTest {

    private final ApparatusId id = ApparatusId.generate();
    private final UnitNumber unitNumber = new UnitNumber("Engine 5");
    private final ApparatusType type = ApparatusType.ENGINE;
    private final StationId stationId = StationId.generate();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void creates_with_required_fields() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Pump repair"
            );

            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
            assertThat(apparatus.status()).isEqualTo(ApparatusStatus.OUT_OF_SERVICE);
            assertThat(apparatus.notes()).isEqualTo("Pump repair");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new OutOfServiceApparatus(
                    null, unitNumber, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_unit_number() {
            assertThatThrownBy(() -> new OutOfServiceApparatus(
                    id, null, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Unit number cannot be null");
        }

        @Test
        void rejects_null_type() {
            assertThatThrownBy(() -> new OutOfServiceApparatus(
                    id, unitNumber, null, null, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus type cannot be null");
        }

        @Test
        void rejects_null_station_id() {
            assertThatThrownBy(() -> new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Station ID cannot be null");
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        void returnToService_transitions_to_InServiceApparatus() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, new Barcode("APP-ENG005"), "Pump repair"
            );

            var inService = apparatus.returnToService();

            assertThat(inService).isInstanceOf(InServiceApparatus.class);
            assertThat(inService.id()).isEqualTo(id);
            assertThat(inService.status()).isEqualTo(ApparatusStatus.IN_SERVICE);
            assertThat(inService.notes()).isNull(); // Notes cleared on return to service
        }

        @Test
        void decommission_transitions_to_DecommissionedApparatus() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "In shop"
            );

            var decommissioned = apparatus.decommission("Frame damage - not repairable");

            assertThat(decommissioned).isInstanceOf(DecommissionedApparatus.class);
            assertThat(decommissioned.id()).isEqualTo(id);
            assertThat(decommissioned.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
            assertThat(decommissioned.notes()).isEqualTo("Frame damage - not repairable");
        }

        @Test
        void returnToService_preserves_vehicle_details() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, barcode, "Pump repair"
            );

            var inService = apparatus.returnToService();

            assertThat(inService.vin()).isEqualTo("VIN123");
            assertThat(inService.make()).isEqualTo("Pierce");
            assertThat(inService.model()).isEqualTo("Arrow");
            assertThat(inService.year()).isEqualTo(Year.of(2020));
            assertThat(inService.barcode()).isEqualTo(barcode);
        }
    }

    @Nested
    @DisplayName("Update methods")
    class UpdateMethods {

        @Test
        void withNotes_updates_notes() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Original"
            );

            var updated = apparatus.withNotes("Updated reason");

            assertThat(updated.notes()).isEqualTo("Updated reason");
            assertThat(updated).isNotSameAs(apparatus);
        }
    }

    @Nested
    @DisplayName("Apparatus interface methods")
    class ApparatusInterfaceMethods {

        @Test
        void isInService_returns_false() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isInService()).isFalse();
        }

        @Test
        void isDecommissioned_returns_false() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isDecommissioned()).isFalse();
        }

        @Test
        void canHaveInventoryChecks_returns_true() {
            var apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }
    }
}
