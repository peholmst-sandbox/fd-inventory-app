# Proposed Fixes for Specification Inconsistencies

**Date:** 2026-01-30

This document proposes specific fixes for the critical and major inconsistencies identified in the specification review. Each fix includes the exact changes needed.

---

## Summary of Issues

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Auto-abandon race condition | Critical | Proposed fix below |
| 2 | Deactivated station access unclear | Major | Proposed fix below |
| 3 | System Administrator scope vague | Major | Proposed fix below |
| 4 | Consumable assignment states undefined | Major | Proposed fix below |
| 5 | Audit vs InventoryCheck redundancy | Major | Proposed fix below |
| 6 | Missing Apparatus Transfer use case | Major | New UC-07 proposed |
| 7 | Missing Reporting use case | Major | New UC-08 proposed |

---

## Fix 1: Auto-Abandon Race Condition

**Files affected:** `UC-01-Shift-Inventory-Check.md`, `Domain/InventoryCheck.md`

### Problem
UC-01 BR-03 states checks are auto-abandoned after 4 hours, but Alternative Flow 6f allows resuming interrupted checks. If a user tries to resume a check that has just been auto-abandoned, the behavior is undefined.

### Proposed Changes

**In UC-01-Shift-Inventory-Check.md, add new BR-06:**

```markdown
| BR-06 | An auto-abandoned check can be resumed within 30 minutes of abandonment by the original user. After 30 minutes, a new check must be started. |
```

**In UC-01-Shift-Inventory-Check.md, update Alternative Flow 6f:**

```markdown
### 6f. Check Interrupted (at any step)
| Step | Actor | System |
|------|-------|--------|
| 6f.1 | Firefighter navigates away or closes app | Saves current progress; check remains "In Progress" |
| 6f.2 | Firefighter returns to app later | Evaluates check status (see below) |
| 6f.3a | Check is "In Progress" (< 4 hours old) | Displays option to resume or abandon the check |
| 6f.3b | Check was auto-abandoned (< 30 min ago) | Displays "Check was auto-abandoned. Resume?" with option to continue or start fresh |
| 6f.3c | Check was auto-abandoned (> 30 min ago) | Displays "Previous check expired. Start new check?" |
| 6f.4 | Firefighter chooses action | Resumes existing, starts new, or cancels |
```

**In Domain/InventoryCheck.md, update Section 7 (Auto-Abandon Behavior):**

```markdown
## 7. Auto-Abandon Behavior

When a check has been IN_PROGRESS for more than 4 hours:
1. System marks status as ABANDONED
2. abandonedAt is set to current timestamp
3. Reason is set to "AUTO_TIMEOUT"
4. Any unverified items remain unverified
5. Audit log records automatic abandonment

**Resume window (30 minutes post-abandon):**
1. Same user can resume the check within 30 minutes of abandonedAt
2. On resume: status returns to IN_PROGRESS, abandonedAt is cleared
3. Previously verified items retain their status
4. Progress continues from where it stopped

**After resume window expires:**
1. Check cannot be resumed
2. User must start a new inventory check
3. The abandoned check remains in history for audit purposes

**Race condition prevention:**
- Auto-abandon job should check `abandonedAt IS NULL` before abandoning
- Resume operation should use optimistic locking on the check record
- If auto-abandon and resume race, resume wins (user intent takes priority)
```

---

## Fix 2: Deactivated Station Access

**Files affected:** `Domain/Station.md`, `NFRs/NFR-04-Security.md`

### Problem
Station.md BR-03 says historical records are preserved when a station is deactivated, but NFR-04 doesn't specify whether firefighters retain access to historical data from their deactivated station.

### Proposed Changes

**In Domain/Station.md, add new BR-06 and BR-07:**

```markdown
| BR-06 | When a station is deactivated, firefighters assigned only to that station lose access to the system |
| BR-07 | Historical data (completed inventory checks, closed issues) from deactivated stations is accessible to maintenance technicians and administrators for audit purposes |
```

**In Domain/Station.md, add new Section after Lifecycle:**

```markdown
## 6. Deactivation Behavior

When a station is deactivated:

| Aspect | Behavior |
|--------|----------|
| **Firefighter access** | Revoked; login redirects to "station inactive" message |
| **Apparatus** | Must be transferred or decommissioned before deactivation |
| **Equipment** | Must be transferred to other stations before deactivation |
| **Open issues** | Must be resolved or transferred before deactivation |
| **Historical data** | Preserved and accessible to maintenance/admin roles |
| **In-progress checks** | Must be completed or abandoned before deactivation |

**Preconditions for deactivation:**
1. No apparatus assigned (all transferred or decommissioned)
2. No equipment in storage (all transferred)
3. No open issues
4. No in-progress inventory checks or audits
```

**In NFRs/NFR-04-Security.md, add to Section 3.3 (Data Isolation):**

```markdown
### 3.4 Deactivated Station Access

| Role | Access to Deactivated Station Data |
|------|-----------------------------------|
| **Firefighter** | None (even if previously assigned) |
| **Maintenance Technician** | Read-only for historical records |
| **System Administrator** | Full read access; can reactivate station |
```

---

## Fix 3: System Administrator Scope

**Files affected:** `NFRs/NFR-04-Security.md`, `Spec/Context.md`

### Problem
"All capabilities" for System Administrator is vague. It's unclear whether this is an operational role or purely administrative.

### Proposed Changes

**In NFRs/NFR-04-Security.md, replace Section 3.2 table with:**

```markdown
### 3.2 Authorisation Model

| Role | Station Scope | Capabilities |
|------|---------------|--------------|
| **Firefighter** | Assigned station only | View apparatus, perform inventory checks, report issues |
| **Maintenance Technician** | All stations | All firefighter capabilities + equipment management, audits, transfers, restocking, reports |
| **System Administrator** | All stations | All maintenance capabilities + user management, station management, system configuration, audit log access |

### 3.2.1 System Administrator Specific Capabilities

System Administrators have all Maintenance Technician capabilities plus:

| Capability | Description |
|------------|-------------|
| User management | Create, modify, deactivate users; assign roles and stations |
| Station management | Create, modify, activate/deactivate stations |
| Equipment type management | Create and modify equipment type definitions |
| System configuration | Configure session timeout, notification settings, etc. |
| Audit log access | Query and export full audit trail |
| Data export | Export data for external reporting or archival |

**Note:** System Administrator is intended as a dual role (operational + administrative). Administrators can perform inventory checks, audits, and other operational tasks in addition to administrative functions.
```

---

## Fix 4: Consumable Assignment States

**Files affected:** `Domain/ConsumableStock.md`

### Problem
EquipmentItem.md defines assignment states (On Apparatus, In Storage, In Transit) but ConsumableStock.md doesn't clarify whether consumables can be "In Transit" or what validates station/apparatus consistency.

### Proposed Changes

**In Domain/ConsumableStock.md, add new Section 6 after Stock Levels:**

```markdown
## 6. Assignment States

Consumable stock has simpler assignment rules than serialised equipment:

| State | apparatusId | compartmentId | stationId | Description |
|-------|-------------|---------------|-----------|-------------|
| On Apparatus | Set | Optional | (derived) | Stock is on an apparatus |
| In Station Storage | Null | Null | Set | Stock is in station storage area |

**Key differences from Equipment Items:**

| Aspect | Equipment Item | Consumable Stock |
|--------|----------------|------------------|
| In Transit state | Yes (during transfer) | No — consumables are adjusted, not transferred |
| Transfer mechanism | TransferRecord entity | Restock records (TRANSFER_OUT/TRANSFER_IN) |
| Compartment required | Yes (when on apparatus) | No (can be "loose" on apparatus) |

**Validation rules:**

| Rule | Description |
|------|-------------|
| V-01 | If apparatusId is set, stationId must match the apparatus's station |
| V-02 | If compartmentId is set, apparatusId must also be set |
| V-03 | If compartmentId is set, it must belong to the specified apparatus |
| V-04 | Either apparatusId or stationId must be set (consumables always have a location) |
```

**In Domain/ConsumableStock.md, add to Business Rules:**

```markdown
| BR-07 | Consumables cannot be in "In Transit" state; inter-location moves are recorded as paired TRANSFER_OUT/TRANSFER_IN restock records |
| BR-08 | stationId is derived from apparatusId when apparatus is set; explicit stationId only for storage |
```

---

## Fix 5: Audit Trail vs Domain Entity Redundancy

**Files affected:** `NFRs/NFR-05-Auditability.md`

### Problem
UC-01 creates InventoryCheck and InventoryCheckItem records. NFR-05 also requires audit events for these same actions. The relationship between domain entities and audit trail is unclear.

### Proposed Changes

**In NFRs/NFR-05-Auditability.md, add new Section 3.5:**

```markdown
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
```

---

## Fix 6: Missing Apparatus Transfer Use Case

**New file:** `UseCases/UC-07-Transfer-Apparatus.md`

### Problem
Apparatus.md Section 10 mentions that apparatus transfers should be rare but the workflow is not specified.

### Proposed New Use Case

See separate file: `UC-07-Transfer-Apparatus.md` (to be created)

Key points:
- Primary actor: System Administrator (requires elevated privileges)
- Precondition: All equipment must be transferred or remain with apparatus
- Workflow: Validate → Update stationId → Reassign users if needed → Audit
- Business rules: Requires reason, apparatus must be OUT_OF_SERVICE during transfer

---

## Fix 7: Missing Reporting/Dashboard Use Case

**New file:** `UseCases/UC-08-View-Reports-Dashboard.md`

### Problem
Context.md success criteria #2 states "Maintenance crews can view real-time stock levels across all stations" but there's no use case for this.

### Proposed New Use Case

See separate file: `UC-08-View-Reports-Dashboard.md` (to be created)

Key points:
- Primary actors: Maintenance Technician, System Administrator
- Views: Station overview, cross-station stock levels, equipment status summary
- Queries: Low stock alerts, overdue checks, unresolved issues, equipment due for testing
- Export: PDF/CSV export for reporting

---

## Implementation Priority

| Priority | Fix | Rationale |
|----------|-----|-----------|
| 1 | Fix 1 (Auto-abandon) | Prevents data loss and user confusion |
| 2 | Fix 4 (Consumable states) | Required for implementation clarity |
| 3 | Fix 5 (Audit relationship) | Required for implementation clarity |
| 4 | Fix 3 (Admin scope) | Affects authorization implementation |
| 5 | Fix 2 (Deactivated stations) | Edge case, but needs definition |
| 6 | Fix 6 (UC-07) | New functionality, lower priority |
| 7 | Fix 7 (UC-08) | New functionality, lower priority |

---

## Next Steps

1. Review and approve proposed fixes
2. Apply fixes to existing specification files
3. Create UC-07 and UC-08 as new documents
4. Update cross-references between documents

