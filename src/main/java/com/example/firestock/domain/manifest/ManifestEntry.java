package com.example.firestock.domain.manifest;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.domain.primitives.ids.EquipmentTypeId;
import com.example.firestock.domain.primitives.ids.ManifestEntryId;
import com.example.firestock.domain.primitives.numbers.RequiredQuantity;

import java.util.Objects;
import java.util.Optional;

/**
 * A manifest entry defining expected equipment in an apparatus compartment.
 *
 * <p>The manifest defines what equipment should be present on each apparatus.
 * During inventory checks and audits, the actual equipment is verified against
 * the manifest to identify missing, excess, or unexpected items.
 *
 * <p>Key concepts:
 * <ul>
 *   <li><b>Critical items:</b> Essential equipment that must be present and
 *       verified every check (e.g., SCBA, medical supplies)</li>
 *   <li><b>Non-critical items:</b> Equipment that should be present but may
 *       have more flexible verification requirements</li>
 *   <li><b>Required quantity:</b> The expected count of items for this entry</li>
 * </ul>
 *
 * @param id the unique identifier for this manifest entry
 * @param apparatusId the apparatus this entry belongs to
 * @param compartmentId the compartment where the equipment should be located
 * @param equipmentTypeId the type of equipment expected
 * @param requiredQuantity how many items of this type are required
 * @param isCritical whether this is critical equipment requiring strict verification
 * @param displayOrder order for display in lists (lower numbers first)
 * @param notes optional notes about this manifest entry
 */
public record ManifestEntry(
        ManifestEntryId id,
        ApparatusId apparatusId,
        CompartmentId compartmentId,
        EquipmentTypeId equipmentTypeId,
        RequiredQuantity requiredQuantity,
        boolean isCritical,
        int displayOrder,
        String notes
) {

    public ManifestEntry {
        Objects.requireNonNull(id, "Manifest entry ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(compartmentId, "Compartment ID cannot be null");
        Objects.requireNonNull(equipmentTypeId, "Equipment type ID cannot be null");
        Objects.requireNonNull(requiredQuantity, "Required quantity cannot be null");

        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order must be non-negative");
        }
    }

    /**
     * Creates a critical manifest entry (equipment must be present and verified).
     *
     * @param id the entry ID
     * @param apparatusId the apparatus ID
     * @param compartmentId the compartment ID
     * @param equipmentTypeId the equipment type ID
     * @param requiredQuantity the required quantity
     * @return a new critical manifest entry
     */
    public static ManifestEntry critical(
            ManifestEntryId id,
            ApparatusId apparatusId,
            CompartmentId compartmentId,
            EquipmentTypeId equipmentTypeId,
            RequiredQuantity requiredQuantity
    ) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, true, 0, null
        );
    }

    /**
     * Creates a critical manifest entry with a specific quantity.
     *
     * @param id the entry ID
     * @param apparatusId the apparatus ID
     * @param compartmentId the compartment ID
     * @param equipmentTypeId the equipment type ID
     * @param quantity the required quantity as an integer
     * @return a new critical manifest entry
     */
    public static ManifestEntry critical(
            ManifestEntryId id,
            ApparatusId apparatusId,
            CompartmentId compartmentId,
            EquipmentTypeId equipmentTypeId,
            int quantity
    ) {
        return critical(id, apparatusId, compartmentId, equipmentTypeId, RequiredQuantity.of(quantity));
    }

    /**
     * Creates a non-critical (optional) manifest entry.
     *
     * @param id the entry ID
     * @param apparatusId the apparatus ID
     * @param compartmentId the compartment ID
     * @param equipmentTypeId the equipment type ID
     * @param requiredQuantity the required quantity
     * @return a new non-critical manifest entry
     */
    public static ManifestEntry optional(
            ManifestEntryId id,
            ApparatusId apparatusId,
            CompartmentId compartmentId,
            EquipmentTypeId equipmentTypeId,
            RequiredQuantity requiredQuantity
    ) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, false, 0, null
        );
    }

    /**
     * Creates a non-critical manifest entry with a specific quantity.
     *
     * @param id the entry ID
     * @param apparatusId the apparatus ID
     * @param compartmentId the compartment ID
     * @param equipmentTypeId the equipment type ID
     * @param quantity the required quantity as an integer
     * @return a new non-critical manifest entry
     */
    public static ManifestEntry optional(
            ManifestEntryId id,
            ApparatusId apparatusId,
            CompartmentId compartmentId,
            EquipmentTypeId equipmentTypeId,
            int quantity
    ) {
        return optional(id, apparatusId, compartmentId, equipmentTypeId, RequiredQuantity.of(quantity));
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
     * Creates a copy marked as critical.
     *
     * @return a new manifest entry marked as critical
     */
    public ManifestEntry asCritical() {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, true, displayOrder, notes
        );
    }

    /**
     * Creates a copy marked as non-critical (optional).
     *
     * @return a new manifest entry marked as non-critical
     */
    public ManifestEntry asOptional() {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, false, displayOrder, notes
        );
    }

    /**
     * Creates a copy with an updated required quantity.
     *
     * @param requiredQuantity the new required quantity
     * @return a new manifest entry with the updated quantity
     */
    public ManifestEntry withRequiredQuantity(RequiredQuantity requiredQuantity) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, isCritical, displayOrder, notes
        );
    }

    /**
     * Creates a copy with an updated required quantity.
     *
     * @param quantity the new required quantity as an integer
     * @return a new manifest entry with the updated quantity
     */
    public ManifestEntry withRequiredQuantity(int quantity) {
        return withRequiredQuantity(RequiredQuantity.of(quantity));
    }

    /**
     * Creates a copy with an updated display order.
     *
     * @param displayOrder the new display order
     * @return a new manifest entry with the updated display order
     */
    public ManifestEntry withDisplayOrder(int displayOrder) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, isCritical, displayOrder, notes
        );
    }

    /**
     * Creates a copy with updated notes.
     *
     * @param notes the new notes
     * @return a new manifest entry with the updated notes
     */
    public ManifestEntry withNotes(String notes) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, isCritical, displayOrder, notes
        );
    }

    /**
     * Creates a copy moved to a different compartment.
     *
     * @param compartmentId the new compartment ID
     * @return a new manifest entry in the specified compartment
     */
    public ManifestEntry moveToCompartment(CompartmentId compartmentId) {
        return new ManifestEntry(
                id, apparatusId, compartmentId, equipmentTypeId,
                requiredQuantity, isCritical, displayOrder, notes
        );
    }
}
