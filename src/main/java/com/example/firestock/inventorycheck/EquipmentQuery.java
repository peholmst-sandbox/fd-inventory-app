package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.strings.Barcode;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.example.firestock.jooq.Tables.*;

/**
 * Query class for equipment read operations.
 */
@Component
class EquipmentQuery {

    private final DSLContext create;

    EquipmentQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds an item by barcode for a specific apparatus.
     *
     * <p>Searches both equipment items and consumable stock entries.
     *
     * @param barcode the barcode to search for
     * @param apparatusId the apparatus to search within
     * @return the checkable item, or empty if not found
     */
    Optional<CheckableItem> findByBarcode(Barcode barcode, ApparatusId apparatusId) {
        // First, try to find an equipment item with this barcode
        var equipment = create.select(
                EQUIPMENT_ITEM.ID,
                EQUIPMENT_TYPE.NAME,
                EQUIPMENT_TYPE.NAME.as("type_name"),
                EQUIPMENT_ITEM.SERIAL_NUMBER,
                EQUIPMENT_ITEM.BARCODE)
            .from(EQUIPMENT_ITEM)
            .join(EQUIPMENT_TYPE).on(EQUIPMENT_ITEM.EQUIPMENT_TYPE_ID.eq(EQUIPMENT_TYPE.ID))
            .where(EQUIPMENT_ITEM.BARCODE.eq(barcode))
            .and(EQUIPMENT_ITEM.APPARATUS_ID.eq(apparatusId))
            .fetchOptional();

        if (equipment.isPresent()) {
            var eq = equipment.get();
            return Optional.of(CheckableItem.forEquipment(
                eq.value1(),
                eq.value2(),
                eq.value3(),
                eq.value4(),
                eq.value5()
            ));
        }

        // If not found, there's no barcode field on consumable_stock,
        // so we can only find equipment by barcode
        return Optional.empty();
    }
}
