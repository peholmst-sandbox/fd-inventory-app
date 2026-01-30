# Use Case: Conduct Formal Audit

**ID:** UC-03  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A maintenance technician performs a comprehensive, formal audit of an apparatus's equipment. Unlike routine shift checks, audits are thorough inspections that may include equipment testing, condition assessment, and verification against the authoritative equipment manifest.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Maintenance Technician** (primary) | Qualified personnel conducting the audit |
| **System** | FireStock application |

## 3. Preconditions

1. Maintenance technician is authenticated
2. The apparatus exists in the system with a defined equipment manifest
3. No audit is currently in progress for this apparatus
4. Technician has physical access to the apparatus

## 4. Postconditions

### Success
1. An audit record is created with status "Completed"
2. Every item on the manifest has an audit status (Verified, Missing, Damaged, Failed Inspection, Expired)
3. Equipment condition notes are recorded where applicable
4. Discrepancies between actual and expected inventory are documented
5. Audit report is available for review and sign-off
6. Audit trail records all findings

### Failure
1. If abandoned: Audit record exists with status "Abandoned" and partial findings preserved
2. If system error: Error logged; user advised to retry

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | Technician opens application on tablet | Displays home screen with station/apparatus selection |
| 2 | Technician selects station and apparatus | Displays apparatus details |
| 3 | Technician taps "Start Audit" | Creates audit record; displays audit overview with compartment list |
| 4 | Technician selects first compartment | Displays compartment contents with expected equipment list |
| 5 | Technician scans or selects first item | Displays item detail view with audit options |
| 6 | Technician verifies item presence | Marks item as "Present" |
| 7 | Technician assesses condition (Good, Fair, Poor) | Records condition |
| 8 | Technician checks expiry date if applicable | Records expiry status (OK, Expiring Soon, Expired) |
| 9 | Technician adds notes if needed | Records notes |
| 10 | Technician confirms item audit | Saves item audit; returns to compartment list; marks item complete |
| 11 | Steps 5-10 repeat for all items in compartment | Progress tracked |
| 12 | Technician marks compartment complete | Compartment status updated; moves to next compartment |
| 13 | Steps 4-12 repeat for all compartments | — |
| 14 | Technician reviews audit summary | Displays summary: total items, issues found, items needing attention |
| 15 | Technician adds overall notes/observations | Records apparatus-level notes |
| 16 | Technician taps "Complete Audit" | Finalises audit; generates audit report |
| 17 | — | Displays confirmation with audit reference and report link |

## 6. Alternative Flows

### 6a. Item Requires Functional Test
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Item is flagged as requiring periodic testing (e.g., SCBA, radio) | Displays test requirements |
| 6a.2 | Technician performs physical test | — |
| 6a.3 | Technician records test result (Passed, Failed) | Records test result with date |
| 6a.4 | If Failed: prompts for failure details | Technician describes failure |
| 6a.5 | — | Creates issue record; marks item for repair/replacement |

### 6b. Unexpected Item Found
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | Technician finds item not on manifest | — |
| 6b.2 | Technician scans item | Item identified but not assigned to this apparatus |
| 6b.3 | — | Displays: "This item is assigned to [other location]. Add to this apparatus?" |
| 6b.4a | Technician confirms | Creates transfer record; adds item to this apparatus |
| 6b.4b | Technician declines | Records finding in audit notes |

### 6c. Item Not Found (Missing)
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | Technician cannot locate item | — |
| 6c.2 | Technician selects item from list, marks as "Missing" | Prompts for notes |
| 6c.3 | Technician enters notes about search | Records finding |
| 6c.4 | — | Creates issue record for missing item |

### 6d. Expired Item Found
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | Technician checks expiry date; item is expired | — |
| 6d.2 | Technician records actual expiry date | System compares to recorded date |
| 6d.3 | — | Marks item as "Expired"; prompts for action |
| 6d.4 | Technician indicates action (Replace, Extend if allowed, Remove) | Records planned action |

### 6e. Consumable Quantity Discrepancy
| Step | Actor | System |
|------|-------|--------|
| 6e.1 | Technician counts consumable stock | — |
| 6e.2 | Technician enters actual quantity | System compares to expected |
| 6e.3 | If discrepancy: prompts for explanation | Technician explains |
| 6e.4 | — | Updates quantity; records discrepancy in audit |

### 6f. Audit Interrupted
| Step | Actor | System |
|------|-------|--------|
| 6f.1 | Technician needs to pause (end of day, emergency) | — |
| 6f.2 | Technician taps "Save and Exit" | Saves all progress; audit remains "In Progress" |
| 6f.3 | Technician returns later | Displays option to resume |
| 6f.4 | Technician resumes | Returns to last incomplete compartment |

## 7. Exception Flows

### 7a. Manifest Discrepancy Discovered
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | Technician believes manifest is incorrect (item should/shouldn't be listed) | — |
| 7a.2 | Technician flags manifest issue | Records note; creates task for manifest review |
| 7a.3 | Technician continues audit with current manifest | — |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only maintenance technicians can conduct audits |
| BR-02 | Only one audit may be in progress per apparatus |
| BR-03 | All items must have an audit status before audit can be completed |
| BR-04 | Audits in progress for more than 7 days are flagged for review |
| BR-05 | Items with "Failed Inspection" status automatically create issue records |
| BR-06 | Audit reports are retained for 7 years |
| BR-07 | Completed audits cannot be modified (only addendum notes allowed) |

## 9. User Interface Requirements

- Optimised for tablet (landscape orientation)
- Split view: compartment list on left, item details on right
- Visual status indicators for each compartment (not started, in progress, complete)
- Quick status buttons for common flows (Verified+Good, Missing, Damaged)
- Support for attaching photos to item audits
- Offline indicator if connection is lost (with warning that data may not save)

## 10. Data Requirements

### Input
- Apparatus selection
- Per-item: presence, condition, expiry status, test results, notes, photos
- Consumable quantities
- Overall audit notes

### Output
- Audit record with all item statuses
- Issue records for problems found
- Transfer records for misplaced items
- Audit report (PDF-ready format)
- Audit trail entries

## 11. Non-Functional Requirements

- Audit state preserved during interruptions (within session)
- Audit report generation: < 10 seconds
- Support for audits with up to 500 items
- Photo storage linked to audit and item

## 12. Frequency

- Monthly or quarterly per apparatus (configurable by organisation)
- May be triggered by specific events (post-incident, new apparatus)

## 13. Assumptions

- Technician has physical access to all compartments and equipment
- Manifest is reasonably accurate (discrepancies are exceptions)
- Tablet has adequate battery for audit duration (1-2 hours)

## 14. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Audit sign-off workflow (supervisor approval)? | Open |
| OI-02 | Audit scheduling and reminders? | Future enhancement |
| OI-03 | Compliance reporting format requirements? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |