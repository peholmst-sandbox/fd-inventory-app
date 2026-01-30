# Use Case: Restock Consumables

**ID:** UC-06  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A maintenance technician replenishes consumable supplies on an apparatus after they have been used or to bring stock up to required levels. This updates quantity-tracked items to reflect the new stock levels.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Maintenance Technician** (primary) | Personnel responsible for restocking |
| **System** | FireStock application |

## 3. Preconditions

1. Maintenance technician is authenticated
2. Apparatus exists with defined consumable requirements
3. Consumable stock is available for restocking (either new or from storage)

## 4. Postconditions

### Success
1. Consumable quantities updated to new levels
2. Restock record created with details of what was added
3. Any low-stock alerts cleared (if applicable)
4. Audit trail records the restocking activity

### Failure
1. If cancelled: No changes made
2. If system error: Transaction rolled back; no partial updates

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | Technician navigates to apparatus | Displays apparatus overview |
| 2 | Technician taps "Restock" | Displays consumables list with current vs required quantities |
| 3 | — | Items below required level highlighted |
| 4 | Technician selects first consumable to restock | Displays item detail with quantity input |
| 5 | Technician enters quantity being added | Validates input; shows new total |
| 6 | Technician enters lot/batch number (if tracked) | Records lot information |
| 7 | Technician enters expiry date (if applicable) | Records expiry |
| 8 | Technician confirms item restock | Saves; returns to consumables list |
| 9 | Steps 4-8 repeat for additional items | Progress tracked |
| 10 | Technician reviews restock summary | Displays all items restocked with quantities |
| 11 | Technician taps "Complete Restock" | Saves all changes; creates restock record |
| 12 | — | Displays confirmation with restock summary |

## 6. Alternative Flows

### 6a. Quick Restock to Required Level
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Technician taps "Restock All to Required" | — |
| 6a.2 | — | Calculates quantities needed to reach required levels |
| 6a.3 | — | Displays summary: "Add [X] of [item], [Y] of [item], ..." |
| 6a.4 | Technician reviews and confirms | All items updated to required levels |
| 6a.5 | System prompts for lot/expiry info for items that require it | Technician enters details |

### 6b. Restock from Storage Transfer
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | Technician indicates stock is from storage | — |
| 6b.2 | Technician selects storage location | Displays available stock at location |
| 6b.3 | Technician selects items and quantities | Validates against storage availability |
| 6b.4 | — | Creates transfer from storage; updates both locations |

### 6c. Partial Restock (Insufficient Stock)
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | Technician cannot fully restock (limited supplies available) | — |
| 6c.2 | Technician enters available quantity (less than needed) | — |
| 6c.3 | — | Accepts partial restock; item remains flagged as below required |
| 6c.4 | — | Optionally creates requisition for additional stock |

### 6d. Replace Expired Stock
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | Item currently on apparatus is expired | Highlighted in list |
| 6d.2 | Technician taps "Replace" | Prompts for quantity removed and quantity added |
| 6d.3 | Technician enters quantities | System validates: removed ≤ current, added > 0 |
| 6d.4 | Technician enters new lot/expiry information | — |
| 6d.5 | — | Updates stock; records disposal of expired items |

### 6e. Restock via Barcode
| Step | Actor | System |
|------|-------|--------|
| 6e.1 | Technician scans consumable barcode | Identifies item type |
| 6e.2 | — | Displays current stock and restock form |
| 6e.3 | Technician enters quantity added | — |
| 6e.4 | Continue from step 6 of basic flow | — |

## 7. Exception Flows

### 7a. Consumable Type Not on Apparatus Manifest
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | Technician scans item not currently configured for this apparatus | — |
| 7a.2 | — | Displays: "This consumable is not on the manifest for this apparatus. Add to manifest?" |
| 7a.3a | Technician confirms | Adds to manifest with required quantity prompt |
| 7a.3b | Technician declines | Returns to consumables list |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only maintenance technicians can restock consumables |
| BR-02 | Quantity added must be greater than 0 |
| BR-03 | Lot/batch numbers required for medical consumables |
| BR-04 | Expiry dates required for items with shelf life |
| BR-05 | Stock transfers from storage must not exceed available quantity |
| BR-06 | Expired stock replacement records both removal and addition |
| BR-07 | Required quantity levels are defined per apparatus manifest |

## 9. Common Consumables

Examples of quantity-tracked consumables:
- Foam concentrate (litres)
- Medical supplies (bandages, gauze, etc.)
- Batteries (various sizes)
- Bottled water
- Disposable gloves
- Chemical absorbent pads
- Flares/markers

## 10. User Interface Requirements

- Clear visual indication of current vs required quantities
- Colour coding: red (critically low), amber (below required), green (at/above required)
- Quick entry for quantity (numeric keypad)
- Barcode scanner integration
- Summary view showing all changes before confirmation
- Support for tablet and desktop

## 11. Data Requirements

### Input
- Apparatus identifier
- Per consumable: quantity added, lot number, expiry date
- Stock source (new or storage location)

### Output
- Updated consumable quantities
- Restock record with details
- Storage transfer record (if from storage)
- Audit trail entries

## 12. Non-Functional Requirements

- Stock update: < 1 second per item
- Total restock completion: < 3 seconds
- Real-time current quantity display

## 13. Frequency

- Regular: after each incident involving consumable use
- Scheduled: weekly/monthly maintenance rounds
- Estimated: 5-10 restock events per station per week

## 14. Assumptions

- Consumable requirements (par levels) are pre-configured per apparatus
- Physical stock is available for restocking
- Expiry dates are visible on packaging

## 15. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Automatic reorder suggestions when stock falls below threshold? | Future enhancement |
| OI-02 | Track cost of consumables? | Out of scope for v1 |
| OI-03 | Multiple expiry dates per consumable type? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |