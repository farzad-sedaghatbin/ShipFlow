-- Add meeting_types_json column to organization_settings for dynamic meeting type configuration
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS meeting_types_json TEXT;

-- Add DOR/DOD checklist items columns to meetings table
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS dor_items_json TEXT;
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS dod_items_json TEXT;

-- Add comment for documentation
COMMENT ON COLUMN organization_settings.meeting_types_json IS 'JSON array of MeetingTypeConfig objects with DOR/DOD checklist items per meeting type';
COMMENT ON COLUMN meetings.dor_items_json IS 'JSON array of completed DOR checklist items for this meeting';
COMMENT ON COLUMN meetings.dod_items_json IS 'JSON array of completed DOD checklist items for this meeting';
