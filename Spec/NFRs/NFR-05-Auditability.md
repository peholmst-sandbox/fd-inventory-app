# Non-Functional Requirement: Auditability

**ID:** NFR-05  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall maintain a complete, immutable audit trail of all significant actions, enabling reconstruction of equipment history, accountability for changes, and compliance with inspection requirements.

## 2. Rationale

Fire service equipment is subject to regulatory inspections and internal reviews. When questions arise ("When was this SCBA last verified?", "Who moved this equipment?", "Why is this item missing?"), the system must provide authoritative answers. An audit trail also supports accountability and helps identify process issues.

## 3. Specification

### 3.1 Auditable Events

| Category | Events |
|----------|--------|
| **Inventory Checks** | Check started, item verified, item marked missing, item marked damaged, check completed, check abandoned |
| **Equipment Lifecycle** | Equipment registered, equipment transferred, equipment retired, equipment disposed |
| **Issue Management** | Issue reported, issue acknowledged, issue resolved |
| **Restocking** | Consumable quantity adjusted (with reason) |
| **User Actions** | Login, logout, failed login attempt |
| **Data Changes** | Equipment details modified, apparatus configuration changed |

### 3.2 Audit Record Structure

Each audit record must capture:

| Field | Description | Example |
|-------|-------------|---------|
| `id` | Unique identifier | `a1b2c3d4-...` |
| `timestamp` | When the event occurred (UTC) | `2026-01-30T14:23:45.123Z` |
| `event_type` | Type of event | `INVENTORY_ITEM_VERIFIED` |
| `actor_id` | User who performed the action | `user-123` |
| `actor_name` | User's display name (denormalised) | `J. Smith` |
| `station_id` | Station context (if applicable) | `station-5` |
| `entity_type` | Type of entity affected | `Equipment` |
| `entity_id` | ID of entity affected | `equip-789` |
| `entity_description` | Human-readable description (denormalised) | `Thermal Imaging Camera TIC-042` |
| `details` | Event-specific data (JSON) | `{"previousStatus": "OK", "newStatus": "DAMAGED"}` |
| `ip_address` | Client IP address | `192.168.1.50` |

### 3.3 Retention Requirements

| Requirement | Specification |
|-------------|---------------|
| Minimum retention | 7 years |
| Deletion policy | Audit records are never deleted; may be archived to cold storage after 2 years |
| Immutability | Audit records cannot be modified or deleted by any user, including administrators |

### 3.4 Query Capabilities

Users with appropriate permissions must be able to query audit records by:
- Date/time range
- User (actor)
- Equipment item
- Apparatus
- Station
- Event type

### 3.5 Relationship to Domain Entities

The audit trail complements but does not replace domain entities:

| Aspect | Domain Entities | Audit Trail |
|--------|-----------------|-------------|
| **Purpose** | Operational data for current state | Historical record of all changes |
| **Queryable by** | Business logic, UI | Compliance, investigation |
| **Mutable** | Yes (status updates, etc.) | No (immutable) |
| **Retention** | Operational lifetime | 7+ years |

**Guiding principles:**

1. **Domain entities are source of truth** for current state (e.g., "Is this check complete?")
2. **Audit trail is source of truth** for historical questions (e.g., "Who changed this status and when?")
3. **Audit events supplement, not duplicate** — audit captures the change, domain entity reflects result
4. **Domain entities may be archived/deleted** after retention period; audit trail is preserved

**Example: Inventory Check**

| Action | Domain Entity Change | Audit Event |
|--------|---------------------|-------------|
| Start check | Create InventoryCheck (IN_PROGRESS) | `INVENTORY_CHECK_STARTED` |
| Verify item | Create InventoryCheckItem (PRESENT) | `INVENTORY_ITEM_VERIFIED` |
| Mark damaged | Create InventoryCheckItem (DAMAGED), Create Issue | `INVENTORY_ITEM_DAMAGED`, `ISSUE_CREATED` |
| Complete check | Update InventoryCheck (COMPLETED) | `INVENTORY_CHECK_COMPLETED` |

The InventoryCheck entity provides fast queries for "show me today's checks" while the audit trail enables "show me everything that happened to equipment X over 5 years."

## 4. Implementation Requirements

### 4.1 Write Path
- Audit records written synchronously as part of the business transaction where feasible
- If asynchronous (for performance), use a reliable queue with at-least-once delivery
- Application failure must not lose audit records for committed transactions

### 4.2 Storage
- Separate audit table(s) from operational data
- Consider append-only table design or event sourcing patterns
- Database user for application should not have DELETE permission on audit tables

### 4.3 Integrity
- Optional: cryptographic chaining of audit records (hash of previous record included in next)
- Regular integrity checks to detect tampering

## 5. Performance Considerations

- Audit writes should not significantly impact user-facing response times (< 50 ms added latency)
- Audit queries may be slower; acceptable response time up to 10 seconds for complex queries
- Consider separate read replica or archive for historical queries if volume becomes significant

## 6. Verification

| Method | Description |
|--------|-------------|
| **Functional testing** | Verify all auditable events generate records |
| **Completeness testing** | Perform known sequence of actions; verify audit trail matches exactly |
| **Integrity testing** | Attempt to modify/delete audit records; verify failure |
| **Retention testing** | Verify records older than 2 years are archived correctly |

## 7. Acceptance Criteria

- [ ] All events listed in section 3.1 generate audit records
- [ ] Audit records contain all fields specified in section 3.2
- [ ] Audit records cannot be modified or deleted via application or direct database access (application user)
- [ ] Audit records can be queried by all specified criteria with results in < 10 seconds
- [ ] Equipment history can be reconstructed from audit trail alone

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added Section 3.5 (Relationship to Domain Entities) |