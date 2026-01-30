# Domain Concept: Manifest Entry

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

A **Manifest Entry** defines a required equipment type and quantity for a specific apparatus and compartment. The collection of manifest entries for an apparatus constitutes its **Equipment Manifest** — the expected inventory that should be present and verified during checks.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `apparatusId` | UUID | Yes | Apparatus this entry belongs to |
| `compartmentId` | UUID | Yes | Compartment where item should be located |
| `equipmentTypeId` | UUID | Yes | Type of equipment required |
| `requiredQuantity` | Integer | Yes | Number of items required |
| `isCritical` | Boolean | Yes | Whether item is critical for apparatus deployment |
| `displayOrder` | Integer | No | Order for display during inventory check |
| `notes` | String | No | Special instructions or notes |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Apparatus | N:1 | Entry belongs to one apparatus manifest |
| Compartment | N:1 | Entry specifies location |
| Equipment Type | N:1 | Entry specifies what type is required |
| Inventory Check Item | 1:N | Entry is verified during checks |

## 4. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Required quantity must be >= 1 |
| BR-02 | Combination of apparatusId + compartmentId + equipmentTypeId must be unique |
| BR-03 | Compartment must belong to the same apparatus |
| BR-04 | If equipment type trackingMethod is SERIALISED, requiredQuantity represents count of individual items |
| BR-05 | If equipment type trackingMethod is QUANTITY, requiredQuantity represents the minimum stock level |
| BR-06 | Critical items must be present for apparatus to be considered deployment-ready |
| BR-07 | Manifest changes should be audited |

## 5. Manifest Completeness

An apparatus manifest is considered complete when:
- All compartments have at least one manifest entry (optional)
- All critical equipment types for the apparatus type are included
- Required quantities match departmental standards

## 6. Verification During Inventory Check

| Equipment Tracking | Verification Approach |
|-------------------|----------------------|
| SERIALISED | Each item scanned/verified individually; count must match requiredQuantity |
| QUANTITY | Stock level checked; quantity must meet or exceed requiredQuantity |

## 7. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440700",
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "compartmentId": "550e8400-e29b-41d4-a716-446655440020",
  "equipmentTypeId": "550e8400-e29b-41d4-a716-446655440100",
  "requiredQuantity": 4,
  "isCritical": true,
  "displayOrder": 1,
  "notes": "Must have 4 SCBA units for full crew deployment",
  "createdAt": "2024-06-01T10:00:00Z",
  "updatedAt": "2025-03-15T14:30:00Z"
}
```

## 8. Example Manifest (Collection View)

A complete manifest for an Engine apparatus:

| Compartment | Equipment Type | Required | Critical |
|-------------|---------------|----------|----------|
| Cab | SCBA | 4 | Yes |
| Cab | Portable Radio | 4 | Yes |
| Driver Side 1 | Halligan Bar | 2 | Yes |
| Driver Side 1 | Flat-head Axe | 2 | Yes |
| Driver Side 2 | Pike Pole | 4 | No |
| Rear | Medical Kit | 1 | Yes |
| Rear | Bandages (consumable) | 20 | No |

## 9. Template-Based Creation

Manifests may be created from templates based on apparatus type:

1. **Engine Template** — Standard equipment for engine companies
2. **Ladder Template** — Standard equipment for ladder/truck companies
3. **Rescue Template** — Standard equipment for rescue units
4. **Custom** — Fully custom manifest for specialized apparatus

Templates provide a starting point; individual apparatus manifests can be customized.

## 10. Query Patterns

| Query | Use Case |
|-------|----------|
| By apparatus | Full manifest for apparatus |
| By apparatus + compartment | Compartment-specific entries |
| By apparatus + isCritical = true | Critical items only |
| By equipment type | Where is this type required? |

## 11. Manifest vs. Actual Inventory

| Concept | Purpose |
|---------|---------|
| Manifest Entry | What SHOULD be on the apparatus |
| Equipment Item / Consumable Stock | What IS on the apparatus |
| Inventory Check Item | Verification that actual matches manifest |

The manifest defines expectations; inventory checks verify reality matches expectations.

## 12. Notes

- Manifests should be reviewed periodically to ensure they reflect current standards
- Changes to manifests should trigger re-verification at next inventory check
- Consider versioning manifests for historical comparison
- Department-wide manifest templates simplify standardization

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
