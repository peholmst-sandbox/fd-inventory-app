# Domain Concept: Issue

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

An **Issue** is a reported problem with equipment or consumables that requires attention. Issues track the lifecycle of problems from initial report through resolution, enabling visibility into equipment health and maintenance needs.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `referenceNumber` | String | Yes | Human-readable identifier (e.g., "ISS-2026-00123") |
| `equipmentItemId` | UUID | No | Reference to specific equipment item (if applicable) |
| `consumableStockId` | UUID | No | Reference to consumable stock (if applicable) |
| `apparatusId` | UUID | Yes | Apparatus where issue was discovered |
| `stationId` | UUID | Yes | Station context (derived from apparatus) |
| `title` | String | Yes | Brief summary of the issue |
| `description` | String | Yes | Detailed description of the problem |
| `severity` | IssueSeverity | Yes | Impact level |
| `category` | IssueCategory | Yes | Type of issue |
| `status` | IssueStatus | Yes | Current status |
| `reportedById` | UUID | Yes | User who reported the issue |
| `reportedAt` | Timestamp | Yes | When issue was reported |
| `acknowledgedById` | UUID | No | User who acknowledged the issue |
| `acknowledgedAt` | Timestamp | No | When issue was acknowledged |
| `resolvedById` | UUID | No | User who resolved the issue |
| `resolvedAt` | Timestamp | No | When issue was resolved |
| `resolutionNotes` | String | No | Description of how issue was resolved |
| `isCrewResponsibility` | Boolean | Yes | Whether issue is for crew-owned equipment (not routed to maintenance) |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### IssueSeverity
- `CRITICAL` — Equipment unsafe to use; apparatus may not be deployable
- `HIGH` — Significant impact on operations; requires prompt attention
- `MEDIUM` — Moderate impact; should be addressed soon
- `LOW` — Minor issue; can be addressed during routine maintenance

### IssueCategory
- `DAMAGE` — Physical damage to equipment
- `MALFUNCTION` — Equipment not working correctly
- `MISSING` — Equipment cannot be located
- `EXPIRED` — Equipment or consumable past expiry date
- `LOW_STOCK` — Consumable below minimum threshold
- `CONTAMINATION` — Equipment contaminated or soiled
- `CALIBRATION` — Equipment out of calibration
- `OTHER` — Other issue type

### IssueStatus
- `OPEN` — Newly reported, awaiting acknowledgment
- `ACKNOWLEDGED` — Seen by maintenance, pending action
- `IN_PROGRESS` — Actively being worked on
- `RESOLVED` — Issue has been fixed
- `CLOSED` — Issue closed (may be duplicate or invalid)

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Equipment Item | N:1 | Issue may relate to a specific equipment item |
| Consumable Stock | N:1 | Issue may relate to consumable stock |
| Apparatus | N:1 | Issue is associated with an apparatus |
| Station | N:1 | Issue belongs to a station context |
| User (Reporter) | N:1 | User who reported the issue |
| User (Acknowledger) | N:1 | User who acknowledged the issue |
| User (Resolver) | N:1 | User who resolved the issue |
| Issue Photo | 1:N | Issue may have attached photos |

## 5. Supporting Entity: IssuePhoto

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique identifier |
| `issueId` | UUID | Yes | Reference to parent issue |
| `filename` | String | Yes | Original filename |
| `storagePath` | String | Yes | Path/URL to stored file |
| `contentType` | String | Yes | MIME type (image/jpeg, image/png) |
| `fileSizeBytes` | Integer | Yes | File size in bytes |
| `uploadedAt` | Timestamp | Yes | When photo was uploaded |

## 6. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Reference number must be unique and auto-generated |
| BR-02 | Either equipmentItemId or consumableStockId should be set, but not required (apparatus-level issues are valid) |
| BR-03 | Status transitions must follow the defined lifecycle |
| BR-04 | Critical issues must be acknowledged within 1 hour (tracked for reporting) |
| BR-05 | Photos must be under 10 MB each |
| BR-06 | Maximum 5 photos per issue |
| BR-07 | Resolving an issue requires resolution notes |
| BR-08 | Critical issues for department equipment should trigger notification to maintenance technicians |
| BR-09 | When equipment item is marked DAMAGED or MISSING, an issue should be created automatically |
| BR-10 | Issues for crew-owned equipment have isCrewResponsibility = true and are not routed to maintenance technicians |
| BR-11 | Crew responsibility issues are visible to station crews for self-management |

## 7. Lifecycle

```
┌────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│  OPEN  │────▶│ ACKNOWLEDGED │────▶│ IN_PROGRESS │────▶│ RESOLVED │
└────────┘     └──────────────┘     └─────────────┘     └──────────┘
     │                                     │                  │
     │                                     │                  ▼
     │                                     │            ┌──────────┐
     └─────────────────────────────────────┴───────────▶│  CLOSED  │
                      (can close at any stage)          └──────────┘
```

## 8. Impact on Related Entities

| Severity | Equipment Status Impact | Apparatus Status Impact |
|----------|------------------------|------------------------|
| CRITICAL | Should be DAMAGED or MISSING | Consider OUT_OF_SERVICE |
| HIGH | Should be DAMAGED | No automatic change |
| MEDIUM | No automatic change | No automatic change |
| LOW | No automatic change | No automatic change |

## 9. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440400",
  "referenceNumber": "ISS-2026-00042",
  "equipmentItemId": "550e8400-e29b-41d4-a716-446655440200",
  "consumableStockId": null,
  "apparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "stationId": "550e8400-e29b-41d4-a716-446655440001",
  "title": "SCBA mask seal damaged",
  "description": "Visible crack in the face seal of SCBA unit SCT-2020-04523. Discovered during morning inventory check. Unit should not be used until repaired.",
  "severity": "CRITICAL",
  "category": "DAMAGE",
  "status": "ACKNOWLEDGED",
  "reportedById": "550e8400-e29b-41d4-a716-446655440300",
  "reportedAt": "2026-01-30T07:45:00Z",
  "acknowledgedById": "550e8400-e29b-41d4-a716-446655440301",
  "acknowledgedAt": "2026-01-30T08:15:00Z",
  "resolvedById": null,
  "resolvedAt": null,
  "resolutionNotes": null,
  "createdAt": "2026-01-30T07:45:00Z",
  "updatedAt": "2026-01-30T08:15:00Z"
}
```

## 10. Query Patterns

| Query | Use Case |
|-------|----------|
| By station + status OPEN/ACKNOWLEDGED | Station dashboard |
| By apparatus | Apparatus detail view |
| By equipment item | Equipment history |
| By severity CRITICAL + status not RESOLVED | Urgent issues alert |
| By reporter | User's reported issues |
| By date range | Reporting and analytics |

## 11. Notes

- Issues provide the primary mechanism for tracking equipment problems
- Integration with external maintenance systems may be added in future
- Consider email/SMS notifications for critical issues
- Issue metrics (time to acknowledge, time to resolve) should be tracked for KPIs
- Crew responsibility issues allow station crews to track problems with their own equipment without involving maintenance technicians

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added isCrewResponsibility for crew-owned equipment issues |
