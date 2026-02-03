package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.example.firestock.jooq.Tables.*;

/**
 * Query class for equipment lookup operations used in issue reporting.
 * Supports station-agnostic lookups (barcode/serial are globally unique).
 */
@Component
public class EquipmentLookupQuery {

    private final DSLContext create;

    public EquipmentLookupQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds equipment by barcode.
     * Barcodes are globally unique, so no station filtering is required.
     *
     * @param barcode the barcode to search for
     * @return the equipment for report, or empty if not found
     */
    public Optional<EquipmentForReport> findByBarcode(Barcode barcode) {
        return create.select(
                EQUIPMENT_ITEM.ID,
                EQUIPMENT_TYPE.NAME,
                EQUIPMENT_ITEM.SERIAL_NUMBER,
                EQUIPMENT_ITEM.BARCODE,
                EQUIPMENT_ITEM.STATUS,
                EQUIPMENT_ITEM.APPARATUS_ID,
                APPARATUS.UNIT_NUMBER,
                APPARATUS.STATION_ID,
                STATION.NAME
            )
            .from(EQUIPMENT_ITEM)
            .join(EQUIPMENT_TYPE).on(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
            .leftJoin(APPARATUS).on(EQUIPMENT_ITEM.APPARATUS_ID.eq(APPARATUS.ID))
            .leftJoin(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .where(EQUIPMENT_ITEM.BARCODE.eq(barcode))
            .fetchOptional(r -> new EquipmentForReport(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7() != null ? r.value7().value() : null,
                r.value8(),
                r.value9()
            ));
    }

    /**
     * Finds equipment by serial number.
     * Serial numbers are globally unique, so no station filtering is required.
     *
     * @param serialNumber the serial number to search for
     * @return the equipment for report, or empty if not found
     */
    public Optional<EquipmentForReport> findBySerialNumber(SerialNumber serialNumber) {
        return create.select(
                EQUIPMENT_ITEM.ID,
                EQUIPMENT_TYPE.NAME,
                EQUIPMENT_ITEM.SERIAL_NUMBER,
                EQUIPMENT_ITEM.BARCODE,
                EQUIPMENT_ITEM.STATUS,
                EQUIPMENT_ITEM.APPARATUS_ID,
                APPARATUS.UNIT_NUMBER,
                APPARATUS.STATION_ID,
                STATION.NAME
            )
            .from(EQUIPMENT_ITEM)
            .join(EQUIPMENT_TYPE).on(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
            .leftJoin(APPARATUS).on(EQUIPMENT_ITEM.APPARATUS_ID.eq(APPARATUS.ID))
            .leftJoin(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .where(EQUIPMENT_ITEM.SERIAL_NUMBER.eq(serialNumber))
            .fetchOptional(r -> new EquipmentForReport(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7() != null ? r.value7().value() : null,
                r.value8(),
                r.value9()
            ));
    }

    /**
     * Gets full equipment details by ID.
     *
     * @param id the equipment item ID
     * @return the equipment for report, or empty if not found
     */
    public Optional<EquipmentForReport> getEquipmentDetails(EquipmentItemId id) {
        return create.select(
                EQUIPMENT_ITEM.ID,
                EQUIPMENT_TYPE.NAME,
                EQUIPMENT_ITEM.SERIAL_NUMBER,
                EQUIPMENT_ITEM.BARCODE,
                EQUIPMENT_ITEM.STATUS,
                EQUIPMENT_ITEM.APPARATUS_ID,
                APPARATUS.UNIT_NUMBER,
                APPARATUS.STATION_ID,
                STATION.NAME
            )
            .from(EQUIPMENT_ITEM)
            .join(EQUIPMENT_TYPE).on(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
            .leftJoin(APPARATUS).on(EQUIPMENT_ITEM.APPARATUS_ID.eq(APPARATUS.ID))
            .leftJoin(STATION).on(APPARATUS.STATION_ID.eq(STATION.ID))
            .where(EQUIPMENT_ITEM.ID.eq(id))
            .fetchOptional(r -> new EquipmentForReport(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7() != null ? r.value7().value() : null,
                r.value8(),
                r.value9()
            ));
    }
}
