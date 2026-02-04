package com.example.firestock.domain.apparatus;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.CompartmentId;
import com.example.firestock.jooq.enums.CompartmentLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * A physical storage area on an apparatus where equipment is kept.
 *
 * <p>Compartments provide organisational structure for equipment and enable
 * systematic inventory checks. Each compartment has a code (for quick reference),
 * a name (for display), and a physical location on the apparatus.
 *
 * <p>During inventory checks and audits, personnel work through compartments
 * systematically, verifying all equipment in each location.
 *
 * @param id the unique identifier for this compartment
 * @param apparatusId the apparatus this compartment belongs to
 * @param code short code for quick reference (e.g., "D1", "P3", "REAR")
 * @param name descriptive name (e.g., "Driver Side Compartment 1")
 * @param location physical location on the apparatus
 * @param description optional detailed description
 * @param displayOrder order for display in lists (lower numbers first)
 */
public record Compartment(
        CompartmentId id,
        ApparatusId apparatusId,
        String code,
        String name,
        CompartmentLocation location,
        String description,
        int displayOrder
) {

    public Compartment {
        Objects.requireNonNull(id, "Compartment ID cannot be null");
        Objects.requireNonNull(apparatusId, "Apparatus ID cannot be null");
        Objects.requireNonNull(code, "Code cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");

        code = code.strip();
        name = name.strip();

        if (code.isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }
        if (code.length() > 20) {
            throw new IllegalArgumentException("Code must be 20 characters or less");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Name must be 100 characters or less");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order must be non-negative");
        }
    }

    /**
     * Creates a new compartment with required fields only.
     *
     * @param id the compartment ID
     * @param apparatusId the apparatus ID
     * @param code the compartment code
     * @param name the compartment name
     * @param location the physical location
     * @return a new compartment
     */
    public static Compartment create(
            CompartmentId id,
            ApparatusId apparatusId,
            String code,
            String name,
            CompartmentLocation location
    ) {
        return new Compartment(id, apparatusId, code, name, location, null, 0);
    }

    /**
     * Creates a new compartment with display order.
     *
     * @param id the compartment ID
     * @param apparatusId the apparatus ID
     * @param code the compartment code
     * @param name the compartment name
     * @param location the physical location
     * @param displayOrder the display order
     * @return a new compartment
     */
    public static Compartment create(
            CompartmentId id,
            ApparatusId apparatusId,
            String code,
            String name,
            CompartmentLocation location,
            int displayOrder
    ) {
        return new Compartment(id, apparatusId, code, name, location, null, displayOrder);
    }

    /**
     * Returns the description as an Optional.
     *
     * @return the description, or empty if not set
     */
    public Optional<String> descriptionOpt() {
        return Optional.ofNullable(description);
    }

    /**
     * Creates a copy with an updated description.
     *
     * @param description the new description
     * @return a new compartment with the updated description
     */
    public Compartment withDescription(String description) {
        return new Compartment(id, apparatusId, code, name, location, description, displayOrder);
    }

    /**
     * Creates a copy with an updated display order.
     *
     * @param displayOrder the new display order
     * @return a new compartment with the updated display order
     */
    public Compartment withDisplayOrder(int displayOrder) {
        return new Compartment(id, apparatusId, code, name, location, description, displayOrder);
    }

    /**
     * Creates a copy with an updated name.
     *
     * @param name the new name
     * @return a new compartment with the updated name
     */
    public Compartment withName(String name) {
        return new Compartment(id, apparatusId, code, name, location, description, displayOrder);
    }

    /**
     * Creates a copy with an updated code.
     *
     * @param code the new code
     * @return a new compartment with the updated code
     */
    public Compartment withCode(String code) {
        return new Compartment(id, apparatusId, code, name, location, description, displayOrder);
    }
}
