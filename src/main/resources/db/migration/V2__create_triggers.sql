-- V2: Create triggers for automatic updated_at timestamp management
-- Note: This migration is excluded from jOOQ code generation (PostgreSQL-specific syntax)

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to all tables with updated_at column
CREATE TRIGGER update_station_updated_at BEFORE UPDATE ON station FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_apparatus_updated_at BEFORE UPDATE ON apparatus FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_compartment_updated_at BEFORE UPDATE ON compartment FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_equipment_type_updated_at BEFORE UPDATE ON equipment_type FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_equipment_item_updated_at BEFORE UPDATE ON equipment_item FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_consumable_stock_updated_at BEFORE UPDATE ON consumable_stock FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_manifest_entry_updated_at BEFORE UPDATE ON manifest_entry FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_inventory_check_updated_at BEFORE UPDATE ON inventory_check FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_issue_updated_at BEFORE UPDATE ON issue FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_transfer_record_updated_at BEFORE UPDATE ON transfer_record FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
