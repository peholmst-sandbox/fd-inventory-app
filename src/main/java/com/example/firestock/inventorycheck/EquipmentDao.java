package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.jooq.enums.EquipmentStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.EQUIPMENT_ITEM;

/**
 * DAO class for equipment write operations.
 */
@Component
class EquipmentDao {

    private final DSLContext create;

    EquipmentDao(DSLContext create) {
        this.create = create;
    }

    /**
     * Updates the status of an equipment item.
     *
     * @param id the equipment item ID
     * @param status the new status
     */
    void updateStatus(EquipmentItemId id, EquipmentStatus status) {
        create.update(EQUIPMENT_ITEM)
            .set(EQUIPMENT_ITEM.STATUS, status)
            .where(EQUIPMENT_ITEM.ID.eq(id))
            .execute();
    }
}
