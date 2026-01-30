# Domain Concept: Inventory Check

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

An **Inventory Check** is a systematic verification of equipment and consumables on an apparatus. Typically performed at shift changes, inventory checks ensure all required items are present and in acceptable condition.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `apparatusId` | UUID | Yes | Apparatus being checked |
| `stationId` | UUID | Yes | Station context (derived from apparatus) |
| `performedById` | UUID | Yes | User performing the check |
| `status` | CheckStatus | Yes | Current status of the check |
| `startedAt` | Timestamp | Yes | When check was started |
| `completedAt` | Timestamp | No | When check was completed |
| `abandonedAt` | Timestamp | No | When check was abandoned (if applicable) |
| `totalItems` | Integer | No | Total number of items to verify |
| `verifiedCount` | Integer | No | Number of items verified |
| `issuesFoundCount` | Integer | No | Number of issues discovered |
| `notes` | String | No | General notes about the check |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### CheckStatus
- `IN_PROGRESS` — Check is currently being performed
- `COMPLETED` — Check finished successfully
- `ABANDONED` — Check was not finished (interrupted or timed out)

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Apparatus | N:1 | Check is performed on one apparatus |
| Station | N:1 | Check belongs to a station context |
| User | N:1 | Check is performed by one user |
| Inventory Check Item | 1:N | Check contains multiple item verifications |
| Issue | 1:N | Issues may be created during check |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only one inventory check may be in progress per apparatus at any time |
| BR-02 | A check must verify all manifest items to be marked COMPLETED |
| BR-03 | Checks in progress for more than 4 hours are automatically marked as ABANDONED |
| BR-04 | An abandoned check can be resumed within 30 minutes by the same user |
| BR-05 | Only firefighters and maintenance technicians can perform inventory checks |
| BR-06 | Check cannot be started on OUT_OF_SERVICE or DECOMMISSIONED apparatus |

## 6. Lifecycle

```
┌─────────────┐     ┌─────────────┐     ┌───────────┐
│ IN_PROGRESS │────▶│  COMPLETED  │     │ ABANDONED │
└─────────────┘     └─────────────┘     └───────────┘
       │                                      ▲
       │                                      │
       └──────────────────────────────────────┘
              (timeout or manual abandon)
```

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

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440500",
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "stationId": "550e8400-e29b-41d4-a716-446655440001",
  "performedById": "550e8400-e29b-41d4-a716-446655440300",
  "status": "COMPLETED",
  "startedAt": "2026-01-30T07:00:00Z",
  "completedAt": "2026-01-30T07:35:00Z",
  "abandonedAt": null,
  "totalItems": 45,
  "verifiedCount": 45,
  "issuesFoundCount": 2,
  "notes": "Two items found damaged - SCBA mask and halligan bar. Issues created.",
  "createdAt": "2026-01-30T07:00:00Z",
  "updatedAt": "2026-01-30T07:35:00Z"
}
```

## 9. Query Patterns

| Query | Use Case |
|-------|----------|
| By apparatus + date range | Check history for apparatus |
| By station + date | Daily check summary |
| By performer | User's check history |
| By status IN_PROGRESS | Active checks dashboard |
| By status ABANDONED | Identify incomplete checks |
| Latest by apparatus | Last check timestamp |

## 10. Metrics

Key metrics derived from inventory checks:
- Average check duration
- Checks per shift/day/week
- Abandonment rate
- Issues found per check
- Time since last check per apparatus

## 11. Notes

- Inventory checks are the primary mechanism for ensuring equipment readiness
- Check frequency is typically shift-based but not enforced by the system
- Consider alerts for apparatus not checked within expected timeframe
- Photo attachments may be added to check items (see InventoryCheckItem)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Clarified auto-abandon timing, resume window, and race condition handling |
