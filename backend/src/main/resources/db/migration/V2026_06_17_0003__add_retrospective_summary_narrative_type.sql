-- Add RETROSPECTIVE_SUMMARY to the allowed narrative types
ALTER TABLE cycle_narratives DROP CONSTRAINT IF EXISTS chk_narrative_type;

ALTER TABLE cycle_narratives ADD CONSTRAINT chk_narrative_type
    CHECK (narrative_type IN (
        'WHAT_WE_BET',
        'WHAT_SHIPPED',
        'WHAT_WE_CUT',
        'SURPRISES',
        'FULL_SUMMARY',
        'RETROSPECTIVE_SUMMARY'
    ));
