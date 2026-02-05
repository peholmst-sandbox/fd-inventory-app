package com.example.firestock.domain.inventorycheck;

import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import com.example.firestock.domain.primitives.ids.InventoryCheckItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.jooq.enums.VerificationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Record representing an individual item verified during an inventory check.
 *
 * <p>Each check item captures the verification result for either an equipment item
 * or a consumable stock entry. The target type is enforced via the sealed
 * {@link CheckedItemTarget} interface, ensuring the XOR constraint between
 * equipment and consumable.
 *
 * @param id the unique identifier for this check item record
 * @param checkId the inventory check this item belongs to
 * @param target the equipment or consumable being verified (XOR constraint)
 * @param compartmentId the compartment where this item is located
 * @param manifestEntryId link to the manifest entry (nullable)
 * @param status the verification status of the item
 * @param quantityFound the actual quantity found (consumables only, nullable)
 * @param quantityExpected the expected quantity (consumables only, nullable)
 * @param conditionNotes free-form notes about the item's condition (nullable)
 * @param verifiedAt when this item was verified
 * @param issueId link to an issue created for this item (nullable)
 */
public record InventoryCheckItem(
        InventoryCheckItemId id,
        InventoryCheckId checkId,
        CheckedItemTarget target,
        CompartmentId compartmentId,
        ManifestEntryId manifestEntryId,
        VerificationStatus status,
        Quantity quantityFound,
        Quantity quantityExpected,
        String conditionNotes,
        Instant verifiedAt,
        IssueId issueId
) {

    public InventoryCheckItem {
        Objects.requireNonNull(id, "Check item ID cannot be null");
        Objects.requireNonNull(checkId, "Check ID cannot be null");
        Objects.requireNonNull(target, "Check target cannot be null");
        Objects.requireNonNull(compartmentId, "Compartment ID cannot be null");
        Objects.requireNonNull(status, "Verification status cannot be null");
        Objects.requireNonNull(verifiedAt, "Verified at cannot be null");

        // Validate that quantity fields are only set for consumable targets
        if ((quantityFound != null || quantityExpected != null) && target.isEquipment()) {
            throw new IllegalArgumentException("Quantity fields can only be set for consumable targets");
        }
    }

    /**
     * Creates a new check item for verifying equipment.
     *
     * @param id the check item ID
     * @param checkId the parent check ID
     * @param target the equipment target
     * @param compartmentId the compartment where the item is located
     * @param manifestEntryId the manifest entry this item is linked to (nullable)
     * @param status the verification status
     * @param conditionNotes notes about the item's condition (nullable)
     * @param verifiedAt when the item was verified
     * @return a new check item for equipment
     */
    public static InventoryCheckItem forEquipment(
            InventoryCheckItemId id,
            InventoryCheckId checkId,
            EquipmentCheckTarget target,
            CompartmentId compartmentId,
            ManifestEntryId manifestEntryId,
            VerificationStatus status,
            String conditionNotes,
            Instant verifiedAt
    ) {
        return new InventoryCheckItem(
                id, checkId, target, compartmentId, manifestEntryId,
                status, null, null, conditionNotes, verifiedAt, null
        );
    }

    /**
     * Creates a new check item for verifying consumables.
     *
     * @param id the check item ID
     * @param checkId the parent check ID
     * @param target the consumable target
     * @param compartmentId the compartment where the item is located
     * @param manifestEntryId the manifest entry this item is linked to (nullable)
     * @param status the verification status
     * @param quantityFound the actual quantity found
     * @param quantityExpected the expected quantity
     * @param conditionNotes notes about the item's condition (nullable)
     * @param verifiedAt when the item was verified
     * @return a new check item for consumables
     */
    public static InventoryCheckItem forConsumable(
            InventoryCheckItemId id,
            InventoryCheckId checkId,
            ConsumableCheckTarget target,
            CompartmentId compartmentId,
            ManifestEntryId manifestEntryId,
            VerificationStatus status,
            Quantity quantityFound,
            Quantity quantityExpected,
            String conditionNotes,
            Instant verifiedAt
    ) {
        return new InventoryCheckItem(
                id, checkId, target, compartmentId, manifestEntryId,
                status, quantityFound, quantityExpected, conditionNotes, verifiedAt, null
        );
    }

    /**
     * Checks if this item requires automatic issue creation.
     *
     * <p>Issues are automatically created for items with statuses:
     * MISSING, PRESENT_DAMAGED, EXPIRED, or LOW_QUANTITY.
     *
     * @return true if an issue should be created for this item
     */
    public boolean requiresIssue() {
        return status == VerificationStatus.MISSING
                || status == VerificationStatus.PRESENT_DAMAGED
                || status == VerificationStatus.EXPIRED
                || status == VerificationStatus.LOW_QUANTITY;
    }

    /**
     * Checks if this item has a quantity discrepancy (for consumables).
     *
     * @return true if the found quantity differs from expected
     */
    public boolean hasQuantityDiscrepancy() {
        if (target.isEquipment()) {
            return false;
        }
        if (quantityFound == null || quantityExpected == null) {
            return false;
        }
        return !quantityFound.equals(quantityExpected);
    }

    /**
     * Returns the manifest entry ID as an Optional.
     *
     * @return the manifest entry ID, or empty if not set
     */
    public Optional<ManifestEntryId> manifestEntryIdOpt() {
        return Optional.ofNullable(manifestEntryId);
    }

    /**
     * Returns the quantity found as an Optional.
     *
     * @return the quantity found, or empty if not applicable
     */
    public Optional<Quantity> quantityFoundOpt() {
        return Optional.ofNullable(quantityFound);
    }

    /**
     * Returns the quantity expected as an Optional.
     *
     * @return the quantity expected, or empty if not applicable
     */
    public Optional<Quantity> quantityExpectedOpt() {
        return Optional.ofNullable(quantityExpected);
    }

    /**
     * Returns the condition notes as an Optional.
     *
     * @return the condition notes, or empty if not set
     */
    public Optional<String> conditionNotesOpt() {
        return Optional.ofNullable(conditionNotes);
    }

    /**
     * Returns the issue ID as an Optional.
     *
     * @return the issue ID, or empty if no issue was created
     */
    public Optional<IssueId> issueIdOpt() {
        return Optional.ofNullable(issueId);
    }

    /**
     * Creates a copy with an associated issue.
     *
     * @param issueId the issue ID to associate
     * @return a new check item with the issue ID set
     */
    public InventoryCheckItem withIssue(IssueId issueId) {
        return new InventoryCheckItem(
                this.id, this.checkId, this.target, this.compartmentId,
                this.manifestEntryId, this.status, this.quantityFound,
                this.quantityExpected, this.conditionNotes, this.verifiedAt, issueId
        );
    }
}
