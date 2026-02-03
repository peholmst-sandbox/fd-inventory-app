package com.example.firestock.infrastructure.persistence.mapper;

import com.example.firestock.domain.issue.AcknowledgedIssue;
import com.example.firestock.domain.issue.ApparatusIssueTarget;
import com.example.firestock.domain.issue.ClosedIssue;
import com.example.firestock.domain.issue.ConsumableIssueTarget;
import com.example.firestock.domain.issue.EquipmentIssueTarget;
import com.example.firestock.domain.issue.InProgressIssue;
import com.example.firestock.domain.issue.Issue;
import com.example.firestock.domain.issue.IssueTarget;
import com.example.firestock.domain.issue.OpenIssue;
import com.example.firestock.domain.issue.ResolvedIssue;
import com.example.firestock.jooq.tables.records.IssueRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between {@link Issue} domain objects and
 * {@link IssueRecord} jOOQ records.
 *
 * <p>Handles the sealed interface hierarchy by mapping based on the status field.
 */
@Component
public class IssueMapper {

    /**
     * Converts a jOOQ record to the appropriate domain type based on status.
     *
     * @param record the jOOQ record
     * @return the domain issue in its correct state
     */
    public Issue toDomain(IssueRecord record) {
        if (record == null) {
            return null;
        }

        var target = extractTarget(record);
        var status = record.getStatus();

        return switch (status) {
            case OPEN -> new OpenIssue(
                    record.getId(),
                    record.getReferenceNumber(),
                    target,
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getTitle(),
                    record.getDescription(),
                    record.getSeverity(),
                    record.getCategory(),
                    record.getReportedById(),
                    record.getReportedAt(),
                    Boolean.TRUE.equals(record.getIsCrewResponsibility())
            );
            case ACKNOWLEDGED -> new AcknowledgedIssue(
                    record.getId(),
                    record.getReferenceNumber(),
                    target,
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getTitle(),
                    record.getDescription(),
                    record.getSeverity(),
                    record.getCategory(),
                    record.getReportedById(),
                    record.getReportedAt(),
                    Boolean.TRUE.equals(record.getIsCrewResponsibility()),
                    record.getAcknowledgedById(),
                    record.getAcknowledgedAt()
            );
            case IN_PROGRESS -> new InProgressIssue(
                    record.getId(),
                    record.getReferenceNumber(),
                    target,
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getTitle(),
                    record.getDescription(),
                    record.getSeverity(),
                    record.getCategory(),
                    record.getReportedById(),
                    record.getReportedAt(),
                    Boolean.TRUE.equals(record.getIsCrewResponsibility()),
                    record.getAcknowledgedById(),
                    record.getAcknowledgedAt(),
                    // Use acknowledgedAt as startedAt since DB doesn't have a separate field
                    record.getAcknowledgedAt()
            );
            case RESOLVED -> new ResolvedIssue(
                    record.getId(),
                    record.getReferenceNumber(),
                    target,
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getTitle(),
                    record.getDescription(),
                    record.getSeverity(),
                    record.getCategory(),
                    record.getReportedById(),
                    record.getReportedAt(),
                    Boolean.TRUE.equals(record.getIsCrewResponsibility()),
                    record.getAcknowledgedById(),
                    record.getAcknowledgedAt(),
                    record.getResolvedById(),
                    record.getResolvedAt(),
                    record.getResolutionNotes()
            );
            case CLOSED -> new ClosedIssue(
                    record.getId(),
                    record.getReferenceNumber(),
                    target,
                    record.getApparatusId(),
                    record.getStationId(),
                    record.getTitle(),
                    record.getDescription(),
                    record.getSeverity(),
                    record.getCategory(),
                    record.getReportedById(),
                    record.getReportedAt(),
                    Boolean.TRUE.equals(record.getIsCrewResponsibility()),
                    // Use updated_at as closedAt since the DB doesn't have a separate closed_at field
                    record.getUpdatedAt()
            );
        };
    }

    /**
     * Updates a jOOQ record from a domain Issue.
     *
     * @param record the record to update
     * @param issue the domain issue
     */
    public void updateRecord(IssueRecord record, Issue issue) {
        record.setId(issue.id());
        record.setReferenceNumber(issue.referenceNumber());
        record.setApparatusId(issue.apparatusId());
        record.setStationId(issue.stationId());
        record.setTitle(issue.title());
        record.setDescription(issue.description());
        record.setSeverity(issue.severity());
        record.setCategory(issue.category());
        record.setStatus(issue.status());
        record.setReportedById(issue.reportedById());
        record.setReportedAt(issue.reportedAt());
        record.setIsCrewResponsibility(issue.isCrewResponsibility());

        // Set target fields
        updateTargetFields(record, issue.target());

        // Set state-specific fields
        switch (issue) {
            case OpenIssue open -> {
                record.setAcknowledgedById(null);
                record.setAcknowledgedAt(null);
                record.setResolvedById(null);
                record.setResolvedAt(null);
                record.setResolutionNotes(null);
            }
            case AcknowledgedIssue acknowledged -> {
                record.setAcknowledgedById(acknowledged.acknowledgedById());
                record.setAcknowledgedAt(acknowledged.acknowledgedAt());
                record.setResolvedById(null);
                record.setResolvedAt(null);
                record.setResolutionNotes(null);
            }
            case InProgressIssue inProgress -> {
                record.setAcknowledgedById(inProgress.acknowledgedById());
                record.setAcknowledgedAt(inProgress.acknowledgedAt());
                record.setResolvedById(null);
                record.setResolvedAt(null);
                record.setResolutionNotes(null);
            }
            case ResolvedIssue resolved -> {
                record.setAcknowledgedById(resolved.acknowledgedById());
                record.setAcknowledgedAt(resolved.acknowledgedAt());
                record.setResolvedById(resolved.resolvedById());
                record.setResolvedAt(resolved.resolvedAt());
                record.setResolutionNotes(resolved.resolutionNotes());
            }
            case ClosedIssue closed -> {
                // Keep existing acknowledgment info if present
                record.setResolvedById(null);
                record.setResolvedAt(null);
                record.setResolutionNotes(null);
            }
        }
    }

    /**
     * Extracts the IssueTarget from a jOOQ record.
     *
     * @param record the jOOQ record
     * @return the issue target
     */
    private IssueTarget extractTarget(IssueRecord record) {
        if (record.getEquipmentItemId() != null) {
            return new EquipmentIssueTarget(record.getEquipmentItemId());
        } else if (record.getConsumableStockId() != null) {
            return new ConsumableIssueTarget(record.getConsumableStockId());
        } else {
            return new ApparatusIssueTarget();
        }
    }

    /**
     * Updates the target fields on a jOOQ record.
     *
     * @param record the record to update
     * @param target the issue target
     */
    private void updateTargetFields(IssueRecord record, IssueTarget target) {
        switch (target) {
            case EquipmentIssueTarget equipment -> {
                record.setEquipmentItemId(equipment.equipmentItemId());
                record.setConsumableStockId(null);
            }
            case ConsumableIssueTarget consumable -> {
                record.setEquipmentItemId(null);
                record.setConsumableStockId(consumable.consumableStockId());
            }
            case ApparatusIssueTarget apparatus -> {
                record.setEquipmentItemId(null);
                record.setConsumableStockId(null);
            }
        }
    }
}
