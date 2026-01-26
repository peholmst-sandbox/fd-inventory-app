-- Flyway migration V1: Create hello_world table
CREATE TABLE hello_world (
    id BIGSERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert a sample record
INSERT INTO hello_world (message) VALUES ('Hello, World!');
