# Domain Concept: Consumable Stock

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

**Consumable Stock** represents a quantity of consumable items at a specific location. Unlike serialised equipment items, consumables are tracked by count or measure rather than individual identity. Stock records track current quantity, lot information, and expiry dates.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `equipmentTypeId` | UUID | Yes | Reference to equipment type (must be quantity-tracked) |
| `stationId` | UUID | No | Station (for storage location) |
| `apparatusId` | UUID | No | Apparatus where stock is located |
| `compartmentId` | UUID | No | Specific compartment |
| `quantity` | Decimal | Yes | Current quantity |
| `unit` | String | Yes | Unit of measure (e.g., "units", "litres", "boxes") |
| `requiredQuantity` | Decimal | No | Target/par level for this location |
| `lotNumber` | String | No | Batch/lot identifier |
| `expiryDate` | Date | No | Expiry date of current stock |
| `lastRestockDate` | Date | No | When stock was last replenished |
| `notes` | String | No | Additional notes |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Equipment Type | N:1 | Stock is of a specific type |
| Station | N:1 | Stock may be in station storage |
| Apparatus | N:1 | Stock may be on an apparatus |
| Compartment | N:1 | Stock may be in a specific compartment |
| Restock Record | 1:N | History of restocking events |

## 4. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Equipment type must have trackingMethod = QUANTITY |
| BR-02 | Quantity must be >= 0 |
| BR-03 | Only one stock record per equipment type per location |
| BR-04 | If type requiresExpiry, expiryDate should be tracked |
| BR-05 | Quantity below requiredQuantity triggers low-stock indicator |
| BR-06 | Multiple lots with different expiry dates may require separate records |

## 5. Stock Levels

| Indicator | Condition |
|-----------|-----------|
| **Critical** | quantity = 0 |
| **Low** | quantity < requiredQuantity * 0.5 |
| **Below Par** | quantity < requiredQuantity |
| **OK** | quantity >= requiredQuantity |
| **Overstocked** | quantity > requiredQuantity * 1.5 |

## 6. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440300",
  "equipmentTypeId": "550e8400-e29b-41d4-a716-446655440101",
  "stationId": null,
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "compartmentId": "550e8400-e29b-41d4-a716-446655440025",
  "quantity": 2,
  "unit": "containers",
  "requiredQuantity": 4,
  "lotNumber": "AFFF-2025-0892",
  "expiryDate": "2028-03-15",
  "lastRestockDate": "2025-11-20",
  "notes": null,
  "createdAt": "2024-01-15T09:00:00Z",
  "updatedAt": "2025-11-20T10:30:00Z"
}
```

## 7. Multi-Lot Handling

When consumables have different lot numbers or expiry dates, there are two approaches:

### Option A: Single Record with Earliest Expiry
- Track total quantity in one record
- expiryDate reflects the earliest expiring lot
- Simpler but less precise

### Option B: Multiple Records per Lot
- Separate record for each lot
- More accurate expiry tracking
- More complex restocking logic

**Recommendation:** Start with Option A; consider Option B for medical supplies where lot tracking is critical.

## 8. Quantity Adjustments

Stock quantities change through:

| Event | Effect |
|-------|--------|
| Restock | Quantity increases |
| Usage (post-incident) | Quantity decreases |
| Inventory check discrepancy | Quantity adjusted |
| Expiry disposal | Quantity decreases |
| Transfer | Quantity moves between locations |

All adjustments should be recorded with a reason.

## 9. Notes

- Consider separate "usage log" for detailed consumption tracking
- Expiry notifications should be generated before expiry date
- Stock value (cost) tracking is out of scope for v1

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |