package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.jooq.enums.AuditItemStatus;
import com.example.firestock.jooq.enums.ExpiryStatus;
import com.example.firestock.jooq.enums.ItemCondition;
import com.example.firestock.jooq.enums.TestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link FormalAuditItem} demonstrating how to record item audit results.
 *
 * <p>A FormalAuditItem captures the verification result for either an equipment item
 * or a consumable stock entry. The target type is enforced via the sealed
 * {@link AuditedItemTarget} interface.
 */
@DisplayName("FormalAuditItem")
class FormalAuditItemTest {

    private final FormalAuditItemId itemId = FormalAuditItemId.generate();
    private final FormalAuditId auditId = FormalAuditId.generate();
    private final EquipmentItemId equipmentId = EquipmentItemId.generate();
    private final ConsumableStockId consumableId = ConsumableStockId.generate();
    private final CompartmentId compartmentId = CompartmentId.generate();
    private final ManifestEntryId manifestEntryId = ManifestEntryId.generate();

    @Nested
    @DisplayName("Creating unaudited items")
    class CreatingUnauditedItems {

        @Test
        void unauditedEquipment_creates_item_with_NOT_AUDITED_status() {
            var item = FormalAuditItem.unauditedEquipment(
                    itemId,
                    auditId,
                    EquipmentTarget.of(equipmentId)
            );

            assertThat(item.id()).isEqualTo(itemId);
            assertThat(item.auditId()).isEqualTo(auditId);
            assertThat(item.target()).isInstanceOf(EquipmentTarget.class);
            assertThat(item.status()).isEqualTo(AuditItemStatus.NOT_AUDITED);
            assertThat(item.auditedAt()).isNull();
            assertThat(item.isUnexpected()).isFalse();
        }

        @Test
        void unauditedEquipment_with_location_creates_item_linked_to_manifest() {
            var item = FormalAuditItem.unauditedEquipment(
                    itemId,
                    auditId,
                    EquipmentTarget.of(equipmentId),
                    compartmentId,
                    manifestEntryId
            );

            assertThat(item.compartmentId()).isEqualTo(compartmentId);
            assertThat(item.manifestEntryId()).isEqualTo(manifestEntryId);
            assertThat(item.isUnexpected()).isFalse();
            assertThat(item.isExpected()).isTrue();
        }

        @Test
        void unauditedConsumable_creates_item_with_NOT_AUDITED_status() {
            var item = FormalAuditItem.unauditedConsumable(
                    itemId,
                    auditId,
                    ConsumableTarget.of(consumableId)
            );

            assertThat(item.id()).isEqualTo(itemId);
            assertThat(item.auditId()).isEqualTo(auditId);
            assertThat(item.target()).isInstanceOf(ConsumableTarget.class);
            assertThat(item.status()).isEqualTo(AuditItemStatus.NOT_AUDITED);
            assertThat(item.auditedAt()).isNull();
            assertThat(item.isUnexpected()).isFalse();
        }

        @Test
        void unauditedConsumable_with_location_creates_item_linked_to_manifest() {
            var item = FormalAuditItem.unauditedConsumable(
                    itemId,
                    auditId,
                    ConsumableTarget.of(consumableId),
                    compartmentId,
                    manifestEntryId
            );

            assertThat(item.compartmentId()).isEqualTo(compartmentId);
            assertThat(item.manifestEntryId()).isEqualTo(manifestEntryId);
            assertThat(item.isUnexpected()).isFalse();
        }

        @Test
        void unaudited_items_have_no_condition_or_test_results() {
            var item = FormalAuditItem.unauditedEquipment(
                    itemId,
                    auditId,
                    EquipmentTarget.of(equipmentId)
            );

            assertThat(item.condition()).isNull();
            assertThat(item.testResult()).isNull();
            assertThat(item.expiryStatus()).isNull();
            assertThat(item.conditionNotes()).isNull();
            assertThat(item.testNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Creating unexpected items")
    class CreatingUnexpectedItems {

        @Test
        void unexpectedEquipment_creates_item_marked_as_unexpected() {
            var item = FormalAuditItem.unexpectedEquipment(
                    itemId,
                    auditId,
                    EquipmentTarget.of(equipmentId),
                    compartmentId
            );

            assertThat(item.isUnexpected()).isTrue();
            assertThat(item.isExpected()).isFalse();
            assertThat(item.manifestEntryId()).isNull();
            assertThat(item.compartmentId()).isEqualTo(compartmentId);
            assertThat(item.status()).isEqualTo(AuditItemStatus.NOT_AUDITED);
        }

        @Test
        void unexpectedConsumable_creates_item_marked_as_unexpected() {
            var item = FormalAuditItem.unexpectedConsumable(
                    itemId,
                    auditId,
                    ConsumableTarget.of(consumableId),
                    compartmentId
            );

            assertThat(item.isUnexpected()).isTrue();
            assertThat(item.isExpected()).isFalse();
            assertThat(item.manifestEntryId()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejects_null_id() {
            assertThatThrownBy(() -> new FormalAuditItem(
                    null, auditId, EquipmentTarget.of(equipmentId),
                    null, null, false,
                    AuditItemStatus.NOT_AUDITED, null, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Audit item ID cannot be null");
        }

        @Test
        void rejects_null_audit_id() {
            assertThatThrownBy(() -> new FormalAuditItem(
                    itemId, null, EquipmentTarget.of(equipmentId),
                    null, null, false,
                    AuditItemStatus.NOT_AUDITED, null, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Audit ID cannot be null");
        }

        @Test
        void rejects_null_target() {
            assertThatThrownBy(() -> new FormalAuditItem(
                    itemId, auditId, null,
                    null, null, false,
                    AuditItemStatus.NOT_AUDITED, null, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Audit target cannot be null");
        }

        @Test
        void rejects_null_status() {
            assertThatThrownBy(() -> new FormalAuditItem(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    null, null, false,
                    null, null, null, null, null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Audit item status cannot be null");
        }

        @Test
        void rejects_quantity_comparison_for_equipment_target() {
            // Quantity comparison only makes sense for consumables
            var quantityComparison = QuantityComparison.of(10, 8);

            assertThatThrownBy(() -> new FormalAuditItem(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    null, null, false,
                    AuditItemStatus.VERIFIED, null, null, null, quantityComparison, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity comparison can only be set for consumable targets");
        }

        @Test
        void rejects_unexpected_item_with_manifest_entry() {
            assertThatThrownBy(() -> new FormalAuditItem(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    compartmentId, manifestEntryId, true,  // isUnexpected=true with manifestEntryId
                    AuditItemStatus.NOT_AUDITED, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unexpected items cannot have a manifest entry ID");
        }
    }

    @Nested
    @DisplayName("Recording equipment audit results")
    class RecordingEquipmentResults {

        @Test
        void withAuditResult_records_verified_equipment() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));
            var auditedAt = Instant.now();

            var audited = item.withAuditResult(
                    AuditItemStatus.VERIFIED,
                    ItemCondition.GOOD,
                    TestResult.PASSED,
                    ExpiryStatus.OK,
                    "All checks passed",
                    "Test notes here",
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.VERIFIED);
            assertThat(audited.condition()).isEqualTo(ItemCondition.GOOD);
            assertThat(audited.testResult()).isEqualTo(TestResult.PASSED);
            assertThat(audited.expiryStatus()).isEqualTo(ExpiryStatus.OK);
            assertThat(audited.conditionNotes()).isEqualTo("All checks passed");
            assertThat(audited.testNotes()).isEqualTo("Test notes here");
            assertThat(audited.auditedAt()).isEqualTo(auditedAt);
        }

        @Test
        void withAuditResult_records_damaged_equipment() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));
            var auditedAt = Instant.now();

            var audited = item.withAuditResult(
                    AuditItemStatus.DAMAGED,
                    ItemCondition.POOR,
                    TestResult.NOT_TESTED,
                    null,
                    "Cracked handle, needs replacement",
                    null,
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.DAMAGED);
            assertThat(audited.condition()).isEqualTo(ItemCondition.POOR);
            assertThat(audited.requiresIssue()).isTrue();
        }

        @Test
        void withAuditResult_records_failed_inspection() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));
            var auditedAt = Instant.now();

            var audited = item.withAuditResult(
                    AuditItemStatus.FAILED_INSPECTION,
                    ItemCondition.FAIR,
                    TestResult.FAILED,
                    null,
                    null,
                    "Battery test failed - 40% capacity",
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.FAILED_INSPECTION);
            assertThat(audited.testResult()).isEqualTo(TestResult.FAILED);
            assertThat(audited.requiresIssue()).isTrue();
        }

        @Test
        void withAuditResult_records_missing_equipment() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));
            var auditedAt = Instant.now();

            var audited = item.withAuditResult(
                    AuditItemStatus.MISSING,
                    null,  // No condition for missing items
                    null,
                    null,
                    "Item not found in assigned compartment",
                    null,
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.MISSING);
            assertThat(audited.condition()).isNull();
            assertThat(audited.requiresIssue()).isTrue();
        }

        @Test
        void withAuditResult_preserves_location_fields() {
            var item = FormalAuditItem.unauditedEquipment(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    compartmentId, manifestEntryId
            );
            var auditedAt = Instant.now();

            var audited = item.withAuditResult(
                    AuditItemStatus.VERIFIED,
                    ItemCondition.GOOD,
                    TestResult.PASSED,
                    null,
                    null,
                    null,
                    auditedAt
            );

            assertThat(audited.compartmentId()).isEqualTo(compartmentId);
            assertThat(audited.manifestEntryId()).isEqualTo(manifestEntryId);
            assertThat(audited.isUnexpected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Recording consumable audit results")
    class RecordingConsumableResults {

        @Test
        void withConsumableAuditResult_records_verified_consumable() {
            var item = FormalAuditItem.unauditedConsumable(itemId, auditId, ConsumableTarget.of(consumableId));
            var auditedAt = Instant.now();
            var quantityComparison = QuantityComparison.matching(com.example.firestock.domain.primitives.numbers.Quantity.of(50));

            var audited = item.withConsumableAuditResult(
                    AuditItemStatus.VERIFIED,
                    ExpiryStatus.OK,
                    quantityComparison,
                    "Quantity verified",
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.VERIFIED);
            assertThat(audited.quantityComparison()).isEqualTo(quantityComparison);
            assertThat(audited.expiryStatus()).isEqualTo(ExpiryStatus.OK);
            assertThat(audited.condition()).isNull(); // No condition for consumables
            assertThat(audited.testResult()).isNull(); // No test for consumables
        }

        @Test
        void withConsumableAuditResult_records_shortage() {
            var item = FormalAuditItem.unauditedConsumable(itemId, auditId, ConsumableTarget.of(consumableId));
            var auditedAt = Instant.now();
            var quantityComparison = QuantityComparison.of(50, 35); // Shortage

            var audited = item.withConsumableAuditResult(
                    AuditItemStatus.VERIFIED, // Could still be VERIFIED if within tolerance
                    ExpiryStatus.OK,
                    quantityComparison,
                    "15 units below expected - restock needed",
                    auditedAt
            );

            assertThat(audited.quantityComparison().isShortage()).isTrue();
            assertThat(audited.quantityComparison().shortageAmount().value().intValue()).isEqualTo(15);
        }

        @Test
        void withConsumableAuditResult_records_expired_consumable() {
            var item = FormalAuditItem.unauditedConsumable(itemId, auditId, ConsumableTarget.of(consumableId));
            var auditedAt = Instant.now();

            var audited = item.withConsumableAuditResult(
                    AuditItemStatus.EXPIRED,
                    ExpiryStatus.EXPIRED,
                    QuantityComparison.of(50, 50),
                    "Lot expired 2024-01-15",
                    auditedAt
            );

            assertThat(audited.status()).isEqualTo(AuditItemStatus.EXPIRED);
            assertThat(audited.expiryStatus()).isEqualTo(ExpiryStatus.EXPIRED);
            assertThat(audited.requiresIssue()).isTrue();
        }

        @Test
        void withConsumableAuditResult_rejects_equipment_target() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));

            assertThatThrownBy(() -> item.withConsumableAuditResult(
                    AuditItemStatus.VERIFIED,
                    ExpiryStatus.OK,
                    QuantityComparison.of(10, 10),
                    null,
                    Instant.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot set consumable audit result on equipment target");
        }

        @Test
        void withConsumableAuditResult_preserves_location_fields() {
            var item = FormalAuditItem.unauditedConsumable(
                    itemId, auditId, ConsumableTarget.of(consumableId),
                    compartmentId, manifestEntryId
            );
            var auditedAt = Instant.now();

            var audited = item.withConsumableAuditResult(
                    AuditItemStatus.VERIFIED,
                    ExpiryStatus.OK,
                    QuantityComparison.of(50, 50),
                    null,
                    auditedAt
            );

            assertThat(audited.compartmentId()).isEqualTo(compartmentId);
            assertThat(audited.manifestEntryId()).isEqualTo(manifestEntryId);
            assertThat(audited.isUnexpected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Checking audit status")
    class CheckingAuditStatus {

        @Test
        void isAudited_returns_false_for_NOT_AUDITED() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));

            assertThat(item.isAudited()).isFalse();
        }

        @Test
        void isAudited_returns_true_for_any_other_status() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));
            var audited = item.withAuditResult(
                    AuditItemStatus.VERIFIED,
                    ItemCondition.GOOD,
                    TestResult.PASSED,
                    null,
                    null,
                    null,
                    Instant.now()
            );

            assertThat(audited.isAudited()).isTrue();
        }
    }

    @Nested
    @DisplayName("Issue creation requirement (BR-05)")
    class IssueCreationRequirement {

        @Test
        void requiresIssue_delegates_to_status() {
            var verifiedItem = createAuditedItem(AuditItemStatus.VERIFIED);
            var missingItem = createAuditedItem(AuditItemStatus.MISSING);
            var damagedItem = createAuditedItem(AuditItemStatus.DAMAGED);
            var failedItem = createAuditedItem(AuditItemStatus.FAILED_INSPECTION);
            var expiredItem = createAuditedItem(AuditItemStatus.EXPIRED);

            assertThat(verifiedItem.requiresIssue()).isFalse();
            assertThat(missingItem.requiresIssue()).isTrue();
            assertThat(damagedItem.requiresIssue()).isTrue();
            assertThat(failedItem.requiresIssue()).isTrue();
            assertThat(expiredItem.requiresIssue()).isTrue();
        }

        private FormalAuditItem createAuditedItem(AuditItemStatus status) {
            return new FormalAuditItem(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    null, null, false,
                    status, null, null, null, null, null, null, Instant.now()
            );
        }
    }

    @Nested
    @DisplayName("Optional field accessors")
    class OptionalAccessors {

        @Test
        void optional_methods_return_empty_for_null_values() {
            var item = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));

            assertThat(item.compartmentIdOpt()).isEmpty();
            assertThat(item.manifestEntryIdOpt()).isEmpty();
            assertThat(item.conditionOpt()).isEmpty();
            assertThat(item.testResultOpt()).isEmpty();
            assertThat(item.expiryStatusOpt()).isEmpty();
            assertThat(item.quantityComparisonOpt()).isEmpty();
            assertThat(item.conditionNotesOpt()).isEmpty();
            assertThat(item.testNotesOpt()).isEmpty();
            assertThat(item.auditedAtOpt()).isEmpty();
        }

        @Test
        void optional_methods_return_values_when_present() {
            var auditedAt = Instant.now();
            var item = new FormalAuditItem(
                    itemId, auditId, EquipmentTarget.of(equipmentId),
                    compartmentId, manifestEntryId, false,
                    AuditItemStatus.VERIFIED,
                    ItemCondition.GOOD,
                    TestResult.PASSED,
                    ExpiryStatus.OK,
                    null,
                    "Condition notes",
                    "Test notes",
                    auditedAt
            );

            assertThat(item.compartmentIdOpt()).contains(compartmentId);
            assertThat(item.manifestEntryIdOpt()).contains(manifestEntryId);
            assertThat(item.conditionOpt()).contains(ItemCondition.GOOD);
            assertThat(item.testResultOpt()).contains(TestResult.PASSED);
            assertThat(item.expiryStatusOpt()).contains(ExpiryStatus.OK);
            assertThat(item.conditionNotesOpt()).contains("Condition notes");
            assertThat(item.testNotesOpt()).contains("Test notes");
            assertThat(item.auditedAtOpt()).contains(auditedAt);
        }
    }

    @Nested
    @DisplayName("Value object semantics")
    class ValueObjectSemantics {

        @Test
        void withAuditResult_returns_new_instance() {
            var original = FormalAuditItem.unauditedEquipment(itemId, auditId, EquipmentTarget.of(equipmentId));

            var updated = original.withAuditResult(
                    AuditItemStatus.VERIFIED,
                    ItemCondition.GOOD,
                    TestResult.PASSED,
                    null,
                    null,
                    null,
                    Instant.now()
            );

            assertThat(updated).isNotSameAs(original);
            assertThat(original.status()).isEqualTo(AuditItemStatus.NOT_AUDITED);
        }
    }
}
