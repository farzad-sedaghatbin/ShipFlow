# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.3.1] - 2026-01-27

### Fixed
- **Critical Database Migration Fixes** (Production Blocking)
  - **V59 Migration** (Pitch Risk History):
    - Fixed H2-incompatible inline INDEX syntax - separated into individual CREATE INDEX statements
    - Fixed TEXT/CLOB type mismatch in `PitchRiskHistory` entity causing schema validation errors
    - Updated entity to use `@Lob` annotation for proper CLOB mapping
  - **V60 Migration** (User Projects):
    - Replaced MySQL-specific `ON DUPLICATE KEY UPDATE` with H2-compatible `MERGE INTO` statement
    - Ensures data migration works correctly across different database environments
  - These fixes ensure application starts successfully with H2 database in all environments

## [0.3.0] - 2026-01-27

### Added
- **Pluggable Vector Store Architecture**: Complete refactoring to pluggable vector database system
  - **Core Architecture**:
    - Created `VectorStoreProvider` interface for all provider implementations
    - Implemented `VectorStoreProviderFactory` with Spring auto-discovery for automatic provider registration
    - Added `VectorStoreProviderType` enum supporting: in-memory, qdrant, chroma, milvus (future), pinecone (future), weaviate (future)
    - Created `VectorStoreProviderConfig` builder pattern for flexible, provider-agnostic configuration
    - Default vector dimension: 384 (matches all-MiniLM-L6-v2 embedding model)
  - **Provider Implementations**:
    - **InMemoryVectorStoreProvider**: Development/testing (non-persistent, no dependencies)
    - **QdrantVectorStoreProvider**: Production recommended - high-performance Rust-based vector DB with API key auth
    - **ChromaVectorStoreProvider**: Alternative option for simpler deployments
  - **Configuration System**:
    - Updated `application.properties` with `app.qa.vectorstore.*` configuration section
    - Dev profile defaults to `in-memory` (no external dependencies)
    - Prod profile defaults to `qdrant` with API key authentication
    - Docker Compose updated with Qdrant service (replaces ChromaDB)
  - **Testing**:
    - Created 6 new test classes covering the vector store plugin system:
      - `VectorStoreProviderTypeTest`: Enum validation and config parsing (10 tests)
      - `VectorStoreProviderConfigTest`: Configuration builder, defaults, and extra params (16 tests)
      - `VectorStoreProviderFactoryTest`: Factory auto-discovery and store creation (13 tests)
      - `InMemoryVectorStoreProviderTest`: In-memory provider tests (9 tests)
      - `QdrantVectorStoreProviderTest`: Qdrant provider validation tests (11 tests)
      - `ChromaVectorStoreProviderTest`: ChromaDB provider tests (8 tests)
    - Total: 67 new unit tests for vector store architecture, 100% pass rate
  - **Documentation**:
    - Updated `RAG_ARCHITECTURE.md` with pluggable vector store section
    - Updated `ENVIRONMENT_SETUP.md` with vector store configuration guide
    - Updated `README.md` to highlight Qdrant as production recommendation

- **LLM Plugin Architecture**: Complete refactoring to pluggable AI provider system
  - **Core Architecture**:
    - Created `LLMProvider` interface for all provider implementations
    - Implemented `LLMProviderFactory` with Spring auto-discovery for automatic provider registration
    - Added `LLMProviderType` enum supporting: ollama, runpod, openai, anthropic (future), google (future), azure-openai (future)
    - Created `LLMProviderConfig` builder pattern for flexible, provider-agnostic configuration
    - Replaced monolithic `RunPodChatModel` class with modular provider implementations
  - **Provider Implementations**:
    - **OllamaLLMProvider**: Local/self-hosted AI (privacy-first, no API costs)
    - **OpenAILLMProvider**: Production-grade ChatGPT integration (gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo)
    - **RunPodLLMProvider**: Cloud GPU serverless compute with async job polling
  - **Configuration System**:
    - Updated `application.properties` with provider-specific sections (ollama, openai, runpod)
    - Added `application-dev.properties` configuration for local development (defaults to Ollama)
    - Created `.env.example` with all provider configuration templates
    - Updated Docker Compose files to support all three providers via environment variables
  - **Testing**:
    - Created 4 new test classes covering the LLM plugin system:
      - `LLMProviderConfigTest`: Configuration builder and extra params (8 tests)
      - `LLMProviderFactoryTest`: Factory auto-discovery and model creation (13 tests)
      - `LLMProviderTypeTest`: Enum validation and config parsing (7 tests)
      - `OllamaLLMProviderTest`, `OpenAILLMProviderTest`, `RunPodLLMProviderTest`: Provider-specific tests (22 tests)
    - Total: 53 new unit tests, 100% pass rate, 94% instruction coverage on LLM package
    - All tests passing: 1018 total, 0 failures
  - **Developer Experience**:
    - Enhanced `start-dev.sh` with provider-specific validation (checks Ollama only when AI_PROVIDER=ollama)
    - Added provider display names and descriptive error messages
    - Created comprehensive README in `backend/src/main/java/.../llm/README.md` with:
      - Architecture overview and diagrams
      - Step-by-step guide for adding new providers
      - Configuration examples and usage patterns
  - **Extensibility**:
    - Framework ready for future providers (Anthropic Claude, Google Gemini, Azure OpenAI)
    - Provider plugins auto-discovered via Spring's component scanning
    - No code changes needed to switch providers (environment variable only)

### Fixed
- **Circuit Breaker Monitor**: Fixed URL parameter handling
  - Changed from `/cycles/:id/circuit-breaker` to `/cycles/:cycleId/circuit-breaker`
  - Updated `useParams` hook to correctly parse `cycleId` parameter
  - Resolved 404 errors when navigating to circuit breaker page from cycle detail
- **AI Cache Controller**: Added missing `AIConfig` dependency injection
  - Injected `AIConfig` to access provider information
  - Added `/api/cache/ai-provider` endpoint to display current AI provider and model
  - Shows provider display name (e.g., "OpenAI ChatGPT", "Ollama (Local)", "RunPod (Cloud GPU)")
- **Circuit Breaker Service**: Fixed axios import to use centralized API client
  - Changed from direct `axios` import to `api` from `./api`
  - Ensures all API requests use consistent base URL and interceptors
  - Maintains authentication headers across all circuit breaker requests
- **Translation Interpolation**: Fixed string interpolation in English and Persian translations
  - Changed `{count}` and `{threshold}` to `{{count}}` and `{{threshold}}` (i18next syntax)
  - Fixed circuit breaker and cycle detail pages showing raw placeholder strings
- **Flyway Configuration**: Changed `spring.flyway.clean-disabled` to `true` in `application-dev.properties`
  - Prevents accidental database wipes during development restarts
  - Preserves sample data and work logs across application restarts
  - Developers can still manually clean database when needed

### Changed
- **AI Risk Analysis Logging**: Enhanced logging with provider and timing information
  - Added provider name and model to AI generation logs ("🤖 Generating AI insights using provider: openai (model: gpt-4o-mini)")
  - Added execution time tracking for AI requests ("✅ AI response received in 2345ms")
  - Improved debugging and performance monitoring capabilities
- **Cycle Risk Overview Component**: Enhanced AI insights rendering
  - Changed from plain list items to Markdown rendering for insights and recommendations
  - Supports bold, italic, code blocks, and formatting in AI-generated content
  - Improves readability of structured AI responses
- **Cycle Detail Navigation**: Added Circuit Breaker button to cycle header
  - Placed ⚡ icon button next to Hill Chart and Edit buttons
  - Improves discoverability of circuit breaker feature
  - Consistent with Shape Up methodology's safety valve concept
- **Competitors Comparison Page**: Updated AI features description
  - Highlights pluggable LLM architecture with provider choice (Ollama, OpenAI, RunPod)
  - Emphasizes "Privacy-first or production-ready—your choice"
  - Clarifies deployment flexibility (local, cloud, or GPU-accelerated)
- **Circuit Breaker Guide**: Added "Re-pitching Killed Work" section
  - Documents how to learn from killed pitches and re-pitch smarter
  - Provides step-by-step guide and example scenario
  - Includes Shape Up wisdom: "The best teams kill pitches early and re-pitch smarter, not harder"
  - Updated access instructions to reflect new navigation (button in cycle header vs. separate link)

### Development
- **Database Migration**: Added V56 with circuit breaker test data
  - Populates Cycle 4 pitches with realistic work logs
  - Demonstrates overflow detection (116%, 120%, 85%, 55%, 118% of appetite)
  - Enables testing of circuit breaker functionality with real data
  - Updates pitch statuses to reflect active development (IN_PROGRESS, STARTED)

- **Project Type System**: Support for both Kanban and Shape Up methodologies
  - **Backend Implementation**:
    - New `ProjectType` enum (SHAPE_UP, KANBAN) with database migration V55
    - Automatic "Continuous Flow" cycle creation for Kanban projects
    - `ProjectDTO` and `CreateProjectRequest` include projectType field
    - Backward compatibility: All existing projects default to SHAPE_UP
    - Comprehensive unit tests in `ProjectTypeTest.java` (5 tests, 100% pass rate)
  - **Frontend Implementation**:
    - Project type selection in create/edit project dialogs
    - `useProject` context with `isKanbanProject` computed property
    - Conditional navigation: Cycles menu hidden for Kanban projects
    - Automatic view switching: Kanban projects default to board view
  - **Kanban-Specific Features**:
    - Pitch/scope fields hidden in task/bug/testcase forms
    - Cycle and pitch filters hidden in list views (BacklogPage, WorkLogsPage, TestCasesPage, BugReportsPage)
    - Terminology changes: "Feature Tasks" vs "Pitch Tasks" based on project type
    - Kanban board enhancements: subtask creation, timer start functionality
  - **Project-Based Filtering**:
    - All pages filter data by currently selected project
    - "All Projects" selection shows data from all projects
    - Consistent filtering across BacklogPage, WorkLogsPage, TestCasesPage, BugReportsPage, MeetingList
  - **Documentation**:
    - Comprehensive architecture doc: `PROJECT_TYPE_ARCHITECTURE.md`
    - Updated README.md with project type feature comparison
    - Test coverage documentation and implementation summary
    - New comprehensive guide: `ProjectTypesGuide.tsx` explaining both modes
    - Updated landing page with dual mode feature highlight
    - Updated competitor comparison page with Kanban support
  - **UI Consistency & Internationalization**:
    - Dashboard hides cycle/pitch widgets for Kanban projects
    - Reports page shows appropriate message for Kanban (no cycle-based reports)
    - Documentation guides updated with project type disclaimers
    - Complete Farsi translations for dual mode features
    - Help & Guides page includes Project Types guide

- **Farsi (Persian) Language Support**: Comprehensive RTL internationalization
  - **Complete Translation Coverage**: 3,650+ translation keys in Persian (fa.json)
    - All UI components, forms, navigation, and messages fully translated
    - Dashboard widgets, reports, meetings, and QA sections localized
    - Chart labels, tooltips, and data visualizations in Farsi
  - **RTL Layout Support**: Full right-to-left layout implementation
    - Tailwind CSS logical properties (me-, ms-, start-, end-) throughout application
    - React Grid Layout configured for RTL with proper direction handling
    - Dynamic text direction based on language selection (ltr/rtl)
    - Bidirectional text rendering for mixed content
  - **Responsive Grid Layouts**: Dynamic width calculation for RTL compatibility
    - Container-aware grid sizing using useRef and resize listeners
    - Prevents widget overflow in RTL mode
    - Proper grid positioning calculations for both LTR and RTL
  - **Language Switching**: Seamless language toggle in user interface
    - Persistent language preference in localStorage
    - Automatic direction and font changes
    - No page reload required for language switch

- **Microsoft Teams Integration**: Full integration with Microsoft Teams for real-time notifications
  - **Backend Features**:
    - Database tables for Teams configuration, channel settings, and notification history (V54 migration)
    - `TeamsConfiguration` entity for tenant-level settings with webhook URL
    - `TeamsChannelConfig` entity for channel-specific notification preferences
    - `TeamsNotificationHistory` entity for audit logging
    - `TeamsIntegrationService` for sending notifications using Adaptive Card format
    - `TeamsIntegrationController` with REST endpoints for configuration management
    - Support for 8 notification types: task assigned/completed/blocked, pitch shaped, cycle started/cooldown, betting completed, sprint started
    - Channel-specific notification filtering and test notification functionality
    - Color-coded notification cards based on event type
    - **Test Coverage**: 17 comprehensive unit tests for `TeamsIntegrationService` with Mockito (100% pass rate)
  - **Frontend Features**:
    - Teams Integration settings page at `/integrations/teams`
    - Tenant configuration UI with webhook URL management
    - Channel-specific notification preference management with toggles
    - Test notification sending interface
    - Built-in setup guide with step-by-step instructions
    - Navigation integration in Administration → Integrations section
  - **API Endpoints**:
    - `POST /api/teams/configurations` - Create/update tenant configuration
    - `GET /api/teams/configurations` - List all configurations
    - `GET /api/teams/configurations/active` - Get active configuration
    - `DELETE /api/teams/configurations/{id}` - Delete configuration
    - `POST /api/teams/configurations/{id}/channels` - Configure channel notifications
    - `POST /api/teams/configurations/{id}/test` - Send test notification
    - `GET /api/teams/configurations/{id}/history` - Get notification history

- **Competitors Comparison Page**: New marketing page comparing ShipFlow with alternatives
  - Feature-by-feature comparison matrix with Linear, Asana, Monday.com, Jira, and Basecamp
  - 40+ features compared across 7 categories (Shape Up, Progress, AI, QA, Team, Integrations, Deployment)
  - Key differentiator cards highlighting ShipFlow's unique advantages
  - Individual competitor breakdown cards explaining when to choose each tool
  - Accessible via `/compare` route and linked from Landing page with "Compare to Alternatives" button
  - Professional design with responsive layout
  - Comparison summary also added to README.md for quick reference

- **Server-Side Search for Traceability Dropdowns**: Optimized performance for large datasets
  - Minimum 3-character search with 300ms debouncing to prevent API spam
  - GET `/api/tasks/search?q={query}` endpoint for task search by title/description
  - GET `/api/hill-chart/search?q={query}` endpoint for scope search by scope/description
  - Database-level LIKE queries with case-insensitive partial matching
  - Context-aware loading: pitch/cycle context loads scoped data, otherwise requires search
  - Custom `useDebounce` hook for frontend search optimization
  - Helpful UI messages: "Type to search", "Searching...", "Type at least 3 characters"
  - Max 50 results per search to maintain performance
  - Scales to millions of records via indexed searches
- **Traceability Relationships**: Optional links between tasks, bug reports, test cases, and scopes
  - Tasks can link to pitch and scope (hill chart point)
  - Bug reports can link to scope and related task
  - Test cases can link to scope and related task
  - All relationships optional to support technical debt/improvement work
  - Database migration V53 with nullable foreign keys
  - Comprehensive test coverage (22 unit tests)
  - Frontend dropdowns with search in BugReportModal and TestCaseFormPage
- **Task Dependencies**: Lightweight dependency tracking system for identifying blockers
  - Three dependency types: BLOCKS, DEPENDS_ON, RELATED_TO
  - Automatic circular dependency detection using DFS algorithm
  - Visual blocker indicators in task lists showing blocked task count
  - **Enhanced blocker tooltips**: Hover over blocker badge to see up to 3 blocker task names (with "... and X more" for additional)
  - **Blocking indicators**: Green shield badge showing how many tasks this task is blocking
  - **Subtask indicators**: List badge showing subtask count with tooltip listing subtask titles
  - **Dependency filtering**: Filter backlog by "All Tasks", "Blocked Tasks", or "Blocking Tasks"
  - Dedicated dependency management section in task detail pages with improved UX
  - Quick Guide in dependency dialog explaining which type to select based on task status
  - Result preview showing what will happen when dependency is added
  - REST API endpoints for managing dependencies
  - Same-cycle validation to prevent cross-cycle dependencies
  - Comprehensive test coverage (unit and integration tests)
  - Database migration V52 for task_dependencies table

### Changed
- **Performance Optimization**: Replaced client-side filtering with server-side search
  - Before: Loaded 200+ items then filtered locally (400KB+ per dropdown)
  - After: 0-50 items loaded only when needed (0-100KB)
  - Dramatically improved performance for deployments with 1000+ scopes/tasks
- **Backlog View**: Now displays blocker badges (🔴 blocked) and blocking badges (🛡️ blocking) for all tasks
- **Task View Dialog**: Added dependency management section to the quick view dialog (eye icon) so users can add/remove dependencies without navigating away
- **Task List UX**: Blocker badge tooltip now shows actual task titles instead of just count
- **Backend**: Task DTOs now include children (subtasks) array for displaying subtask count and details

- **Configurable Risk Factor Weights**:
  - Risk calculation now uses configurable weights instead of fixed percentages
  - 4 risk factors with adjustable weights: Budget (default 25%), Bugs (default 30%), Scope (default 25%), Time (default 20%)
  - Weights must sum to 100% with real-time validation and visual feedback
  - 5 preset profiles for quick setup:
    - Balanced (25/30/25/20): Equal priority across all factors
    - Conservative (35/35/15/15): Emphasis on budget and quality
    - Aggressive (15/25/35/25): Focus on speed and scope completion
    - Quality-Focused (15/40/30/15): Maximum weight on bug severity
    - Time-Critical (20/25/20/35): Prioritize deadline pressure
  - New Organization Settings tab "Risk Weights" with slider controls and profile buttons
  - Backend refactoring: Split calculateRuleBasedRiskLevel into 4 separate factor methods
  - Each factor calculates 0-100 score, then weighted sum produces final risk
  - API endpoint: GET /api/admin/settings/risk-profiles returns all preset profiles
  - Database migration V51: Added risk_weights_json column to organization_settings table
  - RiskWeights DTO with validation: isValid() checks sum, normalize() adjusts to 100%
  - Updated PitchHealthService documentation to reflect configurable weights
  - Backend compilation verified with zero errors
  - Frontend TypeScript build successful with proper RiskProfile interface

- **Cycle Date Auto-Calculation with Role-Based Override**:
  - End dates automatically calculated from organization settings (default 6 weeks)
  - Configurable cycle length in Organization Settings (4-12 weeks supported)
  - Role-based override capability: ADMIN and PROJECT_MANAGER can set custom dates
  - Regular users (DEVELOPER, QA, PRODUCT) restricted to auto-calculated dates
  - Frontend toggle for privileged users to choose between auto or manual dates
  - Backend validation with AccessDeniedException for unauthorized overrides
  - Auto-calculation fallback to 6 weeks if configuration is invalid
  - Prevents configuration conflicts ensuring standardized planning horizons
  - Integration tests: 5 new tests covering auto-calculation and role-based access
  - Unit tests: 8 new tests for CycleService date calculation logic
  - Updated CreateCycleRequest DTO to make endDate optional
  - Documentation updates in README.md and inline code comments

- **Circuit Breaker - Shape Up Safety Valve**:
  - Automated overflow detection with configurable thresholds (default 80%, range 50-150%)
  - Real-time budget monitoring: tracks work logs against pitch appetite in hours
  - Color-coded severity indicators: blue (<80%), yellow (80-89%), orange (90-99%), red (≥100%)
  - Trigger circuit breaker mechanism: flag pitches for team discussion with reason documentation
  - Kill pitch capability: permanently cancel pitches with CIRCUIT_BREAKER status
  - Resolve circuit breaker workflow: clear flags and update status when scope is cut
  - Team notifications: dashboard alerts for all pitch stakeholders on trigger/kill events
  - CircuitBreakerDTO with 12 fields: appetite, hours spent, utilization %, overflow %, status
  - 5 REST endpoints: detect overflow, get triggered, trigger, kill, resolve
  - Integration with RiskAnalysisService: +50 risk points for circuit breaker status
  - Comprehensive help guide: `/help/circuit-breaker` with Shape Up principles
  - Frontend monitor page: `/cycles/:id/circuit-breaker` with threshold slider and action dialogs
  - Added V50 database migration for 3 new Pitch fields: isCircuitBreakerTriggered, circuitBreakerReason, circuitBreakerDate
  - Full test coverage: CircuitBreakerControllerIntegrationTest with 15 test cases

- **Anonymous Retrospective Submissions**:
  - Added `isAnonymous` boolean field to RetroItem entity for psychological safety
  - Checkbox option in retro board UI: "Post anonymously"
  - Author attribution hidden when `isAnonymous=true` (author field set to null)
  - Backend validation: CreateRetroItemRequest and RetroItemDTO updated
  - Frontend state management: per-column isAnonymous tracking
  - Database migration V49: `is_anonymous` column with index on RetroItem table
  - Updated RetrospectivesGuide.tsx with anonymous submission documentation
  - Test coverage: RetroControllerIntegrationTest with 3 anonymous-specific test cases

- **Navigation & UX Refinements**:
  - Added comprehensive project detail page with cycles list, teams, and statistics
  - Implemented search functionality across Projects, Teams, Retrospectives, and Pitch Board pages
  - Added sorting options to all list pages (by name, date, status, team, etc.)
  - Made project cards clickable to navigate to detailed project view
  - Standardized UI patterns: using full pages instead of modals for comprehensive data display
  - Enhanced user experience with consistent search and filter patterns across all list views

- **Automated Health Risk Detection**:
  - Enhanced automated health risk detection with weighted 4-factor scoring algorithm
  - Added configurable risk thresholds (30+ parameters) via Organization Settings
  - Implemented intelligent risk calculation: Budget (25%), Bugs (30%), Scope (25%), Time (20%)
  - Visual enhancements for critical items: 8px red borders, CRITICAL badges, pulse animations
  - Enhanced warning banners in Cycle Health Summary with gradient effects
  - Color-coded progress bars with badges for budget tracking
  - Added comprehensive test coverage for configurable thresholds (7 new test cases)
  
- **Configurable Risk Thresholds**:
  - **Budget Thresholds**: Warning at 80%, Overrun at 100%, Critical at 120% (customizable)
  - **Bug Count Thresholds**: Critical bugs (1/3/5), Major bugs (3/5), Open bugs (5/10/15) levels
  - **Scope Progress Thresholds**: Uphill max position, progress rate expectations, lag detection
  - **Time-based Thresholds**: Urgency at 3 days, Warning at 7 days, Concern at 14 days
  - **Schedule Variance**: Moderate gap at 15%, Significant gap at 30%
  - **Cycle Progress**: Midpoint (50%), Late phase (60%), Final quarter (75%)
  - **Stagnation Detection**: Scope stagnation (7 days), Peak stuck (5 days), No progress (7 days)
  - **Work Rate Indicators**: High hours threshold (15hrs/3days), High appetite usage (90%)
  - All thresholds customizable per organization with sensible defaults

- **Shape Up Pitch Enhancements**:
  - Added comprehensive Shape Up methodology fields to pitch creation and editing
  - Implemented 6 new fields: Problem Statement, Solution, Rabbit Holes, Risks, No-Gos, Wireframe Links
  - Created AI-powered pitch document extraction using RunPod/Mistral
  - Added automatic knowledge base indexing for pitch documents
  - Implemented 3-tab pitch creation dialog: Basic Info, Shape Up Details, Documents
  - Added document upload with drag-and-drop support during pitch creation
  - Enhanced pitch detail page with Shape Up Details card and inline editing
  - Added visual feedback for document extraction (green indicator with filename)
  - Implemented auto-tab switching after successful document extraction
  - Added support for multiple wireframe links (one per line)

- **Document Management**:
  - Added document download functionality with proper Content-Type headers
  - Implemented download endpoint: `GET /api/documents/{id}/download`
  - Added Download button in DocumentDropZone component
  - Support for downloading PDF, DOCX, DOC, TXT, and MD files
  - Files download as attachments with original filenames
  - Added document preview capability (view extracted text)
  - Display document metadata: filename, file size, file type, extraction status

### Changed
- **Database Schema**:
  - Added V48 migration for Shape Up fields (problem_statement, solution, rabbit_holes, risks, no_gos, wireframe_links)
  - Fixed H2 database compatibility in partial index syntax
  - Updated Pitch entity, PitchDTO, and CreatePitchRequest with Shape Up fields

- **AI Configuration**:

  - Enhanced PitchShapingExtractorService with structured JSON extraction prompt
  - Improved knowledge base integration for pitch documents

- **UX Improvements**:
  - Fixed Content-Type header for multipart/form-data uploads
  - Improved visual feedback with "Document Extracted" indicator
  - Auto-navigation to Shape Up tab after extraction
  - Enhanced wireframe links input with clear placeholder and helper text
  - Better document visibility with badges for extraction and indexing status

### Fixed

- **H2 SQL Syntax**: Removed WHERE clause from partial index for H2 compatibility
- **Document Visibility**: Added extractedDocumentName state to show uploaded document name
- **Tab Navigation**: Implemented activeTab state for controlled tab switching

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.1] - 2026-01-14

### Added
- **Branding & Icon System**:
  - Added high-quality application icon (2048x2048) for consistent branding
  - Generated multi-resolution favicons (16x16, 32x32, 48x48, 180x180, 192x192, 512x512)
  - Created PWA-ready web manifest with proper icon configuration
  - Updated logo across application: sidebar, login page, landing page, and README
  - Replaced text-based logos ("SF", "SU") with actual icon image

### Fixed
- **Build System**:
  - Fixed Lombok annotation processing compatibility with Java 21
  - Added explicit annotation processor path in Maven compiler plugin
  - Updated Lombok to version 1.18.36 for better Java 21 support
  - Resolved compilation errors with entity getter/setter methods
  
- **Database Migration**:
  - Fixed CyclePhase enum mismatch: changed "EXECUTION" to "BUILD" in V47 migration
  - Resolved Flyway checksum validation errors
  - Fixed Java/Maven version compatibility issues (enforced Java 17)

## [0.2.0] - 2026-01-14

### Fixed
  - Fixed H2 compatibility issues in screenshot seed data migration
  - Changed `INTERVAL '42 days'` to H2-compatible `DATEADD('DAY', 42, CURRENT_DATE)`
  - Corrected pitches table INSERT to use `appetite_days` instead of non-existent `appetite` column
  - Fixed PitchStatus enum value from `ACCEPTED` to `IN_PROGRESS`
  - Fixed FlywayRepair class package declaration

- **Authentication Token Storage**:
  - Fixed GitHub and Slack services using incorrect localStorage key
  - Changed from `'token'` to `'shipflow_token'` to match AuthContext
  - Resolved 401 Unauthorized errors on `/api/github/repositories` and `/api/slack/configurations`
  - Services now correctly retrieve JWT tokens for authenticated requests

### Removed
- **Navigation Cleanup**:
  - Removed "Help" menu section from sidebar navigation (keyboard shortcuts moved to future release)
  - Removed "Seed Hill Chart Data" button from User Management page (development-only feature)

### Improved
- **Navigation Organization**:
  - Reorganized Administration section with better grouping
  - Created "User & Access" collapsible group containing User Management and Permissions
  - Separated Organization Settings as standalone item for better visibility
  - Maintained Integrations group (Slack, GitHub) under Administration

### Added

- **Help & Guides System**:
  - **Help Center Hub** (`/help`):
    - Comprehensive help center landing page with 8 guide categories
    - Guide cards with icons, descriptions, and color coding
    - Quick access links with keyboard shortcuts displayed
    - Responsive grid layout for guide navigation
  - **Interactive Guide Pages**:
    - **Getting Started Guide** (`/help/getting-started`): Introduction to ShipFlow, navigation, and core concepts
    - **Hill Charts Guide** (`/help/hill-charts`): Master hill chart visualization for progress tracking
    - **Betting Meeting Guide** (`/help/betting-meeting`): Step-by-step guide for effective betting meetings
    - **AI Risk Advisor Guide** (`/help/ai-risk-advisor`): Leverage AI-powered risk assessments
    - **Cycle Setup Guide** (`/help/cycle-setup`): Complete cycle creation and management walkthrough
    - **QA & Testing Guide** (`/help/qa-testing`): Manage test cases, AI test generation, and bug tracking
    - **Retrospectives Guide** (`/help/retrospectives`): Run effective retrospectives
    - **Reports & Dashboards Guide** (`/help/reports`): Visualize metrics and create custom dashboards
  - **Guide Features**:
    - Rich content with step-by-step instructions
    - Screenshots embedded in guides (15+ screenshots in `/public/guides/`)
    - Cross-linked related guides for easy navigation
    - Back to help center navigation on all guide pages
    - Organized sections with clear headings and best practices
  - **Navigation Integration**:
    - Added "Help & Guides" menu item in sidebar with BookOpen icon
    - Accessible via `/help` route
    - Integrated with application layout and navigation system

- **Keyboard Shortcuts System**:
  - **KeyboardShortcutsHelp Component**:
    - Modal dialog displaying all available keyboard shortcuts
    - Organized shortcut list with descriptions
    - Visual key chips showing keyboard combinations (⌘, Ctrl, Alt, ⇧)
    - Accessible interface with ARIA labels
    - Responsive hover effects for better UX
  - **useKeyboardShortcuts Hook**:
    - Custom React hook for managing keyboard shortcuts
    - Support for meta, ctrl, alt, and shift modifiers
    - Global event listener with cleanup
    - TypeScript interface for shortcut definitions
  - **Available Shortcuts**:
    - Quick navigation to common pages
    - Dashboard and cycle management shortcuts
    - Integration with QuickLinks component

- **Quick Links Component**:
  - Quick access widget for frequent actions
  - 8 pre-configured quick links with icons and colors:
    - New Cycle (⇧N)
    - Log Work (⇧W)
    - View Pitches (P)
    - Tasks
    - Run Reports
    - Current Cycle
    - QA Dashboard
    - Hill Chart
  - Keyboard shortcut hints displayed on each link
  - Smooth animations with Framer Motion
  - Tooltip descriptions for better UX
  - Color-coded cards for visual distinction

- **Slack Integration**:
  - **Backend Features**:
    - Database tables for Slack configuration, channel settings, and notification history
    - `SlackConfiguration` entity for workspace-level settings
    - `SlackChannelConfig` entity for channel-specific notification preferences
    - `SlackNotificationHistory` entity for audit logging
    - `SlackIntegrationService` for sending notifications and managing configuration
    - `SlackIntegrationController` with REST endpoints for configuration management
    - Integration with `DashboardNotificationService` for automatic Slack notifications
    - Support for 8 notification types: task assigned, task completed, task blocked, pitch shaped, cycle started, cycle cooldown, betting completed, sprint started
    - Channel-specific notification filtering
    - Test notification functionality
    - RestTemplate HTTP client configuration
  - **Frontend Features**:
    - Slack Integration settings page at `/slack`
    - Workspace configuration UI with webhook URL management
    - Channel-specific notification preference management
    - Test notification sending interface
    - Notification history viewing
    - Navigation integration with MessageSquare icon
    - TypeScript service with full type definitions
  - **Documentation**:
    - Comprehensive Slack integration guide
    - Setup instructions with screenshots
    - API documentation
    - Troubleshooting section
    - Security considerations
  - **Testing**:
    - Controller integration tests with MockMvc
    - Service unit tests with Mockito
    - 90%+ code coverage for Slack integration components
- **Role-Based Access Control (RBAC) System**:
  - **Backend Features**:
    - New `Permission` entity linking roles to resource actions
    - `ResourceType` enum: CYCLE, PITCH, BUG, REPORT, PROJECT, TEAM, USER, DASHBOARD, AI_FEATURES, SYSTEM
    - `PermissionType` enum: CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, APPROVE
    - `PermissionService` for checking and managing permissions
    - `PermissionRepository` with optimized queries for permission lookups
    - `@RequirePermission` annotation for declarative permission checking
    - `PermissionAspect` AOP component for enforcing permissions
    - V44 migration: Creates permissions table and loads default role permissions
    - Default permissions for all roles (ADMIN, PROJECT_MANAGER, PRODUCT, DEVELOPER, QA)
    - Configuration property `app.security.rbac.enabled` to enable/disable RBAC
  - **Frontend Features**:
    - **Permission Management UI** (`/permissions`): Comprehensive interface for viewing and understanding RBAC
    - **Permission Matrix View**: Visual grid showing all roles vs. resources with abbreviated permissions
    - **Role Details View**: Detailed permissions for each role organized by resource type
    - **My Permissions View**: Personal permission dashboard for current user
    - **Search & Filter**: Search resources and filter by resource type
    - **Color-Coded Roles**: Visual distinction between different roles (ADMIN=Red, PM=Blue, etc.)
    - **Permission Service**: TypeScript service for frontend permission operations
    - **Responsive Design**: Mobile-friendly permission management interface
    - **Access Control**: Non-admin users can only view their own permissions
  - **API Endpoints**:
    - `GET /api/permissions/current-user`: Get current user's permissions
    - `GET /api/permissions/role/{role}`: Get permissions for a role (admin only)
    - `GET /api/permissions/resource/{resourceType}`: Get permissions for a resource (admin only)
  - **Documentation**:
    - Comprehensive RBAC_GUIDE.md with architecture, usage, and best practices
    - PERMISSION_MANAGEMENT_UI_GUIDE.md for frontend UI documentation
    - Permission matrix for all roles and resources
    - Migration guide from legacy system
    - Future enhancement roadmap
  - **Testing**:
    - 20+ unit tests for PermissionService (100% coverage)
    - Graceful degradation when permissions table doesn't exist (test mode)
    - Backward compatibility with @PreAuthorize annotations
  - **Security**:
    - Fine-grained permissions per resource type and action
    - Layered security with both Spring Security and RBAC checks
    - Protection for all major controllers (Cycle, Pitch, User, etc.)
    - Extensible design for future custom permissions

### Fixed
- **H2 Database Compatibility for GitHub Integration**:
  - Fixed V43 migration SQL syntax for H2 database compatibility
  - Changed `TEXT` column type to `CLOB` in migration files
  - Changed `UNIQUE KEY` syntax to `CONSTRAINT ... UNIQUE` syntax
  - Removed incompatible `INDEX` creation statements
  - Updated GitHub entity annotations to use `@Lob` instead of `columnDefinition = "TEXT"`
  - Entities updated: `GitHubCommit`, `GitHubPullRequest`, `GitHubWebhookEvent`
  - Migration V43 now successfully executes on H2 in-memory database

### Added
- **Development Environment Improvements**:
  - Added `.env` file configuration support for AI providers
  - Set Ollama as default/recommended AI provider for local development
  - Updated documentation to prioritize Ollama over RunPod for easier setup
  - No API keys required for local development with Ollama

- **Organization Settings - Colors & Bug Configuration**:
  - **Backend Features**:
    - New `colors_json` TEXT column for appetite/actual hour color customization
    - New `bug_statuses_json` TEXT column for bug workflow statuses
    - New `severity_levels_json` TEXT column for bug priority levels
    - V41 migration: Complete organization settings table with new columns
    - V42 migration: Backward compatibility for existing installations
    - JSON serialization/deserialization in `OrganizationSettingsService`
    - Default configurations: 4 colors, 5 bug statuses, 4 severity levels
    - Explicit `@Column(name="snake_case")` annotations for H2 database compatibility
  - **Frontend Features**:
    - New "Colors" tab in Organization Settings with HTML5 color pickers
    - New "Bug Config" tab displaying bug statuses and severity levels
    - `ColorSettings` interface: appetiteHours, actualHours, overBudget, underBudget
    - `BugStatusConfig` interface: name, description, color, isActive, order, isClosed
    - `SeverityLevelConfig` interface: name, description, color, isActive, order, priority
    - Real-time color preview with hex codes
  - **Default Configurations**:
    - **Colors**: `appetiteHours` (Blue #3B82F6), `actualHours` (Green #10B981), `overBudget` (Red #EF4444), `underBudget` (Green #22C55E)
    - **Bug Statuses**: NEW, IN_PROGRESS, FIXED, VERIFIED, WONT_FIX
    - **Severity Levels**: CRITICAL, HIGH, MEDIUM, LOW
  - **Database Compatibility**:
    - Fixed H2 column naming issue with explicit JPA annotations
    - Verified with 34/34 tests passing
    - Successfully applied all 36 migrations (v1 → v42)

- **Custom Dashboards - Smart Context Filter Toggle**:
  - **Backend Features**:
    - New `user_context_filter` boolean column on `custom_dashboards` table (V39 migration)
    - `CustomDashboardService.toggleUserContextFilter()` method for toggling filter state
    - New endpoint: `PUT /api/dashboards/custom/{id}/toggle-context-filter`
    - Intelligent defaults: Developer/QA templates default to personal context, Executive/Manager templates default to organization-wide
    - Stateful toggle persisted per dashboard in database
  - **Frontend Features**:
    - Toggle switch with Filter icon in dashboard header
    - Shows "Personal" vs "Organization-Wide" label based on state
    - Automatic widget data refresh when toggled
    - Success messages indicating current filter mode
  - **Data Filtering**:
    - **Personal Context Mode**: Filters tasks to user's assignments, teams to user's memberships
    - **Organization-Wide Mode**: Shows all data across the organization
    - Client-side filtering using current user from localStorage
    - Supports TASK_LIST, TEAM_STATS, CYCLE_SUMMARY, PITCH_LIST data sources
  - **User Experience**:
    - Developers/QA see their own data by default with option to view organization-wide
    - Executives/Managers see organization-wide data by default with option to view personal context
    - Toggle state persists across sessions per dashboard

- **Dashboard Widget Improvements**:
  - **Table Widget Enhancements**:
    - Fixed sticky header scrolling issue with proper z-index layering
    - Added solid background to prevent data overlap during scroll
    - Improved header visibility with `bg-background` and `z-10` styling
  - **QA Dashboard Template Fixes** (V40 migration):
    - Fixed invalid widget filters (removed non-existent "QA" category and "overdue" field)
    - Updated widget configurations:
      - "Blocked Tasks": Shows tasks with `status = BLOCKED`
      - "High Priority Tasks": Shows tasks with `priority = HIGH`
      - "In Progress Tasks": Shows tasks with `status = IN_PROGRESS`
      - "Recently Completed": Shows tasks with `status = DONE`
    - Increased page size from 5 to 10 for better data visibility
    - All widgets now display data correctly in both personal and organization-wide modes

- **Reports Module - Comprehensive Analytics and Reporting**:
  - **Backend Features**:
    - `EnhancedCycleReportDTO` with comprehensive metrics including risk distribution
    - `RiskDistributionDTO` for risk analysis aggregation (LOW/MEDIUM/HIGH/CRITICAL counts)
    - `ReportService.getEnhancedCycleReport()` method for complete cycle analytics
    - `ReportService.calculateRiskDistribution()` integrating with RiskAnalysisService
    - New endpoint: `GET /api/reports/cycle/{cycleId}/enhanced` for enhanced reports
    - Pitch metrics: total, completed, in-progress, not-started counts
    - Hours analysis: appetite vs actual with variance calculations
    - Efficiency ratios: (actual/appetite) × 100
    - Team member statistics: total, average, max, min hours per member
    - Top performers identification (members with ≥6h/day avg and above-average hours)
    - Over-budget pitches flagging
    - Out-of-scope work (tasks) tracking with estimate vs actual hours
  - **Risk Distribution Analysis**:
    - LOW/MEDIUM/HIGH/CRITICAL risk level counts
    - Average, max, and min risk scores across all pitches
    - Integration with fast rule-based risk analysis for performance
    - Risk score calculations (0-100 scale)
  - **Frontend Features**:
    - Enhanced Reports page with comprehensive UI overhaul
    - Risk Distribution pie chart with color-coded segments
    - Variance Analysis section showing over/under budget metrics
    - Top Performers highlight cards
    - Over-Budget Pitches warning section
    - Appetite vs Actual hours bar chart (side-by-side comparison)
    - Pitch Status Distribution pie chart
    - Hours by Team Member horizontal bar chart
    - Summary statistics cards with 6 key metrics
    - Detailed pitch reports table with variance indicators
    - Member work summary table with role badges
    - Responsive layout optimized for all screen sizes
  - **Export Functionality**:
    - PDF export with all enhanced metrics (backward compatible)
    - CSV export with all enhanced metrics (backward compatible)
    - Automatic filenames: `cycle_report_{cycleId}.pdf/csv`
  - **Performance Optimizations**:
    - Uses fast rule-based risk analysis (not AI) for quick report generation
    - Leverages AICacheService for risk calculation caching
    - Batch processing of pitch and member calculations
    - Optimized database queries with minimal round trips
  - **Test Coverage**:
    - `ReportServiceTest`: 95%+ coverage with comprehensive unit tests
    - `ReportControllerIntegrationTest`: 100% endpoint coverage
    - Tests cover: risk distribution, variance analysis, top performers, empty cycles
    - Integration tests validate JSON responses and export functionality
  - **Documentation**:
    - API examples with sample responses
    - Usage guide for stakeholders and developers
    - Architecture documentation
    - Performance and security considerations

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

  - **Test Coverage**:
    - `CustomDashboardServiceTest`: Added 5 new tests for user context filter toggle functionality
    - Tests cover: toggle from false to true, toggle from true to false, toggle from null, not found, unauthorized access
    - Total dashboard service test coverage: 13 tests (8 scope tests + 5 context filter tests), 100% pass rate
    - All integration tests passing with comprehensive coverage across modules
  
  - **Documentation**:
    - Documented smart context filter toggle usage and behavior
    - Added filter operators reference and best practices
    - Included API endpoints reference for developers
    - Migration history (V38, V39, V40) documented

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
  - shadcn/ui component library (Radix UI primitives with Tailwind CSS)
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
