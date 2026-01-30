# Domain Concept: Station

**Version:** 1.0  
**Date:** 2026-01-30

---

## 1. Definition

A **Station** is a fire service facility where apparatus are housed and firefighters are assigned. It serves as the primary organisational unit for access control and data partitioning in FireStock.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `code` | String | Yes | Short identifier (e.g., "STA-05", "FD-NORTH") |
| `name` | String | Yes | Full station name (e.g., "North District Fire Station") |
| `address` | Address | Yes | Physical location |
| `region` | String | No | Organisational region or district |
| `contactPhone` | String | No | Primary contact number |
| `isActive` | Boolean | Yes | Whether station is operational |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Apparatus | 1:N | A station houses multiple apparatus |
| User | N:M | Users (firefighters) are assigned to one or more stations |
| Inventory Check | 1:N | Checks are performed at a station's apparatus |
| Issue | 1:N | Issues are associated with a station context |

## 4. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Station code must be unique within the system |
| BR-02 | A station must have at least one assigned user to be operational |
| BR-03 | Deactivating a station does not delete its data; historical records are preserved |
| BR-04 | Firefighters can only access data for stations they are assigned to |
| BR-05 | Maintenance technicians can access all stations |
| BR-06 | When a station is deactivated, firefighters assigned only to that station lose system access |
| BR-07 | Historical data from deactivated stations is accessible to maintenance technicians and administrators for audit purposes |

## 5. Lifecycle

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Created   │────▶│   Active    │────▶│  Inactive   │
└─────────────┘     └─────────────┘     └─────────────┘
                          │                    │
                          └────────────────────┘
                            (can reactivate)
```

## 6. Access Control

- **Firefighters:** Can view their assigned station(s) only
- **Maintenance Technicians:** Can view and manage all stations
- **System Administrators:** Full access including create/deactivate

## 7. Deactivation Behavior

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

## 8. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "code": "STA-05",
  "name": "North District Fire Station",
  "address": {
    "street": "123 Firefighter Lane",
    "city": "Metropolis",
    "postcode": "M1 5FD"
  },
  "region": "North District",
  "contactPhone": "+44 161 555 0105",
  "isActive": true,
  "createdAt": "2024-01-15T09:00:00Z",
  "updatedAt": "2025-06-20T14:30:00Z"
}
```

## 9. Notes

- Station is the primary tenant boundary for multi-tenancy
- All queries for firefighter users must be scoped by station
- Station deactivation should be rare and requires admin approval

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
| 1.1 | 2026-01-30 | — | Added BR-06/BR-07 and Section 7 (Deactivation Behavior) |