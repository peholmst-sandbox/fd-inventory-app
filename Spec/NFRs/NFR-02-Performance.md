# Non-Functional Requirement: Performance

**ID:** NFR-02  
**Version:** 1.0  
**Date:** 2026-01-30  
**Status:** Draft

---

## 1. Statement

The FireStock system shall respond to user interactions promptly, ensuring that inventory checks and common operations feel fluid and do not impede workflow.

## 2. Rationale

Firefighters performing shift checks want to complete the task quickly and move on. Slow responses lead to frustration, workarounds (skipping items), and abandonment of the system. Maintenance crews performing audits across multiple apparatus need efficient workflows to complete their work within reasonable timeframes.

## 3. Specification

### 3.1 Response Time Targets

| Operation | Target (95th percentile) | Maximum Acceptable |
|-----------|-------------------------|-------------------|
| Page/view load | < 1.5 seconds | 3 seconds |
| Barcode scan result | < 1 second | 2 seconds |
| Save inventory check item | < 500 ms | 1 second |
| Search equipment (single station) | < 1 second | 2 seconds |
| Search equipment (all stations) | < 2 seconds | 4 seconds |
| Generate report | < 5 seconds | 10 seconds |

### 3.2 Throughput Targets

| Metric | Target |
|--------|--------|
| Concurrent users | 100 simultaneous users |
| Inventory checks in progress | 50 simultaneous checks |
| Peak load tolerance | 2x normal load without degradation |

### 3.3 Load Profile Assumptions

| Time Period | Expected Active Users | Activity |
|-------------|----------------------|----------|
| Shift change (peak) | 50–80 users | Inventory checks |
| Mid-shift (normal) | 10–20 users | Ad-hoc lookups, issue reporting |
| Night (low) | 0–5 users | Occasional maintenance work |

## 4. Constraints

- Vaadin uses server-side rendering with WebSocket connections; each active session consumes server memory
- Database queries must be optimised; JOOQ provides good control but requires attention to N+1 queries
- Mobile devices on station Wi-Fi may have variable latency (50–200 ms typical)
- Barcode scanning involves camera access and image processing on the client, plus server round-trip

## 5. Verification

| Method | Description |
|--------|-------------|
| **Load testing** | Simulate 100 concurrent users performing inventory checks; measure response times |
| **Performance profiling** | Identify slow queries and rendering bottlenecks during development |
| **Real-user monitoring** | Instrument production system to capture actual response times |
| **Synthetic monitoring** | Automated scripts performing key operations every 5 minutes |

## 6. Optimisation Strategies

- **Pagination:** Lists of equipment should load in pages, not all at once
- **Lazy loading:** Compartment contents load on expansion, not on apparatus view load
- **Query optimisation:** Use JOOQ's capabilities to write efficient joins; avoid N+1 patterns
- **Caching:** Cache relatively static data (equipment types, compartment layouts) with appropriate invalidation
- **Connection pooling:** Configure HikariCP appropriately for expected concurrency

## 7. Acceptance Criteria

- [ ] Load test demonstrates 100 concurrent users with 95th percentile response times within targets
- [ ] No single page load exceeds 3 seconds under normal load
- [ ] Barcode scan to confirmation completes within 2 seconds on a mid-range smartphone
- [ ] Database query execution times logged; no query exceeds 500 ms under normal load

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |