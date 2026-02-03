package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import java.time.Year;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ReserveApparatus} demonstrating backup apparatus behaviour.
 *
 * <p>ReserveApparatus represents operational apparatus that is available as backup
 * but not in primary rotation.
 */
@DisplayName("ReserveApparatus")
class ReserveApparatusTest {

    private final ApparatusId id = ApparatusId.generate();
    private final UnitNumber unitNumber = new UnitNumber("Reserve Engine 1");
    private final ApparatusType type = ApparatusType.ENGINE;
    private final StationId stationId = StationId.generate();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void creates_with_required_fields() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Backup unit"
            );

            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
            assertThat(apparatus.status()).isEqualTo(ApparatusStatus.RESERVE);
            assertThat(apparatus.notes()).isEqualTo("Backup unit");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new ReserveApparatus(
                    null, unitNumber, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_unit_number() {
            assertThatThrownBy(() -> new ReserveApparatus(
                    id, null, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Unit number cannot be null");
        }

        @Test
        void rejects_null_type() {
            assertThatThrownBy(() -> new ReserveApparatus(
                    id, unitNumber, null, null, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus type cannot be null");
        }

        @Test
        void rejects_null_station_id() {
            assertThatThrownBy(() -> new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Station ID cannot be null");
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        void activate_transitions_to_InServiceApparatus() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, new Barcode("APP-RES001"), "Backup unit"
            );

            var inService = apparatus.activate();

            assertThat(inService).isInstanceOf(InServiceApparatus.class);
            assertThat(inService.id()).isEqualTo(id);
            assertThat(inService.status()).isEqualTo(ApparatusStatus.IN_SERVICE);
            assertThat(inService.notes()).isNull(); // Notes cleared on activation
        }

        @Test
        void decommission_transitions_to_DecommissionedApparatus() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Backup"
            );

            var decommissioned = apparatus.decommission("Age limit reached");

            assertThat(decommissioned).isInstanceOf(DecommissionedApparatus.class);
            assertThat(decommissioned.id()).isEqualTo(id);
            assertThat(decommissioned.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
            assertThat(decommissioned.notes()).isEqualTo("Age limit reached");
        }

        @Test
        void activate_preserves_vehicle_details() {
            var barcode = new Barcode("APP-RES001");
            var apparatus = new ReserveApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, barcode, "Backup"
            );

            var inService = apparatus.activate();

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
        void assignToStation_updates_station() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );
            var newStationId = StationId.generate();

            var updated = apparatus.assignToStation(newStationId);

            assertThat(updated.stationId()).isEqualTo(newStationId);
            assertThat(updated).isNotSameAs(apparatus);
        }

        @Test
        void withNotes_updates_notes() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Original"
            );

            var updated = apparatus.withNotes("Updated notes");

            assertThat(updated.notes()).isEqualTo("Updated notes");
            assertThat(updated).isNotSameAs(apparatus);
        }
    }

    @Nested
    @DisplayName("Apparatus interface methods")
    class ApparatusInterfaceMethods {

        @Test
        void isInService_returns_false() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isInService()).isFalse();
        }

        @Test
        void isDecommissioned_returns_false() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isDecommissioned()).isFalse();
        }

        @Test
        void canHaveInventoryChecks_returns_true() {
            var apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }
    }
}
