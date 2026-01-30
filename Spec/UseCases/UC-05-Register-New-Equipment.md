# Use Case: Register New Equipment

**ID:** UC-05  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Brief Description

A maintenance technician adds a new piece of equipment to the system, either as a serialised item (individually tracked) or as consumable stock (quantity tracked). This establishes the equipment's identity, assignment, and initial state in FireStock.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Maintenance Technician** (primary) | Personnel responsible for department equipment management |
| **Firefighter** (secondary) | Station crew member registering crew-owned equipment |
| **System** | FireStock application |

## 3. Preconditions

1. Maintenance technician is authenticated
2. Equipment type exists in the system (or technician has permission to create new types)
3. If assigning immediately: destination station, apparatus, and compartment exist

## 4. Postconditions

### Success (Serialised Item)
1. New equipment record created with unique identifier
2. Equipment assigned to apparatus/compartment or marked as in storage
3. Barcode associated with item (if provided)
4. Equipment appears in apparatus inventory
5. Audit trail records registration

### Success (Consumable Stock)
1. Consumable quantity added to existing stock or new stock record created
2. Stock level updated at specified location
3. Audit trail records addition

### Failure
1. If validation fails: User informed of errors; no record created
2. If duplicate detected: User warned; can override or cancel

## 5. Basic Flow (Serialised Item)

| Step | Actor | System |
|------|-------|--------|
| 1 | Technician selects "Register Equipment" from menu | Displays registration form |
| 2 | Technician selects equipment type from list | Populates form with type-specific fields |
| 3 | Technician enters or scans serial number | Records serial number |
| 4 | Technician scans or enters barcode | Associates barcode with item |
| 5 | Technician enters manufacturer (if not auto-filled) | Records manufacturer |
| 6 | Technician enters model (if not auto-filled) | Records model |
| 7 | Technician enters acquisition date | Records date |
| 8 | Technician enters warranty expiry (if applicable) | Records warranty date |
| 9 | Technician selects initial assignment (apparatus or storage) | If apparatus: prompts for compartment |
| 10 | Technician selects compartment | Records assignment |
| 11 | Technician adds optional notes | Records notes |
| 12 | Technician taps "Register" | Validates input; creates record |
| 13 | — | Displays confirmation: "Equipment registered: [ID]" with option to print label |

## 6. Alternative Flows

### 6a. Register Consumable Stock
| Step | Actor | System |
|------|-------|--------|
| 6a.1 | Technician selects consumable equipment type | Form switches to quantity mode |
| 6a.2 | Technician enters quantity being added | Records quantity |
| 6a.3 | Technician enters lot/batch number (if applicable) | Records lot number |
| 6a.4 | Technician enters expiry date (if applicable) | Records expiry |
| 6a.5 | Technician selects destination (apparatus or storage) | Records location |
| 6a.6 | Technician taps "Add Stock" | Updates stock level; creates record |

### 6b. Bulk Registration (Multiple Items)
| Step | Actor | System |
|------|-------|--------|
| 6b.1 | Technician selects "Bulk Registration" | Displays bulk form |
| 6b.2 | Technician selects equipment type | — |
| 6b.3 | Technician enters common details (manufacturer, model, acquisition date) | Pre-fills for all items |
| 6b.4 | Technician scans first barcode | Creates first item; prompts for next |
| 6b.5 | Technician continues scanning | Each scan creates new item with incremented sequence |
| 6b.6 | Technician taps "Complete Batch" | Finalises all registrations |
| 6b.7 | — | Displays summary: "[N] items registered" |

### 6c. Create New Equipment Type
| Step | Actor | System |
|------|-------|--------|
| 6c.1 | Required equipment type doesn't exist | Technician taps "Create Type" |
| 6c.2 | Technician enters type name | — |
| 6c.3 | Technician enters category | — |
| 6c.4 | Technician specifies tracking method (serialised or quantity) | — |
| 6c.5 | Technician specifies if testing required | — |
| 6c.6 | Technician specifies if expiry tracking needed | — |
| 6c.7 | Technician taps "Create Type" | Creates type; returns to registration |

### 6d. Register to Storage (Not Apparatus)
| Step | Actor | System |
|------|-------|--------|
| 6d.1 | Technician selects "Storage" as assignment | — |
| 6d.2 | Technician selects storage location (station or central depot) | Records location |
| 6d.3 | — | Equipment registered without apparatus assignment |

### 6e. Print Barcode Label
| Step | Actor | System |
|------|-------|--------|
| 6e.1 | After registration, technician taps "Print Label" | — |
| 6e.2 | — | Generates label with barcode, serial number, type |
| 6e.3 | — | Sends to connected printer or displays PDF |

### 6f. Register Crew-Owned Equipment (Firefighter)
| Step | Actor | System |
|------|-------|--------|
| 6f.1 | Firefighter selects "Register Crew Equipment" from menu | Displays simplified registration form |
| 6f.2 | Firefighter selects equipment type from allowed list | Only types not requiring testing are shown |
| 6f.3 | Firefighter enters description or serial number | Records identifier |
| 6f.4 | Firefighter optionally scans or enters barcode | Associates barcode with item |
| 6f.5 | — | System automatically sets homeStationId to firefighter's station |
| 6f.6 | Firefighter selects apparatus and compartment (optional) | Records initial assignment |
| 6f.7 | Firefighter adds optional notes | Records notes |
| 6f.8 | Firefighter taps "Register" | Validates input; creates record with ownershipType = CREW_OWNED |
| 6f.9 | — | Displays confirmation: "Crew equipment registered" |

## 7. Exception Flows

### 7a. Duplicate Serial Number
| Step | Actor | System |
|------|-------|--------|
| 7a.1 | Entered serial number already exists | — |
| 7a.2 | — | Displays: "Serial number already registered: [details]. View existing or enter different?" |
| 7a.3a | Technician views existing | Displays existing record |
| 7a.3b | Technician enters different serial number | Continues registration |

### 7b. Duplicate Barcode
| Step | Actor | System |
|------|-------|--------|
| 7b.1 | Scanned barcode already associated with another item | — |
| 7b.2 | — | Displays: "Barcode already in use by: [item]. Re-assign barcode?" |
| 7b.3a | Technician confirms reassignment | Old item's barcode cleared; new association created |
| 7b.3b | Technician cancels | Returns to form to enter different barcode |

## 8. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Maintenance technicians can register department equipment |
| BR-02 | Firefighters can register crew-owned equipment at their assigned station |
| BR-03 | Serial numbers must be unique within the system |
| BR-04 | Barcodes must be unique; reassignment is allowed with confirmation |
| BR-05 | Equipment type is required |
| BR-06 | Acquisition date cannot be in the future |
| BR-07 | New equipment types require approval (or restricted to admins) |
| BR-08 | Consumables must have quantity > 0 |
| BR-09 | Crew-owned equipment can only be of types that do not require testing (safety inspection) |
| BR-10 | Crew-owned equipment is automatically assigned homeStationId based on the registering user's station |
| BR-11 | Crew-owned equipment maintenance is the responsibility of the station crews, not the department |

## 9. Equipment Types

Pre-configured types should include categories such as:
- Personal Protective Equipment (SCBA, helmets, turnout gear)
- Tools (axes, halligan bars, cutters)
- Electronics (radios, thermal cameras)
- Medical supplies (first aid kits, AED)
- Hoses and nozzles
- Consumables (foam, batteries, medical supplies)

## 10. User Interface Requirements

- Equipment type picker with search and categories
- Barcode scanner integration
- Auto-complete for manufacturer and model based on type
- Clear indication of required vs optional fields
- Inline validation (serial number uniqueness)
- Label preview before printing

## 11. Data Requirements

### Input (Serialised)
- Equipment type (required)
- Serial number (required, unique)
- Barcode (optional, unique)
- Manufacturer
- Model
- Acquisition date
- Warranty expiry date
- Initial assignment (station, apparatus, compartment or storage)
- Notes

### Input (Consumable)
- Equipment type (required)
- Quantity (required, > 0)
- Lot/batch number
- Expiry date
- Location (apparatus or storage)

### Output
- Equipment or stock record
- Audit trail entry
- Label (optional)

## 12. Non-Functional Requirements

- Registration: < 2 seconds
- Duplicate check: real-time as user types serial number
- Bulk registration: supports up to 50 items per batch
- Works on tablet and desktop

## 13. Frequency

- Variable; estimated 20-50 new items per month across region
- May spike with new apparatus delivery or grant-funded purchases

## 14. Assumptions

- Barcode labels will be printed and applied to serialised items
- Equipment types are pre-configured for common items
- Physical equipment is available for registration (serial numbers readable)

## 15. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Integration with procurement for auto-registration? | Out of scope for v1 |
| OI-02 | Photo capture during registration? | Nice-to-have |
| OI-03 | QR code support in addition to traditional barcodes? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added crew-owned equipment registration by firefighters |