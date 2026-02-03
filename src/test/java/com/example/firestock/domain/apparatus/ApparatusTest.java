package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Apparatus} sealed interface demonstrating the state-based pattern.
 *
 * <p>The Apparatus sealed interface enforces that all apparatus must be in one of
 * four defined states: InService, OutOfService, Reserve, or Decommissioned.
 */
@DisplayName("Apparatus (sealed interface)")
class ApparatusTest {

    private final ApparatusId id = ApparatusId.generate();
    private final UnitNumber unitNumber = new UnitNumber("Engine 5");
    private final ApparatusType type = ApparatusType.ENGINE;
    private final StationId stationId = StationId.generate();

    @Nested
    @DisplayName("Sealed interface pattern")
    class SealedInterfacePattern {

        @Test
        void all_implementations_extend_Apparatus() {
            Apparatus inService = InServiceApparatus.create(id, unitNumber, type, stationId);
            Apparatus outOfService = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );
            Apparatus reserve = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );
            Apparatus decommissioned = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(inService).isInstanceOf(Apparatus.class);
            assertThat(outOfService).isInstanceOf(Apparatus.class);
            assertThat(reserve).isInstanceOf(Apparatus.class);
            assertThat(decommissioned).isInstanceOf(Apparatus.class);
        }

        @Test
        void pattern_matching_allows_specific_handling() {
            Apparatus apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            String result = switch (apparatus) {
                case InServiceApparatus a -> "In service: " + a.unitNumber();
                case OutOfServiceApparatus a -> "Out of service: " + a.unitNumber();
                case ReserveApparatus a -> "Reserve: " + a.unitNumber();
                case DecommissionedApparatus a -> "Decommissioned: " + a.unitNumber();
            };

            assertThat(result).isEqualTo("In service: Engine 5");
        }

        @Test
        void can_handle_all_apparatus_with_base_type() {
            Apparatus apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            // Common operations work on base type
            assertThat(apparatus.id()).isEqualTo(id);
            assertThat(apparatus.unitNumber()).isEqualTo(unitNumber);
            assertThat(apparatus.type()).isEqualTo(type);
            assertThat(apparatus.stationId()).isEqualTo(stationId);
        }
    }

    @Nested
    @DisplayName("State lifecycle")
    class StateLifecycle {

        @Test
        void complete_lifecycle_from_in_service_to_decommissioned() {
            // Start in service
            InServiceApparatus inService = InServiceApparatus.create(id, unitNumber, type, stationId);
            assertThat(inService.status()).isEqualTo(ApparatusStatus.IN_SERVICE);

            // Take out of service for maintenance
            OutOfServiceApparatus outOfService = inService.putOutOfService("Scheduled maintenance");
            assertThat(outOfService.status()).isEqualTo(ApparatusStatus.OUT_OF_SERVICE);

            // Return to service
            InServiceApparatus returnedToService = outOfService.returnToService();
            assertThat(returnedToService.status()).isEqualTo(ApparatusStatus.IN_SERVICE);

            // Move to reserve
            ReserveApparatus reserve = returnedToService.moveToReserve("Replaced by newer unit");
            assertThat(reserve.status()).isEqualTo(ApparatusStatus.RESERVE);

            // Decommission
            DecommissionedApparatus decommissioned = reserve.decommission("Age limit reached");
            assertThat(decommissioned.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
        }

        @Test
        void can_decommission_directly_from_any_active_state() {
            InServiceApparatus inService = InServiceApparatus.create(id, unitNumber, type, stationId);

            // Can decommission from in-service
            DecommissionedApparatus fromInService = inService.decommission("Total loss");
            assertThat(fromInService.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);

            // Can decommission from out-of-service
            OutOfServiceApparatus outOfService = new OutOfServiceApparatus(
                    ApparatusId.generate(), unitNumber, null, type, null, null, null, stationId, null, null
            );
            DecommissionedApparatus fromOutOfService = outOfService.decommission("Not repairable");
            assertThat(fromOutOfService.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);

            // Can decommission from reserve
            ReserveApparatus reserve = new ReserveApparatus(
                    ApparatusId.generate(), unitNumber, null, type, null, null, null, stationId, null, null
            );
            DecommissionedApparatus fromReserve = reserve.decommission("Budget cuts");
            assertThat(fromReserve.status()).isEqualTo(ApparatusStatus.DECOMMISSIONED);
        }
    }

    @Nested
    @DisplayName("Status-based inventory eligibility")
    class InventoryEligibility {

        @Test
        void in_service_can_have_inventory_checks() {
            Apparatus apparatus = InServiceApparatus.create(id, unitNumber, type, stationId);

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }

        @Test
        void out_of_service_can_have_inventory_checks() {
            Apparatus apparatus = new OutOfServiceApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }

        @Test
        void reserve_can_have_inventory_checks() {
            Apparatus apparatus = new ReserveApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isTrue();
        }

        @Test
        void decommissioned_cannot_have_inventory_checks() {
            Apparatus apparatus = new DecommissionedApparatus(
                    id, unitNumber, null, type, null, null, null, stationId, null, null
            );

            assertThat(apparatus.canHaveInventoryChecks()).isFalse();
        }
    }
}
