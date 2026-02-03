package com.example.firestock.issues;

import com.example.firestock.domain.primitives.ids.EquipmentItemId;
import com.example.firestock.domain.primitives.ids.IssueId;
import com.example.firestock.jooq.enums.IssueStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.firestock.jooq.Tables.ISSUE;

/**
 * Query class for issue read operations.
 */
@Component
public class IssueQuery {

    private static final Set<IssueStatus> OPEN_STATUSES = Set.of(
            IssueStatus.OPEN,
            IssueStatus.ACKNOWLEDGED,
            IssueStatus.IN_PROGRESS
    );

    private final DSLContext create;

    public IssueQuery(DSLContext create) {
        this.create = create;
    }

    /**
     * Finds all open issues for a specific equipment item.
     *
     * @param equipmentItemId the equipment item ID
     * @return list of open issue summaries
     */
    public List<IssueSummary> findOpenIssuesByEquipmentItemId(EquipmentItemId equipmentItemId) {
        return create.select(
                ISSUE.ID,
                ISSUE.REFERENCE_NUMBER,
                ISSUE.TITLE,
                ISSUE.CATEGORY,
                ISSUE.SEVERITY,
                ISSUE.STATUS,
                ISSUE.REPORTED_AT
            )
            .from(ISSUE)
            .where(ISSUE.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
            .and(ISSUE.STATUS.in(OPEN_STATUSES))
            .orderBy(ISSUE.REPORTED_AT.desc())
            .fetch(r -> new IssueSummary(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7()
            ));
    }

    /**
     * Finds an issue by ID.
     *
     * @param issueId the issue ID
     * @return the issue summary, or empty if not found
     */
    public Optional<IssueSummary> findById(IssueId issueId) {
        return create.select(
                ISSUE.ID,
                ISSUE.REFERENCE_NUMBER,
                ISSUE.TITLE,
                ISSUE.CATEGORY,
                ISSUE.SEVERITY,
                ISSUE.STATUS,
                ISSUE.REPORTED_AT
            )
            .from(ISSUE)
            .where(ISSUE.ID.eq(issueId))
            .fetchOptional(r -> new IssueSummary(
                r.value1(),
                r.value2(),
                r.value3(),
                r.value4(),
                r.value5(),
                r.value6(),
                r.value7()
            ));
    }

    /**
     * Checks if an equipment item has any open issues.
     *
     * @param equipmentItemId the equipment item ID
     * @return true if open issues exist
     */
    public boolean hasOpenIssues(EquipmentItemId equipmentItemId) {
        return create.fetchExists(
            create.selectOne()
                .from(ISSUE)
                .where(ISSUE.EQUIPMENT_ITEM_ID.eq(equipmentItemId))
                .and(ISSUE.STATUS.in(OPEN_STATUSES))
        );
    }
}
