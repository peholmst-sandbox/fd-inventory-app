package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.apparatus.Apparatus;
import com.example.firestock.domain.apparatus.ApparatusStatus;
import com.example.firestock.domain.apparatus.ApparatusType;
import com.example.firestock.domain.apparatus.DecommissionedApparatus;
import com.example.firestock.domain.apparatus.InServiceApparatus;
import com.example.firestock.domain.apparatus.OutOfServiceApparatus;
import com.example.firestock.domain.apparatus.ReserveApparatus;
import com.example.firestock.jooq.tables.records.ApparatusRecord;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Mapper for converting between {@link Apparatus} domain objects and
 * {@link ApparatusRecord} jOOQ records.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Component
public class ApparatusMapper {

    /**
     * Converts a jOOQ record to the appropriate domain type based on status.
     *
     * @param record the jOOQ record
     * @return the domain apparatus in its correct state
     */
    public Apparatus toDomain(ApparatusRecord record) {
        if (record == null) {
            return null;
        }

        var status = toDomainStatus(record.getStatus());
        var year = record.getYear() != null ? Year.of(record.getYear()) : null;

        return switch (status) {
            case IN_SERVICE -> new InServiceApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    toDomainType(record.getType()),
                    record.getMake(),
                    record.getModel(),
                    year,
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case OUT_OF_SERVICE -> new OutOfServiceApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    toDomainType(record.getType()),
                    record.getMake(),
                    record.getModel(),
                    year,
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case RESERVE -> new ReserveApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    toDomainType(record.getType()),
                    record.getMake(),
                    record.getModel(),
                    year,
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case DECOMMISSIONED -> new DecommissionedApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    toDomainType(record.getType()),
                    record.getMake(),
                    record.getModel(),
                    year,
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
        };
    }

    /**
     * Updates a jOOQ record from a domain apparatus.
     *
     * @param record the record to update
     * @param apparatus the domain apparatus
     */
    public void updateRecord(ApparatusRecord record, Apparatus apparatus) {
        record.setId(apparatus.id());
        record.setUnitNumber(apparatus.unitNumber());
        record.setVin(apparatus.vin());
        record.setType(toJooqType(apparatus.type()));
        record.setMake(apparatus.make());
        record.setModel(apparatus.model());
        record.setYear(apparatus.year() != null ? apparatus.year().getValue() : null);
        record.setStationId(apparatus.stationId());
        record.setStatus(toJooqStatus(apparatus.status()));
        record.setBarcode(apparatus.barcode());
        record.setNotes(apparatus.notes());
    }

    /**
     * Converts a domain ApparatusStatus to the jOOQ enum.
     *
     * @param status the domain status
     * @return the jOOQ status enum
     */
    public com.example.firestock.jooq.enums.ApparatusStatus toJooqStatus(ApparatusStatus status) {
        return com.example.firestock.jooq.enums.ApparatusStatus.valueOf(status.name());
    }

    /**
     * Converts a jOOQ ApparatusStatus enum to the domain enum.
     *
     * @param status the jOOQ status enum
     * @return the domain status
     */
    public ApparatusStatus toDomainStatus(com.example.firestock.jooq.enums.ApparatusStatus status) {
        return ApparatusStatus.valueOf(status.name());
    }

    /**
     * Converts a domain ApparatusType to the jOOQ enum.
     *
     * @param type the domain type
     * @return the jOOQ type enum
     */
    public com.example.firestock.jooq.enums.ApparatusType toJooqType(ApparatusType type) {
        return com.example.firestock.jooq.enums.ApparatusType.valueOf(type.name());
    }

    /**
     * Converts a jOOQ ApparatusType enum to the domain enum.
     *
     * @param type the jOOQ type enum
     * @return the domain type
     */
    public ApparatusType toDomainType(com.example.firestock.jooq.enums.ApparatusType type) {
        return ApparatusType.valueOf(type.name());
    }
}
