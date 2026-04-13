-- Add generated_files_json column to wise_architecture_advice
-- Stores the agent-consumable Markdown files generated per analysis run as a JSON array.
-- Each element: { filename, title, content, category, stackType }

ALTER TABLE wise_architecture_advice
    ADD COLUMN generated_files_json TEXT;

COMMENT ON COLUMN wise_architecture_advice.generated_files_json IS
    'JSON array of GeneratedMarkdownFile objects produced by WiseArchitectureMarkdownService. '
    'These are agent-ready .md guides (overview, per-stack, api-design, implementation-plan).';
