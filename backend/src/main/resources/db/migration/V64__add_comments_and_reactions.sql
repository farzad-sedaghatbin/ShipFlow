-- V64: Add comments and reactions system for tasks and bug reports
-- This migration creates the necessary tables for a complete commenting system

-- Comments table for tasks and bug reports
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL REFERENCES users(id),
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT comments_entity_type_check CHECK (entity_type IN ('TASK', 'BUG_REPORT'))
);

-- Comment reactions table (supports 8 common emojis)
CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    reaction_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT comment_reactions_type_check CHECK (reaction_type IN ('THUMBS_UP', 'THUMBS_DOWN', 'HEART', 'LAUGH', 'SURPRISED', 'SAD', 'ROCKET', 'EYES')),
    CONSTRAINT comment_reactions_unique UNIQUE (comment_id, user_id, reaction_type)
);

-- Indexes for performance
CREATE INDEX idx_comments_entity ON comments(entity_type, entity_id);
CREATE INDEX idx_comments_author ON comments(author_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);
CREATE INDEX idx_comments_not_deleted ON comments(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_comment_reactions_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reactions_user ON comment_reactions(user_id);

-- Add comment counts to tasks and bug_reports for quick aggregation
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS comment_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bug_reports ADD COLUMN IF NOT EXISTS comment_count INTEGER NOT NULL DEFAULT 0;

-- Function to update comment count on tasks
CREATE OR REPLACE FUNCTION update_task_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.entity_type = 'TASK' AND NEW.deleted_at IS NULL THEN
            UPDATE tasks SET comment_count = comment_count + 1 WHERE id = NEW.entity_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.entity_type = 'TASK' THEN
            IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
                UPDATE tasks SET comment_count = GREATEST(0, comment_count - 1) WHERE id = NEW.entity_id;
            ELSIF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
                UPDATE tasks SET comment_count = comment_count + 1 WHERE id = NEW.entity_id;
            END IF;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.entity_type = 'TASK' AND OLD.deleted_at IS NULL THEN
            UPDATE tasks SET comment_count = GREATEST(0, comment_count - 1) WHERE id = OLD.entity_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Function to update comment count on bug_reports
CREATE OR REPLACE FUNCTION update_bug_report_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.entity_type = 'BUG_REPORT' AND NEW.deleted_at IS NULL THEN
            UPDATE bug_reports SET comment_count = comment_count + 1 WHERE id = NEW.entity_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.entity_type = 'BUG_REPORT' THEN
            IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
                UPDATE bug_reports SET comment_count = GREATEST(0, comment_count - 1) WHERE id = NEW.entity_id;
            ELSIF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
                UPDATE bug_reports SET comment_count = comment_count + 1 WHERE id = NEW.entity_id;
            END IF;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.entity_type = 'BUG_REPORT' AND OLD.deleted_at IS NULL THEN
            UPDATE bug_reports SET comment_count = GREATEST(0, comment_count - 1) WHERE id = OLD.entity_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create triggers
CREATE TRIGGER trigger_update_task_comment_count
    AFTER INSERT OR UPDATE OR DELETE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_task_comment_count();

CREATE TRIGGER trigger_update_bug_report_comment_count
    AFTER INSERT OR UPDATE OR DELETE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_bug_report_comment_count();
