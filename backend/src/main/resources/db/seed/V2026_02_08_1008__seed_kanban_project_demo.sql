-- V2026_02_08_1008__seed_kanban_project_demo.sql
-- Add a Kanban project with demo data

-- Create Kanban project (project_id will be 4 if we have 3 existing projects)
INSERT INTO projects (name, project_key, description, color, owner_id, is_active, project_type, enable_retrospectives, created_at, updated_at)
VALUES 
('E-Commerce Platform', 'ECOM', 'Continuous development of e-commerce platform with Kanban workflow', '#9b59b6', 2, true, 'KANBAN', false, '2025-08-01 09:00:00', CURRENT_TIMESTAMP);

-- Create a long-term "hidden" cycle for the Kanban project (for data consistency, never shown in UI)
-- This cycle has no end date and represents continuous flow
INSERT INTO cycles (name, start_date, end_date, phase, project_id, is_active)
VALUES
('Kanban Continuous Flow', '2025-08-01', '2099-12-31', 'BUILD', 4, true);

-- Add Kanban backlog tasks for the project (associated with the long-term cycle for data consistency)
INSERT INTO tasks (title, description, status, priority, category, estimate_hours, actual_hours, cycle_id, assignee_id, pair_assignee_id, created_by_id, due_date, tags, created_at, updated_at)
VALUES
-- Backlog items
('Implement product search filters', 'Add advanced filtering options for product search including price range, categories, and ratings', 'BACKLOG', 'HIGH', 'PITCH_SCOPE', 8.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), NULL, NULL, 2, '2026-03-01', 'feature,search,frontend', '2025-09-01 09:00:00', '2025-09-01 09:00:00'),
('Add wishlist functionality', 'Allow customers to save products to wishlist for later purchase', 'BACKLOG', 'MEDIUM', 'PITCH_SCOPE', 12.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), NULL, NULL, 2, '2026-03-15', 'feature,wishlist,frontend,backend', '2025-09-05 10:00:00', '2025-09-05 10:00:00'),
('Implement gift card system', 'Support purchasing and redeeming gift cards', 'BACKLOG', 'MEDIUM', 'PITCH_SCOPE', 16.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), NULL, NULL, 2, '2026-04-01', 'feature,payment,backend', '2025-09-10 11:00:00', '2025-09-10 11:00:00'),
('Add product recommendations', 'ML-based product recommendations on product detail and cart pages', 'BACKLOG', 'MEDIUM', 'PITCH_SCOPE', 20.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), NULL, NULL, 3, '2026-04-15', 'feature,ml,recommendations', '2025-09-15 09:00:00', '2025-09-15 09:00:00'),

-- TODO items
('Optimize product image loading', 'Implement lazy loading and WebP format for product images', 'TODO', 'HIGH', 'DEBT_IMPROVEMENT', 6.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, NULL, 2, '2026-02-20', 'tech-debt,performance,frontend', '2025-12-01 09:00:00', '2025-12-01 09:00:00'),
('Fix cart total calculation bug', 'Cart total incorrect when applying multiple discount codes', 'TODO', 'URGENT', 'PITCH_SCOPE', 4.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 6, '2026-02-15', 'bug,cart,backend', '2026-01-10 14:00:00', '2026-01-10 14:00:00'),
('Update payment gateway SDK', 'Upgrade Stripe SDK to latest version for security fixes', 'TODO', 'HIGH', 'DEBT_IMPROVEMENT', 5.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 2, '2026-02-18', 'tech-debt,security,payment', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),
('Add order cancellation feature', 'Allow customers to cancel orders within 1 hour of placement', 'TODO', 'MEDIUM', 'PITCH_SCOPE', 10.0, NULL, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, 6, 2, '2026-02-25', 'feature,orders,backend', '2026-01-20 09:00:00', '2026-01-20 09:00:00'),

-- IN_PROGRESS items
('Implement real-time inventory updates', 'Use WebSocket to show real-time stock availability', 'IN_PROGRESS', 'HIGH', 'PITCH_SCOPE', 14.0, 8.5, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 2, '2026-02-22', 'feature,inventory,backend,websocket', '2026-01-25 09:00:00', CURRENT_TIMESTAMP),
('Add product review moderation', 'Admin interface for approving/rejecting product reviews', 'IN_PROGRESS', 'MEDIUM', 'PITCH_SCOPE', 8.0, 4.0, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, NULL, 2, '2026-02-28', 'feature,reviews,admin', '2026-02-01 10:00:00', CURRENT_TIMESTAMP),
('Redesign checkout flow', 'Simplify 3-step checkout to 2 steps with better UX', 'IN_PROGRESS', 'HIGH', 'PITCH_SCOPE', 16.0, 10.0, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, 5, 2, '2026-03-05', 'feature,checkout,frontend,ux', '2026-02-03 09:00:00', CURRENT_TIMESTAMP),

-- DONE items
('Fix mobile navigation menu', 'Mobile menu not closing after link click on iOS Safari', 'DONE', 'URGENT', 'PITCH_SCOPE', 2.0, 1.5, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, NULL, 6, '2026-01-15', 'bug,mobile,frontend', '2026-01-05 14:00:00', '2026-01-10 16:30:00'),
('Add Google Analytics tracking', 'Implement GA4 tracking for e-commerce events', 'DONE', 'MEDIUM', 'PITCH_SCOPE', 6.0, 5.5, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, NULL, 2, '2026-01-20', 'feature,analytics,tracking', '2025-12-10 09:00:00', '2026-01-18 17:00:00'),
('Optimize database queries', 'Index optimization for product listing and search queries', 'DONE', 'HIGH', 'DEBT_IMPROVEMENT', 8.0, 7.0, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 3, '2026-01-25', 'tech-debt,performance,database', '2025-12-15 10:00:00', '2026-01-22 15:00:00'),
('Implement abandoned cart emails', 'Send reminder emails for carts abandoned over 24 hours', 'DONE', 'MEDIUM', 'PITCH_SCOPE', 10.0, 9.5, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 2, '2026-01-30', 'feature,email,marketing', '2025-12-20 09:00:00', '2026-01-28 16:00:00'),
('Add multi-currency support', 'Support USD, EUR, GBP with real-time exchange rates', 'DONE', 'HIGH', 'PITCH_SCOPE', 18.0, 20.0, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 3, NULL, 2, '2026-02-05', 'feature,payment,i18n', '2026-01-05 09:00:00', '2026-02-04 17:30:00'),
('Fix product filtering on mobile', 'Filter panel not responsive on mobile devices', 'DONE', 'HIGH', 'PITCH_SCOPE', 4.0, 3.5, (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1), 4, NULL, 6, '2026-02-08', 'bug,mobile,frontend', '2026-01-30 11:00:00', '2026-02-07 14:00:00');

-- Add some comments to tasks
INSERT INTO comments (entity_type, entity_id, author_id, content, created_at, updated_at)
VALUES
-- Comments on "Implement real-time inventory updates"
('TASK', (SELECT id FROM tasks WHERE title = 'Implement real-time inventory updates' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 3, 'Working on WebSocket connection logic. Almost done with the backend implementation.', '2026-02-06 10:30:00', '2026-02-06 10:30:00'),
('TASK', (SELECT id FROM tasks WHERE title = 'Implement real-time inventory updates' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 2, 'Great progress! Make sure to add proper error handling for connection drops.', '2026-02-06 11:00:00', '2026-02-06 11:00:00'),

-- Comments on "Redesign checkout flow"
('TASK', (SELECT id FROM tasks WHERE title = 'Redesign checkout flow' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 4, 'Completed the design mockups. Starting implementation today.', '2026-02-05 09:00:00', '2026-02-05 09:00:00'),
('TASK', (SELECT id FROM tasks WHERE title = 'Redesign checkout flow' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 5, 'The new flow looks much cleaner! Let me know when ready for UX review.', '2026-02-05 14:30:00', '2026-02-05 14:30:00');

-- Add work logs for in-progress tasks
INSERT INTO work_logs (task_id, person_id, hours_spent, date, note)
VALUES
-- Work logs for "Implement real-time inventory updates"
((SELECT id FROM tasks WHERE title = 'Implement real-time inventory updates' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 3, 4.0, '2026-02-05', 'Set up WebSocket server and connection handling'),
((SELECT id FROM tasks WHERE title = 'Implement real-time inventory updates' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 3, 4.5, '2026-02-06', 'Implemented inventory update broadcasting and client subscription logic'),

-- Work logs for "Add product review moderation"
((SELECT id FROM tasks WHERE title = 'Add product review moderation' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 4, 4.0, '2026-02-06', 'Created admin UI components for review list and actions'),

-- Work logs for "Redesign checkout flow"
((SELECT id FROM tasks WHERE title = 'Redesign checkout flow' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 4, 5.0, '2026-02-05', 'Implemented step 1 of new checkout flow with validation'),
((SELECT id FROM tasks WHERE title = 'Redesign checkout flow' AND cycle_id = (SELECT id FROM cycles WHERE name = 'Kanban Continuous Flow' LIMIT 1) LIMIT 1), 4, 5.0, '2026-02-06', 'Implemented step 2 with payment integration');

COMMENT ON TABLE projects IS 'Contains both Shape Up and Kanban type projects';
COMMENT ON TABLE cycles IS 'For Kanban projects, contains a long-term hidden cycle for data consistency (never shown in UI)';
