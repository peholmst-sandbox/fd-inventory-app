package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.equipment.EquipmentItem;
import com.example.firestock.jooq.tables.records.EquipmentItemRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link EquipmentItem} domain objects and
 * {@link EquipmentItemRecord} jOOQ records.
 */
@Component
public class EquipmentItemMapper {

    /**
     * Converts a jOOQ record to the domain EquipmentItem.
     *
     * @param record the jOOQ record
     * @return the domain equipment item, or null if record is null
     */
    public EquipmentItem toDomain(EquipmentItemRecord record) {
        if (record == null) {
            return null;
        }

        return new EquipmentItem(
                record.getId(),
                record.getEquipmentTypeId(),
                record.getSerialNumber(),
                record.getBarcode(),
                record.getOwnershipType(),
                record.getHomeStationId(),
                record.getManufacturer(),
                record.getModel(),
                record.getAcquisitionDate(),
                record.getWarrantyExpiryDate(),
                record.getLastTestDate(),
                record.getNextTestDueDate(),
                record.getStatus(),
                record.getStationId(),
                record.getApparatusId(),
                record.getCompartmentId(),
                record.getNotes()
        );
    }

    /**
     * Updates a jOOQ record from a domain EquipmentItem.
     *
     * @param record the record to update
     * @param item the domain equipment item
     */
    public void updateRecord(EquipmentItemRecord record, EquipmentItem item) {
        record.setId(item.id());
        record.setEquipmentTypeId(item.equipmentTypeId());
        record.setSerialNumber(item.serialNumber());
        record.setBarcode(item.barcode());
        record.setOwnershipType(item.ownershipType());
        record.setHomeStationId(item.homeStationId());
        record.setManufacturer(item.manufacturer());
        record.setModel(item.model());
        record.setAcquisitionDate(item.acquisitionDate());
        record.setWarrantyExpiryDate(item.warrantyExpiryDate());
        record.setLastTestDate(item.lastTestDate());
        record.setNextTestDueDate(item.nextTestDueDate());
        record.setStatus(item.status());
        record.setStationId(item.stationId());
        record.setApparatusId(item.apparatusId());
        record.setCompartmentId(item.compartmentId());
        record.setNotes(item.notes());
    }
}
