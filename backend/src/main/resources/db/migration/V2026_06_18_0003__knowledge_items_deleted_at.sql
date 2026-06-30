-- Knowledge Center: add soft-delete column to knowledge_items so KnowledgeSource
-- deletes can cascade as logical deletes rather than hard-removing rows that may
-- still be referenced elsewhere.

ALTER TABLE knowledge_items ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
