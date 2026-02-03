package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DecommissionedApparatus} demonstrating retired apparatus behaviour.
 *
 * <p>DecommissionedApparatus is a terminal state for apparatus that has been
 * permanently retired from service. It cannot transition to any other state.
 */
@DisplayName("DecommissionedApparatus")
class DecommissionedApparatusTest {

    private final ApparatusId id = ApparatusId.generate();
    private final UnitNumber unitNumber = new UnitNumber("Former Engine 5");
    private final ApparatusType type = ApparatusType.ENGINE;
    private final StationId stationId = StationId.generate();

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void creates_with_required_fields() {
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Age limit"
            );

            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
            assertThat(apparatus.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
            assertThat(apparatus.notes()).isEqualTo("Age limit");
        }

        @Test
        void creates_with_all_fields_for_historical_record() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", 2010,
                    stationId, barcode, "Retired after 15 years of service"
            );

            assertThat(apparatus.vin()).isEqualTo("VIN123");
            assertThat(apparatus.make()).isEqualTo("Pierce");
            assertThat(apparatus.model()).isEqualTo("Arrow");
            assertThat(apparatus.year()).isEqualTo(2010);
            assertThat(apparatus.barcode()).isEqualTo(barcode);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new DecommissionedApparatus(
                    null, unitNumber, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_unit_number() {
            assertThatThrownBy(() -> new DecommissionedApparatus(
                    id, null, null, type, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Unit number cannot be null");
        }

        @Test
        void rejects_null_type() {
            assertThatThrownBy(() -> new DecommissionedApparatus(
                    id, unitNumber, null, null, null, null, null, stationId, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus type cannot be null");
        }

        @Test
        void rejects_null_station_id() {
            assertThatThrownBy(() -> new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Station ID cannot be null");
        }
    }

    @Nested
    @DisplayName("Terminal state (no transitions)")
    class TerminalState {

        @Test
        void is_terminal_state_with_no_state_transitions() {
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, "Retired"
            );

            // DecommissionedApparatus has no state transition methods
            // This test documents that it's intentionally a terminal state
            assertThat(apparatus.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
            assertThat(apparatus.isDecommissioned()).isTrue();
        }
    }

    @Nested
    @DisplayName("Apparatus interface methods")
    class ApparatusInterfaceMethods {

        @Test
        void isInService_returns_false() {
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isInService()).isFalse();
        }

        @Test
        void isDecommissioned_returns_true() {
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.isDecommissioned()).isTrue();
        }

        @Test
        void canHaveInventoryChecks_returns_false() {
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isFalse();
        }
    }

    @Nested
    @DisplayName("Historical record preservation")
    class HistoricalRecordPreservation {

        @Test
        void preserves_all_historical_data() {
            var barcode = new Barcode("APP-ENG005");
            var apparatus = new DecommissionedApparatus(
                    id, unitNumber, "VIN123", type, "Pierce", "Arrow", 2010,
                    stationId, barcode, "Sold at auction"
            );

            // All data is preserved for historical reference
            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.vin()).isEqualTo("VIN123");
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.make()).isEqualTo("Pierce");
            assertThat(apparatus.model()).isEqualTo("Arrow");
            assertThat(apparatus.year()).isEqualTo(2010);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
            assertThat(apparatus.barcode()).isEqualTo(barcode);
            assertThat(apparatus.notes()).isEqualTo("Sold at auction");
        }
    }
}
