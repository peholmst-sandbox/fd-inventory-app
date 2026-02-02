-- V3: Add password column to app_user for authentication
-- Password is stored as bcrypt hash with cost factor >= 12

ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(72);

COMMENT ON COLUMN app_user.password_hash IS 'bcrypt password hash (cost factor >= 12)';
