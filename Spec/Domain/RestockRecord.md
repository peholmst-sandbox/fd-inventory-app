# Domain Concept: Restock Record

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

A **Restock Record** documents the addition, removal, or adjustment of consumable stock quantities. It provides an audit trail for consumable inventory changes and supports tracking of usage patterns.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `referenceNumber` | String | Yes | Human-readable identifier (e.g., "RST-2026-00789") |
| `consumableStockId` | UUID | Yes | Consumable stock being modified |
| `operationType` | RestockOperationType | Yes | Type of stock operation |
| `quantityBefore` | Integer | Yes | Quantity before operation |
| `quantityChange` | Integer | Yes | Amount added (positive) or removed (negative) |
| `quantityAfter` | Integer | Yes | Quantity after operation |
| `reason` | String | Yes | Reason for the stock change |
| `lotNumber` | String | No | Lot/batch number (for tracking) |
| `expiryDate` | Date | No | Expiry date of restocked items |
| `performedById` | UUID | Yes | User who performed the operation |
| `performedAt` | Timestamp | Yes | When operation was performed |
| `notes` | String | No | Additional notes |
| `createdAt` | Timestamp | Yes | When record was created |

## 3. Enumerations

### RestockOperationType
- `RESTOCK` — Adding new stock (regular replenishment)
- `USAGE` — Recording consumption/usage
- `ADJUSTMENT` — Inventory correction (count adjustment)
- `EXPIRY_REMOVAL` — Removing expired stock
- `DAMAGE_REMOVAL` — Removing damaged stock
- `TRANSFER_IN` — Stock received from another location
- `TRANSFER_OUT` — Stock sent to another location
- `INITIAL` — Initial stock entry when setting up

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Consumable Stock | N:1 | Record is for one consumable stock entry |
| User | N:1 | User who performed the operation |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Reference number must be unique and auto-generated |
| BR-02 | quantityAfter must equal quantityBefore + quantityChange |
| BR-03 | quantityAfter must be >= 0 (cannot go negative) |
| BR-04 | RESTOCK and TRANSFER_IN must have positive quantityChange |
| BR-05 | USAGE, EXPIRY_REMOVAL, DAMAGE_REMOVAL, TRANSFER_OUT must have negative quantityChange |
| BR-06 | ADJUSTMENT can have positive or negative quantityChange |
| BR-07 | If consumable type requires expiry tracking, expiryDate should be set for RESTOCK |
| BR-08 | Creating a restock record updates the consumable stock quantity |

## 6. Stock Change Flow

```
┌──────────────────┐
│  Restock Action  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐     ┌──────────────────┐
│ Validate Change  │────▶│  Create Record   │
│ (quantity >= 0)  │     │  (with before/   │
└──────────────────┘     │   after values)  │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Update Consumable│
                         │  Stock Quantity  │
                         └──────────────────┘
```

## 7. Example (Restock)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440900",
  "referenceNumber": "RST-2026-00123",
  "consumableStockId": "550e8400-e29b-41d4-a716-446655440250",
  "operationType": "RESTOCK",
  "quantityBefore": 3,
  "quantityChange": 17,
  "quantityAfter": 20,
  "reason": "Regular weekly restock",
  "lotNumber": "LOT-2026-0115",
  "expiryDate": "2027-01-15",
  "performedById": "550e8400-e29b-41d4-a716-446655440301",
  "performedAt": "2026-01-30T09:00:00Z",
  "notes": null,
  "createdAt": "2026-01-30T09:00:00Z"
}
```

## 8. Example (Usage Recording)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440901",
  "referenceNumber": "RST-2026-00124",
  "consumableStockId": "550e8400-e29b-41d4-a716-446655440250",
  "operationType": "USAGE",
  "quantityBefore": 20,
  "quantityChange": -5,
  "quantityAfter": 15,
  "reason": "Used during medical call - Incident #2026-01-30-001",
  "lotNumber": null,
  "expiryDate": null,
  "performedById": "550e8400-e29b-41d4-a716-446655440300",
  "performedAt": "2026-01-30T14:30:00Z",
  "notes": "5 bandage rolls used for patient care",
  "createdAt": "2026-01-30T14:30:00Z"
}
```

## 9. Example (Expiry Replacement)

When replacing expired stock, create two records:

**Record 1: Remove expired**
```json
{
  "operationType": "EXPIRY_REMOVAL",
  "quantityBefore": 10,
  "quantityChange": -4,
  "quantityAfter": 6,
  "reason": "Removing expired stock",
  "expiryDate": "2026-01-15"
}
```

**Record 2: Add new**
```json
{
  "operationType": "RESTOCK",
  "quantityBefore": 6,
  "quantityChange": 4,
  "quantityAfter": 10,
  "reason": "Replacing expired stock",
  "lotNumber": "LOT-2026-0130",
  "expiryDate": "2027-01-30"
}
```

## 10. Query Patterns

| Query | Use Case |
|-------|----------|
| By consumable stock | Stock history for item |
| By apparatus (via consumable stock) | Apparatus consumption patterns |
| By operation type | Track specific operations |
| By performer | User's restock activity |
| By date range | Usage/restock reporting |
| By operation type = USAGE | Consumption analysis |

## 11. Metrics & Analytics

Key metrics derivable from restock records:
- Consumption rate per consumable type
- Average time between restocks
- Stock adjustment frequency (inventory accuracy indicator)
- Expiry removal rates (waste tracking)
- Usage patterns by time of day/week

## 12. Multi-Lot Tracking

For consumables with expiry dates, the recommended approach is:
1. Use single ConsumableStock record per apparatus/compartment
2. Track newest expiryDate in ConsumableStock
3. Use RestockRecord.expiryDate and lotNumber for detailed lot history
4. Query restock records to analyze lot-level details when needed

This balances simplicity (single quantity to check) with traceability (full lot history in records).

## 13. Notes

- Restock records should never be modified after creation (immutable audit trail)
- Consider integration with procurement systems for automated restock tracking
- Usage recording may be simplified with "quick usage" feature post-incident
- High adjustment frequency may indicate inventory control issues

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
