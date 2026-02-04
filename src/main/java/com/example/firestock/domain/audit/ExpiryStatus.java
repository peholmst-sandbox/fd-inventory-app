package com.example.firestock.domain.audit;

/**
 * Expiry status of an item relative to its expiration or certification date.
 *
 * <p>Items with expiration dates (e.g., medical supplies, batteries, certifications)
 * are assessed during audits to determine if they need replacement.
 */
public enum ExpiryStatus {
    /**
     * Item is not expired and has sufficient time remaining before expiry.
     */
    OK,

    /**
     * Item will expire soon (typically within 30 days).
     * Should be flagged for upcoming replacement.
     */
    EXPIRING_SOON,

    /**
     * Item has already expired and must be replaced.
     * Requires issue creation per BR-05.
     */
    EXPIRED
}
