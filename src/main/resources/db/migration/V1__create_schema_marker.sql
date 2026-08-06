CREATE TABLE app_schema_marker (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    description VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_schema_marker (id, description)
VALUES (1, 'Connectivity marker only; domain model pending approval');
