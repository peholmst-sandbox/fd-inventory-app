package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link InProgressAudit} demonstrating audit lifecycle management.
 *
 * <p>An in-progress audit is the active state where items can be verified.
 * Key operations:
 * <ul>
 *   <li>{@code start()} - Create a new audit</li>
 *   <li>{@code pause()}/{@code resume()} - Temporarily suspend work</li>
 *   <li>{@code complete()} - Finalize when all items audited (BR-03)</li>
 *   <li>{@code abandon()} - Cancel with partial findings preserved</li>
 * </ul>
 */
@DisplayName("InProgressAudit")
class InProgressAuditTest {

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
    @DisplayName("Starting a new audit")
    class StartingAudit {

        @Test
        void start_creates_audit_with_initial_progress() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThat(audit.id()).isEqualTo(auditId);
            assertThat(audit.apparatusId()).isEqualTo(apparatusId);
            assertThat(audit.auditorId()).isEqualTo(auditorId);
            assertThat(audit.startedAt()).isEqualTo(startedAt);
            assertThat(audit.progress().totalItems()).isEqualTo(50);
            assertThat(audit.progress().auditedCount()).isZero();
        }

        @Test
        void start_sets_status_to_IN_PROGRESS() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThat(audit.status()).isEqualTo(AuditStatus.IN_PROGRESS);
        }

        @Test
        void start_sets_lastActivityAt_to_startedAt() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThat(audit.lastActivityAt()).isEqualTo(startedAt);
        }

        @Test
        void start_creates_audit_that_is_not_paused() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThat(audit.isPaused()).isFalse();
            assertThat(audit.pausedAt()).isNull();
            assertThat(audit.pausedAtOpt()).isEmpty();
        }

        @Test
        void start_creates_active_non_terminal_audit() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThat(audit.isActive()).isTrue();
            assertThat(audit.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_audit_id() {
            assertThatThrownBy(() -> InProgressAudit.start(null, apparatusId, auditorId, startedAt, 50))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Audit ID cannot be null");
        }

        @Test
        void rejects_null_apparatus_id() {
            assertThatThrownBy(() -> InProgressAudit.start(auditId, null, auditorId, startedAt, 50))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Apparatus ID cannot be null");
        }

        @Test
        void rejects_null_auditor_id() {
            assertThatThrownBy(() -> InProgressAudit.start(auditId, apparatusId, null, startedAt, 50))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Auditor ID cannot be null");
        }

        @Test
        void rejects_null_started_at() {
            assertThatThrownBy(() -> InProgressAudit.start(auditId, apparatusId, auditorId, null, 50))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Started at cannot be null");
        }
    }

    @Nested
    @DisplayName("Pausing and resuming (BR-04 related)")
    class PausingAndResuming {

        @Test
        void pause_sets_pausedAt_timestamp() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var pausedAt = startedAt.plusSeconds(3600);

            var paused = audit.pause(pausedAt);

            assertThat(paused.isPaused()).isTrue();
            assertThat(paused.pausedAt()).isEqualTo(pausedAt);
            assertThat(paused.pausedAtOpt()).contains(pausedAt);
        }

        @Test
        void pause_updates_lastActivityAt() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var pausedAt = startedAt.plusSeconds(3600);

            var paused = audit.pause(pausedAt);

            assertThat(paused.lastActivityAt()).isEqualTo(pausedAt);
        }

        @Test
        void pause_throws_when_already_paused() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .pause(startedAt.plusSeconds(3600));

            assertThatThrownBy(() -> audit.pause(startedAt.plusSeconds(7200)))
                    .isInstanceOf(AuditException.AuditAlreadyPausedException.class)
                    .hasMessageContaining("already paused");
        }

        @Test
        void resume_clears_pausedAt() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .pause(startedAt.plusSeconds(3600));
            var resumedAt = startedAt.plusSeconds(7200);

            var resumed = audit.resume(resumedAt);

            assertThat(resumed.isPaused()).isFalse();
            assertThat(resumed.pausedAt()).isNull();
            assertThat(resumed.pausedAtOpt()).isEmpty();
        }

        @Test
        void resume_updates_lastActivityAt() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .pause(startedAt.plusSeconds(3600));
            var resumedAt = startedAt.plusSeconds(7200);

            var resumed = audit.resume(resumedAt);

            assertThat(resumed.lastActivityAt()).isEqualTo(resumedAt);
        }

        @Test
        void resume_throws_when_not_paused() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            assertThatThrownBy(() -> audit.resume(startedAt.plusSeconds(3600)))
                    .isInstanceOf(AuditException.AuditNotPausedException.class)
                    .hasMessageContaining("not paused");
        }

        @Test
        void can_pause_and_resume_multiple_times() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            // First pause/resume cycle
            var paused1 = audit.pause(startedAt.plusSeconds(1000));
            var resumed1 = paused1.resume(startedAt.plusSeconds(2000));

            // Second pause/resume cycle
            var paused2 = resumed1.pause(startedAt.plusSeconds(3000));
            var resumed2 = paused2.resume(startedAt.plusSeconds(4000));

            assertThat(resumed2.isPaused()).isFalse();
            assertThat(resumed2.lastActivityAt()).isEqualTo(startedAt.plusSeconds(4000));
        }
    }

    @Nested
    @DisplayName("Staleness detection (BR-04)")
    class StalenessDetection {

        @Test
        void isStale_returns_false_within_7_days() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var sixDaysLater = startedAt.plus(Duration.ofDays(6));

            assertThat(audit.isStale(sixDaysLater)).isFalse();
        }

        @Test
        void isStale_returns_true_after_7_days() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var sevenDaysLater = startedAt.plus(Duration.ofDays(7));

            assertThat(audit.isStale(sevenDaysLater)).isTrue();
        }

        @Test
        void isStale_uses_lastActivityAt_not_startedAt() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var activityAt = startedAt.plus(Duration.ofDays(5));

            // Update activity timestamp
            var updated = audit.withItemAudited(false, activityAt);

            // 7 days after start, but only 2 days after last activity
            var checkTime = startedAt.plus(Duration.ofDays(7));
            assertThat(updated.isStale(checkTime)).isFalse();

            // 7 days after last activity
            var staleTime = activityAt.plus(Duration.ofDays(7));
            assertThat(updated.isStale(staleTime)).isTrue();
        }

        @Test
        void staleness_threshold_is_7_days() {
            assertThat(InProgressAudit.STALENESS_DAYS).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Recording progress")
    class RecordingProgress {

        @Test
        void withItemAudited_updates_progress_and_activity() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var activityAt = startedAt.plusSeconds(300);

            var updated = audit.withItemAudited(false, activityAt);

            assertThat(updated.progress().auditedCount()).isEqualTo(1);
            assertThat(updated.progress().issuesFoundCount()).isZero();
            assertThat(updated.lastActivityAt()).isEqualTo(activityAt);
        }

        @Test
        void withItemAudited_increments_issues_when_issue_found() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            var updated = audit.withItemAudited(true, startedAt.plusSeconds(300));

            assertThat(updated.progress().auditedCount()).isEqualTo(1);
            assertThat(updated.progress().issuesFoundCount()).isEqualTo(1);
        }

        @Test
        void withUnexpectedItem_updates_unexpected_count() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var activityAt = startedAt.plusSeconds(300);

            var updated = audit.withUnexpectedItem(false, activityAt);

            assertThat(updated.progress().unexpectedItemsCount()).isEqualTo(1);
            assertThat(updated.progress().auditedCount()).isZero(); // Doesn't affect audited count
            assertThat(updated.lastActivityAt()).isEqualTo(activityAt);
        }

        @Test
        void withProgress_allows_custom_progress_update() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var newProgress = new AuditProgress(50, 25, 3, 1);
            var activityAt = startedAt.plusSeconds(300);

            var updated = audit.withProgress(newProgress, activityAt);

            assertThat(updated.progress()).isEqualTo(newProgress);
            assertThat(updated.lastActivityAt()).isEqualTo(activityAt);
        }
    }

    @Nested
    @DisplayName("Completing audit (BR-03)")
    class CompletingAudit {

        @Test
        void complete_transitions_to_CompletedAudit() {
            var audit = createFullyAuditedInProgress(10);
            var completedAt = startedAt.plusSeconds(3600);

            var completed = audit.complete(completedAt);

            assertThat(completed).isInstanceOf(CompletedAudit.class);
            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);
        }

        @Test
        void complete_preserves_audit_identity() {
            var audit = createFullyAuditedInProgress(10);
            var completedAt = startedAt.plusSeconds(3600);

            var completed = audit.complete(completedAt);

            assertThat(completed.id()).isEqualTo(auditId);
            assertThat(completed.apparatusId()).isEqualTo(apparatusId);
            assertThat(completed.auditorId()).isEqualTo(auditorId);
            assertThat(completed.startedAt()).isEqualTo(startedAt);
        }

        @Test
        void complete_preserves_progress() {
            var audit = createFullyAuditedInProgress(10);

            var completed = audit.complete(startedAt.plusSeconds(3600));

            assertThat(completed.progress().totalItems()).isEqualTo(10);
            assertThat(completed.progress().auditedCount()).isEqualTo(10);
        }

        @Test
        void complete_throws_when_items_remain_unaudited() {
            // BR-03: All items must be audited before completion
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 10)
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(false, startedAt.plusSeconds(200));
            // Only 2 of 10 items audited

            assertThatThrownBy(() -> audit.complete(startedAt.plusSeconds(3600)))
                    .isInstanceOf(AuditException.IncompleteAuditException.class)
                    .hasMessageContaining("8 items have not been audited");
        }

        @Test
        void complete_succeeds_with_empty_manifest() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 0);

            var completed = audit.complete(startedAt.plusSeconds(60));

            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);
        }

        private InProgressAudit createFullyAuditedInProgress(int itemCount) {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, itemCount);
            for (int i = 0; i < itemCount; i++) {
                audit = audit.withItemAudited(false, startedAt.plusSeconds(100 * (i + 1)));
            }
            return audit;
        }
    }

    @Nested
    @DisplayName("Abandoning audit")
    class AbandoningAudit {

        @Test
        void abandon_transitions_to_AbandonedAudit() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);
            var abandonedAt = startedAt.plusSeconds(3600);

            var abandoned = audit.abandon("Apparatus dispatched to incident", abandonedAt);

            assertThat(abandoned).isInstanceOf(AbandonedAudit.class);
            assertThat(abandoned.status()).isEqualTo(AuditStatus.ABANDONED);
        }

        @Test
        void abandon_preserves_partial_progress() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(true, startedAt.plusSeconds(200))
                    .withItemAudited(false, startedAt.plusSeconds(300));

            var abandoned = audit.abandon("Shift ended", startedAt.plusSeconds(3600));

            assertThat(abandoned.progress().auditedCount()).isEqualTo(3);
            assertThat(abandoned.progress().issuesFoundCount()).isEqualTo(1);
        }

        @Test
        void abandon_captures_reason() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            var abandoned = audit.abandon("Technician reassigned", startedAt.plusSeconds(3600));

            assertThat(abandoned.reason()).isEqualTo("Technician reassigned");
        }

        @Test
        void abandon_allows_null_reason() {
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            var abandoned = audit.abandon(null, startedAt.plusSeconds(3600));

            assertThat(abandoned.reason()).isNull();
            assertThat(abandoned.reasonOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("State transition summary")
    class StateTransitionSummary {

        @Test
        void demonstrates_full_audit_lifecycle_to_completion() {
            // 1. Start audit
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 3);
            assertThat(audit.status()).isEqualTo(AuditStatus.IN_PROGRESS);

            // 2. Pause for break
            var paused = audit.pause(startedAt.plusSeconds(1800));
            assertThat(paused.isPaused()).isTrue();

            // 3. Resume
            var resumed = paused.resume(startedAt.plusSeconds(3600));
            assertThat(resumed.isPaused()).isFalse();

            // 4. Audit all items
            var withItems = resumed
                    .withItemAudited(false, startedAt.plusSeconds(3700))
                    .withItemAudited(true, startedAt.plusSeconds(3800))  // Issue found
                    .withItemAudited(false, startedAt.plusSeconds(3900));
            assertThat(withItems.progress().isAllAudited()).isTrue();

            // 5. Complete
            CompletedAudit completed = withItems.complete(startedAt.plusSeconds(4000));
            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);
            assertThat(completed.progress().issuesFoundCount()).isEqualTo(1);
        }

        @Test
        void demonstrates_audit_lifecycle_to_abandonment() {
            // 1. Start audit
            var audit = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            // 2. Audit some items
            var partialAudit = audit
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(true, startedAt.plusSeconds(200));

            // 3. Abandon (emergency dispatch)
            AbandonedAudit abandoned = partialAudit.abandon(
                    "Apparatus dispatched to structure fire",
                    startedAt.plusSeconds(300)
            );

            assertThat(abandoned.status()).isEqualTo(AuditStatus.ABANDONED);
            assertThat(abandoned.hasPartialFindings()).isTrue();
            assertThat(abandoned.itemsAuditedBeforeAbandonment()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void operations_return_new_instances() {
            var original = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            var updated = original.withItemAudited(false, startedAt.plusSeconds(100));

            assertThat(updated).isNotSameAs(original);
            assertThat(original.progress().auditedCount()).isZero();
        }

        @Test
        void pause_returns_new_instance() {
            var original = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50);

            var paused = original.pause(startedAt.plusSeconds(100));

            assertThat(paused).isNotSameAs(original);
            assertThat(original.isPaused()).isFalse();
        }
    }
}
