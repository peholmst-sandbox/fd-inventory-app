-- Add verified_by_id column to inventory_check_item for tracking who verified each item
-- This supports the collaborative inventory check feature where multiple users can check different compartments

ALTER TABLE inventory_check_item
ADD COLUMN verified_by_id UUID REFERENCES app_user(id);

-- Create index for efficient lookups by verifier
CREATE INDEX idx_inventory_check_item_verifier ON inventory_check_item(verified_by_id) WHERE verified_by_id IS NOT NULL;

-- Backfill existing records with the check's performed_by_id
UPDATE inventory_check_item ici
SET verified_by_id = ic.performed_by_id
FROM inventory_check ic
WHERE ici.inventory_check_id = ic.id
AND ici.verified_by_id IS NULL;
