# Domain Concept: Equipment Item

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

An **Equipment Item** is an individual, trackable piece of equipment identified by a unique serial number. Equipment items are instances of serialised equipment types and are assigned to specific apparatus and compartments.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `equipmentTypeId` | UUID | Yes | Reference to equipment type |
| `serialNumber` | String | Yes | Manufacturer or assigned serial number |
| `barcode` | String | No | Barcode for scanning (may differ from serial) |
| `manufacturer` | String | No | Item's manufacturer (may override type default) |
| `model` | String | No | Item's model (may override type default) |
| `acquisitionDate` | Date | No | When item was acquired |
| `warrantyExpiryDate` | Date | No | Warranty end date |
| `lastTestDate` | Date | No | Date of last periodic test |
| `nextTestDueDate` | Date | No | Calculated or manual next test date |
| `status` | EquipmentStatus | Yes | Current status |
| `stationId` | UUID | No | Station (for items in storage) |
| `apparatusId` | UUID | No | Assigned apparatus (null if in storage) |
| `compartmentId` | UUID | No | Assigned compartment |
| `notes` | String | No | General notes |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### EquipmentStatus
- `OK` — Operational and available
- `DAMAGED` — Damaged, needs repair
- `IN_REPAIR` — Currently being repaired
- `MISSING` — Cannot be located
- `FAILED_INSPECTION` — Failed formal inspection/audit, requires attention
- `RETIRED` — No longer in service
- `EXPIRED` — Past expiry date (for items with expiry)

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Equipment Type | N:1 | Item is an instance of a type |
| Station | N:1 | Item may be in station storage |
| Apparatus | N:1 | Item may be assigned to an apparatus |
| Compartment | N:1 | Item may be assigned to a compartment |
| Issue | 1:N | Item may have associated issues |
| Inventory Check Item | 1:N | Item is verified in inventory checks |
| Transfer Record | 1:N | Item has transfer history |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Serial number must be unique within the system |
| BR-02 | Barcode must be unique if specified |
| BR-03 | Item must be assigned to either an apparatus/compartment OR station storage, not both |
| BR-04 | Items with status RETIRED cannot be assigned to apparatus |
| BR-05 | Items with status DAMAGED or MISSING should have an associated issue |
| BR-06 | If type requiresTesting, nextTestDueDate should be tracked |
| BR-07 | Equipment type must have trackingMethod = SERIALISED |

## 6. Lifecycle

```
┌────────────┐     ┌─────┐     ┌─────────┐     ┌───────────┐     ┌─────────┐
│ Registered │────▶│ OK  │────▶│ DAMAGED │────▶│ IN_REPAIR │────▶│ RETIRED │
└────────────┘     └─────┘     └─────────┘     └───────────┘     └─────────┘
                     ▲               │               │
                     │               ▼               │
                     │         ┌─────────┐          │
                     │         │ MISSING │          │
                     │         └─────────┘          │
                     │               │               │
                     │    ┌──────────────────┐      │
                     │    │ FAILED_INSPECTION│      │
                     │    └──────────────────┘      │
                     │               │               │
                     └───────────────┴───────────────┘
                            (can return to OK)
```

## 7. Assignment States

An equipment item is in one of these assignment states:

| State | apparatusId | compartmentId | stationId | Description |
|-------|-------------|---------------|-----------|-------------|
| On Apparatus | Set | Set | (derived) | Assigned to specific compartment |
| In Station Storage | Null | Null | Set | Stored at station, not on apparatus |
| In Transit | Null | Null | Null | Being transferred (temporary state) |

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440200",
  "equipmentTypeId": "550e8400-e29b-41d4-a716-446655440100",
  "serialNumber": "SCT-2020-04523",
  "barcode": "EQ-SCBA-04523",
  "manufacturer": "Scott Safety",
  "model": "Air-Pak X3 Pro",
  "acquisitionDate": "2020-06-15",
  "warrantyExpiryDate": "2025-06-15",
  "lastTestDate": "2025-09-10",
  "nextTestDueDate": "2026-09-10",
  "status": "OK",
  "stationId": null,
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "compartmentId": "550e8400-e29b-41d4-a716-446655440020",
  "notes": "Bottle replaced 2024-03",
  "createdAt": "2020-06-20T10:00:00Z",
  "updatedAt": "2025-09-10T14:30:00Z"
}
```

## 9. Query Patterns

Common queries for equipment items:

| Query | Use Case |
|-------|----------|
| By apparatus | Inventory check, audit |
| By compartment | Compartment-specific view |
| By station (all apparatus + storage) | Station overview |
| By equipment type | Type-specific reports |
| By status (damaged, missing, etc.) | Issue management |
| By next test due | Maintenance planning |
| By serial number or barcode | Lookup during scanning |

## 10. Notes

- Equipment items have a complete audit trail via the Audit Log
- Photos may be associated with items (stored separately, linked by ID)
- Consider soft delete (status = RETIRED) rather than hard delete

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added FAILED_INSPECTION status (referenced by UC-03) |