# Domain Concept: Apparatus

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

An **Apparatus** is a fire service vehicle (fire engine, ladder truck, rescue unit, etc.) that carries equipment and responds to incidents. Each apparatus has a defined set of compartments containing equipment that must be tracked and verified.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `unitNumber` | String | Yes | Operational identifier (e.g., "Engine 5", "Ladder 12") |
| `vin` | String | No | Vehicle Identification Number |
| `type` | ApparatusType | Yes | Type of apparatus (enum) |
| `make` | String | No | Vehicle manufacturer |
| `model` | String | No | Vehicle model |
| `year` | Integer | No | Year of manufacture |
| `stationId` | UUID | Yes | Station where apparatus is currently assigned |
| `status` | ApparatusStatus | Yes | Current operational status |
| `barcode` | String | No | Barcode for quick identification |
| `notes` | String | No | General notes about the apparatus |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### ApparatusType
- `ENGINE` — Pumper / fire engine
- `LADDER` — Ladder truck / aerial
- `RESCUE` — Heavy rescue unit
- `TANKER` — Water tanker
- `AMBULANCE` — Medical response unit
- `COMMAND` — Command / incident support vehicle
- `UTILITY` — Utility / support vehicle
- `OTHER` — Other apparatus type

### ApparatusStatus
- `IN_SERVICE` — Available for response
- `OUT_OF_SERVICE` — Not available (maintenance, repair, etc.)
- `RESERVE` — Reserve apparatus, not primary assignment
- `DECOMMISSIONED` — No longer in use

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Station | N:1 | An apparatus is assigned to one station |
| Compartment | 1:N | An apparatus has multiple compartments |
| Equipment Item | 1:N | Equipment is assigned to an apparatus |
| Consumable Stock | 1:N | Consumables are stocked on an apparatus |
| Inventory Check | 1:N | Checks are performed on an apparatus |
| Audit | 1:N | Audits are conducted on an apparatus |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Unit number must be unique within the region |
| BR-02 | An apparatus must be assigned to exactly one station |
| BR-03 | Only one inventory check can be in progress per apparatus |
| BR-04 | Only one audit can be in progress per apparatus |
| BR-05 | Changing apparatus status to OUT_OF_SERVICE should prompt for reason |
| BR-06 | Decommissioned apparatus cannot have equipment assigned |
| BR-07 | Equipment manifest (required items) is defined per apparatus |

## 6. Lifecycle

```
┌─────────────┐     ┌─────────────┐     ┌───────────────┐     ┌────────────────┐
│  Registered │────▶│ IN_SERVICE  │────▶│OUT_OF_SERVICE │────▶│ DECOMMISSIONED │
└─────────────┘     └─────────────┘     └───────────────┘     └────────────────┘
                          ▲                    │
                          └────────────────────┘
                           (return to service)
```

## 7. Aggregate Boundaries

Apparatus is an aggregate root that encompasses:
- Compartments (owned, lifecycle tied to apparatus)
- Equipment assignments (reference to equipment items)
- Manifest (required equipment list, owned)

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "unitNumber": "Engine 5",
  "vin": "1FDUF5HT5BEA12345",
  "type": "ENGINE",
  "make": "Pierce",
  "model": "Enforcer",
  "year": 2020,
  "stationId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "IN_SERVICE",
  "barcode": "APP-ENG005",
  "notes": "Primary engine for North District",
  "createdAt": "2020-03-15T10:00:00Z",
  "updatedAt": "2025-11-01T08:45:00Z"
}
```

## 9. Equipment Manifest

Each apparatus has a manifest defining required equipment:

| Manifest Entry Attributes | Description |
|--------------------------|-------------|
| `equipmentTypeId` | Type of equipment required |
| `requiredQuantity` | How many items (or quantity for consumables) |
| `compartmentId` | Which compartment the item belongs in |
| `isCritical` | Whether apparatus cannot operate without this item |

## 10. Notes

- Apparatus transfers between stations should be rare and require maintenance approval
- Manifest templates can be created per apparatus type for consistency
- VIN is optional but recommended for fleet management

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |