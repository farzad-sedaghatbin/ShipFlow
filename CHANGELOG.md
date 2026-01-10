# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Meeting Module Enhancements**: Comprehensive improvements to meeting management
  - **Backend Features**:
    - Server-side pagination with configurable page size and sorting
    - Advanced filtering by type, date range, DOR/DOD readiness status, pitch, cycle, and project
    - Dynamic Specification-based queries for flexible filtering
    - New MeetingAction entity for tracking action items with status, assignments, and due dates
    - Retrospective linking for meetings
    - Decisions and attendees text fields
    - Database migration V35 with meeting_actions table and enhanced meeting fields
  - **API Endpoints**:
    - `GET /api/meetings/paginated` - Paginated meetings with DESC sort by default
    - `GET /api/meetings/filter` - Advanced filtering with multiple criteria
  - **Frontend Features**:
    - Collapsible filter panel with type selection, date range pickers, and status toggles
    - Pagination controls with page size selector (10, 20, 50 items per page)
    - Enhanced meeting dialog (max-w-3xl) with retrospective selector
    - Decisions and attendees fields
    - Dynamic action items manager with add/update/remove capabilities
    - Person assignment for action items with status tracking (OPEN, IN_PROGRESS, COMPLETED, CANCELLED)
    - Due date picker for action items
  - **Test Coverage**:
    - 16 unit tests in MeetingServiceTest (up from 8)
    - 15 integration tests in MeetingControllerIntegrationTest (up from 6)
    - Tests cover pagination, filtering, action items, retrospective linking, and error validation

### Fixed
- **Exception Handling**: BadRequestException now correctly returns 400 status instead of 500
  - Added explicit handler in GlobalExceptionHandler for BadRequestException
  - Prevents RuntimeException handler from catching BadRequestException
  - WorkLogTimer validation errors now return proper HTTP 400 responses
  - All 525 backend tests now pass
  - **Test Coverage**:
    - 16 unit tests in MeetingServiceTest (up from 8)
    - 15 integration tests in MeetingControllerIntegrationTest (up from 6)
    - Tests cover pagination, filtering, action items, retrospective linking, and error validation

- **Sub-task Hierarchy**: Tasks can now have parent-child relationships for better organization
  - Database migration V33 adds self-referencing `parent_task_id` column with CASCADE delete
  - Backend support for creating, updating, and querying hierarchical tasks
  - Circular reference prevention (tasks cannot be their own ancestor)
  - New REST endpoints:
    - `GET /api/tasks/{id}/subtasks` - Get direct children of a task
    - `GET /api/tasks/cycle/{cycleId}/roots` - Get root-level tasks (no parent)
    - `GET /api/tasks/cycle/{cycleId}/tree` - Get complete task tree with nested children
  - Comprehensive unit tests (10/10 passing) for hierarchy operations
  - **Frontend UI Features**:
    - Parent task selector in create/edit dialog
    - "Add Sub-task" button on each task row for quick sub-task creation
    - Visual indentation and arrow icon for sub-tasks
    - Display of parent task title below sub-task name
    - Tasks with same parent grouped visually

- **Task-based Time Logging**: Work logs can now be associated with Tasks in addition to Pitches
  - Manual time entry for tasks
  - Toggle between Pitch and Task when logging time
  - Backend API support for task-based work logs
  - Updated work log entities, DTOs, and services
  - Database migration V32 to support optional task references
  - Validation to ensure either pitchId or taskId is provided (but not both)
  - Frontend UI with toggle buttons for selecting Pitch or Task
  - Work log table displays both pitch and task information with badges
  - Edit dialog supports switching between pitch and task
  - Comprehensive unit tests (12/12 passing) for work log operations

- **Timer Integration for Time Tracking**: Added timer-based time tracking alongside manual entry
  - Database migration V34 for work_log_timers table
  - Backend REST API for timer operations:
    - `POST /api/timers/start` - Start timer for pitch or task
    - `POST /api/timers/stop` - Stop timer and create work log entry
    - `GET /api/timers/active` - Get currently running timer
    - `DELETE /api/timers/cancel` - Cancel timer without logging
  - Timer Service with business logic:
    - Automatic time rounding to nearest 0.25 hours (15 minutes)
    - One active timer per user enforcement
    - Elapsed time calculation with real-time updates
    - Quarter-hour rounding on timer stop
  - Frontend Timer Widget (floating card):
    - Real-time elapsed time display (HH:MM:SS format)
    - Shows associated pitch/task and note
    - Stop & Log button with confirmation dialog
    - Cancel button to discard timer
    - Automatically appears when timer is running
  - Timer Integration in My Work Logs page:
    - "Start Timer" button alongside manual entry form
    - Auto-reloads work logs when timer stopped
    - Uses same pitch/task selector as manual entry
  - Comprehensive testing:
    - Backend unit tests: 11/11 passing (WorkLogTimerServiceTest)
    - Backend integration tests: 9/9 passing (WorkLogTimerControllerIntegrationTest)
    - Total timer tests: 20/20 passing

### Testing Summary
- **Backend Tests**: 42 tests passing across all new features
  - Task Hierarchy: 10/10 tests
  - Task-based Work Logs: 12/12 tests
  - Timer Service: 11/11 tests
  - Timer Controller Integration: 9/9 tests
- **Frontend Tests**: 106 tests passing
  - taskService: 19/19 tests (including subtask hierarchy: getSubTasks, getRootTasks, getTaskTree)
  - workLogService: Tests for work log CRUD operations
  - pitchService: Tests for pitch management
  - Other services and components
- **Test Coverage**: Service layer, repository layer, REST endpoints, and error scenarios

### Design Decisions
- **Dual Time Logging**: Both manual entry AND timer integration for maximum flexibility
- **Timer Rounding**: Automatic rounding to 0.25 hours (15-minute increments) for consistency
- **Single Active Timer**: Only one timer per user at a time to prevent accidental double-tracking
- **Seamless Integration**: Timer and manual entry share same UI/UX for pitch/task selection
- **Dashboard Customization**: Users can now customize which widgets appear on their dashboard and in what order
  - Widget visibility toggle
  - Configurable display order with bulk update support
  - Default widgets: Stats Cards, Quick Links, Active Cycles, Recent Pitches, Hill Chart, Recent Activity, Risk Overview
  - Reset to defaults functionality
- **Notification System**: Real-time notifications for important events
  - NotificationCenter component in header with unread badge
  - Notification types: Overdue Tasks, Blocked Tasks, Cycle Deadlines, Stalled Hill Charts
  - Severity levels: INFO, WARNING, ERROR, CRITICAL
  - Click-to-navigate to related entities
  - Mark as read, delete, and mark all as read actions
  - Auto-poll for new notifications every 30 seconds
  - Automated daily generation at 8 AM
  - Automatic cleanup of old notifications (30 days)
- Initial release of ShipFlow - Modern project management application implementing the Shape Up methodology
- **Core Features**:
  - Cycles: 6-week development cycles with betting table
  - Pitches: Shape work with appetite, problem definition, and solution
  - Hill Charts: Visual progress tracking with drag-and-drop dots
  - Tasks: Independent work management during cycles with categorization (Pitch Scope vs. Debt & Improvements)
  - Retrospectives: Team retros with voting and merging capabilities
  - Projects & Teams: Organization-wide project and team management
  - Work Logs & Meetings: Time tracking and meeting documentation
  - Health Dashboard: Project health metrics and risk insights
  
- **AI-Powered Features**:
  - Q&A System: RAG-based knowledge retrieval from project documents with smart relevance filtering, source citation tracking, and conversation memory
  - Test Case Generation: AI-assisted test case generation with type-specific prompts and quality validation
  - Query Decomposition: Handles complex multi-part questions
  - Active Learning: Continuous quality improvement from user feedback
  - LLM Response Caching: 40-60% cost reduction with Redis support
  - Prompt Compression: 10-20% token reduction
  - Content Guardrails: Production safety with toxic content and bias detection
  - Document Processing: Support for PDF, DOCX, DOC, TXT, MD files with automatic text extraction
  
- **User Experience**:
  - WCAG 2.1 AA compliant accessibility (88/100 score)
  - Fully responsive mobile design with touch-friendly 44px minimum touch targets
  - Keyboard shortcuts for navigation and quick actions (press `?` to view all)
  - Page transitions and animated components
  - Breadcrumb navigation
  - Enhanced empty states with illustrations
  - Dark/light theme support
  
- **Backend Architecture**:
  - Spring Boot REST API with comprehensive Swagger documentation
  - PostgreSQL database with Flyway migrations
  - Redis support for distributed caching
  - ChromaDB integration for vector storage
  - Security features: JWT authentication, malicious request filtering, CORS protection
  - Comprehensive test coverage with JUnit and integration tests
  
- **Frontend Architecture**:
  - React with TypeScript and Vite
  - Material-UI (MUI) component library
  - Framer Motion for animations
  - React Query for data fetching
  - Comprehensive form validation and error handling
  
- **DevOps & Configuration**:
  - Docker and Docker Compose support
  - Ollama integration for local LLM inference
  - Redis configuration for production caching
  - Environment-based configuration (dev/prod profiles)
  - Health checks and monitoring endpoints

### Security
- Malicious request detection and blocking (Log4Shell, XSS, SQL Injection, Path Traversal)
- Header injection attack prevention
- JWT-based authentication
- CORS configuration
- Security document filtering for access control
