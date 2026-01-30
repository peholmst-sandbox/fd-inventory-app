# Non-Functional Requirement: Security and Data Isolation

**ID:** NFR-04  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall enforce strict access controls ensuring that firefighters can only view and modify data for their assigned station, while maintenance crews have read and write access across all stations. All access shall be authenticated and authorised appropriately.

## 2. Rationale

The multi-tenant nature of the system (multiple stations sharing one deployment) requires that data isolation be enforced at all layers. A firefighter at Station A must not see Station B's equipment or inventory status. Beyond privacy concerns, incorrect data visibility could lead to confusion and operational errors. Maintenance crews require cross-station access to perform their regional responsibilities.

## 3. Specification

### 3.1 Authentication

| Requirement | Specification |
|-------------|---------------|
| Authentication method | Username/password with option for SSO integration in future |
| Password policy | Minimum 10 characters; no complexity requirements (length over complexity) |
| Session timeout | 8 hours of inactivity (aligns with shift length) |
| Failed login lockout | 5 failed attempts triggers 15-minute lockout |
| Password storage | bcrypt with cost factor ≥ 12 |

### 3.2 Authorisation Model

| Role | Station Scope | Capabilities |
|------|---------------|--------------|
| **Firefighter** | Assigned station only | View apparatus, perform inventory checks, report issues |
| **Maintenance Technician** | All stations | All firefighter capabilities + equipment management, audits, transfers, reports |
| **System Administrator** | All stations | All capabilities + user management, system configuration |

### 3.3 Data Isolation Requirements

| Data Type | Firefighter Access | Maintenance Access |
|-----------|-------------------|-------------------|
| Apparatus in own station | Full | Full |
| Apparatus in other stations | None | Full |
| Equipment in own station | Full | Full |
| Equipment in other stations | None | Full |
| Own inventory checks | Full | Full |
| Other users' inventory checks (own station) | Read-only | Full |
| Other stations' inventory checks | None | Full |

### 3.4 Transport Security

| Requirement | Specification |
|-------------|---------------|
| Protocol | HTTPS only; HTTP redirects to HTTPS |
| TLS version | TLS 1.2 minimum; TLS 1.3 preferred |
| Certificate | Valid certificate from trusted CA |
| HSTS | Enabled with max-age of 1 year |

## 4. Implementation Requirements

### 4.1 Query-Level Enforcement
All database queries accessing station-scoped data must include station filtering. This must be enforced at the repository/DAO layer, not rely on UI-level filtering alone.

```
-- Example: Equipment query for firefighter
SELECT * FROM equipment e
JOIN apparatus a ON e.apparatus_id = a.id
WHERE a.station_id = :userStationId  -- Always applied for firefighter role
```

### 4.2 API-Level Enforcement
All API endpoints accessing station-scoped resources must verify the user's station assignment before returning data.

### 4.3 Defence in Depth
- UI hides inaccessible data (first layer)
- Service layer checks authorisation (second layer)
- Database queries filter by station (third layer)

## 5. Threats and Mitigations

| Threat | Mitigation |
|--------|------------|
| Session hijacking | Secure cookies; regenerate session ID on login |
| IDOR (direct object reference) | Authorisation check on every data access |
| SQL injection | Parameterised queries via JOOQ |
| XSS | Vaadin's built-in escaping; CSP headers |
| CSRF | Vaadin's built-in CSRF protection |
| Privilege escalation | Role checks at service layer; principle of least privilege |

## 6. Verification

| Method | Description |
|--------|-------------|
| **Penetration testing** | Attempt to access other stations' data as firefighter |
| **Code review** | Verify all queries include station filtering |
| **Automated security scanning** | OWASP ZAP or similar against staging environment |
| **Access control testing** | Automated tests verifying role-based access for all endpoints |

## 7. Acceptance Criteria

- [ ] Firefighter cannot view any apparatus or equipment from other stations (verified by penetration test)
- [ ] Maintenance technician can view and modify data across all stations
- [ ] All passwords stored using bcrypt with cost factor ≥ 12
- [ ] TLS 1.2+ enforced; SSLLabs rating of A or higher
- [ ] Session expires after 8 hours of inactivity
- [ ] Failed login lockout triggers after 5 attempts
- [ ] No SQL injection vulnerabilities (verified by automated scan)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |