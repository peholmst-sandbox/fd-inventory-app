package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.ApparatusId;
import com.example.firestock.domain.primitives.ids.ConsumableStockId;
import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.domain.primitives.ids.StationId;
import com.example.firestock.domain.primitives.ids.UserId;
import com.example.firestock.domain.primitives.strings.ReferenceNumber;
import com.example.firestock.jooq.enums.IssueCategory;
import com.example.firestock.jooq.enums.IssueSeverity;
import com.example.firestock.jooq.enums.IssueStatus;
import com.example.firestock.jooq.tables.records.IssueRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.time.Year;

import static com.example.firestock.jooq.Tables.ISSUE;

/**
 * DAO class for issue write operations.
 */
@Component
public class IssueDao {

    private final DSLContext create;

    public IssueDao(DSLContext create) {
        this.create = create;
    }

    /**
     * Creates a new issue.
     *
     * @param equipmentItemId the equipment item, or null for consumables
     * @param consumableStockId the consumable stock, or null for equipment
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedBy the user reporting the issue
     * @param isCrewResponsibility whether the issue is a crew responsibility
     * @return the ID of the created issue
     */
    public IssueId insert(
            EquipmentItemId equipmentItemId,
            ConsumableStockId consumableStockId,
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedBy,
            boolean isCrewResponsibility) {

        IssueRecord record = create.newRecord(ISSUE);
        record.setReferenceNumber(generateReferenceNumber());
        record.setEquipmentItemId(equipmentItemId);
        record.setConsumableStockId(consumableStockId);
        record.setApparatusId(apparatusId);
        record.setStationId(stationId);
        record.setTitle(title);
        record.setDescription(description);
        record.setSeverity(severity);
        record.setCategory(category);
        record.setStatus(IssueStatus.OPEN);
        record.setReportedById(reportedBy);
        record.setIsCrewResponsibility(isCrewResponsibility);
        record.store();
        return record.getId();
    }

    /**
     * Creates a new issue and returns the created issue details.
     *
     * @param equipmentItemId the equipment item, or null for consumables
     * @param consumableStockId the consumable stock, or null for equipment
     * @param apparatusId the apparatus where the issue was found
     * @param stationId the station
     * @param title the issue title
     * @param description the issue description
     * @param severity the severity level
     * @param category the issue category
     * @param reportedBy the user reporting the issue
     * @param isCrewResponsibility whether the issue is a crew responsibility
     * @return the created issue result containing ID and reference number
     */
    public IssueCreatedResult insertAndReturn(
            EquipmentItemId equipmentItemId,
            ConsumableStockId consumableStockId,
            ApparatusId apparatusId,
            StationId stationId,
            String title,
            String description,
            IssueSeverity severity,
            IssueCategory category,
            UserId reportedBy,
            boolean isCrewResponsibility) {

        IssueRecord record = create.newRecord(ISSUE);
        ReferenceNumber referenceNumber = generateReferenceNumber();
        record.setReferenceNumber(referenceNumber);
        record.setEquipmentItemId(equipmentItemId);
        record.setConsumableStockId(consumableStockId);
        record.setApparatusId(apparatusId);
        record.setStationId(stationId);
        record.setTitle(title);
        record.setDescription(description);
        record.setSeverity(severity);
        record.setCategory(category);
        record.setStatus(IssueStatus.OPEN);
        record.setReportedById(reportedBy);
        record.setIsCrewResponsibility(isCrewResponsibility);
        record.store();
        return new IssueCreatedResult(record.getId(), referenceNumber);
    }

    /**
     * Generates a unique reference number for an issue.
     * Format: ISS-YYYY-NNNNN (e.g., ISS-2026-00001)
     *
     * @return the generated reference number
     */
    ReferenceNumber generateReferenceNumber() {
        int currentYear = Year.now().getValue();
        String prefix = "ISS-" + currentYear + "-";

        // Find the highest sequence number for this year
        // Cast to string for LIKE comparison since the field uses a custom converter
        var maxRef = create.select(DSL.max(ISSUE.REFERENCE_NUMBER))
            .from(ISSUE)
            .where(ISSUE.REFERENCE_NUMBER.cast(String.class).like(prefix + "%"))
            .fetchOne(0, ReferenceNumber.class);

        int nextSeq = 1;
        if (maxRef != null) {
            String maxRefStr = maxRef.value();
            String seqPart = maxRefStr.substring(prefix.length());
            nextSeq = Integer.parseInt(seqPart) + 1;
        }

        return new ReferenceNumber(prefix + String.format("%05d", nextSeq));
    }
}
