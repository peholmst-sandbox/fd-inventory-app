package com.example.firestock.infrastructure.persistence;

import com.example.firestock.domain.apparatus.Compartment;
import com.example.firestock.jooq.tables.records.CompartmentRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link Compartment} domain objects and
 * {@link CompartmentRecord} jOOQ records.
 */
@Component
class CompartmentMapper {

    /**
     * Converts a jOOQ record to the domain Compartment.
     *
     * @param record the jOOQ record
     * @return the domain compartment
     */
    public Compartment toDomain(CompartmentRecord record) {
        if (record == null) {
            return null;
        }

        return new Compartment(
                record.getId(),
                record.getApparatusId(),
                record.getCode(),
                record.getName(),
                record.getLocation(),
                record.getDescription(),
                record.getDisplayOrder() != null ? record.getDisplayOrder() : 0
        );
    }

    /**
     * Updates a jOOQ record from a domain Compartment.
     *
     * @param record the record to update
     * @param compartment the domain compartment
     */
    public void updateRecord(CompartmentRecord record, Compartment compartment) {
        record.setId(compartment.id());
        record.setApparatusId(compartment.apparatusId());
        record.setCode(compartment.code());
        record.setName(compartment.name());
        record.setLocation(compartment.location());
        record.setDescription(compartment.description());
        record.setDisplayOrder(compartment.displayOrder());
    }
}
