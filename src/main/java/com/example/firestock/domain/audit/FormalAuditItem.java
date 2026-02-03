package com.example.firestock.domain.audit;

import com.example.firestock.domain.primitives.ids.FormalAuditId;
import com.example.firestock.domain.primitives.ids.FormalAuditItemId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Record representing an individual item audited during a formal audit.
 *
 * <p>Each audit item captures the verification result for either an equipment item
 * or a consumable stock entry. The target type is enforced via the sealed
 * {@link AuditedItemTarget} interface, ensuring the XOR constraint between
 * equipment and consumable.
 *
 * @param id the unique identifier for this audit item record
 * @param auditId the formal audit this item belongs to
 * @param target the equipment or consumable being audited (XOR constraint)
 * @param status the verification status of the item
 * @param condition the physical condition assessment (equipment only, nullable)
 * @param testResult the functional test result (equipment only, nullable)
 * @param expiryStatus the expiry status (for items with expiration dates, nullable)
 * @param quantityComparison the quantity comparison (consumables only, nullable)
 * @param notes free-form notes about the audit finding
 * @param auditedAt when this item was audited (null if NOT_AUDITED)
 */
public record FormalAuditItem(
        FormalAuditItemId id,
        FormalAuditId auditId,
        AuditedItemTarget target,
        AuditItemStatus status,
        ItemCondition condition,
        TestResult testResult,
        ExpiryStatus expiryStatus,
        QuantityComparison quantityComparison,
        String notes,
        Instant auditedAt
) {

    public FormalAuditItem {
        Objects.requireNonNull(id, "Audit item ID cannot be null");
        Objects.requireNonNull(auditId, "Audit ID cannot be null");
        Objects.requireNonNull(target, "Audit target cannot be null");
        Objects.requireNonNull(status, "Audit item status cannot be null");

        // Validate that quantity comparison is only set for consumable targets
        if (quantityComparison != null && target.isEquipment()) {
            throw new IllegalArgumentException("Quantity comparison can only be set for consumable targets");
        }
    }

    /**
     * Creates a new unaudited equipment item.
     *
     * @param id the audit item ID
     * @param auditId the parent audit ID
     * @param target the equipment target
     * @return a new unaudited equipment item
     */
    public static FormalAuditItem unauditedEquipment(
            FormalAuditItemId id,
            FormalAuditId auditId,
            EquipmentTarget target
    ) {
        return new FormalAuditItem(
                id, auditId, target,
                AuditItemStatus.NOT_AUDITED,
                null, null, null, null, null, null
        );
    }

    /**
     * Creates a new unaudited consumable item.
     *
     * @param id the audit item ID
     * @param auditId the parent audit ID
     * @param target the consumable target
     * @return a new unaudited consumable item
     */
    public static FormalAuditItem unauditedConsumable(
            FormalAuditItemId id,
            FormalAuditId auditId,
            ConsumableTarget target
    ) {
        return new FormalAuditItem(
                id, auditId, target,
                AuditItemStatus.NOT_AUDITED,
                null, null, null, null, null, null
        );
    }

    /**
     * Checks if this item requires automatic issue creation per BR-05.
     *
     * <p>Issues are automatically created for items with statuses:
     * MISSING, DAMAGED, FAILED_INSPECTION, or EXPIRED.
     *
     * @return true if an issue should be created for this item
     */
    public boolean requiresIssue() {
        return status.requiresIssue();
    }

    /**
     * Checks if this item has been audited.
     *
     * @return true if the item has a status other than NOT_AUDITED
     */
    public boolean isAudited() {
        return status != AuditItemStatus.NOT_AUDITED;
    }

    /**
     * Returns the condition as an Optional.
     *
     * @return the condition, or empty if not set
     */
    public Optional<ItemCondition> conditionOpt() {
        return Optional.ofNullable(condition);
    }

    /**
     * Returns the test result as an Optional.
     *
     * @return the test result, or empty if not set
     */
    public Optional<TestResult> testResultOpt() {
        return Optional.ofNullable(testResult);
    }

    /**
     * Returns the expiry status as an Optional.
     *
     * @return the expiry status, or empty if not set
     */
    public Optional<ExpiryStatus> expiryStatusOpt() {
        return Optional.ofNullable(expiryStatus);
    }

    /**
     * Returns the quantity comparison as an Optional.
     *
     * @return the quantity comparison, or empty if not set
     */
    public Optional<QuantityComparison> quantityComparisonOpt() {
        return Optional.ofNullable(quantityComparison);
    }

    /**
     * Returns the notes as an Optional.
     *
     * @return the notes, or empty if not set
     */
    public Optional<String> notesOpt() {
        return Optional.ofNullable(notes);
    }

    /**
     * Returns the audited timestamp as an Optional.
     *
     * @return the audited timestamp, or empty if not audited
     */
    public Optional<Instant> auditedAtOpt() {
        return Optional.ofNullable(auditedAt);
    }

    /**
     * Creates a copy of this item with updated audit results.
     *
     * @param status the new status
     * @param condition the condition assessment
     * @param testResult the test result
     * @param expiryStatus the expiry status
     * @param notes any notes
     * @param auditedAt when the item was audited
     * @return a new audit item with the updated values
     */
    public FormalAuditItem withAuditResult(
            AuditItemStatus status,
            ItemCondition condition,
            TestResult testResult,
            ExpiryStatus expiryStatus,
            String notes,
            Instant auditedAt
    ) {
        return new FormalAuditItem(
                this.id, this.auditId, this.target,
                status, condition, testResult, expiryStatus,
                this.quantityComparison, notes, auditedAt
        );
    }

    /**
     * Creates a copy of this consumable item with updated audit results including quantity.
     *
     * @param status the new status
     * @param expiryStatus the expiry status
     * @param quantityComparison the quantity comparison
     * @param notes any notes
     * @param auditedAt when the item was audited
     * @return a new audit item with the updated values
     */
    public FormalAuditItem withConsumableAuditResult(
            AuditItemStatus status,
            ExpiryStatus expiryStatus,
            QuantityComparison quantityComparison,
            String notes,
            Instant auditedAt
    ) {
        if (target.isEquipment()) {
            throw new IllegalStateException("Cannot set consumable audit result on equipment target");
        }
        return new FormalAuditItem(
                this.id, this.auditId, this.target,
                status, null, null, expiryStatus,
                quantityComparison, notes, auditedAt
        );
    }
}
