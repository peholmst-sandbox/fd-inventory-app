# Domain Concept: Compartment

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

A **Compartment** is a physical storage area on an apparatus where equipment is kept. Compartments provide organisational structure for equipment and enable systematic inventory checks.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `apparatusId` | UUID | Yes | Parent apparatus |
| `code` | String | Yes | Short identifier (e.g., "L1", "R2", "CAB") |
| `name` | String | Yes | Descriptive name (e.g., "Left Side Bay 1") |
| `location` | CompartmentLocation | Yes | Position on apparatus |
| `description` | String | No | Additional details |
| `displayOrder` | Integer | Yes | Order for UI display during checks |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### CompartmentLocation
- `FRONT` — Front/cab area
- `LEFT_SIDE` — Driver's side compartments
- `RIGHT_SIDE` — Officer's side compartments
- `REAR` — Rear compartments
- `TOP` — Roof/top storage
- `INTERIOR` — Inside cab or crew area
- `CROSSLAY` — Crosslay hose beds
- `HOSE_BED` — Main hose bed

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Apparatus | N:1 | A compartment belongs to one apparatus |
| Equipment Item | 1:N | Equipment items are assigned to compartments |
| Consumable Stock | 1:N | Consumables may be assigned to compartments |
| Manifest Entry | 1:N | Manifest defines what should be in each compartment |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Compartment code must be unique within an apparatus |
| BR-02 | Compartments are owned by their apparatus (deleted if apparatus is deleted) |
| BR-03 | Equipment assigned to a compartment inherits the apparatus assignment |
| BR-04 | Display order determines sequence during inventory checks |
| BR-05 | A compartment can have a capacity limit (optional, soft enforcement) |

## 6. Standard Compartment Templates

For consistent setup, apparatus types have default compartment templates:

### Engine (Typical)
| Code | Name | Location |
|------|------|----------|
| CAB | Cab Interior | INTERIOR |
| L1 | Left Bay 1 | LEFT_SIDE |
| L2 | Left Bay 2 | LEFT_SIDE |
| L3 | Left Bay 3 | LEFT_SIDE |
| R1 | Right Bay 1 | RIGHT_SIDE |
| R2 | Right Bay 2 | RIGHT_SIDE |
| R3 | Right Bay 3 | RIGHT_SIDE |
| REAR | Rear Compartment | REAR |
| XLAY | Crosslay | CROSSLAY |
| HBED | Hose Bed | HOSE_BED |

## 7. Aggregate Boundaries

Compartment is a child entity within the Apparatus aggregate:
- Created/deleted with apparatus
- Cannot exist independently
- Accessed through apparatus context

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440020",
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "code": "L1",
  "name": "Left Side Bay 1 - SCBA",
  "location": "LEFT_SIDE",
  "description": "Primary SCBA storage, 4 bottle capacity",
  "displayOrder": 2,
  "createdAt": "2020-03-15T10:00:00Z",
  "updatedAt": "2024-06-10T11:30:00Z"
}
```

## 9. UI Considerations

- Compartment list should follow physical apparatus layout
- Consider visual/graphical apparatus representation for large touchscreens
- Colour coding by completion status during checks (not started, in progress, complete)

## 10. Notes

- Compartment structure varies significantly by apparatus type and manufacturer
- Allow flexibility for custom compartment definitions
- Photos of compartments may be helpful for training/reference

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |