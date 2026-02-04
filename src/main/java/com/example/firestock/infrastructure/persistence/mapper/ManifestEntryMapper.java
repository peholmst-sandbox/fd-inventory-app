package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.manifest.ManifestEntry;
import com.example.firestock.jooq.tables.records.ManifestEntryRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link ManifestEntry} domain objects and
 * {@link ManifestEntryRecord} jOOQ records.
 */
@Component
public class ManifestEntryMapper {

    /**
     * Converts a jOOQ record to the domain ManifestEntry.
     *
     * @param record the jOOQ record
     * @return the domain manifest entry
     */
    public ManifestEntry toDomain(ManifestEntryRecord record) {
        if (record == null) {
            return null;
        }

        return new ManifestEntry(
                record.getId(),
                record.getApparatusId(),
                record.getCompartmentId(),
                record.getEquipmentTypeId(),
                record.getRequiredQuantity(),
                Boolean.TRUE.equals(record.getIsCritical()),
                record.getDisplayOrder() != null ? record.getDisplayOrder() : 0,
                record.getNotes()
        );
    }

    /**
     * Updates a jOOQ record from a domain ManifestEntry.
     *
     * @param record the record to update
     * @param entry the domain manifest entry
     */
    public void updateRecord(ManifestEntryRecord record, ManifestEntry entry) {
        record.setId(entry.id());
        record.setApparatusId(entry.apparatusId());
        record.setCompartmentId(entry.compartmentId());
        record.setEquipmentTypeId(entry.equipmentTypeId());
        record.setRequiredQuantity(entry.requiredQuantity());
        record.setIsCritical(entry.isCritical());
        record.setDisplayOrder(entry.displayOrder());
        record.setNotes(entry.notes());
    }
}
