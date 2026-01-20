-- V55: Add project type to projects table
-- This migration adds support for different project methodologies (Shape Up vs Kanban)

-- Add project_type column with default value SHAPE_UP for backward compatibility
ALTER TABLE projects ADD COLUMN project_type VARCHAR(10) NOT NULL DEFAULT 'SHAPE_UP';

-- Add check constraint for valid values
ALTER TABLE projects ADD CONSTRAINT chk_project_type CHECK (project_type IN ('SHAPE_UP', 'KANBAN'));

-- Comment: Existing projects will automatically get SHAPE_UP type
-- This ensures backward compatibility with existing data
