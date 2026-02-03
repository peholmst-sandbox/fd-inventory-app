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
 * Tests for {@link InServiceApparatus} demonstrating active apparatus behaviour.
 *
 * <p>InServiceApparatus is the primary state for apparatus that can respond to
 * incidents and requires regular inventory checks and audits.
 */
@DisplayName("InServiceApparatus")
class InServiceApparatusTest {

    private final ApparatusId id = ApparatusId.generate();
    private final UnitNumber unitNumber = new UnitNumber("Engine 5");
    private final ApparatusType type = ApparatusType.ENGINE;
    private final StationId stationId = StationId.generate();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void creates_with_required_fields_only() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
            assertThat(apparatus.status()).isEqualTo(ApparatusStatus.IN_SERVICE);
        }

        @Test
        void creates_with_null_optional_fields() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.vin()).isNull();
            assertThat(apparatus.make()).isNull();
            assertThat(apparatus.model()).isNull();
            assertThat(apparatus.year()).isNull();
            assertThat(apparatus.barcode()).isNull();
            assertThat(apparatus.notes()).isNull();
        }

        @Test
        void creates_with_all_fields() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new InServiceApparatus(
                    id, unitNumber, "1FDXE4FS7FDA12345", type,
                    "Pierce", "Arrow XT", Year.of(2020), stationId, barcode, "Primary engine"
            );

            assertThat(apparatus.vin()).isEqualTo("1FDXE4FS7FDA12345");
            assertThat(apparatus.make()).isEqualTo("Pierce");
            assertThat(apparatus.model()).isEqualTo("Arrow XT");
            assertThat(apparatus.year()).isEqualTo(Year.of(2020));
            assertThat(apparatus.barcode()).isEqualTo(barcode);
            assertThat(apparatus.notes()).isEqualTo("Primary engine");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new InServiceApparatus(
                    null, unitNumber, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_unit_number() {
            assertThatThrownBy(() -> new InServiceApparatus(
                    id, null, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Unit number cannot be null");
        }

        @Test
        void rejects_null_type() {
            assertThatThrownBy(() -> new InServiceApparatus(
                    id, unitNumber, null, null, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus type cannot be null");
        }

        @Test
        void rejects_null_station_id() {
            assertThatThrownBy(() -> new InServiceApparatus(
                    id, unitNumber, null, type, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Station ID cannot be null");
        }
    }

    @Nested
    @DisplayName("State transitions")
    class StateTransitions {

        @Test
        void putOutOfService_transitions_to_OutOfServiceApparatus() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            var outOfService = apparatus.putOutOfService("Engine failure - pump malfunction");

            assertThat(outOfService).isInstanceOf(OutOfServiceApparatus.class);
            assertThat(outOfService.id()).isEqualTo(id);
            assertThat(outOfService.unitNumber()).isEqualTo(unitNumber);
            assertThat(outOfService.status()).isEqualTo(ApparatusStatus.OUT_OF_SERVICE);
            assertThat(outOfService.notes()).isEqualTo("Engine failure - pump malfunction");
        }

        @Test
        void moveToReserve_transitions_to_ReserveApparatus() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            var reserve = apparatus.moveToReserve("Replaced by new engine");

            assertThat(reserve).isInstanceOf(ReserveApparatus.class);
            assertThat(reserve.id()).isEqualTo(id);
            assertThat(reserve.status()).isEqualTo(ApparatusStatus.RESERVE);
            assertThat(reserve.notes()).isEqualTo("Replaced by new engine");
        }

        @Test
        void decommission_transitions_to_DecommissionedApparatus() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            var decommissioned = apparatus.decommission("Beyond economical repair");

            assertThat(decommissioned).isInstanceOf(DecommissionedApparatus.class);
            assertThat(decommissioned.id()).isEqualTo(id);
            assertThat(decommissioned.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
            assertThat(decommissioned.notes()).isEqualTo("Beyond economical repair");
        }

        @Test
        void state_transitions_preserve_all_fields() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new InServiceApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, barcode, "Original notes"
            );

            var outOfService = apparatus.putOutOfService("New notes");

            assertThat(outOfService.vin()).isEqualTo("VIN123");
            assertThat(outOfService.make()).isEqualTo("Pierce");
            assertThat(outOfService.model()).isEqualTo("Arrow");
            assertThat(outOfService.year()).isEqualTo(Year.of(2020));
            assertThat(outOfService.barcode()).isEqualTo(barcode);
        }
    }

    @Nested
    @DisplayName("Update methods")
    class UpdateMethods {

        @Test
        void withVehicleDetails_updates_vehicle_info() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            var updated = apparatus.withVehicleDetails("VIN123", "Pierce", "Arrow XT", Year.of(2020));

            assertThat(updated.vin()).isEqualTo("VIN123");
            assertThat(updated.make()).isEqualTo("Pierce");
            assertThat(updated.model()).isEqualTo("Arrow XT");
            assertThat(updated.year()).isEqualTo(Year.of(2020));
            assertThat(updated).isNotSameAs(apparatus);
        }

        @Test
        void withBarcode_updates_barcode() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);
            var barcode = new Barcode("APP-ENG005");

            var updated = apparatus.withBarcode(barcode);

            assertThat(updated.barcode()).isEqualTo(barcode);
            assertThat(updated).isNotSameAs(apparatus);
        }

        @Test
        void assignToStation_updates_station() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);
            var newStationId = StationId.generate();

            var updated = apparatus.assignToStation(newStationId);

            assertThat(updated.stationId()).isEqualTo(newStationId);
            assertThat(updated).isNotSameAs(apparatus);
        }

        @Test
        void withNotes_updates_notes() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            var updated = apparatus.withNotes("Updated notes");

            assertThat(updated.notes()).isEqualTo("Updated notes");
            assertThat(updated).isNotSameAs(apparatus);
        }
    }

    @Nested
    @DisplayName("Apparatus interface methods")
    class ApparatusInterfaceMethods {

        @Test
        void isInService_returns_true() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.isInService()).isTrue();
        }

        @Test
        void isDecommissioned_returns_false() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.isDecommissioned()).isFalse();
        }

        @Test
        void canHaveInventoryChecks_returns_true() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }
    }

    @Nested
    @DisplayName("Optional field accessors")
    class OptionalAccessors {

        @Test
        void optional_methods_return_empty_for_null_values() {
            var apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.vinOpt()).isEmpty();
            assertThat(apparatus.makeOpt()).isEmpty();
            assertThat(apparatus.modelOpt()).isEmpty();
            assertThat(apparatus.yearOpt()).isEmpty();
            assertThat(apparatus.barcodeOpt()).isEmpty();
            assertThat(apparatus.notesOpt()).isEmpty();
        }

        @Test
        void optional_methods_return_values_when_present() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new InServiceApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", Year.of(2020),
                    stationId, barcode, "Notes"
            );

            assertThat(apparatus.vinOpt()).contains("VIN123");
            assertThat(apparatus.makeOpt()).contains("Pierce");
            assertThat(apparatus.modelOpt()).contains("Arrow");
            assertThat(apparatus.yearOpt()).contains(Year.of(2020));
            assertThat(apparatus.barcodeOpt()).contains(barcode);
            assertThat(apparatus.notesOpt()).contains("Notes");
        }
    }
}
