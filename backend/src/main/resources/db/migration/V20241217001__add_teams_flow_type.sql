-- Add flow_type column to teams_channel_config table
ALTER TABLE teams_channel_config ADD COLUMN flow_type VARCHAR(50);

-- Update existing records to default to WEBHOOK
UPDATE teams_channel_config SET flow_type = 'WEBHOOK' WHERE flow_type IS NULL;

-- Make flow_type NOT NULL after setting defaults
ALTER TABLE teams_channel_config ALTER COLUMN flow_type SET NOT NULL;