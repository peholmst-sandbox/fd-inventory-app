package com.example.firestock.domain.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AuditProgress} demonstrating how to track audit progress.
 *
 * <p>AuditProgress is a value object that tracks:
 * <ul>
 *   <li>Total items to audit</li>
 *   <li>Number of items audited</li>
 *   <li>Number of issues found</li>
 *   <li>Number of unexpected items discovered</li>
 * </ul>
 */
@DisplayName("AuditProgress")
class AuditProgressTest {

    @Nested
    @DisplayName("Creating progress")
    class CreatingProgress {

        @Test
        void initial_creates_progress_with_zero_audited_items() {
            // Use initial() to start tracking progress for a known number of items
            var progress = AuditProgress.initial(50);

            assertThat(progress.totalItems()).isEqualTo(50);
            assertThat(progress.auditedCount()).isZero();
            assertThat(progress.issuesFoundCount()).isZero();
            assertThat(progress.unexpectedItemsCount()).isZero();
        }

        @Test
        void empty_creates_progress_with_no_items() {
            // Use empty() when the manifest has no items
            var progress = AuditProgress.empty();

            assertThat(progress.totalItems()).isZero();
            assertThat(progress.auditedCount()).isZero();
        }

        @Test
        void constructor_accepts_all_zero_values() {
            var progress = new AuditProgress(0, 0, 0, 0);

            assertThat(progress.totalItems()).isZero();
        }

        @Test
        void constructor_accepts_valid_progress_state() {
            // You can construct progress at any valid state
            var progress = new AuditProgress(100, 75, 5, 2);

            assertThat(progress.totalItems()).isEqualTo(100);
            assertThat(progress.auditedCount()).isEqualTo(75);
            assertThat(progress.issuesFoundCount()).isEqualTo(5);
            assertThat(progress.unexpectedItemsCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_negative_total_items() {
            assertThatThrownBy(() -> new AuditProgress(-1, 0, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Total items cannot be negative");
        }

        @Test
        void rejects_negative_audited_count() {
            assertThatThrownBy(() -> new AuditProgress(10, -1, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Audited count cannot be negative");
        }

        @Test
        void rejects_audited_count_exceeding_total() {
            assertThatThrownBy(() -> new AuditProgress(10, 11, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Audited count (11) cannot exceed total items (10)");
        }

        @Test
        void rejects_negative_issues_count() {
            assertThatThrownBy(() -> new AuditProgress(10, 5, -1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Issues found count cannot be negative");
        }

        @Test
        void rejects_negative_unexpected_items_count() {
            assertThatThrownBy(() -> new AuditProgress(10, 5, 0, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unexpected items count cannot be negative");
        }
    }

    @Nested
    @DisplayName("Calculating progress percentage")
    class CalculatingPercentage {

        @Test
        void progressPercentage_returns_zero_when_nothing_audited() {
            var progress = AuditProgress.initial(100);

            assertThat(progress.progressPercentage()).isZero();
        }

        @Test
        void progressPercentage_returns_50_when_half_audited() {
            var progress = new AuditProgress(100, 50, 0, 0);

            assertThat(progress.progressPercentage()).isEqualTo(50);
        }

        @Test
        void progressPercentage_returns_100_when_all_audited() {
            var progress = new AuditProgress(100, 100, 0, 0);

            assertThat(progress.progressPercentage()).isEqualTo(100);
        }

        @Test
        void progressPercentage_returns_100_for_empty_manifest() {
            // An empty manifest is considered 100% complete
            var progress = AuditProgress.empty();

            assertThat(progress.progressPercentage()).isEqualTo(100);
        }

        @Test
        void progressPercentage_truncates_decimal() {
            // 33 out of 100 = 33%
            var progress = new AuditProgress(100, 33, 0, 0);

            assertThat(progress.progressPercentage()).isEqualTo(33);
        }
    }

    @Nested
    @DisplayName("Checking completion status (BR-03)")
    class CheckingCompletion {

        @Test
        void isAllAudited_returns_false_when_items_remain() {
            var progress = new AuditProgress(10, 9, 0, 0);

            assertThat(progress.isAllAudited()).isFalse();
        }

        @Test
        void isAllAudited_returns_true_when_all_items_audited() {
            // BR-03: All items must be audited before completion
            var progress = new AuditProgress(10, 10, 0, 0);

            assertThat(progress.isAllAudited()).isTrue();
        }

        @Test
        void isAllAudited_returns_true_for_empty_manifest() {
            var progress = AuditProgress.empty();

            assertThat(progress.isAllAudited()).isTrue();
        }

        @Test
        void remainingItems_returns_count_of_unaudited_items() {
            var progress = new AuditProgress(50, 35, 0, 0);

            assertThat(progress.remainingItems()).isEqualTo(15);
        }

        @Test
        void remainingItems_returns_zero_when_all_audited() {
            var progress = new AuditProgress(50, 50, 0, 0);

            assertThat(progress.remainingItems()).isZero();
        }
    }

    @Nested
    @DisplayName("Tracking issues")
    class TrackingIssues {

        @Test
        void hasIssues_returns_false_when_no_issues() {
            var progress = new AuditProgress(10, 10, 0, 0);

            assertThat(progress.hasIssues()).isFalse();
        }

        @Test
        void hasIssues_returns_true_when_issues_exist() {
            var progress = new AuditProgress(10, 10, 3, 0);

            assertThat(progress.hasIssues()).isTrue();
        }

        @Test
        void hasUnexpectedItems_returns_false_when_none_found() {
            var progress = new AuditProgress(10, 10, 0, 0);

            assertThat(progress.hasUnexpectedItems()).isFalse();
        }

        @Test
        void hasUnexpectedItems_returns_true_when_found() {
            var progress = new AuditProgress(10, 10, 0, 2);

            assertThat(progress.hasUnexpectedItems()).isTrue();
        }
    }

    @Nested
    @DisplayName("Recording audit activity")
    class RecordingActivity {

        @Test
        void withItemAudited_increments_audited_count() {
            var progress = AuditProgress.initial(10);

            var updated = progress.withItemAudited(false);

            assertThat(updated.auditedCount()).isEqualTo(1);
            assertThat(updated.issuesFoundCount()).isZero();
        }

        @Test
        void withItemAudited_increments_issues_count_when_issue_found() {
            var progress = AuditProgress.initial(10);

            var updated = progress.withItemAudited(true);

            assertThat(updated.auditedCount()).isEqualTo(1);
            assertThat(updated.issuesFoundCount()).isEqualTo(1);
        }

        @Test
        void withItemAudited_can_be_chained_for_multiple_items() {
            var progress = AuditProgress.initial(10)
                    .withItemAudited(false)  // Item 1: OK
                    .withItemAudited(true)   // Item 2: Issue
                    .withItemAudited(false)  // Item 3: OK
                    .withItemAudited(true);  // Item 4: Issue

            assertThat(progress.auditedCount()).isEqualTo(4);
            assertThat(progress.issuesFoundCount()).isEqualTo(2);
        }

        @Test
        void withUnexpectedItem_increments_unexpected_count() {
            var progress = AuditProgress.initial(10);

            var updated = progress.withUnexpectedItem(false);

            assertThat(updated.unexpectedItemsCount()).isEqualTo(1);
            assertThat(updated.auditedCount()).isZero(); // Doesn't affect audited count
        }

        @Test
        void withUnexpectedItem_increments_issues_count_when_issue_found() {
            var progress = AuditProgress.initial(10);

            var updated = progress.withUnexpectedItem(true);

            assertThat(updated.unexpectedItemsCount()).isEqualTo(1);
            assertThat(updated.issuesFoundCount()).isEqualTo(1);
        }

        @Test
        void withTotalItems_updates_total() {
            // Use when manifest changes during audit
            var progress = AuditProgress.initial(10);

            var updated = progress.withTotalItems(15);

            assertThat(updated.totalItems()).isEqualTo(15);
            assertThat(updated.auditedCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void equals_compares_all_fields() {
            var progress1 = new AuditProgress(10, 5, 2, 1);
            var progress2 = new AuditProgress(10, 5, 2, 1);

            assertThat(progress1).isEqualTo(progress2);
        }

        @Test
        void operations_return_new_instances() {
            var original = AuditProgress.initial(10);

            var updated = original.withItemAudited(false);

            assertThat(updated).isNotSameAs(original);
            assertThat(original.auditedCount()).isZero(); // Original unchanged
        }
    }
}
