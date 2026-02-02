package com.example.firestock.inventorycheck.dto;

import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.numbers.Quantity;
import com.example.firestock.domain.primitives.strings.Barcode;
import com.example.firestock.domain.primitives.strings.SerialNumber;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an item that can be verified during an inventory check.
 *
 * <p>This is a union type representing either an equipment item or a consumable stock entry.
 * Exactly one of {@code equipmentItemId} or {@code consumableStockId} will be non-null.
 *
 * @param equipmentItemId the equipment item ID, or null if this is a consumable
 * @param consumableStockId the consumable stock ID, or null if this is equipment
 * @param name the display name of the item or consumable type
 * @param typeName the equipment type name
 * @param serialNumber the serial number for equipment, null for consumables
 * @param barcode the barcode if assigned, null otherwise
 * @param isConsumable true if this is a consumable stock entry
 * @param requiredQuantity for consumables, the quantity that should be present
 * @param currentQuantity for consumables, the current recorded quantity
 * @param expiryDate the expiry date if applicable, null otherwise
 */
public record CheckableItem(
    EquipmentItemId equipmentItemId,
    ConsumableStockId consumableStockId,
    String name,
    String typeName,
    SerialNumber serialNumber,
    Barcode barcode,
    boolean isConsumable,
    BigDecimal requiredQuantity,
    Quantity currentQuantity,
    LocalDate expiryDate
) {
    /**
     * Creates a CheckableItem for an equipment item.
     */
    public static CheckableItem forEquipment(
            EquipmentItemId id,
            String name,
            String typeName,
            SerialNumber serialNumber,
            Barcode barcode) {
        return new CheckableItem(id, null, name, typeName, serialNumber, barcode, false, null, null, null);
    }

    /**
     * Creates a CheckableItem for a consumable stock entry.
     */
    public static CheckableItem forConsumable(
            ConsumableStockId id,
            String name,
            String typeName,
            BigDecimal requiredQuantity,
            Quantity currentQuantity,
            LocalDate expiryDate) {
        return new CheckableItem(null, id, name, typeName, null, null, true, requiredQuantity, currentQuantity, expiryDate);
    }
}
