# Use Case: Perform Shift Inventory Check

**ID:** UC-01  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A firefighter verifies that all required equipment is present and in working condition on an apparatus at the start of their shift. This is the primary daily use of the system.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Firefighter** (primary) | Station crew member performing the check |
| **System** | FireStock application |

## 3. Preconditions

1. Firefighter is authenticated and has an active session
2. Firefighter is assigned to the station where the apparatus is located
3. The apparatus exists in the system with a defined equipment list
4. No other inventory check is currently in progress for this apparatus

## 4. Postconditions

### Success
1. An inventory check record is created with status "Completed"
2. Each equipment item has a verification status (Present, Missing, Damaged)
3. Any issues reported are linked to the inventory check
4. Audit trail records the check completion with all item statuses

### Failure
1. If abandoned: Inventory check record exists with status "Abandoned"
2. If system error: Partial progress is preserved where possible

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | Firefighter opens the application on mobile device | Displays home screen with station's apparatus list |
| 2 | Firefighter selects an apparatus to check | Displays apparatus details with "Start Inventory Check" option |
| 3 | Firefighter taps "Start Inventory Check" | Creates inventory check record; displays first compartment with equipment list |
| 4 | Firefighter scans barcode of first item | Identifies item; marks as "Present"; provides visual/audio confirmation |
| 5 | Firefighter continues scanning items in compartment | Each scan marks item as Present; progress counter updates |
| 6 | Firefighter advances to next compartment | Displays next compartment's equipment list; previous compartment shows completion status |
| 7 | Steps 4-6 repeat until all compartments complete | — |
| 8 | Firefighter reviews summary showing any issues | Displays summary: items checked, items missing, items damaged |
| 9 | Firefighter taps "Complete Check" | Marks check as Completed; records timestamp; returns to home screen with confirmation |

## 6. Alternative Flows

### 6a. Item Identified Manually (at step 4)
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Firefighter cannot scan barcode (damaged, missing, dirty) | — |
| 6a.2 | Firefighter taps item name in list | Marks item as "Present"; provides confirmation |
| 6a.3 | Return to step 5 | — |

### 6b. Item Not Present (at step 4 or 5)
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | Firefighter cannot find item | — |
| 6b.2 | Firefighter taps item, selects "Mark Missing" | Prompts for optional note |
| 6b.3 | Firefighter enters note (optional) and confirms | Marks item as "Missing"; creates issue record; highlights item in red |
| 6b.4 | Return to step 5 | — |

### 6c. Item Damaged (at step 4 or 5)
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | Firefighter finds item but it is damaged | — |
| 6c.2 | Firefighter taps item, selects "Report Damage" | Prompts for damage description and optional photo |
| 6c.3 | Firefighter enters description, optionally attaches photo, confirms | Marks item as "Damaged"; creates issue record with details |
| 6c.4 | Return to step 5 | — |

### 6d. Barcode Scan Fails (at step 4)
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | — | Camera cannot read barcode after 3 attempts |
| 6d.2 | — | Displays "Unable to scan. Try manual entry?" with option to search by serial number |
| 6d.3 | Firefighter enters serial number or selects from list | System matches item; marks as Present |
| 6d.4 | Return to step 5 | — |

### 6e. Unexpected Item Scanned (at step 4)
| Step | Actor | System |
|------|-------|--------|
| 6e.1 | — | Scanned barcode matches equipment not assigned to this apparatus |
| 6e.2 | — | Displays warning: "This item is assigned to [other apparatus]. Report as found here?" |
| 6e.3 | Firefighter confirms or cancels | If confirmed: creates transfer suggestion for maintenance review |
| 6e.4 | Return to step 5 | — |

### 6f. Check Interrupted (at any step)
| Step | Actor | System |
|------|-------|--------|
| 6f.1 | Firefighter navigates away or closes app | Saves current progress; check remains "In Progress" |
| 6f.2 | Firefighter returns to app later | Evaluates check status (see below) |
| 6f.3a | Check is "In Progress" (< 4 hours old) | Displays option to resume or abandon the check |
| 6f.3b | Check was auto-abandoned (< 30 min ago) | Displays "Check was auto-abandoned. Resume?" with option to continue or start fresh |
| 6f.3c | Check was auto-abandoned (> 30 min ago) | Displays "Previous check expired. Start new check?" |
| 6f.4 | Firefighter chooses action | Resumes existing, starts new, or cancels |

### 6g. Consumable Quantity Check (at step 4)
| Step | Actor | System |
|------|-------|--------|
| 6g.1 | Item is a consumable tracked by quantity | Displays expected quantity and input field |
| 6g.2 | Firefighter enters actual quantity | Compares to expected; marks as OK or flags discrepancy |
| 6g.3 | If discrepancy: prompts for note | Firefighter explains (e.g., "Used 2 canisters on last call") |
| 6g.4 | Return to step 5 | — |

### 6h. Crew-Owned Equipment (at step 4 or 5)
| Step | Actor | System |
|------|-------|--------|
| 6h.1 | Item is crew-owned equipment | Displays item with "Crew" indicator |
| 6h.2 | Firefighter verifies item presence/condition | Same verification process as department equipment |
| 6h.3 | If damaged or missing | Issue is created but flagged as crew responsibility (not routed to maintenance) |
| 6h.4 | Return to step 5 | — |

## 7. Exception Flows

### 7a. Network Failure (at any step)
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | — | Network request fails |
| 7a.2 | — | Displays error: "Connection lost. Retrying..." |
| 7a.3 | — | Retries up to 3 times with exponential backoff |
| 7a.4a | — | If reconnected: continues operation; no data loss |
| 7a.4b | — | If still disconnected: "Cannot reach server. Please check your connection and try again." Progress is NOT saved (Vaadin limitation) |

### 7b. Concurrent Check Attempt
| Step | Actor | System |
|------|-------|--------|
| 7b.1 | Another user already has a check in progress for this apparatus | — |
| 7b.2 | Firefighter taps "Start Inventory Check" | Displays: "Inventory check already in progress by [name]. Started [time]." |
| 7b.3 | — | Offers option to view status or contact the other user |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only one inventory check may be in progress per apparatus at any time |
| BR-02 | A check is considered complete only when all items have been verified (Present, Missing, or Damaged) |
| BR-03 | Checks in progress for more than 4 hours are automatically marked as Abandoned |
| BR-04 | Damaged and missing items automatically create issue records for maintenance review |
| BR-05 | Consumable quantity discrepancies greater than 20% require a note |
| BR-06 | An auto-abandoned check can be resumed within 30 minutes of abandonment by the original user. After 30 minutes, a new check must be started. |
| BR-07 | Crew-owned equipment is included in inventory checks alongside department equipment |
| BR-08 | Issues reported for crew-owned equipment are flagged as crew responsibility (not routed to maintenance technicians) |

## 9. User Interface Requirements

- Large touch targets suitable for post-glove use
- High contrast for apparatus bay lighting conditions
- Clear visual distinction between Present (green), Missing (red), Damaged (orange)
- Audio feedback on successful scan (can be disabled)
- Progress indicator always visible: "12 of 47 items • Compartment 3 of 8"
- One-handed operation possible on mobile

## 10. Data Requirements

### Input
- Apparatus selection
- Barcode scans or manual selections
- Optional notes and photos for issues
- Consumable quantities

### Output
- Inventory check record with completion status
- Individual item verification records
- Issue records for missing/damaged items
- Audit trail entries

## 11. Non-Functional Requirements

- Response time for barcode scan confirmation: < 1 second
- Must work on mobile devices with cameras
- Session state preserved during interruptions (within Vaadin session)

## 12. Frequency

- 2-3 times per day per apparatus (at each shift change)
- Peak times: 07:00, 15:00, 19:00 (typical shift changes)

## 13. Assumptions

- Firefighter has a mobile device with camera and network access
- Equipment items have barcode labels (or serial numbers for manual lookup)
- Compartment structure is pre-configured in the system

## 14. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Should partial checks be visible to other users? | Open |
| OI-02 | Photo upload size limit? | Open |
| OI-03 | Allow "skip" for items that will be checked later? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added BR-06 (30-min resume window); clarified 6f interrupted flow |
| 1.2 | 2026-01-30 | — | Added crew-owned equipment handling in inventory checks |