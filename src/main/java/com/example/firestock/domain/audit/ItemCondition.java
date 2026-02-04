package com.example.firestock.domain.audit;

/**
 * Physical condition assessment of an audited equipment item.
 *
 * <p>Maintenance technicians assess the condition of each equipment item
 * during formal audits. This helps prioritize maintenance and replacement.
 */
public enum ItemCondition {
    /**
     * Item is in good condition with no visible wear or defects.
     * Suitable for continued service.
     */
    GOOD,

    /**
     * Item shows normal wear but is still serviceable.
     * May need attention during next maintenance cycle.
     */
    FAIR,

    /**
     * Item has significant wear or minor damage.
     * Should be prioritized for repair or replacement.
     */
    POOR
}
