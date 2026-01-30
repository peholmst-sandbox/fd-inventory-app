# FireStock: Regional Fire Apparatus Inventory System

## General Context Document

**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Problem Statement

Fire stations in the region lack a unified system to track equipment on their apparatus (fire trucks and other emergency vehicles). Currently, inventory management relies on paper checklists, spreadsheets, or informal processes. This leads to:

- **Uncertainty about equipment readiness** — Crews may discover missing or damaged equipment only when needed
- **Inefficient restocking** — Maintenance crews lack visibility into consumption patterns across stations
- **Poor traceability** — No reliable history of equipment usage, transfers, or maintenance
- **Duplicated effort** — Each station maintains its own tracking methods with no standardisation

## 2. Vision

FireStock provides a single, authoritative system for tracking all equipment across fire apparatus in the region. Firefighters can quickly verify their truck's readiness at shift start. Maintenance crews can monitor stock levels, plan restocking runs, and maintain equipment history across all stations.

## 3. Stakeholders

| Stakeholder | Role | Key Concerns |
|-------------|------|--------------|
| **Firefighters** | Primary users at station level | Quick shift checks, easy reporting of issues, minimal administrative burden |
| **Maintenance Crews** | Regional users, equipment lifecycle management | Cross-station visibility, restocking efficiency, equipment history |
| **Station Officers** | Oversight of station operations | Confidence in readiness, audit compliance |
| **Regional Command** | Strategic oversight | Reporting, resource allocation across stations |

## 4. System Scope

### 4.1 In Scope

- Inventory tracking for all apparatus across multiple stations in the region
- Serialised equipment tracking (individual items with unique identifiers)
- Quantity-based tracking for consumables (e.g., foam canisters, medical supplies)
- **Crew-owned equipment**: Equipment purchased or built by station crews, carried on apparatus but not subject to safety inspections; stays at the home station when apparatus transfers
- Shift inventory checks performed by firefighters
- Formal audits performed by maintenance crews
- Equipment lifecycle: registration, transfer, damage reporting, retirement
- Barcode scanning and manual item identification
- Multi-device access: mobile phones, tablets, desktop computers

### 4.2 Out of Scope

- Field use during active incidents
- Integration with CAD (Computer-Aided Dispatch) systems
- Integration with procurement or fleet management systems
- Offline operation (system requires network connectivity)
- Equipment maintenance scheduling (tracking only, not workflow)
- Financial tracking (costs, depreciation)

## 5. Users and Access Model

### 5.1 User Roles

| Role | Description | Access Scope |
|------|-------------|--------------|
| **Firefighter** | Station crew member | Own station's apparatus only |
| **Maintenance Technician** | Regional maintenance staff | All stations, all apparatus |
| **System Administrator** | Technical administration | Full system access, user management |

### 5.2 Device Usage Patterns

| Device | Primary Users | Typical Use |
|--------|---------------|-------------|
| **Mobile (phone)** | Firefighters | Shift inventory checks, quick issue reporting |
| **Tablet** | Maintenance crews at station | Audits, detailed inspections, restocking |
| **Desktop** | Maintenance crews at repair shop | Equipment history, reports, bulk operations |

## 6. Key Constraints

### 6.1 Technical Constraints

- **Technology stack:** Java, Vaadin (PWA), JOOQ, PostgreSQL
- **Connectivity required:** Vaadin's server-side rendering model requires network access; true offline operation is not supported
- **Barcode compatibility:** Must support common barcode formats (Code 128, QR codes) via device camera

### 6.2 Operational Constraints

- **Station-only use:** System is used at the station before/after shifts, not during active incidents
- **Minimal training:** Users have varying technical proficiency; interface must be intuitive
- **Gloves and conditions:** Mobile use may occur in apparatus bays (poor lighting, users may have just removed gloves)

### 6.3 Organisational Constraints

- **Multi-tenancy:** Strict data isolation between stations for firefighter users
- **No dedicated IT staff at stations:** System must be low-maintenance

## 7. Success Criteria

1. **Shift check completion time:** Average shift inventory check takes less than 10 minutes per apparatus
2. **Equipment visibility:** Maintenance crews can view real-time stock levels across all stations
3. **Issue resolution:** Time from damage report to resolution is tracked and reduced
4. **Adoption:** System becomes the authoritative source of equipment truth within 6 months

## 8. Assumptions

1. All stations have adequate Wi-Fi coverage in apparatus bays
2. Users have access to smartphones or station-provided devices
3. Barcode labels will be applied to serialised equipment
4. Consumables are tracked at the container/package level, not individual units

## 9. Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Poor Wi-Fi in apparatus bays | Users cannot complete checks | Medium | Survey connectivity before rollout; consider Wi-Fi extenders |
| Low user adoption | System becomes stale | Medium | Involve firefighters in design; keep mobile UI minimal |
| Barcode label degradation | Scanning fails, manual entry increases | Medium | Use durable label materials; fallback to manual always available |
| Data isolation breach | Firefighters see other stations' data | Low | Thorough testing of access control; security audit |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added crew-owned equipment to scope |