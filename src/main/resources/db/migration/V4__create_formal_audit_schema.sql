-- V4: Create formal audit schema for UC-03: Conduct Formal Audit
-- Formal audits are comprehensive equipment inspections performed by maintenance technicians

-- ============================================================================
-- ENUMS
-- ============================================================================

-- Audit status (similar to check_status but for formal audits)
CREATE TYPE audit_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'ABANDONED');

-- Audit item status for individual item verification
CREATE TYPE audit_item_status AS ENUM (
    'VERIFIED',           -- Item verified as present and functional
    'MISSING',            -- Item not found
    'DAMAGED',            -- Item found but damaged
    'FAILED_INSPECTION',  -- Item failed functional test
    'EXPIRED',            -- Item has expired
    'NOT_AUDITED'         -- Item not yet audited (placeholder)
);

-- Equipment condition grades for formal audit
CREATE TYPE item_condition AS ENUM ('GOOD', 'FAIR', 'POOR');

-- Functional test results
CREATE TYPE test_result AS ENUM ('PASSED', 'FAILED', 'NOT_TESTED');

-- Expiry status for items
CREATE TYPE expiry_status AS ENUM ('OK', 'EXPIRING_SOON', 'EXPIRED');

-- ============================================================================
-- TABLES
-- ============================================================================

-- formal_audit: Header record for a formal audit session
CREATE TABLE formal_audit (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apparatus_id        UUID NOT NULL REFERENCES apparatus(id),
    station_id          UUID NOT NULL REFERENCES station(id),
    performed_by_id     UUID NOT NULL REFERENCES app_user(id),
    status              audit_status NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    abandoned_at        TIMESTAMP,
    paused_at           TIMESTAMP,
    total_items         INTEGER NOT NULL DEFAULT 0,
    audited_count       INTEGER NOT NULL DEFAULT 0,
    issues_found_count  INTEGER NOT NULL DEFAULT 0,
    unexpected_items_count INTEGER NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- BR-02: Only one audit IN_PROGRESS per apparatus at a time
CREATE UNIQUE INDEX idx_formal_audit_active
    ON formal_audit(apparatus_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_formal_audit_apparatus ON formal_audit(apparatus_id);
CREATE INDEX idx_formal_audit_station ON formal_audit(station_id);
CREATE INDEX idx_formal_audit_performer ON formal_audit(performed_by_id);
CREATE INDEX idx_formal_audit_status ON formal_audit(status);
CREATE INDEX idx_formal_audit_started_at ON formal_audit(started_at);

-- formal_audit_item: Individual item audit records within a formal audit
CREATE TABLE formal_audit_item (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    formal_audit_id         UUID NOT NULL REFERENCES formal_audit(id) ON DELETE CASCADE,
    equipment_item_id       UUID REFERENCES equipment_item(id),
    consumable_stock_id     UUID REFERENCES consumable_stock(id),
    manifest_entry_id       UUID REFERENCES manifest_entry(id),
    compartment_id          UUID NOT NULL REFERENCES compartment(id),
    audit_item_status       audit_item_status NOT NULL,
    item_condition          item_condition,
    test_result             test_result,
    expiry_status           expiry_status,
    quantity_found          DECIMAL(10,2),
    quantity_expected       DECIMAL(10,2),
    condition_notes         TEXT,
    test_notes              TEXT,
    is_unexpected           BOOLEAN NOT NULL DEFAULT false,
    audited_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issue_id                UUID REFERENCES issue(id),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Either equipment item OR consumable stock must be set for manifest items
    -- For unexpected items, both can be null if it's a completely new/unknown item
    CONSTRAINT chk_audit_item_target CHECK (
        (is_unexpected = true) OR
        (equipment_item_id IS NOT NULL AND consumable_stock_id IS NULL) OR
        (equipment_item_id IS NULL AND consumable_stock_id IS NOT NULL)
    )
);

CREATE INDEX idx_formal_audit_item_audit ON formal_audit_item(formal_audit_id);
CREATE INDEX idx_formal_audit_item_equipment ON formal_audit_item(equipment_item_id) WHERE equipment_item_id IS NOT NULL;
CREATE INDEX idx_formal_audit_item_consumable ON formal_audit_item(consumable_stock_id) WHERE consumable_stock_id IS NOT NULL;
CREATE INDEX idx_formal_audit_item_compartment ON formal_audit_item(compartment_id);

-- Add trigger for updated_at on formal_audit
CREATE TRIGGER update_formal_audit_updated_at
    BEFORE UPDATE ON formal_audit
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
