# Use Case: Transfer Apparatus Between Stations

**ID:** UC-07
**Version:** 1.0
**Date:** 2026-01-30
**Status:** Draft

---

## 1. Brief Description

A System Administrator transfers an apparatus from one station to another. This is a rare operation typically occurring due to station closures, rebalancing of resources, or long-term apparatus reassignment.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **System Administrator** (primary) | User with administrative privileges |
| **System** | FireStock application |

## 3. Preconditions

1. Administrator is authenticated with System Administrator role
2. Source and destination stations both exist and are active
3. Apparatus exists and is assigned to the source station
4. Apparatus status is OUT_OF_SERVICE (cannot transfer while in service)
5. No inventory check or audit is in progress for the apparatus

## 4. Postconditions

### Success
1. Apparatus stationId updated to destination station
2. All **department-owned** equipment on apparatus remains assigned to apparatus (moves with it)
3. All **crew-owned** equipment is automatically unassigned from apparatus and placed in station storage at the original station
4. Audit trail records the transfer with source, destination, reason, and timestamp
5. Station equipment counts updated for both stations

### Failure
1. Apparatus remains at source station
2. Error logged with reason for failure

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | Administrator navigates to apparatus management | Displays apparatus list with station filter |
| 2 | Administrator selects apparatus to transfer | Displays apparatus details with "Transfer to Station" option |
| 3 | Administrator clicks "Transfer to Station" | Validates preconditions; displays transfer dialog |
| 4 | Administrator selects destination station from dropdown | Shows station details and confirmation prompt |
| 5 | Administrator enters reason for transfer | Reason is required field |
| 6 | Administrator clicks "Confirm Transfer" | Validates destination; performs transfer; shows success message |
| 7 | — | Updates apparatus stationId; creates audit record; refreshes view |

## 6. Alternative Flows

### 6a. Apparatus Has Department Equipment That Should Not Transfer
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Administrator needs to keep some department equipment at source station | — |
| 6a.2 | Administrator clicks "Review Equipment" before confirming | Displays list of all equipment on apparatus, grouped by ownership type |
| 6a.3 | Administrator uses UC-04 to transfer department equipment off apparatus first | Equipment moved to storage or other apparatus |
| 6a.4 | Administrator returns to transfer dialog and confirms | Proceeds with apparatus transfer |

### 6b. Apparatus Has Crew-Owned Equipment
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | — | Apparatus has crew-owned equipment assigned to it |
| 6b.2 | Administrator clicks "Confirm Transfer" | System displays: "This apparatus has [N] crew-owned equipment items that will remain at [source station]." |
| 6b.3 | Administrator acknowledges | Crew-owned equipment automatically moved to station storage at source station |

### 6c. Destination Station Has No Capacity
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | — | Destination station has reached apparatus limit (if configured) |
| 6c.2 | — | Displays warning: "Destination station has [N] apparatus. Proceed anyway?" |
| 6c.3 | Administrator confirms or cancels | If confirmed, proceeds; otherwise returns to selection |

## 7. Exception Flows

### 7a. Apparatus In Service
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | Administrator attempts to transfer IN_SERVICE apparatus | — |
| 7a.2 | — | Displays error: "Apparatus must be OUT_OF_SERVICE to transfer. Current status: IN_SERVICE" |
| 7a.3 | — | Offers link to change apparatus status |

### 7b. Active Inventory Check
| Step | Actor | System |
|------|-------|--------|
| 7b.1 | — | Inventory check in progress for this apparatus |
| 7b.2 | — | Displays error: "Cannot transfer while inventory check is in progress. Started by [user] at [time]." |
| 7b.3 | — | Offers option to contact user or wait |

### 7c. Destination Station Inactive
| Step | Actor | System |
|------|-------|--------|
| 7c.1 | — | Selected destination station is inactive |
| 7c.2 | — | Displays error: "Cannot transfer to inactive station. Reactivate station first." |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only System Administrators can transfer apparatus between stations |
| BR-02 | Apparatus must be OUT_OF_SERVICE during transfer |
| BR-03 | Transfer reason is required and must be at least 10 characters |
| BR-04 | All **department-owned** equipment on apparatus transfers with it |
| BR-05 | All **crew-owned** equipment does NOT transfer; it is automatically moved to station storage at the source station |
| BR-06 | Manifest entries for apparatus are preserved during transfer |
| BR-07 | User station assignments are NOT automatically updated; firefighters at destination must be assigned separately |
| BR-08 | Transfer creates a single atomic transaction; partial transfer is not possible |

## 9. User Interface Requirements

- Transfer dialog clearly shows source and destination stations
- Equipment counts displayed separately for department and crew-owned equipment:
  - "This apparatus has 42 department equipment items that will transfer"
  - "This apparatus has 5 crew-owned equipment items that will remain at [source station]"
- Confirmation requires explicit acknowledgment
- Success message includes link to view apparatus at new station

## 10. Data Requirements

### Input
- Apparatus ID
- Destination station ID
- Transfer reason (required, min 10 chars)

### Output
- Updated apparatus record
- Audit trail entry with full details

## 11. Non-Functional Requirements

- Transfer operation should complete in < 5 seconds
- Transfer must be atomic (all-or-nothing)
- Audit record must be created even if subsequent operations fail

## 12. Frequency

- Rare: estimated 1-5 times per year across entire region
- Peak: During station reorganizations or closures

## 13. Assumptions

- Apparatus physically moves to new station (system only tracks assignment)
- Network connectivity available during transfer
- Administrator has verified physical apparatus location

## 14. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Should there be an approval workflow for apparatus transfers? | Open |
| OI-02 | Should equipment manifests be updated based on destination station standards? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added crew-owned equipment handling (stays at source station) |
