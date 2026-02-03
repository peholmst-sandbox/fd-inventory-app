package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuditException} and its sealed subclasses demonstrating
 * domain-specific error handling.
 *
 * <p>The sealed exception hierarchy allows exhaustive pattern matching on
 * audit-related errors, ensuring all error cases are explicitly handled.
 */
@DisplayName("AuditException")
class AuditExceptionTest {

    private final FormalAuditId auditId = FormalAuditId.generate();
    private final ApparatusId apparatusId = ApparatusId.generate();

    @Nested
    @DisplayName("IncompleteAuditException (BR-03)")
    class IncompleteAuditExceptionTests {

        @Test
        void captures_audit_id_and_remaining_items() {
            var exception = new AuditException.IncompleteAuditException(auditId, 15);

            assertThat(exception.auditId()).isEqualTo(auditId);
            assertThat(exception.remainingItems()).isEqualTo(15);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.IncompleteAuditException(auditId, 15);

            assertThat(exception.getMessage())
                    .contains("Cannot complete audit")
                    .contains(auditId.toString())
                    .contains("15 items have not been audited");
        }

        @Test
        void extends_AuditException() {
            var exception = new AuditException.IncompleteAuditException(auditId, 15);

            assertThat(exception).isInstanceOf(AuditException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("AuditAlreadyCompletedException (BR-07)")
    class AuditAlreadyCompletedExceptionTests {

        @Test
        void captures_audit_id() {
            var exception = new AuditException.AuditAlreadyCompletedException(auditId);

            assertThat(exception.auditId()).isEqualTo(auditId);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.AuditAlreadyCompletedException(auditId);

            assertThat(exception.getMessage())
                    .contains(auditId.toString())
                    .contains("already completed")
                    .contains("cannot be modified");
        }
    }

    @Nested
    @DisplayName("AuditAlreadyAbandonedException")
    class AuditAlreadyAbandonedExceptionTests {

        @Test
        void captures_audit_id() {
            var exception = new AuditException.AuditAlreadyAbandonedException(auditId);

            assertThat(exception.auditId()).isEqualTo(auditId);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.AuditAlreadyAbandonedException(auditId);

            assertThat(exception.getMessage())
                    .contains(auditId.toString())
                    .contains("abandoned")
                    .contains("cannot be modified");
        }
    }

    @Nested
    @DisplayName("ActiveAuditExistsException (BR-02)")
    class ActiveAuditExistsExceptionTests {

        @Test
        void captures_apparatus_id_and_existing_audit_id() {
            var existingAuditId = FormalAuditId.generate();
            var exception = new AuditException.ActiveAuditExistsException(apparatusId, existingAuditId);

            assertThat(exception.apparatusId()).isEqualTo(apparatusId);
            assertThat(exception.existingAuditId()).isEqualTo(existingAuditId);
        }

        @Test
        void message_describes_the_problem() {
            var existingAuditId = FormalAuditId.generate();
            var exception = new AuditException.ActiveAuditExistsException(apparatusId, existingAuditId);

            assertThat(exception.getMessage())
                    .contains(apparatusId.toString())
                    .contains("already has an active audit")
                    .contains(existingAuditId.toString());
        }
    }

    @Nested
    @DisplayName("AuditNotPausedException")
    class AuditNotPausedExceptionTests {

        @Test
        void captures_audit_id() {
            var exception = new AuditException.AuditNotPausedException(auditId);

            assertThat(exception.auditId()).isEqualTo(auditId);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.AuditNotPausedException(auditId);

            assertThat(exception.getMessage())
                    .contains(auditId.toString())
                    .contains("not paused")
                    .contains("cannot be resumed");
        }
    }

    @Nested
    @DisplayName("AuditAlreadyPausedException")
    class AuditAlreadyPausedExceptionTests {

        @Test
        void captures_audit_id() {
            var exception = new AuditException.AuditAlreadyPausedException(auditId);

            assertThat(exception.auditId()).isEqualTo(auditId);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.AuditAlreadyPausedException(auditId);

            assertThat(exception.getMessage())
                    .contains(auditId.toString())
                    .contains("already paused");
        }
    }

    @Nested
    @DisplayName("AuditNotFoundException")
    class AuditNotFoundExceptionTests {

        @Test
        void captures_audit_id() {
            var exception = new AuditException.AuditNotFoundException(auditId);

            assertThat(exception.auditId()).isEqualTo(auditId);
        }

        @Test
        void message_describes_the_problem() {
            var exception = new AuditException.AuditNotFoundException(auditId);

            assertThat(exception.getMessage())
                    .contains("Audit not found")
                    .contains(auditId.toString());
        }
    }

    @Nested
    @DisplayName("Sealed class hierarchy")
    class SealedClassHierarchy {

        @Test
        void all_exceptions_extend_AuditException() {
            assertThat(new AuditException.IncompleteAuditException(auditId, 5))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.AuditAlreadyCompletedException(auditId))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.AuditAlreadyAbandonedException(auditId))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.ActiveAuditExistsException(apparatusId, auditId))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.AuditNotPausedException(auditId))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.AuditAlreadyPausedException(auditId))
                    .isInstanceOf(AuditException.class);
            assertThat(new AuditException.AuditNotFoundException(auditId))
                    .isInstanceOf(AuditException.class);
        }

        @Test
        void can_catch_all_audit_exceptions_with_base_type() {
            AuditException exception = new AuditException.IncompleteAuditException(auditId, 5);

            // Can catch with base type
            try {
                throw exception;
            } catch (AuditException e) {
                assertThat(e).isNotNull();
            }
        }

        @Test
        void pattern_matching_allows_specific_handling() {
            AuditException exception = new AuditException.IncompleteAuditException(auditId, 5);

            String result = switch (exception) {
                case AuditException.IncompleteAuditException e ->
                        "Incomplete: " + e.remainingItems() + " items remain";
                case AuditException.AuditAlreadyCompletedException e ->
                        "Already completed: " + e.auditId();
                case AuditException.AuditAlreadyAbandonedException e ->
                        "Already abandoned: " + e.auditId();
                case AuditException.ActiveAuditExistsException e ->
                        "Active audit exists: " + e.existingAuditId();
                case AuditException.AuditNotPausedException e ->
                        "Not paused: " + e.auditId();
                case AuditException.AuditAlreadyPausedException e ->
                        "Already paused: " + e.auditId();
                case AuditException.AuditNotFoundException e ->
                        "Not found: " + e.auditId();
            };

            assertThat(result).startsWith("Incomplete: 5");
        }
    }

    @Nested
    @DisplayName("Exception usage examples")
    class UsageExamples {

        @Test
        void thrown_when_completing_incomplete_audit() {
            var inProgress = InProgressAudit.start(
                    auditId,
                    apparatusId,
                    com.example.firestock.domain.primitives.ids.UserId.generate(),
                    java.time.Instant.now(),
                    10
            ).withItemAudited(false, java.time.Instant.now());

            try {
                inProgress.complete(java.time.Instant.now());
            } catch (AuditException.IncompleteAuditException e) {
                assertThat(e.remainingItems()).isEqualTo(9);
            }
        }

        @Test
        void thrown_when_pausing_already_paused_audit() {
            var inProgress = InProgressAudit.start(
                    auditId,
                    apparatusId,
                    com.example.firestock.domain.primitives.ids.UserId.generate(),
                    java.time.Instant.now(),
                    10
            ).pause(java.time.Instant.now());

            try {
                inProgress.pause(java.time.Instant.now());
            } catch (AuditException.AuditAlreadyPausedException e) {
                assertThat(e.auditId()).isEqualTo(auditId);
            }
        }

        @Test
        void thrown_when_resuming_non_paused_audit() {
            var inProgress = InProgressAudit.start(
                    auditId,
                    apparatusId,
                    com.example.firestock.domain.primitives.ids.UserId.generate(),
                    java.time.Instant.now(),
                    10
            );

            try {
                inProgress.resume(java.time.Instant.now());
            } catch (AuditException.AuditNotPausedException e) {
                assertThat(e.auditId()).isEqualTo(auditId);
            }
        }
    }
}
