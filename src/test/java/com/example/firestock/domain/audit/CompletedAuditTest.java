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
 * Tests for {@link CompletedAudit} demonstrating the read-only terminal state.
 *
 * <p>Per BR-07, completed audits cannot be modified. This is enforced at compile time
 * by not providing any mutation methods on this record.
 *
 * <p>CompletedAudit is typically created via {@code InProgressAudit.complete()}.
 */
@DisplayName("CompletedAudit")
class CompletedAuditTest {

    private FormalAuditId auditId;
    private ApparatusId apparatusId;
    private UserId auditorId;
    private Instant startedAt;
    private Instant completedAt;

    @BeforeEach
    void setUp() {
        auditId = FormalAuditId.generate();
        apparatusId = ApparatusId.generate();
        auditorId = UserId.generate();
        startedAt = Instant.now().minus(Duration.ofHours(2));
        completedAt = Instant.now();
    }

    @Nested
    @DisplayName("Creating completed audit")
    class CreatingCompletedAudit {

        @Test
        void created_via_InProgressAudit_complete() {
            // The normal way to create a CompletedAudit
            var inProgress = InProgressAudit.start(auditId, apparatusId, auditorId, startedAt, 10);
            // Audit all items
            for (int i = 0; i < 10; i++) {
                inProgress = inProgress.withItemAudited(i % 3 == 0, startedAt.plusSeconds(100 * (i + 1)));
            }

            CompletedAudit completed = inProgress.complete(completedAt);

            assertThat(completed.id()).isEqualTo(auditId);
            assertThat(completed.apparatusId()).isEqualTo(apparatusId);
            assertThat(completed.auditorId()).isEqualTo(auditorId);
            assertThat(completed.startedAt()).isEqualTo(startedAt);
            assertThat(completed.completedAt()).isEqualTo(completedAt);
        }

        @Test
        void can_be_constructed_directly_for_reconstitution() {
            // For loading from persistence
            var progress = new AuditProgress(50, 50, 5, 2);

            var completed = new CompletedAudit(
                    auditId,
                    apparatusId,
                    auditorId,
                    startedAt,
                    progress,
                    completedAt
            );

            assertThat(completed.progress()).isEqualTo(progress);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_incomplete_progress() {
            // Cannot create CompletedAudit with unaudited items
            var incompleteProgress = new AuditProgress(50, 45, 0, 0);

            assertThatThrownBy(() -> new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, incompleteProgress, completedAt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have all items audited")
                    .hasMessageContaining("5 items remain");
        }

        @Test
        void rejects_completedAt_before_startedAt() {
            var progress = new AuditProgress(10, 10, 0, 0);
            var invalidCompletedAt = startedAt.minus(Duration.ofHours(1));

            assertThatThrownBy(() -> new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, invalidCompletedAt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Completed at cannot be before started at");
        }

        @Test
        void rejects_null_audit_id() {
            var progress = new AuditProgress(10, 10, 0, 0);

            assertThatThrownBy(() -> new CompletedAudit(
                    null, apparatusId, auditorId, startedAt, progress, completedAt))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_null_completed_at() {
            var progress = new AuditProgress(10, 10, 0, 0);

            assertThatThrownBy(() -> new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Completed at cannot be null");
        }
    }

    @Nested
    @DisplayName("Terminal state (BR-07)")
    class TerminalState {

        @Test
        void status_is_COMPLETED() {
            var completed = createCompletedAudit();

            assertThat(completed.status()).isEqualTo(AuditStatus.COMPLETED);
        }

        @Test
        void isTerminal_returns_true() {
            var completed = createCompletedAudit();

            assertThat(completed.isTerminal()).isTrue();
        }

        @Test
        void isActive_returns_false() {
            var completed = createCompletedAudit();

            assertThat(completed.isActive()).isFalse();
        }

        @Test
        void has_no_mutation_methods() {
            // CompletedAudit is immutable by design - no setters, no with* methods
            // This test documents that BR-07 is enforced at compile time
            var completed = createCompletedAudit();

            // The only way to verify this is to check that the class has
            // no methods that return CompletedAudit (which would indicate mutation)
            var methods = CompletedAudit.class.getDeclaredMethods();
            for (var method : methods) {
                assertThat(method.getReturnType())
                        .as("Method %s should not return CompletedAudit (would indicate mutation)", method.getName())
                        .isNotEqualTo(CompletedAudit.class);
            }
        }
    }

    @Nested
    @DisplayName("Querying audit results")
    class QueryingResults {

        @Test
        void duration_calculates_time_from_start_to_completion() {
            var completed = createCompletedAudit();

            assertThat(completed.duration()).isEqualTo(Duration.between(startedAt, completedAt));
        }

        @Test
        void foundIssues_returns_true_when_issues_exist() {
            var progress = new AuditProgress(50, 50, 5, 0);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.foundIssues()).isTrue();
        }

        @Test
        void foundIssues_returns_false_when_no_issues() {
            var progress = new AuditProgress(50, 50, 0, 0);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.foundIssues()).isFalse();
        }

        @Test
        void foundUnexpectedItems_returns_true_when_present() {
            var progress = new AuditProgress(50, 50, 0, 3);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.foundUnexpectedItems()).isTrue();
        }

        @Test
        void foundUnexpectedItems_returns_false_when_none() {
            var progress = new AuditProgress(50, 50, 0, 0);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.foundUnexpectedItems()).isFalse();
        }

        @Test
        void totalItemsAudited_returns_total_count() {
            var progress = new AuditProgress(75, 75, 0, 0);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.totalItemsAudited()).isEqualTo(75);
        }

        @Test
        void issueCount_returns_number_of_issues() {
            var progress = new AuditProgress(50, 50, 8, 0);
            var completed = new CompletedAudit(
                    auditId, apparatusId, auditorId, startedAt, progress, completedAt);

            assertThat(completed.issueCount()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Implements FormalAudit interface")
    class ImplementsFormalAudit {

        @Test
        void is_instance_of_FormalAudit() {
            var completed = createCompletedAudit();

            assertThat(completed).isInstanceOf(FormalAudit.class);
        }

        @Test
        void can_be_assigned_to_FormalAudit() {
            FormalAudit audit = createCompletedAudit();

            assertThat(audit.status()).isEqualTo(AuditStatus.COMPLETED);
            assertThat(audit.isTerminal()).isTrue();
        }

        @Test
        void pattern_matching_identifies_completed() {
            FormalAudit audit = createCompletedAudit();

            String result = switch (audit) {
                case InProgressAudit ip -> "in-progress";
                case CompletedAudit c -> "completed with " + c.issueCount() + " issues";
                case AbandonedAudit a -> "abandoned";
            };

            assertThat(result).startsWith("completed with");
        }
    }

    private CompletedAudit createCompletedAudit() {
        var progress = new AuditProgress(50, 50, 3, 1);
        return new CompletedAudit(auditId, apparatusId, auditorId, startedAt, progress, completedAt);
    }
}
