package com.example.firestock.audit;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.jooq.enums.EquipmentStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.CONSUMABLE_STOCK;
import static com.example.firestock.jooq.Tables.EQUIPMENT_ITEM;

/**
 * DAO class for equipment operations used by formal audits.
 */
@Component("auditEquipmentDao")
class AuditEquipmentDao {

    private final DSLContext create;

    AuditEquipmentDao(DSLContext create) {
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

    /**
     * Counts the total number of items (equipment + consumables) on an apparatus.
     *
     * @param apparatusId the apparatus ID
     * @return the total item count
     */
    int countItemsOnApparatus(ApparatusId apparatusId) {
        int equipmentCount = create.fetchCount(EQUIPMENT_ITEM,
                EQUIPMENT_ITEM.APPARATUS_ID.eq(apparatusId));

        int consumableCount = create.fetchCount(CONSUMABLE_STOCK,
                CONSUMABLE_STOCK.APPARATUS_ID.eq(apparatusId));

        return equipmentCount + consumableCount;
    }
}
