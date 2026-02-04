package com.example.firestock.domain.audit;

/**
 * Result of a functional test performed on equipment during an audit.
 *
 * <p>Some equipment types require functional testing during formal audits
 * (e.g., battery checks, pressure tests, certification verification).
 */
public enum TestResult {
    /**
     * Equipment passed all required functional tests.
     */
    PASSED,

    /**
     * Equipment failed one or more functional tests.
     * Requires issue creation per BR-05.
     */
    FAILED,

    /**
     * No functional test was performed. This may be appropriate for
     * equipment types that don't require testing, or when testing
     * equipment is unavailable.
     */
    NOT_TESTED
}
