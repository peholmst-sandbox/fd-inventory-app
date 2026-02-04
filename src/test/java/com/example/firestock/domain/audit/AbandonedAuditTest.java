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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AbandonedAudit} demonstrating the terminal state for cancelled audits.
 *
 * <p>When an audit cannot be completed (apparatus dispatched, technician reassigned, etc.),
 * it is abandoned. Partial findings are preserved for reference but the audit cannot be resumed.
 *
 * <p>AbandonedAudit is typically created via {@code InProgressAudit.abandon()}.
 */
@DisplayName("AbandonedAudit")
class AbandonedAuditTest {

    private FormalAuditId auditId;
    private ApparatusId apparatusId;
    private UserId auditorId;
    private Instant startedAt;
    private Instant abandonedAt;

    @BeforeEach
    void setUp() {
        auditId = FormalAuditId.generate();
        apparatusId = ApparatusId.generate();
        auditorId = UserId.generate();
        startedAt = Instant.now().minus(Duration.ofHours(1));
        abandonedAt = Instant.now();
    }

    @Nested
    @DisplayName("Creating abandoned audit")
    class CreatingAbandonedAudit {

        @Test
        void created_via_InProgressAudit_abandon() {
            // The normal way to create an AbandonedAudit
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(true, startedAt.plusSeconds(200));

            AbandonedAudit abandoned = inProgress.abandon("Apparatus dispatched", abandonedAt);

            assertThat(abandoned.id()).isEqualTo(auditId);
            assertThat(abandoned.apparatusId()).isEqualTo(apparatusId);
            assertThat(abandoned.auditorId()).isEqualTo(auditorId);
            assertThat(abandoned.startedAt()).isEqualTo(startedAt);
            assertThat(abandoned.abandonedAt()).isEqualTo(abandonedAt);
            assertThat(abandoned.reason()).isEqualTo("Apparatus dispatched");
        }

        @Test
        void can_be_constructed_directly_for_reconstitution() {
            // For loading from persistence
            var progress = new AuditProgress(50, 15, 2, 0);

            var abandoned = new AbandonedAudit(
                    auditId,
                    apparatusId,
                    auditorId,
                    startedAt,
                    progress,
                    abandonedAt,
                    "Equipment failure"
            );

            assertThat(abandoned.progress()).isEqualTo(progress);
            assertThat(abandoned.reason()).isEqualTo("Equipment failure");
        }

        @Test
        void reason_can_be_null() {
            var progress = new AuditProgress(50, 10, 0, 0);

            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.reason()).isNull();
            assertThat(abandoned.reasonOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_abandonedAt_before_startedAt() {
            var progress = new AuditProgress(50, 10, 0, 0);
            var invalidAbandonedAt = startedAt.minus(Duration.ofHours(1));

            assertThatThrownBy(() -> new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, invalidAbandonedAt, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Abandoned at cannot be before started at");
        }

        @Test
        void rejects_null_audit_id() {
            var progress = new AuditProgress(50, 10, 0, 0);

            assertThatThrownBy(() -> new AbandonedAudit(
                    null, apparatusId, auditorId, startedAt, progress, abandonedAt, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_null_abandoned_at() {
            var progress = new AuditProgress(50, 10, 0, 0);

            assertThatThrownBy(() -> new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Abandoned at cannot be null");
        }

        @Test
        void accepts_progress_with_no_items_audited() {
            // Can abandon immediately after starting
            var progress = AuditProgress.initial(50);

            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, "Emergency");

            assertThat(abandoned.hasPartialFindings()).isFalse();
        }

        @Test
        void accepts_fully_audited_progress() {
            // Rare but possible: all items audited but then abandoned instead of completed
            var progress = new AuditProgress(50, 50, 0, 0);

            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt,
                    "Decided to restart fresh");

            assertThat(abandoned.progress().isAllAudited()).isTrue();
        }
    }

    @Nested
    @DisplayName("Terminal state")
    class TerminalState {

        @Test
        void status_is_ABANDONED() {
            var abandoned = createAbandonedAudit();

            assertThat(abandoned.status()).isEqualTo(AuditStatus.ABANDONED);
        }

        @Test
        void isTerminal_returns_true() {
            var abandoned = createAbandonedAudit();

            assertThat(abandoned.isTerminal()).isTrue();
        }

        @Test
        void isActive_returns_false() {
            var abandoned = createAbandonedAudit();

            assertThat(abandoned.isActive()).isFalse();
        }

        @Test
        void has_no_mutation_methods() {
            // AbandonedAudit is immutable by design - cannot be modified after abandonment
            var abandoned = createAbandonedAudit();

            var methods = AbandonedAudit.class.getDeclaredMethods();
            for (var method : methods) {
                assertThat(method.getReturnType())
                        .as("Method %s should not return AbandonedAudit (would indicate mutation)", method.getName())
                        .isNotEqualTo(AbandonedAudit.class);
            }
        }
    }

    @Nested
    @DisplayName("Querying partial findings")
    class QueryingPartialFindings {

        @Test
        void hasPartialFindings_returns_true_when_items_were_audited() {
            var progress = new AuditProgress(50, 15, 2, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.hasPartialFindings()).isTrue();
        }

        @Test
        void hasPartialFindings_returns_false_when_no_items_audited() {
            var progress = AuditProgress.initial(50);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.hasPartialFindings()).isFalse();
        }

        @Test
        void itemsAuditedBeforeAbandonment_returns_audited_count() {
            var progress = new AuditProgress(50, 23, 3, 1);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.itemsAuditedBeforeAbandonment()).isEqualTo(23);
        }

        @Test
        void itemsNotAudited_returns_remaining_count() {
            var progress = new AuditProgress(50, 23, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.itemsNotAudited()).isEqualTo(27);
        }

        @Test
        void completionPercentageAtAbandonment_returns_progress_percentage() {
            var progress = new AuditProgress(100, 75, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.completionPercentageAtAbandonment()).isEqualTo(75);
        }

        @Test
        void foundIssuesBeforeAbandonment_returns_true_when_issues_exist() {
            var progress = new AuditProgress(50, 20, 5, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.foundIssuesBeforeAbandonment()).isTrue();
        }

        @Test
        void foundIssuesBeforeAbandonment_returns_false_when_no_issues() {
            var progress = new AuditProgress(50, 20, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.foundIssuesBeforeAbandonment()).isFalse();
        }
    }

    @Nested
    @DisplayName("Duration tracking")
    class DurationTracking {

        @Test
        void duration_calculates_time_from_start_to_abandonment() {
            var progress = new AuditProgress(50, 10, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.duration()).isEqualTo(Duration.between(startedAt, abandonedAt));
        }
    }

    @Nested
    @DisplayName("Reason tracking")
    class ReasonTracking {

        @Test
        void reasonOpt_returns_reason_when_present() {
            var progress = new AuditProgress(50, 10, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt,
                    "Apparatus dispatched to structure fire");

            assertThat(abandoned.reasonOpt()).contains("Apparatus dispatched to structure fire");
        }

        @Test
        void reasonOpt_returns_empty_when_null() {
            var progress = new AuditProgress(50, 10, 0, 0);
            var abandoned = new AbandonedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, null);

            assertThat(abandoned.reasonOpt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Implements FormalAudit interface")
    class ImplementsFormalAudit {

        @Test
        void is_instance_of_FormalAudit() {
            var abandoned = createAbandonedAudit();

            assertThat(abandoned).isInstanceOf(FormalAudit.class);
        }

        @Test
        void can_be_assigned_to_FormalAudit() {
            FormalAudit audit = createAbandonedAudit();

            assertThat(audit.status()).isEqualTo(AuditStatus.ABANDONED);
            assertThat(audit.isTerminal()).isTrue();
        }

        @Test
        void pattern_matching_identifies_abandoned() {
            FormalAudit audit = createAbandonedAudit();

            String result = switch (audit) {
                case InProgressAudit ip -> "in-progress";
                case CompletedAudit c -> "completed";
                case AbandonedAudit a -> "abandoned: " + a.reasonOpt().orElse("no reason");
            };

            assertThat(result).startsWith("abandoned:");
        }
    }

    @Nested
    @DisplayName("Common abandonment scenarios")
    class CommonScenarios {

        @Test
        void apparatus_dispatched_to_incident() {
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .withItemAudited(false, startedAt.plusSeconds(100))
                    .withItemAudited(false, startedAt.plusSeconds(200));

            var abandoned = inProgress.abandon("Apparatus dispatched to structure fire", abandonedAt);

            assertThat(abandoned.reason()).isEqualTo("Apparatus dispatched to structure fire");
            assertThat(abandoned.itemsAuditedBeforeAbandonment()).isEqualTo(2);
        }

        @Test
        void technician_reassigned() {
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 50)
                    .withItemAudited(true, startedAt.plusSeconds(100)); // Found an issue

            var abandoned = inProgress.abandon("Technician reassigned to urgent repair", abandonedAt);

            assertThat(abandoned.foundIssuesBeforeAbandonment()).isTrue();
        }

        @Test
        void shift_ended_before_completion() {
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 100);
            // Audit 60% of items
            for (int i = 0; i < 60; i++) {
                inProgress = inProgress.withItemAudited(false, startedAt.plusSeconds(100 * (i + 1)));
            }

            var abandoned = inProgress.abandon("Shift ended", abandonedAt);

            assertThat(abandoned.completionPercentageAtAbandonment()).isEqualTo(60);
            assertThat(abandoned.itemsNotAudited()).isEqualTo(40);
        }

        @Test
        void equipment_malfunction_prevented_testing() {
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 30)
                    .withItemAudited(false, startedAt.plusSeconds(100));

            var abandoned = inProgress.abandon("Test equipment malfunction - cannot complete inspections", abandonedAt);

            assertThat(abandoned.reason()).contains("malfunction");
        }
    }

    private AbandonedAudit createAbandonedAudit() {
        var progress = new AuditProgress(50, 15, 2, 1);
        return new AbandonedAudit(auditId, apparatusId, auditorId, startedAt, progress, abandonedAt, "Test reason");
    }
}
