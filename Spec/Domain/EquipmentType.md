# Domain Concept: Equipment Type

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

An **Equipment Type** is a template or category that defines a class of equipment. It specifies common attributes, tracking method (serialised or quantity-based), and any special requirements such as testing schedules or expiry tracking. Individual equipment items and consumable stocks are instances of an equipment type.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `code` | String | Yes | Short identifier (e.g., "SCBA", "TIC", "FOAM-5L") |
| `name` | String | Yes | Full name (e.g., "Self-Contained Breathing Apparatus") |
| `category` | EquipmentCategory | Yes | Classification category |
| `trackingMethod` | TrackingMethod | Yes | How items are tracked |
| `description` | String | No | Detailed description |
| `manufacturer` | String | No | Default/common manufacturer |
| `model` | String | No | Default/common model |
| `requiresExpiry` | Boolean | Yes | Whether items have expiry dates |
| `requiresTesting` | Boolean | Yes | Whether items require periodic testing |
| `testIntervalDays` | Integer | No | Days between required tests (if requiresTesting) |
| `isActive` | Boolean | Yes | Whether type is available for new items |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### EquipmentCategory
- `PPE` — Personal Protective Equipment
- `BREATHING` — Respiratory/breathing equipment
- `TOOLS_HAND` — Hand tools
- `TOOLS_POWER` — Power tools and equipment
- `ELECTRONICS` — Electronic devices
- `MEDICAL` — Medical and first aid
- `RESCUE` — Rescue equipment
- `HOSE` — Hoses and fittings
- `NOZZLES` — Nozzles and appliances
- `LIGHTING` — Lights and illumination
- `CONSUMABLE` — Consumable supplies
- `OTHER` — Other equipment

### TrackingMethod
- `SERIALISED` — Individual items tracked by serial number
- `QUANTITY` — Items tracked by quantity/count

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Equipment Item | 1:N | Items are instances of a type (if serialised) |
| Consumable Stock | 1:N | Stock entries are instances of a type (if quantity) |
| Manifest Entry | 1:N | Manifests reference types |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Code must be unique within the system |
| BR-02 | TrackingMethod cannot be changed after items exist |
| BR-03 | If requiresTesting is true, testIntervalDays must be specified |
| BR-04 | Deactivating a type prevents new items but preserves existing |
| BR-05 | Categories are predefined; cannot be extended by users |
| BR-06 | Types with requiresTesting = true cannot be used for crew-owned equipment (safety equipment must be department-owned) |

## 6. Example (Serialised)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440100",
  "code": "SCBA",
  "name": "Self-Contained Breathing Apparatus",
  "category": "BREATHING",
  "trackingMethod": "SERIALISED",
  "description": "Positive pressure breathing apparatus for firefighting operations",
  "manufacturer": "Scott Safety",
  "model": "Air-Pak X3 Pro",
  "requiresExpiry": false,
  "requiresTesting": true,
  "testIntervalDays": 365,
  "isActive": true,
  "createdAt": "2024-01-10T09:00:00Z",
  "updatedAt": "2024-01-10T09:00:00Z"
}
```

## 7. Example (Quantity-Tracked)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440101",
  "code": "FOAM-AFFF-20L",
  "name": "AFFF Foam Concentrate (20L)",
  "category": "CONSUMABLE",
  "trackingMethod": "QUANTITY",
  "description": "Aqueous Film-Forming Foam concentrate, 20 litre container",
  "manufacturer": "Chemguard",
  "model": null,
  "requiresExpiry": true,
  "requiresTesting": false,
  "testIntervalDays": null,
  "isActive": true,
  "createdAt": "2024-01-10T09:00:00Z",
  "updatedAt": "2024-01-10T09:00:00Z"
}
```

## 8. Seed Data Approach

The system should be pre-populated with common equipment types:

| Category | Example Types |
|----------|---------------|
| BREATHING | SCBA, Spare Cylinder, SCBA Mask |
| PPE | Helmet, Turnout Coat, Turnout Pants, Boots, Gloves |
| TOOLS_HAND | Halligan Bar, Flat-Head Axe, Pike Pole |
| TOOLS_POWER | Rotary Saw, Spreaders, Cutters |
| ELECTRONICS | Portable Radio, Thermal Imaging Camera |
| MEDICAL | AED, First Aid Kit, Trauma Kit |
| HOSE | 65mm Attack Hose, 100mm Supply Hose |
| NOZZLES | Combination Nozzle, Fog Nozzle |
| CONSUMABLE | Foam Concentrate, Batteries, Medical Supplies |

## 9. Notes

- Equipment types are shared across all stations
- Changes to types affect all items of that type
- Consider versioning if significant type changes are needed

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added BR-06 (crew-owned equipment restriction on testing types) |