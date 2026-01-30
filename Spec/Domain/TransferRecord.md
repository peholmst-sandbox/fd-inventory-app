# Domain Concept: Transfer Record

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

A **Transfer Record** documents the movement of equipment items between locations (apparatus, stations, or storage). It provides an audit trail of equipment custody and enables tracking of equipment history.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `referenceNumber` | String | Yes | Human-readable identifier (e.g., "TRF-2026-00456") |
| `equipmentItemId` | UUID | Yes | Equipment item being transferred |
| `transferType` | TransferType | Yes | Type of transfer |
| `sourceType` | LocationType | Yes | Type of source location |
| `sourceApparatusId` | UUID | No | Source apparatus (if from apparatus) |
| `sourceCompartmentId` | UUID | No | Source compartment (if from apparatus) |
| `sourceStationId` | UUID | No | Source station (if from storage or for context) |
| `destinationType` | LocationType | Yes | Type of destination location |
| `destinationApparatusId` | UUID | No | Destination apparatus (if to apparatus) |
| `destinationCompartmentId` | UUID | No | Destination compartment (if to apparatus) |
| `destinationStationId` | UUID | No | Destination station (if to storage or for context) |
| `reason` | String | Yes | Reason for transfer |
| `status` | TransferStatus | Yes | Current status |
| `initiatedById` | UUID | Yes | User who initiated the transfer |
| `initiatedAt` | Timestamp | Yes | When transfer was initiated |
| `completedAt` | Timestamp | No | When transfer was completed |
| `notes` | String | No | Additional notes |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### TransferType
- `APPARATUS_TO_APPARATUS` — Between apparatus (same or different station)
- `APPARATUS_TO_STORAGE` — From apparatus to station storage
- `STORAGE_TO_APPARATUS` — From station storage to apparatus
- `STATION_TO_STATION` — Between stations (inter-station transfer)
- `TO_REPAIR` — Sent for repair/maintenance
- `FROM_REPAIR` — Returned from repair/maintenance
- `RETIREMENT` — Removed from service

### LocationType
- `APPARATUS` — On an apparatus
- `STORAGE` — In station storage
- `REPAIR` — At repair facility
- `EXTERNAL` — External location (vendor, other department)

### TransferStatus
- `PENDING` — Transfer initiated, awaiting completion
- `IN_TRANSIT` — Item in transit between locations
- `COMPLETED` — Transfer completed successfully
- `CANCELLED` — Transfer was cancelled

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Equipment Item | N:1 | Transfer is for one equipment item |
| Apparatus (Source) | N:1 | Source apparatus if applicable |
| Apparatus (Destination) | N:1 | Destination apparatus if applicable |
| Compartment (Source) | N:1 | Source compartment if applicable |
| Compartment (Destination) | N:1 | Destination compartment if applicable |
| Station (Source) | N:1 | Source station context |
| Station (Destination) | N:1 | Destination station context |
| User | N:1 | User who initiated transfer |

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Reference number must be unique and auto-generated |
| BR-02 | Source location must match equipment item's current location |
| BR-03 | Only one pending/in-transit transfer per equipment item at a time |
| BR-04 | Inter-station transfers require maintenance technician role |
| BR-05 | Equipment item status must be OK or DAMAGED to transfer (not RETIRED) |
| BR-06 | Completing a transfer updates the equipment item's location fields |
| BR-07 | Cancelled transfers do not modify equipment item location |
| BR-08 | Transfer to REPAIR should update equipment status to IN_REPAIR |

## 6. Lifecycle

```
┌─────────┐     ┌────────────┐     ┌───────────┐
│ PENDING │────▶│ IN_TRANSIT │────▶│ COMPLETED │
└─────────┘     └────────────┘     └───────────┘
     │               │
     │               │
     ▼               ▼
┌───────────┐  ┌───────────┐
│ CANCELLED │  │ CANCELLED │
└───────────┘  └───────────┘
```

## 7. Transfer Flow

```
┌──────────────┐     ┌───────────────┐     ┌────────────────┐
│   Initiate   │────▶│  Item marked  │────▶│    Complete    │
│   Transfer   │     │  "In Transit" │     │    Transfer    │
└──────────────┘     └───────────────┘     └────────────────┘
        │                    │                      │
        │                    │                      │
        ▼                    ▼                      ▼
 Equipment item       Item location         Item location
 validated            set to null           set to destination
```

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440800",
  "referenceNumber": "TRF-2026-00042",
  "equipmentItemId": "550e8400-e29b-41d4-a716-446655440200",
  "transferType": "APPARATUS_TO_APPARATUS",
  "sourceType": "APPARATUS",
  "sourceApparatusId": "550e8400-e29b-41d4-a716-446655440010",
  "sourceCompartmentId": "550e8400-e29b-41d4-a716-446655440020",
  "sourceStationId": "550e8400-e29b-41d4-a716-446655440001",
  "destinationType": "APPARATUS",
  "destinationApparatusId": "550e8400-e29b-41d4-a716-446655440011",
  "destinationCompartmentId": "550e8400-e29b-41d4-a716-446655440025",
  "destinationStationId": "550e8400-e29b-41d4-a716-446655440001",
  "reason": "Rebalancing SCBA units between apparatus",
  "status": "COMPLETED",
  "initiatedById": "550e8400-e29b-41d4-a716-446655440301",
  "initiatedAt": "2026-01-30T10:00:00Z",
  "completedAt": "2026-01-30T10:15:00Z",
  "notes": null,
  "createdAt": "2026-01-30T10:00:00Z",
  "updatedAt": "2026-01-30T10:15:00Z"
}
```

## 9. Query Patterns

| Query | Use Case |
|-------|----------|
| By equipment item | Complete transfer history for item |
| By source station | Outgoing transfers from station |
| By destination station | Incoming transfers to station |
| By status PENDING/IN_TRANSIT | Active transfers |
| By initiator | User's transfer history |
| By date range | Transfer activity reporting |

## 10. Concurrent Transfer Prevention

To prevent race conditions:
1. Check for existing PENDING or IN_TRANSIT transfers before creating new transfer
2. Use optimistic locking on equipment item when updating location
3. Transfer creation should be atomic with item location validation

## 11. Notes

- Transfer records provide chain of custody for equipment
- Consider batch transfers for moving multiple items together
- Inter-station transfers may require additional approval workflow in future
- Transfer metrics can identify equipment circulation patterns

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
