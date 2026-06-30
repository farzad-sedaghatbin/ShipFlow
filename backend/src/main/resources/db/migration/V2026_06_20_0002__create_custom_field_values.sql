-- Custom Fields v1.8.0: field value table
-- Single TEXT value column; type-appropriate encoding handled in service layer.
-- Unique constraint prevents duplicate values per definition+entity combination.

CREATE TABLE custom_field_values (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    definition_id BIGINT NOT NULL REFERENCES custom_field_definitions(id),
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     BIGINT      NOT NULL,
    value         TEXT,
    updated_by_id BIGINT REFERENCES users(id),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cfv_def_entity UNIQUE (definition_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_cfv_definition ON custom_field_values(definition_id);
CREATE INDEX IF NOT EXISTS idx_cfv_entity     ON custom_field_values(entity_type, entity_id);
