package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.apparatus.Apparatus;
import com.example.firestock.domain.apparatus.DecommissionedApparatus;
import com.example.firestock.domain.apparatus.InServiceApparatus;
import com.example.firestock.domain.apparatus.OutOfServiceApparatus;
import com.example.firestock.domain.apparatus.ReserveApparatus;
import com.example.firestock.jooq.tables.records.ApparatusRecord;
import org.springframework.stereotype.Component;

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

        var status = record.getStatus();

        return switch (status) {
            case IN_SERVICE -> new InServiceApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    record.getType(),
                    record.getMake(),
                    record.getModel(),
                    record.getYear(),
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case OUT_OF_SERVICE -> new OutOfServiceApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    record.getType(),
                    record.getMake(),
                    record.getModel(),
                    record.getYear(),
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case RESERVE -> new ReserveApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    record.getType(),
                    record.getMake(),
                    record.getModel(),
                    record.getYear(),
                    record.getStationId(),
                    record.getBarcode(),
                    record.getNotes()
            );
            case DECOMMISSIONED -> new DecommissionedApparatus(
                    record.getId(),
                    record.getUnitNumber(),
                    record.getVin(),
                    record.getType(),
                    record.getMake(),
                    record.getModel(),
                    record.getYear(),
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
        record.setType(apparatus.type());
        record.setMake(apparatus.make());
        record.setModel(apparatus.model());
        record.setYear(apparatus.year());
        record.setStationId(apparatus.stationId());
        record.setStatus(apparatus.status());
        record.setBarcode(apparatus.barcode());
        record.setNotes(apparatus.notes());
    }
}
