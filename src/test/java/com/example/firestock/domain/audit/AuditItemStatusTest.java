package com.example.firestock.domain.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuditItemStatus} demonstrating item audit outcomes.
 *
 * <p>Each item in a formal audit receives a status indicating the verification result.
 * Per BR-05, certain statuses automatically trigger issue creation.
 */
@DisplayName("AuditItemStatus")
class AuditItemStatusTest {

    @Nested
    @DisplayName("Available statuses")
    class AvailableStatuses {

        @Test
        void has_all_expected_status_values() {
            assertThat(AuditItemStatus.values()).containsExactlyInAnyOrder(
                    AuditItemStatus.VERIFIED,
                    AuditItemStatus.MISSING,
                    AuditItemStatus.DAMAGED,
                    AuditItemStatus.FAILED_INSPECTION,
                    AuditItemStatus.EXPIRED,
                    AuditItemStatus.NOT_AUDITED
            );
        }
    }

    @Nested
    @DisplayName("Issue creation requirement (BR-05)")
    class IssueCreationRequirement {

        @Test
        void VERIFIED_does_not_require_issue() {
            assertThat(AuditItemStatus.VERIFIED.requiresIssue()).isFalse();
        }

        @Test
        void NOT_AUDITED_does_not_require_issue() {
            assertThat(AuditItemStatus.NOT_AUDITED.requiresIssue()).isFalse();
        }

        @Test
        void MISSING_requires_issue() {
            // BR-05: Missing items trigger automatic issue creation
            assertThat(AuditItemStatus.MISSING.requiresIssue()).isTrue();
        }

        @Test
        void DAMAGED_requires_issue() {
            // BR-05: Damaged items trigger automatic issue creation
            assertThat(AuditItemStatus.DAMAGED.requiresIssue()).isTrue();
        }

        @Test
        void FAILED_INSPECTION_requires_issue() {
            // BR-05: Failed inspection triggers automatic issue creation
            assertThat(AuditItemStatus.FAILED_INSPECTION.requiresIssue()).isTrue();
        }

        @Test
        void EXPIRED_requires_issue() {
            // BR-05: Expired items trigger automatic issue creation
            assertThat(AuditItemStatus.EXPIRED.requiresIssue()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = AuditItemStatus.class, names = {"MISSING", "DAMAGED", "FAILED_INSPECTION", "EXPIRED"})
        void problematic_statuses_all_require_issues(AuditItemStatus status) {
            assertThat(status.requiresIssue())
                    .as("Status %s should require issue creation", status)
                    .isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = AuditItemStatus.class, names = {"VERIFIED", "NOT_AUDITED"})
        void non_problematic_statuses_do_not_require_issues(AuditItemStatus status) {
            assertThat(status.requiresIssue())
                    .as("Status %s should not require issue creation", status)
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Status descriptions")
    class StatusDescriptions {

        @Test
        void VERIFIED_indicates_item_is_acceptable() {
            // Item found and in acceptable condition
            var status = AuditItemStatus.VERIFIED;
            assertThat(status.name()).isEqualTo("VERIFIED");
        }

        @Test
        void MISSING_indicates_item_not_found() {
            // Item was not found at its expected location
            var status = AuditItemStatus.MISSING;
            assertThat(status.name()).isEqualTo("MISSING");
        }

        @Test
        void DAMAGED_indicates_physical_damage() {
            // Item present but has physical damage
            var status = AuditItemStatus.DAMAGED;
            assertThat(status.name()).isEqualTo("DAMAGED");
        }

        @Test
        void FAILED_INSPECTION_indicates_functional_failure() {
            // Item failed its functional test
            var status = AuditItemStatus.FAILED_INSPECTION;
            assertThat(status.name()).isEqualTo("FAILED_INSPECTION");
        }

        @Test
        void EXPIRED_indicates_past_expiration() {
            // Item is past its expiration or certification date
            var status = AuditItemStatus.EXPIRED;
            assertThat(status.name()).isEqualTo("EXPIRED");
        }

        @Test
        void NOT_AUDITED_indicates_pending_verification() {
            // Item has not yet been checked
            var status = AuditItemStatus.NOT_AUDITED;
            assertThat(status.name()).isEqualTo("NOT_AUDITED");
        }
    }
}
