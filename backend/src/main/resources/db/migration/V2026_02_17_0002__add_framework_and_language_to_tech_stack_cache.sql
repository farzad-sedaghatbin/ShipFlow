-- Add framework and primary_language columns to repository_tech_stacks table
-- These columns store the detected framework (e.g., "Spring Boot", "React") and 
-- primary language (e.g., "Java", "TypeScript") for consistency between cached and live detections

ALTER TABLE repository_tech_stacks 
ADD COLUMN framework VARCHAR(100),
ADD COLUMN primary_language VARCHAR(50);

-- Add index for common queries
CREATE INDEX idx_repository_tech_stacks_framework ON repository_tech_stacks(framework);

COMMENT ON COLUMN repository_tech_stacks.framework IS 'Detected framework (e.g., Spring Boot, React, NestJS)';
COMMENT ON COLUMN repository_tech_stacks.primary_language IS 'Primary programming language (e.g., Java, TypeScript, Python)';
