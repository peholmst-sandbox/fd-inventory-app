package com.example.firestock.domain.issue;

/**
 * Issue target representing an apparatus-level issue with no specific item.
 *
 * <p>Use this when reporting issues that affect the apparatus as a whole
 * rather than a specific piece of equipment or consumable.
 */
public record ApparatusIssueTarget() implements IssueTarget {
}
