# Use Case: Report Equipment Issue

**ID:** UC-02  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A user reports that an equipment item is damaged, malfunctioning, or missing outside of a routine inventory check. This allows issues to be captured at any time, not just during shift changes.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Firefighter** (primary) | Station crew member who discovers the issue |
| **Maintenance Technician** (primary) | May also report issues discovered during other work |
| **System** | FireStock application |

## 3. Preconditions

1. User is authenticated with appropriate role
2. The equipment item exists in the system
3. For firefighters: the equipment belongs to an apparatus at their station

## 4. Postconditions

### Success
1. An issue record is created with status "Open"
2. The issue is linked to the specific equipment item
3. The equipment item's status is updated to reflect the issue (Damaged or Missing)
4. Audit trail records the issue creation
5. Relevant parties can view the issue (maintenance crews see all; firefighters see their station)

### Failure
1. If validation fails: User is informed of the problem; no issue created
2. If system error: Error message displayed; user advised to retry

## 5. Basic Flow

| Step | Actor | System |
|------|-------|--------|
| 1 | User navigates to apparatus or searches for equipment | Displays apparatus/equipment view |
| 2 | User locates the equipment item | Displays item details with "Report Issue" button |
| 3 | User taps "Report Issue" | Displays issue report form |
| 4 | User selects issue type (Damaged, Missing, Malfunctioning) | Updates form based on selection |
| 5 | User enters description of the issue | — |
| 6 | User optionally attaches photo(s) | Stores photo references |
| 7 | User selects severity (Critical, High, Medium, Low) | — |
| 8 | User taps "Submit" | Validates input; creates issue record; updates equipment status |
| 9 | — | Displays confirmation: "Issue reported. Reference: ISS-2026-0142" |
| 10 | — | Returns to equipment view showing updated status |

## 6. Alternative Flows

### 6a. Report via Barcode Scan
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | User selects "Report Issue" from home screen | Displays barcode scanner |
| 6a.2 | User scans equipment barcode | Identifies equipment; displays issue report form pre-populated with item details |
| 6a.3 | Continue from step 4 of basic flow | — |

### 6b. Equipment Already Has Open Issue
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | User attempts to report issue on item with existing open issue | — |
| 6b.2 | — | Displays warning: "This item has an open issue: [summary]. Add to existing issue or create new?" |
| 6b.3a | User selects "Add to existing" | Opens existing issue; user adds comment/photo |
| 6b.3b | User selects "Create new" | Proceeds with new issue creation |

### 6c. Critical Issue Reported
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | User selects "Critical" severity | — |
| 6c.2 | — | Displays additional confirmation: "Critical issues indicate apparatus may not be safe to deploy. Confirm severity?" |
| 6c.3 | User confirms | Proceeds with submission |
| 6c.4 | — | Issue created with critical flag; apparatus status updated if applicable |

### 6d. Missing Item with Unknown Last Location
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | User reports item as Missing | — |
| 6d.2 | — | Prompts: "When was this item last seen?" with options |
| 6d.3 | User selects timeframe (Today, Yesterday, Last week, Unknown) | Records last-seen information |
| 6d.4 | — | Prompts: "Was this item possibly used on an incident?" |
| 6d.5 | User responds | Records context; may affect follow-up process |

## 7. Exception Flows

### 7a. Photo Upload Fails
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | — | Photo upload times out or fails |
| 7a.2 | — | Displays: "Photo upload failed. Submit without photo or retry?" |
| 7a.3a | User selects "Submit without photo" | Proceeds without photo |
| 7a.3b | User selects "Retry" | Attempts upload again |

### 7b. Equipment Not Found
| Step | Actor | System |
|------|-------|--------|
| 7b.1 | Scanned barcode not recognised | — |
| 7b.2 | — | Displays: "Equipment not found. It may not be registered in the system." |
| 7b.3 | — | Offers: "Search manually" or "Report unregistered item" |
| 7b.4 | If unregistered: User provides description | Creates issue for maintenance to investigate |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Issues must have a description of at least 10 characters |
| BR-02 | Critical issues require confirmation |
| BR-03 | Equipment status automatically updates when issue is created (Damaged → Damaged status) |
| BR-04 | Firefighters can only report issues for equipment at their station |
| BR-05 | Maintenance technicians can report issues for any equipment |
| BR-06 | Photos must be under 10 MB each; maximum 5 photos per issue |

## 9. User Interface Requirements

- Quick access to "Report Issue" from home screen
- Barcode scanner readily available
- Issue type selection with clear icons
- Photo capture integrated (camera access)
- Severity selection with visual indicators (colour-coded)
- Confirmation screen with issue reference number

## 10. Data Requirements

### Input
- Equipment identifier (barcode, manual selection, or search)
- Issue type (enum: Damaged, Missing, Malfunctioning)
- Description (text, minimum 10 characters)
- Severity (enum: Critical, High, Medium, Low)
- Photos (optional, max 5, max 10 MB each)
- Last seen information (for missing items)

### Output
- Issue record with unique reference number
- Updated equipment status
- Audit trail entry
- Photo storage references

## 11. Non-Functional Requirements

- Form submission: < 2 seconds (excluding photo upload)
- Photo upload: progress indicator; timeout at 30 seconds
- Must work on mobile and tablet devices

## 12. Frequency

- Variable; estimated 2-5 issues per station per week
- May spike after incidents

## 13. Assumptions

- Users have devices with cameras for photo capture
- Network connectivity available at stations
- Issue workflow (assignment, resolution) handled separately (see UC-03)

## 14. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Should critical issues trigger notifications? | Open |
| OI-02 | Integration with external maintenance/ticketing system? | Out of scope for v1 |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |