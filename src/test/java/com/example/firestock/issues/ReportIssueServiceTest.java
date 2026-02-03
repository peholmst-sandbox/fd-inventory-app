package com.example.firestock.issues;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.primitives.ids.*;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import com.example.firestock.domain.primitives.strings.StationCode;
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

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReportIssueServiceTest {

    @Autowired
    private ReportIssueService service;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private StationId otherStationId;
    private ApparatusId testApparatusId;
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
            .set(STATION.CODE, new StationCode("ST01"))
            .set(STATION.NAME, "Test Station")
            .execute();

        // Create a second station for access denial tests
        otherStationId = StationId.generate();
        create.insertInto(STATION)
            .set(STATION.ID, otherStationId)
            .set(STATION.CODE, new StationCode("ST02"))
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

        var compartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, compartmentId)
            .set(COMPARTMENT.APPARATUS_ID, testApparatusId)
            .set(COMPARTMENT.CODE, "L1")
            .set(COMPARTMENT.NAME, "Left Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.LEFT_SIDE)
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
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, compartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .set(EQUIPMENT_ITEM.SERIAL_NUMBER, new SerialNumber("SN-12345"))
            .set(EQUIPMENT_ITEM.BARCODE, new Barcode("BC-12345"))
            .execute();
    }

    @Test
    void reportIssue_createsIssueWithCorrectFields() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "The handle is cracked and needs replacement",
            IssueSeverity.MEDIUM,
            false
        );

        var result = service.reportIssue(request, testUserId);

        assertNotNull(result);
        assertNotNull(result.issueId());
        assertNotNull(result.referenceNumber());
        assertTrue(result.referenceNumber().value().startsWith("ISS-"));

        // Verify issue was created in database
        var issue = create.selectFrom(ISSUE)
            .where(ISSUE.ID.eq(result.issueId()))
            .fetchOne();

        assertNotNull(issue);
        assertEquals(testEquipmentItemId, issue.getEquipmentItemId());
        assertEquals(IssueCategory.DAMAGE, issue.getCategory());
        assertEquals(IssueSeverity.MEDIUM, issue.getSeverity());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertTrue(issue.getDescription().contains("cracked"));
    }

    @Test
    void reportIssue_updatesEquipmentStatus_whenDamaged() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "Equipment is damaged and unusable",
            IssueSeverity.HIGH,
            false
        );

        var result = service.reportIssue(request, testUserId);

        assertEquals(EquipmentStatus.DAMAGED, result.updatedEquipmentStatus());

        // Verify equipment status was updated in database
        var equipment = create.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.ID.eq(testEquipmentItemId))
            .fetchOne();

        assertEquals(EquipmentStatus.DAMAGED, equipment.getStatus());
    }

    @Test
    void reportIssue_updatesEquipmentStatus_whenMissing() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.MISSING,
            "Equipment cannot be found in the compartment",
            IssueSeverity.HIGH,
            false
        );

        var result = service.reportIssue(request, testUserId);

        assertEquals(EquipmentStatus.MISSING, result.updatedEquipmentStatus());

        // Verify equipment status was updated in database
        var equipment = create.selectFrom(EQUIPMENT_ITEM)
            .where(EQUIPMENT_ITEM.ID.eq(testEquipmentItemId))
            .fetchOne();

        assertEquals(EquipmentStatus.MISSING, equipment.getStatus());
    }

    @Test
    void reportIssue_throwsException_whenDescriptionTooShort() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "Short", // Less than 10 characters
            IssueSeverity.MEDIUM,
            false
        );

        assertThrows(IllegalArgumentException.class, () ->
            service.reportIssue(request, testUserId)
        );
    }

    @Test
    void reportIssue_throwsException_whenCriticalNotConfirmed() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "This is a critical safety issue with the equipment",
            IssueSeverity.CRITICAL,
            false // Not confirmed
        );

        assertThrows(IllegalArgumentException.class, () ->
            service.reportIssue(request, testUserId)
        );
    }

    @Test
    void reportIssue_succeeds_whenCriticalIsConfirmed() {
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "This is a critical safety issue with the equipment",
            IssueSeverity.CRITICAL,
            true // Confirmed
        );

        var result = service.reportIssue(request, testUserId);

        assertNotNull(result);
        assertNotNull(result.issueId());

        // Verify severity was set correctly
        var issue = create.selectFrom(ISSUE)
            .where(ISSUE.ID.eq(result.issueId()))
            .fetchOne();

        assertEquals(IssueSeverity.CRITICAL, issue.getSeverity());
    }

    @Test
    void reportIssue_deniesFirefighter_forOtherStationEquipment() {
        // Create equipment at other station
        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 2"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        var otherCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, otherCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, otherApparatusId)
            .set(COMPARTMENT.CODE, "R1")
            .set(COMPARTMENT.NAME, "Right Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.RIGHT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        var otherEquipmentId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, otherEquipmentId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, otherApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, otherCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        var request = new ReportIssueRequest(
            otherEquipmentId,
            IssueCategory.DAMAGE,
            "This equipment is damaged",
            IssueSeverity.MEDIUM,
            false
        );

        // Firefighter is assigned to testStationId, not otherStationId
        assertThrows(AccessDeniedException.class, () ->
            service.reportIssue(request, testUserId)
        );
    }

    @Test
    void reportIssue_allowsMaintenanceTech_forAnyEquipment() {
        TestSecurityUtils.authenticateAsMaintenance();

        // Create equipment at other station
        var otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
            .set(APPARATUS.ID, otherApparatusId)
            .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 3"))
            .set(APPARATUS.TYPE, ApparatusType.ENGINE)
            .set(APPARATUS.STATION_ID, otherStationId)
            .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
            .execute();

        var otherCompartmentId = CompartmentId.generate();
        create.insertInto(COMPARTMENT)
            .set(COMPARTMENT.ID, otherCompartmentId)
            .set(COMPARTMENT.APPARATUS_ID, otherApparatusId)
            .set(COMPARTMENT.CODE, "R1")
            .set(COMPARTMENT.NAME, "Right Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.RIGHT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        var otherEquipmentId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, otherEquipmentId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, otherApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, otherCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        var request = new ReportIssueRequest(
            otherEquipmentId,
            IssueCategory.DAMAGE,
            "This equipment is damaged",
            IssueSeverity.MEDIUM,
            false
        );

        // Maintenance technician should be able to report for any station
        var result = service.reportIssue(request, testUserId);

        assertNotNull(result);
        assertNotNull(result.issueId());
    }

    @Test
    void getOpenIssues_returnsOnlyOpenIssues() {
        // Create an open issue
        var openIssueRequest = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "First issue - still open",
            IssueSeverity.MEDIUM,
            false
        );
        service.reportIssue(openIssueRequest, testUserId);

        // Create another issue and close it manually
        var closedIssueRequest = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.MALFUNCTION,
            "Second issue - will be closed",
            IssueSeverity.LOW,
            false
        );
        var closedResult = service.reportIssue(closedIssueRequest, testUserId);

        // Close the second issue
        create.update(ISSUE)
            .set(ISSUE.STATUS, IssueStatus.CLOSED)
            .where(ISSUE.ID.eq(closedResult.issueId()))
            .execute();

        // Get open issues
        var openIssues = service.getOpenIssues(testEquipmentItemId);

        // Should only return the open issue
        assertEquals(1, openIssues.size());
        assertEquals(IssueCategory.DAMAGE, openIssues.getFirst().category());
    }

    @Test
    void findByBarcode_returnsEquipment_whenFound() {
        var result = service.findByBarcode(new Barcode("BC-12345"));

        assertTrue(result.isPresent());
        assertEquals(testEquipmentItemId, result.get().id());
        assertEquals("Halligan Bar", result.get().name());
    }

    @Test
    void findByBarcode_returnsEmpty_whenNotFound() {
        var result = service.findByBarcode(new Barcode("NONEXISTENT"));

        assertTrue(result.isEmpty());
    }

    @Test
    void findBySerialNumber_returnsEquipment_whenFound() {
        var result = service.findBySerialNumber(new SerialNumber("SN-12345"));

        assertTrue(result.isPresent());
        assertEquals(testEquipmentItemId, result.get().id());
    }

    @Test
    void findBySerialNumber_returnsEmpty_whenNotFound() {
        var result = service.findBySerialNumber(new SerialNumber("NONEXISTENT"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getEquipmentForReport_returnsEquipment_withAccessControl() {
        var result = service.getEquipmentForReport(testEquipmentItemId);

        assertNotNull(result);
        assertEquals(testEquipmentItemId, result.id());
        assertEquals("Halligan Bar", result.name());
        assertEquals(testStationId, result.stationId());
        assertEquals("Test Station", result.stationName());
    }

    @Test
    void getEquipmentForReport_deniesAccess_forOtherStation() {
        // Create equipment at other station
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
            .set(COMPARTMENT.CODE, "R1")
            .set(COMPARTMENT.NAME, "Right Compartment 1")
            .set(COMPARTMENT.LOCATION, CompartmentLocation.RIGHT_SIDE)
            .set(COMPARTMENT.DISPLAY_ORDER, 1)
            .execute();

        var otherEquipmentId = EquipmentItemId.generate();
        create.insertInto(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.ID, otherEquipmentId)
            .set(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID, testEquipmentTypeId)
            .set(EQUIPMENT_ITEM.APPARATUS_ID, otherApparatusId)
            .set(EQUIPMENT_ITEM.COMPARTMENT_ID, otherCompartmentId)
            .set(EQUIPMENT_ITEM.STATUS, EquipmentStatus.OK)
            .execute();

        // Firefighter is assigned to testStationId, not otherStationId
        assertThrows(AccessDeniedException.class, () ->
            service.getEquipmentForReport(otherEquipmentId)
        );
    }

    @Test
    void addToExistingIssue_appendsNotesToDescription() {
        // Create an issue first
        var request = new ReportIssueRequest(
            testEquipmentItemId,
            IssueCategory.DAMAGE,
            "Original issue description",
            IssueSeverity.MEDIUM,
            false
        );
        var result = service.reportIssue(request, testUserId);

        // Add notes to the existing issue
        service.addToExistingIssue(result.issueId(), "Additional observation about the damage", testUserId);

        // Verify notes were appended
        var issue = create.selectFrom(ISSUE)
            .where(ISSUE.ID.eq(result.issueId()))
            .fetchOne();

        assertTrue(issue.getDescription().contains("Original issue description"));
        assertTrue(issue.getDescription().contains("Additional observation about the damage"));
        assertTrue(issue.getDescription().contains("Additional notes"));
    }
}
