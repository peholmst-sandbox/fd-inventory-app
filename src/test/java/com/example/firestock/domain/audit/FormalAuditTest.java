package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.jooq.enums.AuditStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FormalAudit} sealed interface demonstrating the audit lifecycle
 * and polymorphic behavior across all audit states.
 *
 * <p>FormalAudit is the aggregate root for formal audits with three permitted implementations:
 * <ul>
 *   <li>{@link InProgressAudit} - Active, mutable state</li>
 *   <li>{@link CompletedAudit} - Terminal, read-only state (BR-07)</li>
 *   <li>{@link AbandonedAudit} - Terminal state with partial findings</li>
 * </ul>
 *
 * <p>The sealed interface enables exhaustive pattern matching and type-safe state handling.
 */
@DisplayName("FormalAudit (sealed interface)")
class FormalAuditTest {

    private FormalAuditId auditId;
    private ApparatusId apparatusId;
    private UserId auditorId;
    private Instant startedAt;

    @BeforeEach
    void setUp() {
        auditId = FormalAuditId.generate();
        apparatusId = ApparatusId.generate();
        auditorId = UserId.generate();
        startedAt = Instant.now();
    }

    @Nested
    @DisplayName("Sealed interface design")
    class SealedInterfaceDesign {

        @Test
        void has_exactly_three_permitted_implementations() {
            var permittedSubclasses = FormalAudit.class.getPermittedSubclasses();

            assertThat(permittedSubclasses).hasSize(3);
            assertThat(permittedSubclasses).containsExactlyInAnyOrder(
                    InProgressAudit.class,
                    CompletedAudit.class,
                    AbandonedAudit.class
            );
        }

        @Test
        void all_implementations_share_common_properties() {
            var inProgress = createInProgressAudit();
            var completed = createCompletedAudit();
            var abandoned = createAbandonedAudit();

            // All implementations expose the same core properties
            for (FormalAudit audit : new FormalAudit[]{inProgress, completed, abandoned}) {
                assertThat(audit.id()).isNotNull();
                assertThat(audit.apparatusId()).isNotNull();
                assertThat(audit.auditorId()).isNotNull();
                assertThat(audit.startedAt()).isNotNull();
                assertThat(audit.progress()).isNotNull();
                assertThat(audit.status()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Pattern matching on audit state")
    class PatternMatchingOnState {

        @Test
        void can_exhaustively_match_all_states() {
            FormalAudit[] audits = {
                    createInProgressAudit(),
                    createCompletedAudit(),
                    createAbandonedAudit()
            };

            for (FormalAudit audit : audits) {
                // Compiler ensures all cases are handled
                String description = switch (audit) {
                    case InProgressAudit ip -> describeInProgress(ip);
                    case CompletedAudit c -> describeCompleted(c);
                    case AbandonedAudit a -> describeAbandoned(a);
                };
                assertThat(description).isNotEmpty();
            }
        }

        @Test
        void pattern_matching_extracts_state_specific_data() {
            FormalAudit inProgress = createInProgressAudit();
            FormalAudit completed = createCompletedAudit();
            FormalAudit abandoned = createAbandonedAudit();

            // In-progress specific: can check pause status
            if (inProgress instanceof InProgressAudit ip) {
                assertThat(ip.isPaused()).isFalse();
            }

            // Completed specific: has completion timestamp
            if (completed instanceof CompletedAudit c) {
                assertThat(c.completedAt()).isNotNull();
            }

            // Abandoned specific: may have reason
            if (abandoned instanceof AbandonedAudit a) {
                assertThat(a.reasonOpt()).isPresent();
            }
        }

        private String describeInProgress(InProgressAudit audit) {
            return "In Progress: " + audit.progress().progressPercentage() + "% complete" +
                    (audit.isPaused() ? " (paused)" : "");
        }

        private String describeCompleted(CompletedAudit audit) {
            return "Completed: " + audit.totalItemsAudited() + " items, " +
                    audit.issueCount() + " issues";
        }

        private String describeAbandoned(AbandonedAudit audit) {
            return "Abandoned: " + audit.reasonOpt().orElse("no reason") +
                    " (" + audit.completionPercentageAtAbandonment() + "% done)";
        }
    }

    @Nested
    @DisplayName("Terminal vs active state")
    class TerminalVsActiveState {

        @Test
        void in_progress_audit_is_active() {
            FormalAudit audit = createInProgressAudit();

            assertThat(audit.isActive()).isTrue();
            assertThat(audit.isTerminal()).isFalse();
        }

        @Test
        void completed_audit_is_terminal() {
            FormalAudit audit = createCompletedAudit();

            assertThat(audit.isTerminal()).isTrue();
            assertThat(audit.isActive()).isFalse();
        }

        @Test
        void abandoned_audit_is_terminal() {
            FormalAudit audit = createAbandonedAudit();

            assertThat(audit.isTerminal()).isTrue();
            assertThat(audit.isActive()).isFalse();
        }

        @Test
        void can_filter_audits_by_terminal_state() {
            FormalAudit[] allAudits = {
                    createInProgressAudit(),
                    createCompletedAudit(),
                    createAbandonedAudit()
            };

            var activeAudits = java.util.Arrays.stream(allAudits)
                    .filter(FormalAudit::isActive)
                    .toList();

            var terminalAudits = java.util.Arrays.stream(allAudits)
                    .filter(FormalAudit::isTerminal)
                    .toList();

            assertThat(activeAudits).hasSize(1);
            assertThat(terminalAudits).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Status enum correspondence")
    class StatusEnumCorrespondence {

        @Test
        void in_progress_has_IN_PROGRESS_status() {
            FormalAudit audit = createInProgressAudit();

            assertThat(audit.status()).isEqualTo(AuditStatus.IN_PROGRESS);
        }

        @Test
        void completed_has_COMPLETED_status() {
            FormalAudit audit = createCompletedAudit();

            assertThat(audit.status()).isEqualTo(AuditStatus.COMPLETED);
        }

        @Test
        void abandoned_has_ABANDONED_status() {
            FormalAudit audit = createAbandonedAudit();

            assertThat(audit.status()).isEqualTo(AuditStatus.ABANDONED);
        }

        @Test
        void can_filter_audits_by_status() {
            FormalAudit[] allAudits = {
                    createInProgressAudit(),
                    createCompletedAudit(),
                    createAbandonedAudit()
            };

            var byStatus = java.util.Arrays.stream(allAudits)
                    .filter(a -> a.status() == AuditStatus.IN_PROGRESS)
                    .toList();

            assertThat(byStatus).hasSize(1);
            assertThat(byStatus.get(0)).isInstanceOf(InProgressAudit.class);
        }
    }

    @Nested
    @DisplayName("Complete audit lifecycle example")
    class CompleteLifecycleExample {

        @Test
        void demonstrates_audit_from_start_to_completion() {
            // STEP 1: Start a new audit
            FormalAudit audit = InProgressAudit.start(
                    auditId, apparatusId, auditorId, startedAt, 5);

            assertThat(audit.status()).isEqualTo(AuditStatus.IN_PROGRESS);
            assertThat(audit.progress().auditedCount()).isZero();

            // STEP 2: Audit items (requires casting to InProgressAudit for mutations)
            if (audit instanceof InProgressAudit inProgress) {
                audit = inProgress
                        .withItemAudited(false, startedAt.plusSeconds(100))
                        .withItemAudited(true, startedAt.plusSeconds(200))  // Found issue
                        .withItemAudited(false, startedAt.plusSeconds(300))
                        .withItemAudited(false, startedAt.plusSeconds(400))
                        .withItemAudited(false, startedAt.plusSeconds(500));
            }

            assertThat(audit.progress().isAllAudited()).isTrue();
            assertThat(audit.progress().issuesFoundCount()).isEqualTo(1);

            // STEP 3: Complete the audit
            if (audit instanceof InProgressAudit inProgress) {
                audit = inProgress.complete(startedAt.plusSeconds(600));
            }

            // STEP 4: Verify completed state
            assertThat(audit.status()).isEqualTo(AuditStatus.COMPLETED);
            assertThat(audit.isTerminal()).isTrue();

            if (audit instanceof CompletedAudit completed) {
                assertThat(completed.foundIssues()).isTrue();
                assertThat(completed.duration()).isPositive();
            }
        }

        @Test
        void demonstrates_audit_from_start_to_abandonment() {
            // STEP 1: Start audit
            FormalAudit audit = InProgressAudit.start(
                    auditId, apparatusId, auditorId, startedAt, 50);

            // STEP 2: Partial progress
            if (audit instanceof InProgressAudit inProgress) {
                audit = inProgress
                        .withItemAudited(false, startedAt.plusSeconds(100))
                        .withItemAudited(false, startedAt.plusSeconds(200));
            }

            // STEP 3: Emergency dispatch - must abandon
            if (audit instanceof InProgressAudit inProgress) {
                audit = inProgress.abandon(
                        "Apparatus dispatched to structure fire",
                        startedAt.plusSeconds(300));
            }

            // STEP 4: Verify abandoned state preserves partial work
            assertThat(audit.status()).isEqualTo(AuditStatus.ABANDONED);

            if (audit instanceof AbandonedAudit abandoned) {
                assertThat(abandoned.hasPartialFindings()).isTrue();
                assertThat(abandoned.itemsAuditedBeforeAbandonment()).isEqualTo(2);
                assertThat(abandoned.reason()).contains("structure fire");
            }
        }

        @Test
        void demonstrates_audit_with_pause_resume_cycle() {
            // STEP 1: Start audit
            InProgressAudit audit = InProgressAudit.start(
                    auditId, apparatusId, auditorId, startedAt, 10);

            // STEP 2: Do some work
            audit = audit
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(false, startedAt.plusSeconds(200));

            // STEP 3: Pause for lunch
            audit = audit.pause(startedAt.plusSeconds(300));
            assertThat(audit.isPaused()).isTrue();

            // STEP 4: Resume after break
            audit = audit.resume(startedAt.plus(Duration.ofHours(1)));
            assertThat(audit.isPaused()).isFalse();

            // STEP 5: Continue and finish
            for (int i = 2; i < 10; i++) {
                audit = audit.withItemAudited(false, startedAt.plus(Duration.ofHours(1)).plusSeconds(100 * i));
            }

            // STEP 6: Complete
            CompletedAudit completed = audit.complete(startedAt.plus(Duration.ofHours(2)));
            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Business rules enforcement")
    class BusinessRulesEnforcement {

        @Test
        void BR_03_all_items_must_be_audited_before_completion() {
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 10)
                    .withItemAudited(false, startedAt.plusSeconds(100));
            // Only 1 of 10 items audited

            try {
                inProgress.complete(startedAt.plusSeconds(200));
                assertThat(true).as("Should have thrown IncompleteAuditException").isFalse();
            } catch (AuditException.IncompleteAuditException e) {
                assertThat(e.remainingItems()).isEqualTo(9);
            }
        }

        @Test
        void BR_04_staleness_is_tracked_on_in_progress_audits() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            // Not stale within 7 days
            assertThat(audit.isStale(startedAt.plus(Duration.ofDays(6)))).isFalse();

            // Stale after 7 days
            assertThat(audit.isStale(startedAt.plus(Duration.ofDays(7)))).isTrue();
        }

        @Test
        void BR_07_completed_audits_are_immutable() {
            // CompletedAudit has no mutation methods - verified by not having
            // any methods that return CompletedAudit or modify state
            var completed = createCompletedAudit();

            // The only way to "modify" would be to create a new audit
            // CompletedAudit intentionally provides no such capability
            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);

            // Can only read data, not change it
            assertThat(completed.foundIssues()).isTrue();
            assertThat(completed.totalItemsAudited()).isEqualTo(50);
        }
    }

    // Helper methods to create test audits

    private InProgressAudit createInProgressAudit() {
        return InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                .withItemAudited(false, startedAt.plusSeconds(100))
                .withItemAudited(true, startedAt.plusSeconds(200));
    }

    private CompletedAudit createCompletedAudit() {
        var progress = new AuditProgress(50, 50, 3, 1);
        return new CompletedAudit(
                auditId, apparatusId, auditorId, startedAt, progress,
                startedAt.plus(Duration.ofHours(2)));
    }

    private AbandonedAudit createAbandonedAudit() {
        var progress = new AuditProgress(50, 20, 2, 0);
        return new AbandonedAudit(
                auditId, apparatusId, auditorId, startedAt, progress,
                startedAt.plus(Duration.ofHours(1)),
                "Apparatus dispatched");
    }
}
