# Use Case: View Reports and Dashboard

**ID:** UC-08
**Version:** 1.0
**Date:** 2026-01-30
**Status:** Draft

---

## 1. Brief Description

Maintenance technicians and administrators view dashboards and reports showing equipment status, stock levels, and operational metrics across stations. This provides the cross-station visibility needed for resource planning and compliance.

## 2. Actors

| Actor | Description |
|-------|-------------|
| **Maintenance Technician** (primary) | Regional maintenance staff |
| **System Administrator** | Technical administration |
| **System** | FireStock application |

## 3. Preconditions

1. User is authenticated with Maintenance Technician or System Administrator role
2. At least one station exists with apparatus and equipment data

## 4. Postconditions

### Success
1. User views requested dashboard or report
2. Data reflects current system state (real-time or near-real-time)
3. Export completed successfully (if requested)

### Failure
1. Error message displayed with guidance
2. Partial data shown if possible

## 5. Dashboard Views

### 5.1 Regional Overview Dashboard

| Component | Description |
|-----------|-------------|
| Station summary cards | Count of apparatus, equipment items, open issues per station |
| Alert banner | Critical items requiring attention (low stock, overdue checks, critical issues) |
| Recent activity feed | Last 10 inventory checks, issues, transfers across region |
| Quick filters | Filter by station, date range, status |

### 5.2 Stock Levels Dashboard

| Component | Description |
|-----------|-------------|
| Low stock alerts | Consumables below 50% of required quantity |
| Critical stock (zero) | Consumables at zero quantity |
| Expiring soon | Items expiring within 30 days |
| Stock by station | Heatmap or table showing stock health per station |

### 5.3 Equipment Status Dashboard

| Component | Description |
|-----------|-------------|
| Status breakdown | Pie chart: OK, Damaged, Missing, In Repair, etc. |
| Testing due | Equipment with nextTestDueDate approaching or past |
| Warranty expiring | Equipment with warranties expiring soon |
| Equipment by type | Count and status by equipment type |

### 5.4 Compliance Dashboard

| Component | Description |
|-----------|-------------|
| Inventory check frequency | Days since last check per apparatus |
| Overdue checks | Apparatus not checked in > 24 hours (configurable) |
| Audit status | Last audit date per apparatus |
| Issue resolution time | Average time from report to resolution |

## 6. Basic Flow - View Dashboard

| Step | Actor | System |
|------|-------|--------|
| 1 | User navigates to Reports section | Displays dashboard selection menu |
| 2 | User selects a dashboard | Loads dashboard with default filters (all stations, last 7 days) |
| 3 | User adjusts filters (optional) | Refreshes dashboard with filtered data |
| 4 | User clicks on a metric for details | Drills down to detailed view |
| 5 | User returns to dashboard | Returns to summary view |

## 7. Basic Flow - Generate Report

| Step | Actor | System |
|------|-------|--------|
| 1 | User navigates to Reports > Generate Report | Displays report type selection |
| 2 | User selects report type | Displays report configuration options |
| 3 | User configures parameters (date range, stations, etc.) | Validates parameters |
| 4 | User clicks "Generate" | Generates report; displays preview |
| 5 | User reviews report | Shows report with data |
| 6 | User clicks "Export" (optional) | Prompts for format (PDF, CSV, Excel) |
| 7 | User selects format and confirms | Downloads file |

## 8. Report Types

| Report | Description | Parameters |
|--------|-------------|------------|
| **Inventory Check Summary** | Summary of checks by apparatus, date, completeness | Date range, station(s) |
| **Equipment Inventory** | Full list of equipment with status | Station(s), status filter, type filter |
| **Issue Report** | All issues with status and resolution details | Date range, station(s), status, severity |
| **Stock Levels** | Current consumable quantities vs. required | Station(s), below-par only toggle |
| **Equipment History** | Full history for specific equipment item | Equipment ID or serial number |
| **Audit Trail Export** | Audit events for compliance | Date range, event types, entity type |
| **Testing Schedule** | Equipment due for testing | Date range (upcoming), station(s) |
| **Transfer History** | Equipment transfers | Date range, station(s) |

## 9. Alternative Flows

### 9a. Drill Down to Detail
| Step | Actor | System |
|------|-------|--------|
| 9a.1 | User clicks on a dashboard metric | — |
| 9a.2 | — | Displays detailed view (e.g., list of items comprising that metric) |
| 9a.3 | User clicks on specific item | Navigates to that item's detail page |

### 9b. Save Dashboard Configuration
| Step | Actor | System |
|------|-------|--------|
| 9b.1 | User configures filters and layout | — |
| 9b.2 | User clicks "Save as Default" | Saves configuration to user preferences |
| 9b.3 | — | Next visit loads saved configuration |

### 9c. Schedule Report (Future Enhancement)
| Step | Actor | System |
|------|-------|--------|
| 9c.1 | User configures report | — |
| 9c.2 | User clicks "Schedule" | Prompts for frequency and email address |
| 9c.3 | User configures schedule | Saves scheduled report |
| 9c.4 | — | System sends report on schedule |

## 10. Exception Flows

### 10a. No Data Available
| Step | Actor | System |
|------|-------|--------|
| 10a.1 | — | Query returns no results |
| 10a.2 | — | Displays "No data available for selected criteria" with suggestions |

### 10b. Report Generation Timeout
| Step | Actor | System |
|------|-------|--------|
| 10b.1 | — | Report query exceeds timeout (60 seconds) |
| 10b.2 | — | Displays "Report is taking longer than expected. Try narrowing your date range or filters." |
| 10b.3 | User adjusts parameters | Retries with narrower scope |

### 10c. Export Failure
| Step | Actor | System |
|------|-------|--------|
| 10c.1 | — | Export file generation fails |
| 10c.2 | — | Displays error with retry option |

## 11. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Firefighters cannot access cross-station reports |
| BR-02 | Dashboard data refreshes automatically every 5 minutes (configurable) |
| BR-03 | Reports are generated from current data; historical snapshots not supported in v1 |
| BR-04 | Export files are generated server-side and streamed to client |
| BR-05 | Large exports (>10,000 rows) may be paginated or require background generation |
| BR-06 | Audit trail export requires System Administrator role |

## 12. User Interface Requirements

- Responsive design works on tablet and desktop
- Dashboard cards are clickable for drill-down
- Charts use accessible color schemes
- Loading indicators for data fetches
- Export buttons clearly visible
- Filter selections persist during session

## 13. Data Requirements

### Metrics to Calculate

| Metric | Calculation |
|--------|-------------|
| Days since last check | CURRENT_DATE - last check completedAt |
| Stock health % | (quantity / requiredQuantity) * 100 |
| Issue resolution time | resolvedAt - reportedAt (average) |
| Check completion rate | completed checks / total checks |

### Aggregations

- By station
- By apparatus
- By equipment type
- By date (day, week, month)
- By user (for activity reports)

## 14. Non-Functional Requirements

- Dashboard initial load: < 3 seconds
- Dashboard refresh: < 2 seconds
- Report generation: < 30 seconds for typical reports
- Export generation: < 60 seconds for up to 10,000 rows
- Concurrent users: Support 50 simultaneous dashboard viewers

## 15. Frequency

- Dashboard views: Multiple times daily by maintenance technicians
- Report generation: Weekly/monthly for compliance
- Exports: As needed for external reporting

## 16. Assumptions

- Users have modern browsers with JavaScript enabled
- Network bandwidth sufficient for dashboard updates
- Data volume manageable with direct queries (no data warehouse needed for v1)

## 17. Open Issues

| ID | Issue | Status |
|----|-------|--------|
| OI-01 | Should firefighters have access to their own station's dashboard? | Open |
| OI-02 | Scheduled/emailed reports - include in v1? | Deferred to future |
| OI-03 | Custom report builder - include in v1? | Deferred to future |
| OI-04 | Data export format preferences (Excel vs. CSV vs. PDF)? | Open |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-30 | — | Initial draft |
