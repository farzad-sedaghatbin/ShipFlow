-- =============================================================================
-- V2026_02_15_0001: Link More Pitches to Epics for Wise Architecture Demo
-- =============================================================================
-- This seed data enhances the roadmap demo by linking additional pitches
-- to epics. This allows Wise Architecture to demonstrate roadmap context
-- integration with multiple related pitches per epic.
-- =============================================================================

-- Link more pitches to existing epics
DO $$
DECLARE
    v_epic_checkout_id BIGINT;
    v_epic_security_id BIGINT;
    v_epic_selfservice_id BIGINT;
    v_epic_cicd_id BIGINT;
    v_epic_offline_id BIGINT;
    v_pitch_id BIGINT;
BEGIN
    -- Get epic IDs
    SELECT id INTO v_epic_checkout_id FROM epics WHERE name = 'Mobile Checkout Redesign';
    SELECT id INTO v_epic_security_id FROM epics WHERE name = 'Security & Compliance Hardening';
    SELECT id INTO v_epic_selfservice_id FROM epics WHERE name = 'Self-Service Account Management';
    SELECT id INTO v_epic_cicd_id FROM epics WHERE name = 'CI/CD Pipeline Improvements';
    SELECT id INTO v_epic_offline_id FROM epics WHERE name = 'Mobile Offline Support';
    
    -- Link Real-time Notifications pitch to Mobile Checkout (both mobile features)
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE 'Real-time Notification%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_checkout_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_checkout_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link API Rate Limiting pitch to Security epic
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Rate Limit%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_security_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_security_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link Customer Dashboard pitch to Self-Service Account epic
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Dashboard%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_selfservice_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_selfservice_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link Build Automation pitch to CI/CD epic
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Build%' OR title LIKE '%Deploy%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_cicd_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_cicd_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link Session Management pitch to Mobile Offline (related mobile feature)
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Session%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_offline_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_offline_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link Data Export pitch to Self-Service Account (customer self-service)
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Export%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_selfservice_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_selfservice_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Link Search Enhancement pitch to Self-Service Account
    SELECT id INTO v_pitch_id FROM pitches WHERE title LIKE '%Search%' LIMIT 1;
    IF v_pitch_id IS NOT NULL AND v_epic_selfservice_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_selfservice_id WHERE id = v_pitch_id AND epic_id IS NULL;
    END IF;
    
    -- Second pass: Link remaining shaped/in-progress pitches to Security epic
    -- (gives Security epic more related pitches for demo)
    UPDATE pitches 
    SET epic_id = v_epic_security_id 
    WHERE epic_id IS NULL 
      AND status IN ('SHAPED', 'IN_PROGRESS', 'PENDING')
      AND v_epic_security_id IS NOT NULL
      AND id IN (SELECT id FROM pitches WHERE epic_id IS NULL ORDER BY id LIMIT 3);
    
    -- Log what we did
    RAISE NOTICE 'Enhanced epic-pitch links for Wise Architecture demo';
END $$;

-- =============================================================================
-- Summary: Creates multiple pitch-epic relationships to demonstrate
-- how Wise Architecture uses roadmap context for solution generation
-- =============================================================================
