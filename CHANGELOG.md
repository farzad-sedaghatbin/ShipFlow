# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Health Overview - Automated Risk Detection**: Comprehensive risk analysis system for pitch health monitoring
  - **Backend Features**:
    - Automated risk level calculation based on multiple factors (bugs, scope completion, budget, timeline)
    - Bug count analysis: Critical/blocker bugs add significant risk scores
    - Scope completion tracking via Hill Chart positions to detect stagnant work
    - Work hours analysis comparing budget usage vs timeline progress
    - Risk trend indicators (IMPROVING, STABLE, WORSENING) based on recent changes
    - Enhanced `PitchHealthService.calculateRuleBasedRiskLevel()` with comprehensive scoring
    - New `PitchHealthService.calculateRiskTrend()` method analyzing last 3-7 days of activity
    - Risk scoring thresholds: CRITICAL (≥70), HIGH (≥50), MEDIUM (≥25), LOW (<25)
  - **Risk Detection Rules**:
    - **Critical Bugs**: 3+ critical/blocker bugs = +35 points, 1+ = +20 points
    - **Open Bugs**: >10 open bugs with <7 days = +15 points
    - **Budget Overruns**: >120% = +40 points, >100% = +25 points, >80% = +10 points
    - **Stagnant Scopes**: Scopes unchanged for 7+ days in uphill phase = +10-30 points
    - **Timeline Pressure**: <3 days remaining without testing/done status = +30 points
    - **Behind Schedule**: Time progress exceeding work progress by 30% = +30 points
  - **Trend Analysis**:
    - Recent critical bugs (last 3 days) trigger WORSENING trend
    - Hill chart updates (last 7 days) trigger IMPROVING trend
    - Accelerating budget burn (>15 hours in 3 days while >90% budget) = WORSENING
    - No progress with <14 days remaining = WORSENING
  - **Frontend Features**:
    - Priority sorting: Pitches automatically sorted by risk level (CRITICAL → HIGH → MEDIUM → LOW)
    - Pulsing animations on CRITICAL and HIGH risk items for immediate attention
    - Dynamic border widths: Critical (6px), High (5px), Medium/Low (4px)
    - Shadow effects with red glow on critical pitches
    - Alert banner showing count of critical pitches requiring attention
    - Risk trend badges with directional icons (↓ green, ↑ red, − gray)
    - URGENT badge on critical pitches with pulsing animation
    - Days-left badges when ≤3 days remaining
    - Enhanced stat cards with hover effects and time-sensitive coloring
    - Improved visual hierarchy emphasizing critical items
  - **UI/UX Improvements**:
    - Critical pitch stat card pulses and shows "Needs attention!" label
    - Days-left counter turns orange when ≤7 days
    - Smooth transitions and animations for better user feedback
    - Tooltips explaining risk trends and status indicators
  - **Test Coverage**:
    - 14 unit tests in `PitchHealthServiceTest` covering all risk scenarios
    - 15 integration tests in `PitchHealthControllerIntegrationTest`
    - Tests verify bug detection, budget analysis, scope tracking, and trend calculation
    - Tests cover healthy, at-risk, and critical pitch scenarios
  - **API Endpoints**: Existing endpoints enhanced with new risk data
    - `GET /api/health/pitch/{pitchId}` - Returns risk level, color, and trend
    - `GET /api/health/cycle/{cycleId}` - Aggregated health with risk breakdown
    - `GET /api/health/active-cycles` - All active cycles with risk metrics

## [0.1.0] - 2026-01-10

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

>>>>>>> 7aef3fd (feat(health): implement automated risk detection for Health Overview)
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
