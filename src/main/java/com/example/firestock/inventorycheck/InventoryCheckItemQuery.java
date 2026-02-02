package com.example.firestock.inventorycheck;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.InventoryCheckId;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static com.example.firestock.jooq.Tables.INVENTORY_CHECK_ITEM;

/**
 * Query class for inventory check item read operations.
 */
@Component
class InventoryCheckItemQuery {

    private final DSLContext create;

    InventoryCheckItemQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Counts the number of verified items for a check.
     *
     * @param checkId the inventory check ID
     * @return the count of verified items
     */
    int countByCheckId(InventoryCheckId checkId) {
        return create.fetchCount(INVENTORY_CHECK_ITEM,
            INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId));
    }

    /**
     * Checks if an item has already been verified in this check.
     *
     * @param checkId the inventory check ID
     * @param equipmentItemId the equipment item ID, or null
     * @param consumableStockId the consumable stock ID, or null
     * @return true if already verified
     */
    boolean existsForItem(InventoryCheckId checkId, EquipmentItemId equipmentItemId,
                                  ConsumableStockId consumableStockId) {
        var condition = INVENTORY_CHECK_ITEM.INVENTORY_CHECK_ID.eq(checkId);

        if (equipmentItemId != null) {
            condition = condition.and(INVENTORY_CHECK_ITEM.EQUIPMENT_ITEM_ID.eq(equipmentItemId));
        } else if (consumableStockId != null) {
            condition = condition.and(INVENTORY_CHECK_ITEM.CONSUMABLE_STOCK_ID.eq(consumableStockId));
        } else {
            return false;
        }

        return create.fetchExists(INVENTORY_CHECK_ITEM, condition);
    }
}
