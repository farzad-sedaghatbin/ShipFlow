-- V26: Add feedback columns to qa_interactions for active learning

-- Add feedback_helpful column for simple helpful/unhelpful feedback
ALTER TABLE qa_interactions ADD COLUMN feedback_helpful BOOLEAN;

-- Add feedback_text column for additional feedback details
ALTER TABLE qa_interactions ADD COLUMN feedback_text TEXT;

-- Create index for filtering by feedback
CREATE INDEX idx_qa_feedback ON qa_interactions(feedback_helpful);
