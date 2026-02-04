package com.example.firestock.inventorycheck;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.primitives.ids.*;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.*;
import com.example.firestock.security.TestSecurityUtils;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ShiftInventoryCheckServiceTest {

    @Autowired
    private ShiftInventoryCheckService service;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private StationId otherStationId;
    private ApparatusId testApparatusId;
    private CompartmentId testCompartmentId;
    private EquipmentItemId testEquipmentItemId;
    private EquipmentTypeId testEquipmentTypeId;
    private UserId testUserId;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAuthentication();
    }

    @BeforeEach
    void setUp() {
        // Clean up test data in reverse dependency order
        create.deleteFrom(INVENTORY_CHECK_ITEM).execute();
        create.deleteFrom(ISSUE).execute();
        create.deleteFrom(INVENTORY_CHECK).execute();
        create.deleteFrom(EQUIPMENT_ITEM).execute();
        create.deleteFrom(CONSUMABLE_STOCK).execute();
        create.deleteFrom(MANIFEST_ENTRY).execute();
        create.deleteFrom(COMPARTMENT).execute();
        create.deleteFrom(APPARATUS).execute();
        create.deleteFrom(USER_STATION_ASSIGNMENT).execute();
        create.deleteFrom(APP_USER).execute();
        create.deleteFrom(STATION).execute();
        create.deleteFrom(EQUIPMENT_TYPE).execute();

        // Create test data
        testStationId = StationId.generate();
        create.insertInto(STATION)
            .set(STATION.ID, testStationId)
            .set(STATION.CODE, new com.example.firestock.domain.primitives.strings.StationCode("ST01"))
            .set(STATION.NAME, "Test Station")
            .execute();

        // Create a second station for access denial tests
        otherStationId = StationId.generate();
        create.insertInto(STATION)
            .set(STATION.ID, otherStationId)
            .set(STATION.CODE, new com.example.firestock.domain.primitives.strings.StationCode("ST02"))
            .set(STATION.NAME, "Other Station")
            .execute();

        // Authenticate as firefighter assigned to test station
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        testUserId = UserId.generate();
        create.insertInto(APP_USER)
            .set(APP_USER.ID, testUserId)
            .set(APP_USER.BADGE_NUMBER, new com.example.firestock.domain.primitives.strings.BadgeNumber("B001"))
            .set(APP_USER.FIRST_NAME, "Test")
            .set(APP_USER.LAST_NAME, "User")
            .set(APP_USER.EMAIL, new com.example.firestock.domain.primitives.strings.EmailAddress("test@example.com"))
            .set(APP_USER.ROLE, UserRole.FIREFIGHTER)
            .execute();

        testApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, testApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 1"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, testStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        testCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, testCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
            .set(COMPARTMENT.CODE, "L1")
            .set(COMPARTMENT.NAME, "Left Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.DRIVER_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        testEquipmentTypeId = EquipmentTypeId.generate();
        create.insertInto(EQUIPMENT_TYPE)
            .set(EQUIPMENT_TYPE.ID, testEquipmentTypeId)
            .set(EQUIPMENT_TYPE.CODE, "HALLIGAN")
            .set(EQUIPMENT_TYPE.NAME, "Halligan Bar")
            .set(EQUIPMENT_TYPE.CATEGORY, EquipmentCategory.TOOLS_HAND)
            .set(EQUIPMENT_TYPE.TRACKING_METHOD, TrackingMethod.SERIALIZED)
            .execute();

        testEquipmentItemId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, testEquipmentItemId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, testApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, testCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();
    }

    @Test
    void startCheck_createsNewCheck_whenNoActiveCheckExists() {
        var summary = service.startCheck(testApparatusId, testUserId);

        assertNotNull(summary);
        assertNotNull(summary.id());
        assertEquals(testApparatusId, summary.apparatusId());
        assertEquals(CheckStatus.IN_PROGRESS, summary.status());
        assertEquals(1, summary.totalItems()); // One equipment item
        assertEquals(0, summary.verifiedCount());
        assertEquals(0, summary.issuesFoundCount());
    }

    @Test
    void startCheck_throwsException_whenActiveCheckExists() {
        // Start a check
        service.startCheck(testApparatusId, testUserId);

        // Try to start another - should fail (BR-01)
        assertThrows(ShiftInventoryCheckService.ActiveCheckExistsException.class, () ->
            service.startCheck(testApparatusId, testUserId)
        );
    }

    @Test
    void verifyItem_recordsVerification_whenItemIsPresent() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        var request = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.PRESENT,
            null,
            null,
            null
        );

        service.verifyItem(request, testUserId);

        var updatedCheck = service.getCheck(checkSummary.id());
        assertEquals(1, updatedCheck.verifiedCount());
        assertEquals(0, updatedCheck.issuesFoundCount());
    }

    @Test
    void verifyItem_createsIssue_whenStatusIsMissing() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        var request = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.MISSING,
            "Item not found in compartment",
            null,
            null
        );

        service.verifyItem(request, testUserId);

        var updatedCheck = service.getCheck(checkSummary.id());
        assertEquals(1, updatedCheck.verifiedCount());
        assertEquals(1, updatedCheck.issuesFoundCount());

        // Verify issue was created
        var issues = create.selectFrom(ISSUE)
            .where(ISSUE.APPARATUS_ID.eq(testApparatusId))
            .and(ISSUE.CATEGORY.eq(IssueCategory.MISSING))
            .fetch();
        assertEquals(1, issues.size());

        // Verify equipment status was updated
        var equipment = create.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.ID.eq(testEquipmentItemId))
            .fetchOne();
        assertEquals(EquipmentStatus.MISSING, equipment.getStatus());
    }

    @Test
    void verifyItem_createsIssue_whenStatusIsDamaged() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        var request = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.PRESENT_DAMAGED,
            "Handle is cracked",
            null,
            null
        );

        service.verifyItem(request, testUserId);

        var updatedCheck = service.getCheck(checkSummary.id());
        assertEquals(1, updatedCheck.issuesFoundCount());

        // Verify issue was created
        var issues = create.selectFrom(ISSUE)
            .where(ISSUE.APPARATUS_ID.eq(testApparatusId))
            .and(ISSUE.CATEGORY.eq(IssueCategory.DAMAGE))
            .fetch();
        assertEquals(1, issues.size());

        // Verify equipment status was updated
        var equipment = create.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.ID.eq(testEquipmentItemId))
            .fetchOne();
        assertEquals(EquipmentStatus.DAMAGED, equipment.getStatus());
    }

    @Test
    void verifyItem_throwsException_whenItemAlreadyVerified() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        var request = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.PRESENT,
            null,
            null,
            null
        );

        // First verification
        service.verifyItem(request, testUserId);

        // Second verification should fail
        assertThrows(ShiftInventoryCheckService.ItemAlreadyVerifiedException.class, () ->
            service.verifyItem(request, testUserId)
        );
    }

    @Test
    void completeCheck_throwsException_whenNotAllItemsVerified() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        // Try to complete without verifying (BR-02)
        assertThrows(ShiftInventoryCheckService.IncompleteCheckException.class, () ->
            service.completeCheck(checkSummary.id())
        );
    }

    @Test
    void completeCheck_succeeds_whenAllItemsVerified() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        // Verify the item
        var request = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.PRESENT,
            null,
            null,
            null
        );
        service.verifyItem(request, testUserId);

        // Now complete
        var completedCheck = service.completeCheck(checkSummary.id());

        assertEquals(CheckStatus.COMPLETED, completedCheck.status());
        assertNotNull(completedCheck.completedAt());
    }

    @Test
    void abandonCheck_marksCheckAsAbandoned() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        service.abandonCheck(checkSummary.id());

        var abandonedCheck = service.getCheck(checkSummary.id());
        assertEquals(CheckStatus.ABANDONED, abandonedCheck.status());
    }

    @Test
    void abandonCheck_allowsNewCheckToBeStarted() {
        var firstCheck = service.startCheck(testApparatusId, testUserId);

        service.abandonCheck(firstCheck.id());

        // Should be able to start a new check
        var secondCheck = service.startCheck(testApparatusId, testUserId);
        assertNotNull(secondCheck);
        assertNotEquals(firstCheck.id(), secondCheck.id());
    }

    @Test
    void getActiveCheck_returnsEmpty_whenNoActiveCheck() {
        var result = service.getActiveCheck(testApparatusId);
        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveCheck_returnsCheck_whenActiveCheckExists() {
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        var result = service.getActiveCheck(testApparatusId);

        assertTrue(result.isPresent());
        assertEquals(checkSummary.id(), result.get().id());
    }

    @Test
    void getApparatusForStation_returnsApparatusList() {
        var result = service.getApparatusForStation(testStationId);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testApparatusId, result.getFirst().id());
        assertEquals("Test Station", result.getFirst().stationName());
    }

    @Test
    void getApparatusDetails_returnsCompartmentsAndItems() {
        var result = service.getApparatusDetails(testApparatusId);

        assertEquals(testApparatusId, result.id());
        assertEquals(1, result.compartments().size());
        assertEquals(1, result.compartments().getFirst().items().size());
        assertEquals(1, result.totalItemCount());
    }

    @Test
    void verifyItem_throwsException_forQuantityDiscrepancyWithoutNotes() {
        // Create a consumable stock entry
        var consumableStockId = ConsumableStockId.generate();
        create.insertInto(CONSUMABLE_STOCK)
            .set(CONSUMABLE_STOCK.ID, consumableStockId)
            .set(CONSUMABLE_STOCK.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(CONSUMABLE_STOCK.APPARATUS_ID, testApparatusId)
            .set(CONSUMABLE_STOCK.COMPARTMENT_ID, testCompartmentId)
            .set(CONSUMABLE_STOCK.QUANTITY, Quantity.of(10))
            .set(CONSUMABLE_STOCK.REQUIRED_QUANTITY, new BigDecimal("10"))
            .execute();

        // Update apparatus details to include the consumable
        var checkSummary = service.startCheck(testApparatusId, testUserId);

        // Verify equipment first
        var equipmentRequest = new ItemVerificationRequest(
            checkSummary.id(),
            testEquipmentItemId,
            null,
            testCompartmentId,
            null,
            VerificationStatus.PRESENT,
            null,
            null,
            null
        );
        service.verifyItem(equipmentRequest, testUserId);

        // Try to verify consumable with >20% discrepancy without notes (BR-05)
        var consumableRequest = new ItemVerificationRequest(
            checkSummary.id(),
            null,
            consumableStockId,
            testCompartmentId,
            null,
            VerificationStatus.LOW_QUANTITY,
            null, // No notes
            Quantity.of(5), // 50% less than expected
            Quantity.of(10)
        );

        assertThrows(ShiftInventoryCheckService.QuantityDiscrepancyRequiresNotesException.class, () ->
            service.verifyItem(consumableRequest, testUserId)
        );
    }

    // ==================== Authorization Tests ====================

    @Test
    void getApparatusForStation_allowsFirefighterForAssignedStation() {
        // Already authenticated as firefighter with testStationId in setUp
        var result = service.getApparatusForStation(testStationId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getApparatusForStation_deniesFirefighterForUnassignedStation() {
        // Firefighter is assigned to testStationId, not otherStationId
        assertThrows(AccessDeniedException.class, () ->
            service.getApparatusForStation(otherStationId)
        );
    }

    @Test
    void getApparatusForStation_allowsMaintenanceTechnicianForAnyStation() {
        TestSecurityUtils.authenticateAsMaintenance();

        // Should be able to access test station
        var result1 = service.getApparatusForStation(testStationId);
        assertNotNull(result1);

        // Should also be able to access other station
        var result2 = service.getApparatusForStation(otherStationId);
        assertNotNull(result2);
    }

    @Test
    void getApparatusForStation_allowsAdminForAnyStation() {
        TestSecurityUtils.authenticateAsAdmin();

        // Should be able to access test station
        var result1 = service.getApparatusForStation(testStationId);
        assertNotNull(result1);

        // Should also be able to access other station
        var result2 = service.getApparatusForStation(otherStationId);
        assertNotNull(result2);
    }

    @Test
    void startCheck_deniesFirefighterForUnassignedStation() {
        // Create apparatus at other station
        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 2"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        // Firefighter is assigned to testStationId, not otherStationId
        assertThrows(AccessDeniedException.class, () ->
            service.startCheck(otherApparatusId, testUserId)
        );
    }

    @Test
    void getApparatusDetails_deniesFirefighterForUnassignedStation() {
        // Create apparatus at other station
        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 3"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        // Firefighter is assigned to testStationId, not otherStationId
        assertThrows(AccessDeniedException.class, () ->
            service.getApparatusDetails(otherApparatusId)
        );
    }

    @Test
    void verifyItem_deniesFirefighterForUnassignedStation() {
        // Create apparatus at other station and start check as maintenance
        TestSecurityUtils.authenticateAsMaintenance();

        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 4"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        var otherCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, otherCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, otherApparatusId)
            .set(COMPARTMENT.CODE, "L1")
            .set(COMPARTMENT.NAME, "Left Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.DRIVER_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        var otherEquipmentItemId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, otherEquipmentItemId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, otherApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, otherCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        // Start check as maintenance (cross-station access)
        var checkSummary = service.startCheck(otherApparatusId, testUserId);

        // Now switch to firefighter without access to other station
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        var request = new ItemVerificationRequest(
            checkSummary.id(),
            otherEquipmentItemId,
            null,
            otherCompartmentId,
            null,
            VerificationStatus.PRESENT,
            null,
            null,
            null
        );

        // Should be denied
        assertThrows(AccessDeniedException.class, () ->
            service.verifyItem(request, testUserId)
        );
    }

    @Test
    void completeCheck_deniesFirefighterForUnassignedStation() {
        // Create check at other station as maintenance
        TestSecurityUtils.authenticateAsMaintenance();

        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 5"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        // No items, so check has 0 total items
        var checkSummary = service.startCheck(otherApparatusId, testUserId);

        // Switch to firefighter without access
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        assertThrows(AccessDeniedException.class, () ->
            service.completeCheck(checkSummary.id())
        );
    }

    @Test
    void abandonCheck_deniesFirefighterForUnassignedStation() {
        // Create check at other station as maintenance
        TestSecurityUtils.authenticateAsMaintenance();

        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 6"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        var checkSummary = service.startCheck(otherApparatusId, testUserId);

        // Switch to firefighter without access
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        assertThrows(AccessDeniedException.class, () ->
            service.abandonCheck(checkSummary.id())
        );
    }
}
