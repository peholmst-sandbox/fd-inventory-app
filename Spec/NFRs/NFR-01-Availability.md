# Non-Functional Requirement: Availability

**ID:** NFR-01  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall be available for use during all hours when stations are operational, with minimal unplanned downtime.

## 2. Rationale

Shift inventory checks occur at predictable times (shift changes, typically 07:00, 15:00, 19:00). If the system is unavailable during these windows, crews cannot complete required checks, leading to either delayed shifts or skipped verifications. While not as critical as a field-deployed system, unavailability erodes trust and drives users back to paper-based methods.

## 3. Specification

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| **Uptime** | 99.5% measured monthly | Automated health checks every 60 seconds |
| **Planned maintenance window** | Maximum 2 hours, once per month | Scheduled outside peak shift-change times (avoid 06:00–08:00, 14:00–16:00, 18:00–20:00) |
| **Unplanned downtime** | Maximum 4 hours per incident | Incident tracking |
| **Recovery Time Objective (RTO)** | 2 hours | Time from failure detection to service restoration |
| **Recovery Point Objective (RPO)** | 1 hour | Maximum acceptable data loss |

## 4. Scenarios

### 4.1 Normal Operation
System responds to all requests within performance targets (see NFR-02). All features available to all authorised users.

### 4.2 Degraded Operation
If external dependencies (e.g., barcode lookup service) fail, core functionality (manual entry, inventory checks) remains available. Users are notified of reduced capability.

### 4.3 Maintenance Mode
During planned maintenance, users see a clear message indicating expected restoration time. No data entry is possible; read-only access may be provided if feasible.

## 5. Constraints

- Vaadin's stateful server-side architecture means user sessions are lost on server restart
- Database backups must not impact system responsiveness during peak hours
- Single-region deployment acceptable for initial release; multi-region for disaster recovery is out of scope

## 6. Verification

| Method | Description |
|--------|-------------|
| **Monitoring** | Uptime monitoring service (e.g., health endpoint checks) with alerting |
| **Incident review** | Monthly review of any downtime incidents |
| **Load testing** | Verify system remains available under expected peak load (see NFR-02) |

## 7. Dependencies

- Reliable hosting infrastructure (cloud provider or on-premises with redundancy)
- Database backup and restore procedures
- Monitoring and alerting tooling

## 8. Acceptance Criteria

- [ ] Health check endpoint responds within 5 seconds, checked every 60 seconds
- [ ] Uptime of 99.5% demonstrated over a 30-day pilot period
- [ ] Planned maintenance procedure documented and tested
- [ ] Backup and restore procedure documented and tested with RTO/RPO targets met

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |