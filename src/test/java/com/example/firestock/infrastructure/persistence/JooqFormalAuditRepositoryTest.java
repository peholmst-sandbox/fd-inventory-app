package com.example.firestock.infrastructure.persistence;

import com.example.firestock.TestcontainersConfiguration;
import com.example.firestock.domain.audit.AbandonedAudit;
import com.example.firestock.domain.audit.AuditProgress;
import com.example.firestock.domain.audit.AuditStatus;
import com.example.firestock.domain.audit.CompletedAudit;
import com.example.firestock.domain.audit.FormalAuditRepository;
import com.example.firestock.domain.audit.InProgressAudit;
import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.BadgeNumber;
import com.example.firestock.domain.primitives.strings.EmailAddress;
import com.example.firestock.domain.primitives.strings.StationCode;
import com.example.firestock.domain.primitives.strings.UnitNumber;
import com.example.firestock.jooq.enums.ApparatusStatus;
import com.example.firestock.jooq.enums.ApparatusType;
import com.example.firestock.jooq.enums.UserRole;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.example.firestock.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JooqFormalAuditRepositoryTest {

    @Autowired
    private FormalAuditRepository repository;

    @Autowired
    private DSLContext create;

    private StationId testStationId;
    private ApparatusId testApparatusId;
    private ApparatusId otherApparatusId;
    private UserId testUserId;
    private UserId otherUserId;

    @BeforeEach
    void setUp() {
        // Clean up test data
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

        // Create test station
        testStationId = StationId.generate();
        create.insertInto(STATION)
                .set(STATION.ID, testStationId)
                .set(STATION.CODE, new StationCode("ST01"))
                .set(STATION.NAME, "Test Station")
                .execute();

        // Create test users
        testUserId = UserId.generate();
        create.insertInto(APP_USER)
                .set(APP_USER.ID, testUserId)
                .set(APP_USER.BADGE_NUMBER, new BadgeNumber("M001"))
                .set(APP_USER.FIRST_NAME, "Test")
                .set(APP_USER.LAST_NAME, "Technician")
                .set(APP_USER.EMAIL, new EmailAddress("tech@example.com"))
                .set(APP_USER.ROLE, UserRole.MAINTENANCE_TECHNICIAN)
                .execute();

        otherUserId = UserId.generate();
        create.insertInto(APP_USER)
                .set(APP_USER.ID, otherUserId)
                .set(APP_USER.BADGE_NUMBER, new BadgeNumber("M002"))
                .set(APP_USER.FIRST_NAME, "Other")
                .set(APP_USER.LAST_NAME, "Tech")
                .set(APP_USER.EMAIL, new EmailAddress("other@example.com"))
                .set(APP_USER.ROLE, UserRole.MAINTENANCE_TECHNICIAN)
                .execute();

        // Create test apparatus
        testApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
                .set(APPARATUS.ID, testApparatusId)
                .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Engine 1"))
                .set(APPARATUS.TYPE, ApparatusType.ENGINE)
                .set(APPARATUS.STATION_ID, testStationId)
                .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
                .execute();

        otherApparatusId = ApparatusId.generate();
        create.insertInto(APPARATUS)
                .set(APPARATUS.ID, otherApparatusId)
                .set(APPARATUS.UNIT_NUMBER, new UnitNumber("Ladder 1"))
                .set(APPARATUS.TYPE, ApparatusType.LADDER)
                .set(APPARATUS.STATION_ID, testStationId)
                .set(APPARATUS.STATUS, ApparatusStatus.IN_SERVICE)
                .execute();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts new InProgressAudit")
        void insertsNewInProgressAudit() {
            var now = Instant.now();
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    10
            );

            var saved = repository.save(audit);

            assertNotNull(saved);
            assertEquals(audit.id(), saved.id());
            assertTrue(saved instanceof InProgressAudit);
            assertEquals(AuditStatus.IN_PROGRESS, saved.status());
        }

        @Test
        @DisplayName("updates existing InProgressAudit with progress")
        void updatesExistingInProgressAuditWithProgress() {
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit);

            var updated = audit.withItemAudited(false, Instant.now());
            repository.save(updated);

            var loaded = repository.findById(audit.id());
            assertTrue(loaded.isPresent());
            var progress = loaded.get().progress();
            assertEquals(1, progress.auditedCount());
        }

        @Test
        @DisplayName("saves CompletedAudit")
        void savesCompletedAudit() {
            var now = Instant.now();
            // Create an audit with 1 item and mark it as completed
            var inProgress = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    1
            );
            repository.save(inProgress);

            // Mark item as audited
            var audited = inProgress.withItemAudited(false, now.plusSeconds(10));
            repository.save(audited);

            // Complete the audit
            var completed = audited.complete(now.plusSeconds(20));
            repository.save(completed);

            var loaded = repository.findById(completed.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get() instanceof CompletedAudit);
            assertEquals(AuditStatus.COMPLETED, loaded.get().status());
        }

        @Test
        @DisplayName("saves AbandonedAudit")
        void savesAbandonedAudit() {
            var now = Instant.now();
            var inProgress = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    10
            );
            repository.save(inProgress);

            var abandoned = inProgress.abandon("Apparatus dispatched", now.plusSeconds(10));
            repository.save(abandoned);

            var loaded = repository.findById(abandoned.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get() instanceof AbandonedAudit);
            assertEquals(AuditStatus.ABANDONED, loaded.get().status());
            assertEquals("Apparatus dispatched", ((AbandonedAudit) loaded.get()).reason());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            var result = repository.findById(FormalAuditId.generate());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("maps InProgressAudit with pausedAt")
        void mapsInProgressAuditWithPausedAt() {
            var now = Instant.now();
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    10
            );
            var paused = audit.pause(now.plusSeconds(10));
            repository.save(paused);

            var loaded = repository.findInProgressById(paused.id());
            assertTrue(loaded.isPresent());
            assertTrue(loaded.get().isPaused());
            assertNotNull(loaded.get().pausedAt());
        }

        @Test
        @DisplayName("maps AuditProgress correctly")
        void mapsAuditProgressCorrectly() {
            var now = Instant.now();
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    10
            );
            var withAudited = audit
                    .withItemAudited(true, now.plusSeconds(1))
                    .withItemAudited(false, now.plusSeconds(2))
                    .withUnexpectedItem(true, now.plusSeconds(3));
            repository.save(withAudited);

            var loaded = repository.findById(withAudited.id());
            assertTrue(loaded.isPresent());
            var progress = loaded.get().progress();
            assertEquals(10, progress.totalItems());
            assertEquals(2, progress.auditedCount());
            assertEquals(2, progress.issuesFoundCount());
            assertEquals(1, progress.unexpectedItemsCount());
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("returns true when exists")
        void returnsTrueWhenExists() {
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit);

            assertTrue(repository.existsById(audit.id()));
        }

        @Test
        @DisplayName("returns false when not exists")
        void returnsFalseWhenNotExists() {
            assertFalse(repository.existsById(FormalAuditId.generate()));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("deletes existing audit")
        void deletesExistingAudit() {
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit);

            repository.deleteById(audit.id());

            assertFalse(repository.existsById(audit.id()));
        }
    }

    @Nested
    @DisplayName("findInProgressById()")
    class FindInProgressByIdTests {

        @Test
        @DisplayName("returns InProgressAudit when in progress")
        void returnsWhenInProgress() {
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit);

            var result = repository.findInProgressById(audit.id());
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("returns empty when completed")
        void returnsEmptyWhenCompleted() {
            var now = Instant.now();
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    1
            );
            repository.save(audit);
            var audited = audit.withItemAudited(false, now.plusSeconds(1));
            repository.save(audited);
            var completed = audited.complete(now.plusSeconds(2));
            repository.save(completed);

            var result = repository.findInProgressById(audit.id());
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findActiveByApparatusId()")
    class FindActiveByApparatusIdTests {

        @Test
        @DisplayName("returns active audit for apparatus (BR-02)")
        void returnsActiveAuditForApparatus() {
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit);

            var result = repository.findActiveByApparatusId(testApparatusId);

            assertTrue(result.isPresent());
            assertEquals(audit.id(), result.get().id());
        }

        @Test
        @DisplayName("returns empty when no active audit")
        void returnsEmptyWhenNoActiveAudit() {
            var result = repository.findActiveByApparatusId(testApparatusId);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty when audit is completed")
        void returnsEmptyWhenAuditCompleted() {
            var now = Instant.now();
            var audit = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    1
            );
            repository.save(audit);
            var audited = audit.withItemAudited(false, now.plusSeconds(1));
            repository.save(audited);
            var completed = audited.complete(now.plusSeconds(2));
            repository.save(completed);

            var result = repository.findActiveByApparatusId(testApparatusId);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByApparatusId()")
    class FindByApparatusIdTests {

        @Test
        @DisplayName("returns all audits for apparatus ordered by startedAt desc")
        void returnsAllAuditsForApparatusOrderedByStartedAt() {
            var now = Instant.now();

            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now.minus(2, ChronoUnit.DAYS),
                    1
            );
            var audited1 = audit1.withItemAudited(false, now.minus(2, ChronoUnit.DAYS).plusSeconds(1));
            var completed1 = audited1.complete(now.minus(2, ChronoUnit.DAYS).plusSeconds(2));
            repository.save(completed1);

            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now.minus(1, ChronoUnit.DAYS),
                    10
            );
            repository.save(audit2);

            var result = repository.findByApparatusId(testApparatusId);

            assertEquals(2, result.size());
            // Should be ordered by startedAt descending
            assertEquals(audit2.id(), result.get(0).id());
            assertEquals(audit1.id(), result.get(1).id());
        }
    }

    @Nested
    @DisplayName("findAllInProgress()")
    class FindAllInProgressTests {

        @Test
        @DisplayName("returns all in-progress audits")
        void returnsAllInProgressAudits() {
            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    otherApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit1);
            repository.save(audit2);

            var result = repository.findAllInProgress();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("findInProgressByAuditorId()")
    class FindInProgressByAuditorIdTests {

        @Test
        @DisplayName("returns in-progress audits for specific auditor")
        void returnsInProgressAuditsForAuditor() {
            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    otherApparatusId,
                    otherUserId,
                    Instant.now(),
                    10
            );
            repository.save(audit1);
            repository.save(audit2);

            var result = repository.findInProgressByAuditorId(testUserId);

            assertEquals(1, result.size());
            assertEquals(audit1.id(), result.get(0).id());
        }
    }

    @Nested
    @DisplayName("findCompletedByApparatusIdAndDateRange()")
    class FindCompletedByDateRangeTests {

        @Test
        @DisplayName("returns completed audits within date range")
        void returnsCompletedAuditsWithinDateRange() {
            var now = Instant.now();
            var weekAgo = now.minus(7, ChronoUnit.DAYS);
            var twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);

            // Create audit completed 5 days ago (within range)
            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now.minus(5, ChronoUnit.DAYS),
                    1
            );
            var audited1 = audit1.withItemAudited(false, now.minus(5, ChronoUnit.DAYS).plusSeconds(1));
            var completed1 = audited1.complete(now.minus(5, ChronoUnit.DAYS).plusSeconds(2));
            repository.save(completed1);

            // Create audit completed 10 days ago (outside range)
            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now.minus(10, ChronoUnit.DAYS),
                    1
            );
            var audited2 = audit2.withItemAudited(false, now.minus(10, ChronoUnit.DAYS).plusSeconds(1));
            var completed2 = audited2.complete(now.minus(10, ChronoUnit.DAYS).plusSeconds(2));
            repository.save(completed2);

            var result = repository.findCompletedByApparatusIdAndDateRange(testApparatusId, weekAgo, now);

            assertEquals(1, result.size());
            assertEquals(completed1.id(), result.get(0).id());
        }
    }

    @Nested
    @DisplayName("countByApparatusId()")
    class CountByApparatusIdTests {

        @Test
        @DisplayName("counts all audits for apparatus")
        void countsAllAuditsForApparatus() {
            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now(),
                    10
            );
            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    Instant.now().plusSeconds(1),
                    10
            );
            repository.save(audit1.abandon("Test", Instant.now()));
            repository.save(audit2);

            assertEquals(2, repository.countByApparatusId(testApparatusId));
        }
    }

    @Nested
    @DisplayName("countInProgress()")
    class CountInProgressTests {

        @Test
        @DisplayName("counts only in-progress audits")
        void countsOnlyInProgressAudits() {
            var now = Instant.now();

            var audit1 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    testApparatusId,
                    testUserId,
                    now,
                    10
            );
            repository.save(audit1);

            var audit2 = InProgressAudit.start(
                    FormalAuditId.generate(),
                    otherApparatusId,
                    testUserId,
                    now,
                    1
            );
            var audited2 = audit2.withItemAudited(false, now.plusSeconds(1));
            var completed2 = audited2.complete(now.plusSeconds(2));
            repository.save(completed2);

            assertEquals(1, repository.countInProgress());
        }
    }
}
