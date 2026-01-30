# Use Case: Transfer Equipment Between Apparatus

**ID:** UC-04  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A maintenance technician moves equipment from one apparatus to another, updating the system to reflect the new assignment. This may be due to rebalancing stock, covering for an apparatus under repair, or correcting a previous misplacement.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Maintenance Technician** (primary) | Personnel authorised to reassign equipment |
| **System** | FireStock application |

## 3. Preconditions

1. Maintenance technician is authenticated
2. Source apparatus and equipment exist in the system
3. Destination apparatus exists in the system
4. Equipment is not currently flagged as "In Repair" or "Retired"

## 4. Postconditions

### Success
1. Equipment assignment updated from source to destination apparatus
2. Compartment assignment updated if specified
3. Transfer record created with source, destination, timestamp, and reason
4. Both apparatus inventories reflect the change
5. Audit trail records the transfer

### Failure
1. If cancelled: No changes made
2. If system error: No partial changes; transaction rolled back

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | Technician navigates to equipment item (via search or browse) | Displays equipment details |
| 2 | Technician taps "Transfer" | Displays transfer form |
| 3 | Technician selects destination station | Displays apparatus list for selected station |
| 4 | Technician selects destination apparatus | Displays compartment list for destination |
| 5 | Technician selects destination compartment | Records selection |
| 6 | Technician selects transfer reason from list | Records reason |
| 7 | Technician adds optional notes | Records notes |
| 8 | Technician taps "Confirm Transfer" | Validates transfer; displays confirmation summary |
| 9 | Technician confirms | Executes transfer; updates both apparatus |
| 10 | — | Displays success: "Equipment transferred to [destination]" |

## 6. Alternative Flows

### 6a. Bulk Transfer (Multiple Items)
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Technician navigates to source apparatus | Displays apparatus inventory |
| 6a.2 | Technician taps "Bulk Transfer" | Enables multi-select mode |
| 6a.3 | Technician selects multiple items | Tracks selection count |
| 6a.4 | Technician taps "Transfer Selected" | Displays transfer form with item list |
| 6a.5 | Continue from step 3 of basic flow | All selected items transferred together |

### 6b. Transfer via Barcode Scan
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | Technician selects "Quick Transfer" from home | Displays scanner |
| 6b.2 | Technician scans equipment barcode | Identifies equipment; displays current assignment |
| 6b.3 | Technician scans destination apparatus barcode (if available) | Identifies destination |
| 6b.4 | — | Displays transfer confirmation |
| 6b.5 | Technician confirms | Executes transfer |

### 6c. Transfer to Unassigned (Remove from Apparatus)
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | Technician selects "Unassigned / In Storage" as destination | — |
| 6c.2 | Technician selects storage location (station or central depot) | Records location |
| 6c.3 | — | Equipment removed from apparatus; marked as in storage |

### 6d. Transfer Requires Approval
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | Equipment type is flagged as requiring approval for transfer | — |
| 6d.2 | — | Displays: "This transfer requires supervisor approval" |
| 6d.3 | Technician submits transfer request | Creates pending transfer request |
| 6d.4 | Supervisor reviews and approves (separate flow) | Transfer executed upon approval |

## 7. Exception Flows

### 7a. Equipment Currently Flagged for Repair
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | Technician attempts to transfer equipment with open damage issue | — |
| 7a.2 | — | Displays warning: "This equipment has an open issue: [summary]. Transfer anyway?" |
| 7a.3a | Technician confirms | Transfer proceeds; issue remains linked |
| 7a.3b | Technician cancels | Returns to equipment view |

### 7b. Destination Compartment Full
| Step | Actor | System |
|------|-------|--------|
| 7b.1 | Destination compartment has reached capacity (if enforced) | — |
| 7b.2 | — | Displays warning: "Compartment capacity reached. Select different compartment?" |
| 7b.3 | Technician selects alternative compartment or overrides | Records decision |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only maintenance technicians can transfer equipment |
| BR-02 | Transfers must have a reason selected from predefined list |
| BR-03 | Some equipment types may require approval for inter-station transfers |
| BR-04 | Equipment with status "Retired" cannot be transferred |
| BR-05 | Transfer history is preserved and cannot be deleted |
| BR-06 | Same-apparatus transfers (compartment change only) are allowed |

## 9. Transfer Reasons

Standard reasons to select from:
- Rebalancing inventory
- Covering for apparatus under maintenance
- Correcting previous error
- Upgrading equipment
- Requested by station
- Other (requires note)

## 10. User Interface Requirements

- Clear display of current assignment (station, apparatus, compartment)
- Station/apparatus/compartment pickers with search
- Confirmation screen showing before and after state
- Support for bulk selection on tablet/desktop
- Barcode scanning for quick transfers

## 11. Data Requirements

### Input
- Equipment identifier(s)
- Destination station, apparatus, compartment
- Transfer reason (enum)
- Optional notes

### Output
- Updated equipment assignment
- Transfer record with full details
- Audit trail entry

## 12. Non-Functional Requirements

- Transfer confirmation: < 2 seconds
- Atomic transaction: all-or-nothing for bulk transfers
- Works on tablet and desktop

## 13. Frequency

- Variable; estimated 10-20 transfers per week across region
- May increase during apparatus maintenance periods

## 14. Assumptions

- Technician has physical access to move the equipment
- Destination apparatus has space for the equipment
- Compartment assignments are flexible (not strictly enforced)

## 15. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Approval workflow for high-value equipment? | Open |
| OI-02 | Integration with physical asset tags/RFID? | Out of scope for v1 |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |