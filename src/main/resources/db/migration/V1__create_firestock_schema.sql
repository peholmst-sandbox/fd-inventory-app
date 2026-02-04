-- V1: Create FireStock schema
-- Complete database schema for the FireStock fire apparatus inventory system

-- ============================================================================
-- ENUMS
-- ============================================================================

-- User roles
CREATE TYPE user_role AS ENUM ('FIREFIGHTER', 'MAINTENANCE_TECHNICIAN', 'SYSTEM_ADMINISTRATOR');

-- Apparatus
CREATE TYPE apparatus_type AS ENUM ('ENGINE', 'LADDER', 'RESCUE', 'TANKER', 'AMBULANCE', 'COMMAND', 'UTILITY', 'OTHER');
CREATE TYPE apparatus_status AS ENUM ('IN_SERVICE', 'OUT_OF_SERVICE', 'RESERVE', 'DECOMMISSIONED');

-- Compartment
CREATE TYPE compartment_location AS ENUM ('FRONT', 'DRIVER_SIDE', 'PASSENGER_SIDE', 'REAR', 'TOP', 'INTERIOR', 'CROSSLAY');

-- Equipment
CREATE TYPE equipment_category AS ENUM ('PPE', 'BREATHING', 'TOOLS_HAND', 'TOOLS_POWER', 'ELECTRONICS', 'MEDICAL', 'RESCUE', 'HOSE', 'NOZZLES', 'LIGHTING', 'CONSUMABLE', 'OTHER');
CREATE TYPE tracking_method AS ENUM ('SERIALIZED', 'QUANTITY');
CREATE TYPE equipment_status AS ENUM ('OK', 'DAMAGED', 'IN_REPAIR', 'MISSING', 'FAILED_INSPECTION', 'RETIRED', 'EXPIRED');
CREATE TYPE ownership_type AS ENUM ('DEPARTMENT', 'CREW_OWNED');

-- Inventory check
CREATE TYPE check_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'ABANDONED');
CREATE TYPE verification_status AS ENUM ('PRESENT', 'PRESENT_DAMAGED', 'MISSING', 'EXPIRED', 'LOW_QUANTITY', 'SKIPPED');

-- Issues
CREATE TYPE issue_severity AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');
CREATE TYPE issue_category AS ENUM ('DAMAGE', 'MALFUNCTION', 'MISSING', 'EXPIRED', 'LOW_STOCK', 'CONTAMINATION', 'CALIBRATION', 'OTHER');
CREATE TYPE issue_status AS ENUM ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');

-- Transfers
CREATE TYPE transfer_type AS ENUM ('APPARATUS_TO_APPARATUS', 'APPARATUS_TO_STORAGE', 'STORAGE_TO_APPARATUS', 'STATION_TO_STATION', 'TO_REPAIR', 'FROM_REPAIR', 'RETIREMENT');
CREATE TYPE location_type AS ENUM ('APPARATUS', 'STORAGE', 'REPAIR', 'EXTERNAL');
CREATE TYPE transfer_status AS ENUM ('PENDING', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED');

-- Restock
CREATE TYPE restock_operation_type AS ENUM ('RESTOCK', 'USAGE', 'ADJUSTMENT', 'EXPIRY_REMOVAL', 'DAMAGE_REMOVAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'INITIAL');

-- ============================================================================
-- TABLES
-- ============================================================================

-- 1. station
CREATE TABLE station (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    address_street  VARCHAR(200),
    address_city    VARCHAR(100),
    address_state   VARCHAR(50),
    address_zip     VARCHAR(20),
    region          VARCHAR(100),
    contact_phone   VARCHAR(30),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. app_user (avoiding reserved keyword 'user')
CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    badge_number    VARCHAR(50) UNIQUE,
    phone           VARCHAR(30),
    role            user_role NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. user_station_assignment
CREATE TABLE user_station_assignment (
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    station_id      UUID NOT NULL REFERENCES station(id) ON DELETE CASCADE,
    is_primary      BOOLEAN NOT NULL DEFAULT false,
    assigned_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, station_id)
);

CREATE INDEX idx_user_station_assignment_station ON user_station_assignment(station_id);

-- 4. apparatus
CREATE TABLE apparatus (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_number     VARCHAR(50) NOT NULL UNIQUE,
    vin             VARCHAR(50),
    type            apparatus_type NOT NULL,
    make            VARCHAR(100),
    model           VARCHAR(100),
    year            INTEGER,
    station_id      UUID NOT NULL REFERENCES station(id),
    status          apparatus_status NOT NULL DEFAULT 'IN_SERVICE',
    barcode         VARCHAR(100),
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_apparatus_station ON apparatus(station_id);
CREATE INDEX idx_apparatus_status ON apparatus(status);

-- 5. compartment
CREATE TABLE compartment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apparatus_id    UUID NOT NULL REFERENCES apparatus(id) ON DELETE CASCADE,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    location        compartment_location NOT NULL,
    description     TEXT,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (apparatus_id, code)
);

CREATE INDEX idx_compartment_apparatus ON compartment(apparatus_id);

-- 6. equipment_type
CREATE TABLE equipment_type (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    category            equipment_category NOT NULL,
    tracking_method     tracking_method NOT NULL,
    description         TEXT,
    manufacturer        VARCHAR(100),
    model               VARCHAR(100),
    requires_expiry     BOOLEAN NOT NULL DEFAULT false,
    requires_testing    BOOLEAN NOT NULL DEFAULT false,
    test_interval_days  INTEGER,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_test_interval CHECK (
        (requires_testing = false) OR (test_interval_days IS NOT NULL AND test_interval_days > 0)
    )
);

CREATE INDEX idx_equipment_type_category ON equipment_type(category);
CREATE INDEX idx_equipment_type_tracking ON equipment_type(tracking_method);

-- 7. equipment_item
CREATE TABLE equipment_item (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipment_type_id       UUID NOT NULL REFERENCES equipment_type(id),
    serial_number           VARCHAR(100) UNIQUE,
    barcode                 VARCHAR(100) UNIQUE,
    ownership_type          ownership_type NOT NULL DEFAULT 'DEPARTMENT',
    home_station_id         UUID REFERENCES station(id),
    manufacturer            VARCHAR(100),
    model                   VARCHAR(100),
    acquisition_date        DATE,
    warranty_expiry_date    DATE,
    last_test_date          DATE,
    next_test_due_date      DATE,
    status                  equipment_status NOT NULL DEFAULT 'OK',
    station_id              UUID REFERENCES station(id),
    apparatus_id            UUID REFERENCES apparatus(id),
    compartment_id          UUID REFERENCES compartment(id),
    notes                   TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BR-08: Crew-owned must have home_station_id
    CONSTRAINT chk_crew_owned_home_station CHECK (
        (ownership_type = 'DEPARTMENT' AND home_station_id IS NULL) OR
        (ownership_type = 'CREW_OWNED' AND home_station_id IS NOT NULL)
    ),
    -- Assignment states: on apparatus (apparatus+compartment set), in storage (station set), in transit (all null)
    CONSTRAINT chk_location_consistency CHECK (
        (apparatus_id IS NOT NULL AND compartment_id IS NOT NULL AND station_id IS NULL) OR
        (apparatus_id IS NULL AND compartment_id IS NULL AND station_id IS NOT NULL) OR
        (apparatus_id IS NULL AND compartment_id IS NULL AND station_id IS NULL)
    )
);

CREATE INDEX idx_equipment_item_type ON equipment_item(equipment_type_id);
CREATE INDEX idx_equipment_item_apparatus ON equipment_item(apparatus_id);
CREATE INDEX idx_equipment_item_station ON equipment_item(station_id);
CREATE INDEX idx_equipment_item_status ON equipment_item(status);
CREATE INDEX idx_equipment_item_home_station ON equipment_item(home_station_id) WHERE home_station_id IS NOT NULL;

-- 8. consumable_stock
CREATE TABLE consumable_stock (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipment_type_id   UUID NOT NULL REFERENCES equipment_type(id),
    station_id          UUID REFERENCES station(id),
    apparatus_id        UUID REFERENCES apparatus(id),
    compartment_id      UUID REFERENCES compartment(id),
    quantity            DECIMAL(10,2) NOT NULL DEFAULT 0,
    unit                VARCHAR(50) NOT NULL DEFAULT 'units',
    required_quantity   DECIMAL(10,2),
    lot_number          VARCHAR(100),
    expiry_date         DATE,
    last_restock_date   DATE,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BR-03: Only one stock record per equipment type per location
    CONSTRAINT chk_consumable_location CHECK (
        (apparatus_id IS NOT NULL AND compartment_id IS NOT NULL AND station_id IS NULL) OR
        (apparatus_id IS NULL AND compartment_id IS NULL AND station_id IS NOT NULL)
    ),
    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0)
);

CREATE UNIQUE INDEX idx_consumable_stock_apparatus_location
    ON consumable_stock(equipment_type_id, apparatus_id, compartment_id)
    WHERE apparatus_id IS NOT NULL;
CREATE UNIQUE INDEX idx_consumable_stock_station_location
    ON consumable_stock(equipment_type_id, station_id)
    WHERE station_id IS NOT NULL;
CREATE INDEX idx_consumable_stock_type ON consumable_stock(equipment_type_id);
CREATE INDEX idx_consumable_stock_apparatus ON consumable_stock(apparatus_id);

-- 9. manifest_entry
CREATE TABLE manifest_entry (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apparatus_id        UUID NOT NULL REFERENCES apparatus(id) ON DELETE CASCADE,
    compartment_id      UUID NOT NULL REFERENCES compartment(id) ON DELETE CASCADE,
    equipment_type_id   UUID NOT NULL REFERENCES equipment_type(id),
    required_quantity   INTEGER NOT NULL DEFAULT 1,
    is_critical         BOOLEAN NOT NULL DEFAULT false,
    display_order       INTEGER NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (apparatus_id, compartment_id, equipment_type_id),
    CONSTRAINT chk_required_quantity_positive CHECK (required_quantity > 0)
);

CREATE INDEX idx_manifest_entry_apparatus ON manifest_entry(apparatus_id);
CREATE INDEX idx_manifest_entry_compartment ON manifest_entry(compartment_id);

-- 10. inventory_check
CREATE TABLE inventory_check (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    apparatus_id        UUID NOT NULL REFERENCES apparatus(id),
    station_id          UUID NOT NULL REFERENCES station(id),
    performed_by_id     UUID NOT NULL REFERENCES app_user(id),
    status              check_status NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    abandoned_at        TIMESTAMP,
    total_items         INTEGER NOT NULL DEFAULT 0,
    verified_count      INTEGER NOT NULL DEFAULT 0,
    issues_found_count  INTEGER NOT NULL DEFAULT 0,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- BR-01: Only one check IN_PROGRESS per apparatus
CREATE UNIQUE INDEX idx_inventory_check_active
    ON inventory_check(apparatus_id)
    WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_inventory_check_apparatus ON inventory_check(apparatus_id);
CREATE INDEX idx_inventory_check_station ON inventory_check(station_id);
CREATE INDEX idx_inventory_check_performer ON inventory_check(performed_by_id);
CREATE INDEX idx_inventory_check_status ON inventory_check(status);

-- 11. issue
CREATE TABLE issue (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number        VARCHAR(50) NOT NULL UNIQUE,
    equipment_item_id       UUID REFERENCES equipment_item(id),
    consumable_stock_id     UUID REFERENCES consumable_stock(id),
    apparatus_id            UUID NOT NULL REFERENCES apparatus(id),
    station_id              UUID NOT NULL REFERENCES station(id),
    title                   VARCHAR(200) NOT NULL,
    description             TEXT,
    severity                issue_severity NOT NULL,
    category                issue_category NOT NULL,
    status                  issue_status NOT NULL DEFAULT 'OPEN',
    reported_by_id          UUID NOT NULL REFERENCES app_user(id),
    reported_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by_id      UUID REFERENCES app_user(id),
    acknowledged_at         TIMESTAMP,
    resolved_by_id          UUID REFERENCES app_user(id),
    resolved_at             TIMESTAMP,
    resolution_notes        TEXT,
    is_crew_responsibility  BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Either equipment item OR consumable stock, not both (or neither for general issues)
    CONSTRAINT chk_issue_target CHECK (
        NOT (equipment_item_id IS NOT NULL AND consumable_stock_id IS NOT NULL)
    )
);

CREATE INDEX idx_issue_equipment ON issue(equipment_item_id) WHERE equipment_item_id IS NOT NULL;
CREATE INDEX idx_issue_consumable ON issue(consumable_stock_id) WHERE consumable_stock_id IS NOT NULL;
CREATE INDEX idx_issue_apparatus ON issue(apparatus_id);
CREATE INDEX idx_issue_station ON issue(station_id);
CREATE INDEX idx_issue_status ON issue(status);
CREATE INDEX idx_issue_severity ON issue(severity);

-- 12. issue_photo
CREATE TABLE issue_photo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id        UUID NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
    filename        VARCHAR(255) NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size_bytes INTEGER NOT NULL,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_photo_issue ON issue_photo(issue_id);

-- 13. inventory_check_item
CREATE TABLE inventory_check_item (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_check_id      UUID NOT NULL REFERENCES inventory_check(id) ON DELETE CASCADE,
    equipment_item_id       UUID REFERENCES equipment_item(id),
    consumable_stock_id     UUID REFERENCES consumable_stock(id),
    manifest_entry_id       UUID REFERENCES manifest_entry(id),
    compartment_id          UUID NOT NULL REFERENCES compartment(id),
    verification_status     verification_status NOT NULL,
    quantity_found          DECIMAL(10,2),
    quantity_expected       DECIMAL(10,2),
    condition_notes         TEXT,
    verified_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issue_id                UUID REFERENCES issue(id),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BR-01: Either equipment item OR consumable stock, not both
    CONSTRAINT chk_check_item_target CHECK (
        (equipment_item_id IS NOT NULL AND consumable_stock_id IS NULL) OR
        (equipment_item_id IS NULL AND consumable_stock_id IS NOT NULL)
    )
);

CREATE INDEX idx_inventory_check_item_check ON inventory_check_item(inventory_check_id);
CREATE INDEX idx_inventory_check_item_equipment ON inventory_check_item(equipment_item_id) WHERE equipment_item_id IS NOT NULL;
CREATE INDEX idx_inventory_check_item_consumable ON inventory_check_item(consumable_stock_id) WHERE consumable_stock_id IS NOT NULL;

-- 14. transfer_record
CREATE TABLE transfer_record (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number            VARCHAR(50) NOT NULL UNIQUE,
    equipment_item_id           UUID NOT NULL REFERENCES equipment_item(id),
    transfer_type               transfer_type NOT NULL,
    source_type                 location_type NOT NULL,
    source_apparatus_id         UUID REFERENCES apparatus(id),
    source_compartment_id       UUID REFERENCES compartment(id),
    source_station_id           UUID REFERENCES station(id),
    destination_type            location_type NOT NULL,
    destination_apparatus_id    UUID REFERENCES apparatus(id),
    destination_compartment_id  UUID REFERENCES compartment(id),
    destination_station_id      UUID REFERENCES station(id),
    reason                      TEXT,
    status                      transfer_status NOT NULL DEFAULT 'PENDING',
    initiated_by_id             UUID NOT NULL REFERENCES app_user(id),
    initiated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at                TIMESTAMP,
    notes                       TEXT,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- BR-03: Only one PENDING/IN_TRANSIT transfer per equipment item
CREATE UNIQUE INDEX idx_transfer_active
    ON transfer_record(equipment_item_id)
    WHERE status IN ('PENDING', 'IN_TRANSIT');
CREATE INDEX idx_transfer_equipment ON transfer_record(equipment_item_id);
CREATE INDEX idx_transfer_status ON transfer_record(status);
CREATE INDEX idx_transfer_source_apparatus ON transfer_record(source_apparatus_id) WHERE source_apparatus_id IS NOT NULL;
CREATE INDEX idx_transfer_dest_apparatus ON transfer_record(destination_apparatus_id) WHERE destination_apparatus_id IS NOT NULL;

-- 15. restock_record
CREATE TABLE restock_record (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number    VARCHAR(50) NOT NULL UNIQUE,
    consumable_stock_id UUID NOT NULL REFERENCES consumable_stock(id),
    operation_type      restock_operation_type NOT NULL,
    quantity_before     DECIMAL(10,2) NOT NULL,
    quantity_change     DECIMAL(10,2) NOT NULL,
    quantity_after      DECIMAL(10,2) NOT NULL,
    reason              TEXT,
    lot_number          VARCHAR(100),
    expiry_date         DATE,
    performed_by_id     UUID NOT NULL REFERENCES app_user(id),
    performed_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BR-02: quantity_after = quantity_before + quantity_change
    CONSTRAINT chk_quantity_calculation CHECK (quantity_after = quantity_before + quantity_change),
    -- BR-03: quantity_after >= 0
    CONSTRAINT chk_quantity_after_non_negative CHECK (quantity_after >= 0)
);

CREATE INDEX idx_restock_consumable ON restock_record(consumable_stock_id);
CREATE INDEX idx_restock_operation ON restock_record(operation_type);
CREATE INDEX idx_restock_performed_at ON restock_record(performed_at);

