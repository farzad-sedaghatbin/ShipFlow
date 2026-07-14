-- Polymorphic Pitch/Task -> WikiPage reference links (research/docs), separate from the
-- existing file-attachment system. H2-compatible DDL (no jsonb, no uuid, no SERIAL, no
-- CREATE INDEX CONCURRENTLY).

CREATE TABLE entity_wiki_links (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT NOT NULL,
    wiki_page_id BIGINT NOT NULL,
    linked_at TIMESTAMP NOT NULL,
    linked_by_user_id BIGINT,
    CONSTRAINT fk_entity_wiki_links_wiki_page FOREIGN KEY (wiki_page_id) REFERENCES wiki_pages(id),
    CONSTRAINT fk_entity_wiki_links_linked_by FOREIGN KEY (linked_by_user_id) REFERENCES users(id),
    CONSTRAINT uq_entity_wiki_links UNIQUE (entity_type, entity_id, wiki_page_id)
);

CREATE INDEX IF NOT EXISTS idx_entity_wiki_links_entity ON entity_wiki_links(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_wiki_links_wiki_page ON entity_wiki_links(wiki_page_id);
