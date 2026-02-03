package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.apparatus.Compartment;
import com.example.firestock.domain.apparatus.CompartmentLocation;
import com.example.firestock.jooq.tables.records.CompartmentRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link Compartment} domain objects and
 * {@link CompartmentRecord} jOOQ records.
 */
@Component
public class CompartmentMapper {

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
                toDomainLocation(record.getLocation()),
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
        record.setLocation(toJooqLocation(compartment.location()));
        record.setDescription(compartment.description());
        record.setDisplayOrder(compartment.displayOrder());
    }

    /**
     * Converts a domain CompartmentLocation to the jOOQ enum.
     *
     * <p>Handles naming differences between domain and database:
     * <ul>
     *   <li>DRIVER_SIDE → LEFT_SIDE (Australian convention: driver is on the left)</li>
     *   <li>PASSENGER_SIDE → RIGHT_SIDE</li>
     * </ul>
     *
     * @param location the domain location
     * @return the jOOQ location enum
     */
    public com.example.firestock.jooq.enums.CompartmentLocation toJooqLocation(CompartmentLocation location) {
        return switch (location) {
            case DRIVER_SIDE -> com.example.firestock.jooq.enums.CompartmentLocation.LEFT_SIDE;
            case PASSENGER_SIDE -> com.example.firestock.jooq.enums.CompartmentLocation.RIGHT_SIDE;
            case FRONT -> com.example.firestock.jooq.enums.CompartmentLocation.FRONT;
            case REAR -> com.example.firestock.jooq.enums.CompartmentLocation.REAR;
            case TOP -> com.example.firestock.jooq.enums.CompartmentLocation.TOP;
            case INTERIOR -> com.example.firestock.jooq.enums.CompartmentLocation.INTERIOR;
            case CROSSLAY -> com.example.firestock.jooq.enums.CompartmentLocation.CROSSLAY;
        };
    }

    /**
     * Converts a jOOQ CompartmentLocation enum to the domain enum.
     *
     * <p>Handles naming differences between database and domain:
     * <ul>
     *   <li>LEFT_SIDE → DRIVER_SIDE (Australian convention: driver is on the left)</li>
     *   <li>RIGHT_SIDE → PASSENGER_SIDE</li>
     *   <li>HOSE_BED → REAR (treated as rear for domain purposes)</li>
     * </ul>
     *
     * @param location the jOOQ location enum
     * @return the domain location
     */
    public CompartmentLocation toDomainLocation(com.example.firestock.jooq.enums.CompartmentLocation location) {
        return switch (location) {
            case LEFT_SIDE -> CompartmentLocation.DRIVER_SIDE;
            case RIGHT_SIDE -> CompartmentLocation.PASSENGER_SIDE;
            case FRONT -> CompartmentLocation.FRONT;
            case REAR -> CompartmentLocation.REAR;
            case TOP -> CompartmentLocation.TOP;
            case INTERIOR -> CompartmentLocation.INTERIOR;
            case CROSSLAY -> CompartmentLocation.CROSSLAY;
            case HOSE_BED -> CompartmentLocation.REAR; // Map hose bed to rear as closest equivalent
        };
    }
}
