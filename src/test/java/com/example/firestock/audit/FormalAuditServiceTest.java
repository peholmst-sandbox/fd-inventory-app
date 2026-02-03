package com.example.firestock.audit;

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
class FormalAuditServiceTest {

    @Autowired
    private FormalAuditService service;

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
        create.deleteFrom(FORMAL_AUDIT_ITEM).execute();
        create.deleteFrom(FORMAL_AUDIT).execute();
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

        // Authenticate as maintenance technician (required for formal audits)
        TestSecurityUtils.authenticateAsMaintenance();

        testUserId = UserId.generate();
        create.insertInto(APP_USER)
                .set(APP_USER.ID, testUserId)
                .set(APP_USER.BADGE_NUMBER, new com.example.firestock.domain.primitives.strings.BadgeNumber("M001"))
                .set(APP_USER.FIRST_NAME, "Test")
                .set(APP_USER.LAST_NAME, "Technician")
                .set(APP_USER.EMAIL, new com.example.firestock.domain.primitives.strings.EmailAddress("tech@example.com"))
                .set(APP_USER.ROLE, UserRole.MAINTENANCE_TECHNICIAN)
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
                .set(EQUIPMENT_TYPE.REQUIRES_TESTING, true)
                .set(EQUIPMENT_TYPE.TEST_INTERVAL_DAYS, 365)
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

    // ==================== BR-01: Only maintenance technicians can conduct audits ====================

    @Test
    void startAudit_deniesFirefighter() {
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        assertThrows(AccessDeniedException.class, () ->
                service.startAudit(testApparatusId, testUserId)
        );
    }

    @Test
    void startAudit_allowsMaintenanceTechnician() {
        TestSecurityUtils.authenticateAsMaintenance();

        var summary = service.startAudit(testApparatusId, testUserId);

        assertNotNull(summary);
        assertNotNull(summary.id());
        assertEquals(AuditStatus.IN_PROGRESS, summary.status());
    }

    // ==================== BR-02: Only one active audit per apparatus ====================

    @Test
    void startAudit_createsNewAudit_whenNoActiveAuditExists() {
        var summary = service.startAudit(testApparatusId, testUserId);

        assertNotNull(summary);
        assertNotNull(summary.id());
        assertEquals(testApparatusId, summary.apparatusId());
        assertEquals(AuditStatus.IN_PROGRESS, summary.status());
        assertEquals(1, summary.totalItems()); // One equipment item
        assertEquals(0, summary.auditedCount());
        assertEquals(0, summary.issuesFoundCount());
    }

    @Test
    void startAudit_throwsException_whenActiveAuditExists() {
        // Start an audit
        service.startAudit(testApparatusId, testUserId);

        // Try to start another - should fail (BR-02)
        assertThrows(FormalAuditService.ActiveAuditExistsException.class, () ->
                service.startAudit(testApparatusId, testUserId)
        );
    }

    // ==================== BR-03: All items must be audited before completion ====================

    @Test
    void completeAudit_throwsException_whenNotAllItemsAudited() {
        var summary = service.startAudit(testApparatusId, testUserId);

        // Try to complete without auditing (BR-03)
        assertThrows(FormalAuditService.IncompleteAuditException.class, () ->
                service.completeAudit(summary.id())
        );
    }

    @Test
    void completeAudit_succeeds_whenAllItemsAudited() {
        var summary = service.startAudit(testApparatusId, testUserId);

        // Audit the item
        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.VERIFIED,
                ItemCondition.GOOD,
                TestResult.PASSED,
                null,
                null,
                null,
                null,
                null,
                false
        );
        service.auditItem(request, testUserId);

        // Now complete
        var completedAudit = service.completeAudit(summary.id());

        assertEquals(AuditStatus.COMPLETED, completedAudit.status());
        assertNotNull(completedAudit.completedAt());
    }

    // ==================== BR-05: Issues created for FAILED_INSPECTION, MISSING, etc. ====================

    @Test
    void auditItem_createsIssue_whenStatusIsMissing() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.MISSING,
                null,
                null,
                null,
                "Item not found in compartment",
                null,
                null,
                null,
                false
        );

        service.auditItem(request, testUserId);

        var updatedAudit = service.getAudit(summary.id());
        assertEquals(1, updatedAudit.auditedCount());
        assertEquals(1, updatedAudit.issuesFoundCount());

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
    void auditItem_createsIssue_whenStatusIsFailedInspection() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.FAILED_INSPECTION,
                ItemCondition.POOR,
                TestResult.FAILED,
                null,
                "Handle is cracked",
                "Failed functional test - grip compromised",
                null,
                null,
                false
        );

        service.auditItem(request, testUserId);

        var updatedAudit = service.getAudit(summary.id());
        assertEquals(1, updatedAudit.issuesFoundCount());

        // Verify issue was created with MALFUNCTION category
        var issues = create.selectFrom(ISSUE)
                .where(ISSUE.APPARATUS_ID.eq(testApparatusId))
                .and(ISSUE.CATEGORY.eq(IssueCategory.MALFUNCTION))
                .fetch();
        assertEquals(1, issues.size());

        // Verify equipment status was updated
        var equipment = create.selectFrom(EQUIPMENT_ITEM)
                .where(EQUIPMENT_ITEM.ID.eq(testEquipmentItemId))
                .fetchOne();
        assertEquals(EquipmentStatus.FAILED_INSPECTION, equipment.getStatus());
    }

    @Test
    void auditItem_createsIssue_whenStatusIsDamaged() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.DAMAGED,
                ItemCondition.POOR,
                null,
                null,
                "Visible damage to handle",
                null,
                null,
                null,
                false
        );

        service.auditItem(request, testUserId);

        var updatedAudit = service.getAudit(summary.id());
        assertEquals(1, updatedAudit.issuesFoundCount());

        // Verify issue was created
        var issues = create.selectFrom(ISSUE)
                .where(ISSUE.APPARATUS_ID.eq(testApparatusId))
                .and(ISSUE.CATEGORY.eq(IssueCategory.DAMAGE))
                .fetch();
        assertEquals(1, issues.size());
    }

    @Test
    void auditItem_recordsConditionAndTestResult() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.VERIFIED,
                ItemCondition.FAIR,
                TestResult.PASSED,
                null,
                "Minor wear but functional",
                "All tests passed",
                null,
                null,
                false
        );

        service.auditItem(request, testUserId);

        // Verify the audit item was recorded with condition and test result
        var auditItems = create.selectFrom(FORMAL_AUDIT_ITEM)
                .where(FORMAL_AUDIT_ITEM.FORMAL_AUDIT_ID.eq(summary.id()))
                .fetch();
        assertEquals(1, auditItems.size());
        assertEquals(ItemCondition.FAIR, auditItems.getFirst().getItemCondition());
        assertEquals(TestResult.PASSED, auditItems.getFirst().getTestResult());
        assertEquals("Minor wear but functional", auditItems.getFirst().getConditionNotes());
        assertEquals("All tests passed", auditItems.getFirst().getTestNotes());
    }

    @Test
    void auditItem_throwsException_whenItemAlreadyAudited() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.VERIFIED,
                ItemCondition.GOOD,
                TestResult.PASSED,
                null,
                null,
                null,
                null,
                null,
                false
        );

        // First audit
        service.auditItem(request, testUserId);

        // Second audit should fail
        assertThrows(FormalAuditService.ItemAlreadyAuditedException.class, () ->
                service.auditItem(request, testUserId)
        );
    }

    // ==================== Pause/Resume functionality ====================

    @Test
    void saveAndExit_pausesAudit() {
        var summary = service.startAudit(testApparatusId, testUserId);

        service.saveAndExit(summary.id());

        var paused = service.getAudit(summary.id());
        assertTrue(paused.isPaused());
        assertEquals(AuditStatus.IN_PROGRESS, paused.status());
    }

    @Test
    void resumeAudit_clearsPausedState() {
        var summary = service.startAudit(testApparatusId, testUserId);
        service.saveAndExit(summary.id());

        service.resumeAudit(summary.id());

        var resumed = service.getAudit(summary.id());
        assertFalse(resumed.isPaused());
    }

    // ==================== Abandon functionality ====================

    @Test
    void abandonAudit_marksAuditAsAbandoned() {
        var summary = service.startAudit(testApparatusId, testUserId);

        service.abandonAudit(summary.id());

        var abandoned = service.getAudit(summary.id());
        assertEquals(AuditStatus.ABANDONED, abandoned.status());
    }

    @Test
    void abandonAudit_allowsNewAuditToBeStarted() {
        var firstAudit = service.startAudit(testApparatusId, testUserId);

        service.abandonAudit(firstAudit.id());

        // Should be able to start a new audit
        var secondAudit = service.startAudit(testApparatusId, testUserId);
        assertNotNull(secondAudit);
        assertNotEquals(firstAudit.id(), secondAudit.id());
    }

    // ==================== Active audit queries ====================

    @Test
    void getActiveAudit_returnsEmpty_whenNoActiveAudit() {
        var result = service.getActiveAudit(testApparatusId);
        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveAudit_returnsAudit_whenActiveAuditExists() {
        var summary = service.startAudit(testApparatusId, testUserId);

        var result = service.getActiveAudit(testApparatusId);

        assertTrue(result.isPresent());
        assertEquals(summary.id(), result.get().id());
    }

    // ==================== Authorization Tests ====================

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
    void auditItem_deniesFirefighter() {
        // Start audit as maintenance
        var summary = service.startAudit(testApparatusId, testUserId);

        // Switch to firefighter
        TestSecurityUtils.authenticateAsFirefighter(testStationId);

        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.VERIFIED,
                ItemCondition.GOOD,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThrows(AccessDeniedException.class, () ->
                service.auditItem(request, testUserId)
        );
    }

    // ==================== BR-07: Completed audits are read-only ====================

    @Test
    void updateNotes_throwsException_forCompletedAudit() {
        var summary = service.startAudit(testApparatusId, testUserId);

        // Audit the item
        var request = new AuditItemRequest(
                summary.id(),
                testEquipmentItemId,
                null,
                testCompartmentId,
                null,
                AuditItemStatus.VERIFIED,
                ItemCondition.GOOD,
                TestResult.PASSED,
                null,
                null,
                null,
                null,
                null,
                false
        );
        service.auditItem(request, testUserId);

        // Complete the audit
        service.completeAudit(summary.id());

        // Try to update notes - should fail (BR-07)
        assertThrows(IllegalArgumentException.class, () ->
                service.updateNotes(summary.id(), "New notes")
        );
    }
}
