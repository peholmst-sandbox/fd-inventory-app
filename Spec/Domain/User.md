# Domain Concept: User

**Version:** 1.0
**Date:** 2026-01-30

---

## 1. Definition

A **User** is an authenticated individual who interacts with FireStock. Users are assigned roles that determine their permissions and may be associated with one or more stations depending on their role.

## 2. Attributes

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Unique system identifier |
| `email` | String | Yes | Email address (used for login) |
| `firstName` | String | Yes | User's first name |
| `lastName` | String | Yes | User's last name |
| `badgeNumber` | String | No | Employee/badge identifier |
| `phone` | String | No | Contact phone number |
| `role` | UserRole | Yes | User's role in the system |
| `isActive` | Boolean | Yes | Whether user can log in |
| `lastLoginAt` | Timestamp | No | Last successful login |
| `createdAt` | Timestamp | Yes | When record was created |
| `updatedAt` | Timestamp | Yes | When record was last modified |

## 3. Enumerations

### UserRole
- `FIREFIGHTER` — Station-based personnel performing inventory checks and reporting issues
- `MAINTENANCE_TECHNICIAN` — Personnel conducting audits, transfers, and equipment management
- `SYSTEM_ADMINISTRATOR` — Full system access including user and station management

## 4. Relationships

| Related Concept | Cardinality | Description |
|-----------------|-------------|-------------|
| Station | N:M | Users may be assigned to multiple stations (via UserStationAssignment) |
| Inventory Check | 1:N | User performs inventory checks |
| Issue | 1:N | User reports issues |
| Transfer Record | 1:N | User initiates transfers |
| Audit Log | 1:N | User's actions are logged |

## 5. Supporting Entity: UserStationAssignment

Links users to their assigned stations.

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | UUID | Yes | Reference to user |
| `stationId` | UUID | Yes | Reference to station |
| `isPrimary` | Boolean | Yes | Whether this is the user's primary station |
| `assignedAt` | Timestamp | Yes | When assignment was made |

## 6. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Email must be unique within the system |
| BR-02 | Badge number must be unique if specified |
| BR-03 | Firefighters must be assigned to at least one station |
| BR-04 | Maintenance technicians have implicit access to all stations (no assignment required) |
| BR-05 | System administrators have implicit access to all data (no assignment required) |
| BR-06 | Deactivating a user does not delete their data; historical records are preserved |
| BR-07 | A user's role cannot be changed while they have in-progress inventory checks |

## 7. Lifecycle

```
┌───────────┐     ┌─────────┐     ┌──────────┐
│  Created  │────▶│  Active │────▶│ Inactive │
└───────────┘     └─────────┘     └──────────┘
                       │               │
                       └───────────────┘
                        (can reactivate)
```

## 8. Access Control by Role

| Role | Station Access | Capabilities |
|------|----------------|--------------|
| Firefighter | Assigned stations only | View apparatus, perform inventory checks, report issues |
| Maintenance Technician | All stations | All firefighter capabilities + audits, transfers, equipment registration |
| System Administrator | All stations | All capabilities + user management, station management, system configuration |

## 9. Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440300",
  "email": "john.smith@firedept.gov",
  "firstName": "John",
  "lastName": "Smith",
  "badgeNumber": "FD-1234",
  "phone": "+44 161 555 0199",
  "role": "FIREFIGHTER",
  "isActive": true,
  "lastLoginAt": "2026-01-30T07:15:00Z",
  "createdAt": "2023-03-01T09:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z"
}
```

## 10. Authentication Notes

- Authentication is handled via external identity provider or local credentials
- Session timeout: 8 hours of inactivity (configurable per deployment)
- Failed login attempts should be logged but lockout policy is deployment-specific
- Password requirements are deployment-specific (minimum 8 characters recommended)

## 11. Notes

- User deletion should be soft delete (isActive = false) to preserve audit trail integrity
- When a station is deactivated, firefighters assigned only to that station should be notified
- Consider future support for temporary station assignments (e.g., mutual aid)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
