# Domain Concept: Inventory Check Item

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

An **Inventory Check Item** records the verification result for a single equipment item or consumable stock entry during an inventory check. It captures what was checked, the outcome, and any observations.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `inventoryCheckId` | UUID | Yes | Parent inventory check |
| `equipmentItemId` | UUID | No | Equipment item being verified (if serialised) |
| `consumableStockId` | UUID | No | Consumable stock being verified (if quantity-tracked) |
| `manifestEntryId` | UUID | No | Reference to manifest entry (if checking against manifest) |
| `compartmentId` | UUID | No | Compartment where item was checked |
| `verificationStatus` | VerificationStatus | Yes | Result of verification |
| `quantityFound` | Integer | No | Actual quantity found (for consumables) |
| `quantityExpected` | Integer | No | Expected quantity (from manifest) |
| `conditionNotes` | String | No | Notes about item condition |
| `verifiedAt` | Timestamp | Yes | When verification was recorded |
| `issueId` | UUID | No | Reference to issue if one was created |
| `createdAt` | Timestamp | Yes | When record was created |

## 3. Enumerations

### VerificationStatus
- `PRESENT` — Item found and in acceptable condition
- `PRESENT_DAMAGED` — Item found but damaged
- `MISSING` — Item not found
- `EXPIRED` — Item found but past expiry date
- `LOW_QUANTITY` — Consumable below required level
- `SKIPPED` — Item verification skipped (with reason in notes)

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Inventory Check | N:1 | Item belongs to one check |
| Equipment Item | N:1 | Verification of a serialised item |
| Consumable Stock | N:1 | Verification of consumable quantity |
| Manifest Entry | N:1 | What was expected per manifest |
| Compartment | N:1 | Location checked |
| Issue | N:1 | Issue created from verification |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Either equipmentItemId or consumableStockId must be set, but not both |
| BR-02 | If status is PRESENT_DAMAGED or MISSING, an issue should be created |
| BR-03 | If status is LOW_QUANTITY, quantityFound and quantityExpected must be set |
| BR-04 | An equipment item can only be verified once per inventory check |
| BR-05 | Verification status PRESENT_DAMAGED should update equipment item status to DAMAGED |
| BR-06 | Verification status MISSING should update equipment item status to MISSING |

## 6. Verification Flow

```
┌────────────────┐
│  Start Check   │
└───────┬────────┘
        ▼
┌────────────────┐     ┌─────────────────┐
│  Scan/Select   │────▶│ Record Status   │
│     Item       │     │ (verification)  │
└────────────────┘     └───────┬─────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌───────────────┐    ┌─────────────────┐    ┌───────────────┐
│    PRESENT    │    │ PRESENT_DAMAGED │    │    MISSING    │
│  (continue)   │    │ (create issue)  │    │ (create issue)│
└───────────────┘    └─────────────────┘    └───────────────┘
```

## 7. Status Impact on Equipment

| Verification Status | Equipment Item Status Change | Issue Created |
|--------------------|------------------------------|---------------|
| PRESENT | No change | No |
| PRESENT_DAMAGED | → DAMAGED | Yes |
| MISSING | → MISSING | Yes |
| EXPIRED | → EXPIRED | Yes |
| LOW_QUANTITY | N/A (consumable) | Yes (if below minimum) |
| SKIPPED | No change | No |

## 8. Example (Equipment Item)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440600",
  "inventoryCheckId": "550e8400-e29b-41d4-a716-446655440500",
  "equipmentItemId": "550e8400-e29b-41d4-a716-446655440200",
  "consumableStockId": null,
  "manifestEntryId": "550e8400-e29b-41d4-a716-446655440700",
  "compartmentId": "550e8400-e29b-41d4-a716-446655440020",
  "verificationStatus": "PRESENT_DAMAGED",
  "quantityFound": null,
  "quantityExpected": null,
  "conditionNotes": "Visible crack in face seal",
  "verifiedAt": "2026-01-30T07:15:00Z",
  "issueId": "550e8400-e29b-41d4-a716-446655440400",
  "createdAt": "2026-01-30T07:15:00Z"
}
```

## 9. Example (Consumable Stock)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440601",
  "inventoryCheckId": "550e8400-e29b-41d4-a716-446655440500",
  "equipmentItemId": null,
  "consumableStockId": "550e8400-e29b-41d4-a716-446655440250",
  "manifestEntryId": "550e8400-e29b-41d4-a716-446655440701",
  "compartmentId": "550e8400-e29b-41d4-a716-446655440021",
  "verificationStatus": "LOW_QUANTITY",
  "quantityFound": 3,
  "quantityExpected": 10,
  "conditionNotes": "Only 3 bandage rolls remaining",
  "verifiedAt": "2026-01-30T07:20:00Z",
  "issueId": "550e8400-e29b-41d4-a716-446655440401",
  "createdAt": "2026-01-30T07:20:00Z"
}
```

## 10. Query Patterns

| Query | Use Case |
|-------|----------|
| By inventory check | All verifications for a check |
| By equipment item + date range | Verification history for item |
| By status (MISSING, DAMAGED) | Problem items in a check |
| By compartment | Compartment-specific results |

## 11. Notes

- This entity provides the audit trail for each item's verification
- Photos may be attached to check items for damage documentation
- Consider bulk verification for compartments with many similar items
- Verification timestamp enables tracking check efficiency

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
