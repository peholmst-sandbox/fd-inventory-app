# Non-Functional Requirement: Scalability

**ID:** NFR-06  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall accommodate growth in the number of stations, apparatus, equipment items, and users without significant architectural changes or degradation in performance.

## 2. Rationale

While the initial deployment may cover a limited number of stations, success may lead to regional expansion or adoption by neighbouring fire services. The system design should not impose artificial limits that require expensive rearchitecting to overcome.

## 3. Specification

### 3.1 Capacity Targets

| Dimension | Initial Target | Growth Target (3 years) |
|-----------|---------------|------------------------|
| Stations | 20 | 100 |
| Apparatus per station | 5 | 10 |
| Total apparatus | 100 | 1,000 |
| Equipment items per apparatus | 150 | 200 |
| Total equipment items | 15,000 | 200,000 |
| Users (firefighters) | 300 | 2,000 |
| Users (maintenance) | 20 | 100 |
| Concurrent users (peak) | 100 | 500 |
| Inventory checks per day | 200 | 2,000 |
| Audit records per year | 500,000 | 10,000,000 |

### 3.2 Performance at Scale

At growth target volumes, the system must still meet performance requirements defined in NFR-02:
- Page load < 1.5 seconds (95th percentile)
- Search results < 2 seconds (single station), < 4 seconds (all stations)
- Inventory check operations < 500 ms

### 3.3 Scaling Strategies

| Component | Initial | Scaled |
|-----------|---------|--------|
| Application servers | Single instance | Horizontal scaling behind load balancer |
| Database | Single PostgreSQL instance | Primary + read replicas; consider partitioning |
| Session management | In-memory (Vaadin default) | Sticky sessions or external session store |
| Audit storage | Same database | Separate database or time-series store |

## 4. Design Considerations

### 4.1 Database Design
- Use appropriate indexes for common query patterns (station_id, apparatus_id, equipment_type_id)
- Avoid unbounded queries; always paginate lists
- Consider partitioning audit tables by date for archival
- Monitor query performance and add indexes proactively

### 4.2 Application Design
- Stateless business logic where possible (Vaadin UI is inherently stateful)
- Cache reference data (equipment types, compartment templates)
- Design APIs to work with pages/batches, not unbounded lists
- Avoid operations that load all equipment or all apparatus into memory

### 4.3 Multi-Tenancy
- Station-based data isolation is already required (NFR-04)
- Query patterns naturally partition by station, aiding database performance
- Consider database-level row security policies for defence in depth

## 5. Limits and Constraints

| Constraint | Limit | Rationale |
|------------|-------|-----------|
| Equipment items per apparatus | 500 | UI/UX manageable list length |
| Apparatus per station | 20 | Expected upper bound |
| Concurrent inventory checks per apparatus | 1 | Business rule: only one check at a time |
| Search result page size | 100 | Performance and usability |

## 6. Monitoring and Alerting

To detect scaling issues early:
- Monitor database query times; alert if 95th percentile exceeds 200 ms
- Monitor application memory usage; alert if heap usage exceeds 80%
- Monitor connection pool exhaustion
- Track concurrent user counts; alert at 80% of tested capacity

## 7. Verification

| Method | Description |
|--------|-------------|
| **Load testing at scale** | Simulate growth target volumes; verify performance requirements met |
| **Database capacity planning** | Project storage requirements for 3-year growth |
| **Stress testing** | Push beyond growth targets to find breaking points |

## 8. Acceptance Criteria

- [ ] System handles initial target volumes with performance requirements met
- [ ] Load test at 2x initial volumes shows no degradation
- [ ] Database schema supports growth target volumes without structural changes
- [ ] Documented scaling runbook for adding application server capacity
- [ ] Monitoring and alerting configured for capacity indicators

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |