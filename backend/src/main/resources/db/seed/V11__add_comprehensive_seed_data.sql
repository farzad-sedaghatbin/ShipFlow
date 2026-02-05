-- Comprehensive seed data for ShipFlow

-- Insert sample persons
INSERT INTO persons (id, name, email, skills, department, bio, is_active, created_at, updated_at) 
OVERRIDING SYSTEM VALUE
VALUES
(1, 'Admin User', 'admin@shipflow.com', 'Management, Leadership', 'Management', 'System administrator', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Alice Johnson', 'alice@shipflow.com', 'React, TypeScript, UI/UX', 'Engineering', 'Frontend developer with 5 years experience', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Bob Smith', 'bob@shipflow.com', 'Java, Spring Boot, PostgreSQL', 'Engineering', 'Backend developer specializing in microservices', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Carol Davis', 'carol@shipflow.com', 'Product Design, User Research', 'Product', 'Product manager with focus on user experience', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'David Wilson', 'david@shipflow.com', 'DevOps, AWS, Docker', 'Engineering', 'DevOps engineer managing cloud infrastructure', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Emma Martinez', 'emma@shipflow.com', 'QA, Automation Testing', 'Engineering', 'QA engineer ensuring product quality', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert users (password is 'admin123' for all users)
INSERT INTO users (id, username, password, role, person_id, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE
VALUES
(1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'ADMIN', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'alice', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'PROJECT_MANAGER', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'bob', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'DEVELOPER', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'carol', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'PROJECT_MANAGER', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'david', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'DEVELOPER', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'emma', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cyhyBOIKoJDgdlGrjqDSNEr8WZvr6', 'DEVELOPER', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample projects
INSERT INTO projects (id, name, project_key, description, color, owner_id, is_active, created_at, updated_at)
OVERRIDING SYSTEM VALUE
VALUES
(1, 'Customer Portal', 'CP', 'Building a new customer-facing portal for self-service', '#3498db', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Mobile App', 'MA', 'Native mobile application for iOS and Android', '#e74c3c', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Internal Tools', 'IT', 'Suite of internal tools for team productivity', '#2ecc71', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
