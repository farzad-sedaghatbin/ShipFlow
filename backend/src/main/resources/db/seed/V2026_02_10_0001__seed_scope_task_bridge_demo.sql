-- Seed data for Scope-Task Auto-Bridge feature demonstration
-- This migration creates example data showing:
-- 1. Tasks with auto-created scopes (linked)
-- 2. Scopes with auto-progress enabled
-- 3. Subtasks that drive scope progress
-- 4. Scopes with auto-progress disabled (manual control)

-- Only create demo data if we have the required entities
DO $$
DECLARE
  v_cycle_id BIGINT;
  v_pitch_id_1 BIGINT;
  v_pitch_id_2 BIGINT;
  v_user_id_1 BIGINT;
  v_user_id_2 BIGINT;
  v_task_id_1 BIGINT;
  v_task_id_2 BIGINT;
  v_task_id_3 BIGINT;
  v_scope_id_1 BIGINT;
  v_scope_id_2 BIGINT;
  v_scope_id_3 BIGINT;
BEGIN
  -- Get any existing cycle
  SELECT id INTO v_cycle_id FROM cycles ORDER BY id LIMIT 1;
  
  -- Get any two pitches
  SELECT id INTO v_pitch_id_1 FROM pitches ORDER BY id LIMIT 1;
  SELECT id INTO v_pitch_id_2 FROM pitches ORDER BY id LIMIT 1 OFFSET 1;
  
  -- Get any two users
  SELECT id INTO v_user_id_1 FROM users ORDER BY id LIMIT 1;
  SELECT id INTO v_user_id_2 FROM users ORDER BY id LIMIT 1 OFFSET 1;
  
  -- Only proceed if we have the minimum required data
  IF v_cycle_id IS NOT NULL AND v_pitch_id_1 IS NOT NULL AND v_user_id_1 IS NOT NULL THEN
    
    -- Example 1: Task with auto-created scope and auto-progress (50% complete)
    INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, assignee_id, created_at, updated_at)
    VALUES 
    ('Scope-Task Bridge Demo: Mobile App Redesign', 
     'Demonstrates auto-created scope with 50% progress from subtasks.',
     'IN_PROGRESS', 
     'HIGH', 
     'PITCH_SCOPE', 
     40.0, 
     v_cycle_id,
     v_pitch_id_1,
     v_user_id_1,
     CURRENT_TIMESTAMP, 
     CURRENT_TIMESTAMP)
    RETURNING id INTO v_task_id_1;

    -- Create linked scope for the above task
    INSERT INTO hill_chart_points (pitch_id, scope, description, position, linked_task_id, auto_progress_enabled, created_at, updated_at)
    VALUES 
    (v_pitch_id_1,
     'Scope-Task Bridge Demo: Mobile App Redesign',
     'Auto-created scope with auto-progress enabled',
     25,
     v_task_id_1,
     true,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP)
    RETURNING id INTO v_scope_id_1;

    -- Link scope back to task
    UPDATE tasks SET auto_created_scope_id = v_scope_id_1 WHERE id = v_task_id_1;

    -- Create 4 subtasks (2 completed, 1 in progress, 1 todo = 50% complete)
    INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, parent_task_id, assignee_id, created_at, updated_at)
    VALUES 
    ('Design navigation patterns', 'Create mockups', 'DONE', 'HIGH', 'PITCH_SCOPE', 8.0, v_cycle_id, v_pitch_id_1, v_task_id_1, v_user_id_1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Implement responsive layouts', 'Build components', 'DONE', 'HIGH', 'PITCH_SCOPE', 12.0, v_cycle_id, v_pitch_id_1, v_task_id_1, v_user_id_1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Add dark mode support', 'Implement theming', 'IN_PROGRESS', 'MEDIUM', 'PITCH_SCOPE', 10.0, v_cycle_id, v_pitch_id_1, v_task_id_1, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('User testing and feedback', 'Conduct testing', 'TODO', 'MEDIUM', 'PITCH_SCOPE', 10.0, v_cycle_id, v_pitch_id_1, v_task_id_1, v_user_id_1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- Update scope to 50% position (2 of 4 subtasks complete)
    UPDATE hill_chart_points SET position = 50, updated_at = CURRENT_TIMESTAMP WHERE id = v_scope_id_1;

    -- Example 2: Scope with auto-created task, auto-progress DISABLED (manual control)
    IF v_pitch_id_2 IS NOT NULL THEN
      INSERT INTO hill_chart_points (pitch_id, scope, description, position, auto_progress_enabled, created_at, updated_at)
      VALUES 
      (v_pitch_id_2,
       'Scope-Task Bridge Demo: Cache Layer',
       'Manual position control despite subtask completion',
       60,
       false, -- Auto-progress disabled
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP)
      RETURNING id INTO v_scope_id_2;

      -- Auto-create linked task
      INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, auto_created_scope_id, assignee_id, created_at, updated_at)
      VALUES 
      ('Scope-Task Bridge Demo: Cache Layer',
       'Demonstrates manual position control (auto-progress OFF)',
       'IN_PROGRESS',
       'HIGH',
       'PITCH_SCOPE',
       30.0,
       v_cycle_id,
       v_pitch_id_2,
       v_scope_id_2,
       COALESCE(v_user_id_2, v_user_id_1),
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP)
      RETURNING id INTO v_task_id_2;

      -- Link scope to task
      UPDATE hill_chart_points SET linked_task_id = v_task_id_2, updated_at = CURRENT_TIMESTAMP WHERE id = v_scope_id_2;

      -- Create 4 subtasks (3 completed, 1 todo = 75% complete, but scope stays at manual position 60)
      INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, parent_task_id, assignee_id, created_at, updated_at)
      VALUES 
      ('Design cache architecture', 'Design distributed cache', 'DONE', 'HIGH', 'PITCH_SCOPE', 8.0, v_cycle_id, v_pitch_id_2, v_task_id_2, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Implement Redis integration', 'Set up Redis cluster', 'DONE', 'HIGH', 'PITCH_SCOPE', 10.0, v_cycle_id, v_pitch_id_2, v_task_id_2, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Add cache invalidation logic', 'Implement invalidation', 'DONE', 'MEDIUM', 'PITCH_SCOPE', 6.0, v_cycle_id, v_pitch_id_2, v_task_id_2, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
      ('Performance testing', 'Load test cache', 'TODO', 'MEDIUM', 'PITCH_SCOPE', 6.0, v_cycle_id, v_pitch_id_2, v_task_id_2, v_user_id_1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;

    -- Example 3: Fully completed task with scope at position 100
    INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, assignee_id, created_at, updated_at)
    VALUES 
    ('Scope-Task Bridge Demo: Email System',
     'Demonstrates 100% completion with all subtasks done',
     'DONE',
     'MEDIUM',
     'PITCH_SCOPE',
     16.0,
     v_cycle_id,
     v_pitch_id_1,
     v_user_id_1,
     CURRENT_TIMESTAMP - INTERVAL '2 weeks',
     CURRENT_TIMESTAMP)
    RETURNING id INTO v_task_id_3;

    -- Create scope at position 100
    INSERT INTO hill_chart_points (pitch_id, scope, description, position, linked_task_id, auto_progress_enabled, created_at, updated_at)
    VALUES 
    (v_pitch_id_1,
     'Scope-Task Bridge Demo: Email System',
     'Fully completed - all subtasks done',
     100,
     v_task_id_3,
     true,
     CURRENT_TIMESTAMP - INTERVAL '2 weeks',
     CURRENT_TIMESTAMP)
    RETURNING id INTO v_scope_id_3;

    -- Link back
    UPDATE tasks SET auto_created_scope_id = v_scope_id_3 WHERE id = v_task_id_3;

    -- Create 4 completed subtasks
    INSERT INTO tasks (title, description, status, priority, category, estimate_hours, cycle_id, pitch_id, parent_task_id, assignee_id, created_at, updated_at)
    VALUES 
    ('Email template engine', 'Build template system', 'DONE', 'HIGH', 'PITCH_SCOPE', 4.0, v_cycle_id, v_pitch_id_1, v_task_id_3, v_user_id_1, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    ('SMTP integration', 'Connect to email provider', 'DONE', 'HIGH', 'PITCH_SCOPE', 4.0, v_cycle_id, v_pitch_id_1, v_task_id_3, v_user_id_1, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    ('Notification preferences', 'User settings for notifications', 'DONE', 'MEDIUM', 'PITCH_SCOPE', 4.0, v_cycle_id, v_pitch_id_1, v_task_id_3, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('Email delivery tracking', 'Track open rates', 'DONE', 'LOW', 'PITCH_SCOPE', 4.0, v_cycle_id, v_pitch_id_1, v_task_id_3, COALESCE(v_user_id_2, v_user_id_1), CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day');

  END IF;
END $$;
