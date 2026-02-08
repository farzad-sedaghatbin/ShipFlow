# <img src="frontend/public/icon.png" alt="ShipFlow Logo" width="32" height="32" style="vertical-align: middle;"> ShipFlow

A modern project management application implementing the [Shape Up](https://basecamp.com/shapeup) methodology by Basecamp.

🌐 **Live Demo**: [shipflow.dev](https://shipflow.dev)


## 📸 Screenshots & Demo

<img src="screenshots/demo.webp" alt="ShipFlow Demo" width="100%" />

| Dashboard | Hill Charts | Pitch Board |
|-----------|-------------|-------------|
| ![Dashboard](screenshots/dashboard.png) | ![Hill Chart](screenshots/hill-chart.png) | ![Pitch Board](screenshots/pitch-board.png) |

## ✨ Features

- **Dual Project Modes**: Flexible support for different project methodologies
  - **Shape Up Mode**: 6-week cycle methodology with pitches, betting, hill charts
  - **Kanban Mode**: Continuous flow with board-first visualization
  - Automatic UI adaptation based on project type (cycles hidden for Kanban)
  - Default "Continuous Flow" cycle created automatically for Kanban projects
  - Pitch and scope fields hidden in Kanban projects (Shape Up concepts)
- **Cycles**: 6-week development cycles with betting table (Shape Up projects)
  - **Auto-Calculated Cycle Dates**: End dates automatically calculated from organization settings
    - Default 6-week cycles aligned with Shape Up methodology
    - Configurable cycle length in Organization Settings (4-12 weeks)
    - Role-based override: ADMIN and PROJECT_MANAGER can set custom cycle lengths
    - Regular users (DEVELOPER, QA, PRODUCT) use auto-calculated dates
    - Prevents configuration conflicts and ensures standardized planning horizons
- **Pitches**: Shape work with appetite, problem definition, and solution
  - **Shape Up Methodology Support**: Comprehensive pitch creation with all Shape Up elements
    - Problem Statement, Solution, Rabbit Holes, Risks, No-Gos, Wireframe Links
    - AI-powered pitch document extraction from PDF/DOCX/TXT files
    - Automatic knowledge base indexing for Q&A
    - Document upload, preview, and download capabilities
    - Inline editing of Shape Up fields on pitch detail page
- **Hill Charts**: Visual progress tracking with drag-and-drop dots
- **Tasks**: Independent work management during cycles
  - **Traceability**: Optional links to pitches and scopes for improved reporting
    - Tasks can optionally link to specific pitch and scope (hill chart point)
    - Supports technical debt and improvement work (no pitch required)
    - Server-side search with debouncing for scalable scope/task selection
    - Minimum 3-character search prevents performance issues with large datasets
  - **Task Dependencies**: Lightweight dependency tracking to identify blockers
    - Three dependency types: BLOCKS, DEPENDS_ON, RELATED_TO
    - Automatic circular dependency detection using depth-first search
    - Visual blocker indicators in task lists and detail pages
    - Same-cycle validation for dependency relationships
    - Clean UI for adding/removing dependencies
    - See [Task Dependencies Guide](TASK_DEPENDENCIES.md) for details
  - **Soft Delete**: Safe deletion with recovery options
    - Records are marked as deleted, not permanently removed
    - Complete audit trail with deletion timestamp and user tracking
    - Data can be restored if needed while maintaining referential integrity
    - Available for pitches, tasks, and test cases with role-based permissions
- **Document Management**: Upload, preview, and download project documents
  - Support for PDF, DOCX, DOC, TXT, and MD files
  - Text extraction from uploaded documents
  - Document preview with extracted text
  - Download with original filenames and proper Content-Type headers
  - Automatic indexing for AI-powered Q&A
- **Organization Settings**: Centralized configuration management
  - Cycle length and risk threshold customization
  - Color schemes for appetite/actual hours visualization (4 configurable colors)
  - Bug workflow statuses (5 predefined states: NEW, IN_PROGRESS, FIXED, VERIFIED, WONT_FIX)
  - Severity levels for bug prioritization (CRITICAL, HIGH, MEDIUM, LOW)
  - AI features toggle and notification preferences
- **Reports**: Comprehensive analytics and reporting with export capabilities
  - Pitch metrics (total, completed, in-progress, appetite vs actual hours)
  - Risk distribution analysis (LOW, MEDIUM, HIGH, CRITICAL)
  - Team member statistics and performance tracking
  - Variance analysis and efficiency ratios
  - Out-of-scope work (tasks) tracking with traceability
  - PDF and CSV export functionality
- **QA & Testing**: Bug tracking and test case management
  - **Bug Reports**: Comprehensive bug tracking with severity and status workflows
    - **Direct Project Association**: Bugs can be created at project level (ideal for Kanban)
    - Optional traceability to cycles, pitches, scopes, and related tasks
    - Auto-derives project from cycle/pitch when not explicitly set
    - Server-side search for finding related scopes/tasks (min 3 chars, 300ms debounce)
    - Context-aware dropdowns (pitch → scopes, cycle → tasks)
  - **Test Cases**: Structured test case management
    - Optional links to scopes and related tasks for better coverage tracking
    - Debounced search prevents performance issues with large test suites
    - Multiple test types: FUNCTIONAL, INTEGRATION, UNIT, E2E, REGRESSION, SMOKE, PERFORMANCE, SECURITY
- **Help & Guides**: Built-in comprehensive documentation and interactive tour
  - **Interactive Tour**: Step-by-step walkthrough for new users
  - **Rich Guides**: Detailed guides for Getting Started, Cycle Setup, Betting Meetings, Hill Charts, and AI Risk Advisor
  - **Context-Aware**: Access relevant guides directly from related pages
- **Comments & Collaboration**: Full commenting system for tasks and bug reports
  - **@Mentions**: Type `@` to mention users with autocomplete suggestions
  - **Mention Notifications**: Mentioned users receive in-app and Slack notifications
  - **Emoji Reactions**: 8 emoji reactions (👍, 👎, ❤️, 😄, 😮, 😢, 🚀, 👀) with toggle behavior
  - **CRUD Operations**: Create, edit, delete comments with permission checks
  - **Edit Tracking**: Comments show "edited" badge when modified
  - **Author Controls**: Only authors can edit; authors and admins can delete
- **Retrospectives**: Team retros with voting and merging
  - **Anonymous Submissions**: Post feedback anonymously for psychological safety
  - Checkbox option to hide author attribution on sensitive items
  - Standard columns: Went Well, Needs Improvement, Action Items
  - Real-time collaboration and voting
  - **Flexible Action Conversion (v0.5)**: Transform retro insights into actionable work
    - **Convert to Pitch**: Create draft pitches for the next betting table
    - **Convert to Tasks**: Generate tasks for immediate work
    - **Mark as Acted On**: Track completion without creating new items
    - Batch processing of multiple retro items with customizable titles and notes
    - Automatic status tracking with notes and timestamps
  - **Action Tracking (v0.5)**: Track whether teams act on retrospective insights
    - "Did we act on this?" checkbox for Action Items (ACTIONS column)
    - Notes and attribution for action follow-through
    - Follow-through rate calculation per retrospective
  - **Tag-Based Linking**: Connect retro items to future pitches via shared tags
    - Link learnings to future bets
    - Cross-cycle pattern detection
- **Cycle Signals (v0.5)**: Decision support from historical patterns
  - **Appetite Accuracy**: Track how well estimates match reality over time
    - Per-cycle ratio analysis with trend detection
    - Contextual recommendations for estimate calibration
  - **Shaping Quality**: Detect over-shaping or under-shaping patterns
    - Analysis of uncertainty scores and rabbit holes
    - Quality classification with improvement guidance
  - **Risk Prediction**: Measure risk forecast accuracy
    - Compare predicted vs actual outcomes
    - Correlation strength indicators
  - **Retro Follow-Through**: Surface action item completion rates
    - Per-retrospective and cross-cycle analysis
    - Pending action visibility
  - **Health Score**: Combined signal health (0-100) for process overview
- **Narrative Summaries (v0.5)**: AI-generated cycle narratives
  - **What We Bet On**: Committed pitches and appetite allocations
  - **What Shipped**: Completed work with key outcomes
  - **What We Cut**: Descoped items and rationale
  - **Surprises**: Unexpected discoveries and lessons learned
  - Template fallback when AI is unavailable
  - Markdown export for stakeholder communication
- **Meetings**: Comprehensive meeting management with customizable types
  - **Configurable Meeting Types**: Manage 7+ meeting types (SHAPING, BETTING, KICKOFF, STANDUP, DEMO, RETROSPECTIVE, HILL_CHART_REVIEW)
  - **DOR/DOD Checklists**: Definition of Ready (DOR) and Definition of Done (DOD) checklist items per meeting type
  - **View Mode**: Click meeting type badge to view read-only summary showing only completed checklist items
  - **Edit Mode**: Full editing with all DOR/DOD items visible and editable
  - **Smart Filtering**: Deleted checklist items automatically filtered from new meetings
  - **Action Items**: Track meeting decisions with assignees, due dates, and status tracking
  - **Meeting Documents**: Attach and manage meeting-related documents
  - **Meeting History**: Full audit trail of all meeting changes
- **Entity Change History (Audit Trail)**: Complete change tracking with Hibernate Envers
  - **Full Audit Trail**: Track all changes to Tasks, Bug Reports, Pitches, and Test Cases
  - **Selective Field Auditing**: Status, priority, severity, assignee, title, description, and more
  - **User Attribution**: Every change records who made it and when
  - **Jira-Style Activity Timeline**: Embedded activity view showing all changes inline (no popup required)
    - Bug View: Tabbed interface with Details, Activity, and Comments tabs
    - Task Detail Page: Activity timeline card showing complete change history
  - **Visual Timeline**: Timeline with colored dots (green=created, blue=modified, red=deleted)
  - **Relative Time Display**: Shows "5 minutes ago", "2 hours ago" for recent changes
  - **Field Change Display**: Color-coded old → new value comparisons with strikethrough
  - **Internationalization**: Full i18n support (English/Persian) for history labels
- **Circuit Breaker**: Shape Up's fixed-time safety valve for overflow detection
  - **Automated Overflow Detection**: Real-time budget monitoring with configurable thresholds (50-150%)
  - **Color-Coded Severity**: Visual indicators (blue/yellow/orange/red) based on appetite utilization
  - **Trigger Mechanism**: Flag pitches for team discussion when scope expansion occurs
  - **Kill Pitch Capability**: Cancel pitches that can't meet appetite constraints
  - **Resolve Workflow**: Clear circuit breaker flags when scope is successfully cut
  - **Team Notifications**: Automatic dashboard alerts for all pitch stakeholders
  - Integrated help guide explaining Shape Up's fixed-time, variable-scope principle
- **Health Overview**: Automated risk detection and health monitoring
  - **Weighted Risk Algorithm**: 4-factor scoring with configurable weights
    - Budget utilization (default 25%): Tracks appetite consumption vs timeline
    - Bug severity (default 30%): Monitors critical/major/open bug counts
    - Scope progress (default 25%): Analyzes hill chart position and movement
    - Time pressure (default 20%): Evaluates days remaining and urgency
  - **Configurable Risk Weights**: Customize factor importance via Organization Settings
    - Adjust weights for budget, bugs, scope, and time factors (must sum to 100%)
    - Preset profiles for quick setup: Balanced, Conservative, Aggressive, Quality-Focused, Time-Critical
    - Real-time validation with visual feedback (sum indicator and warnings)
    - Slider controls for intuitive adjustment
  - **Configurable Thresholds**: 30+ customizable parameters via Organization Settings
    - Budget thresholds (warning, overrun, critical levels)
    - Bug count thresholds (by severity and total open bugs)
    - Scope progress expectations (position and lag thresholds)
    - Time-based urgency levels (urgent, warning, concern)
    - Schedule variance detection (moderate and significant gaps)
    - Cycle progress milestones (midpoint, late phase, final quarter)
    - Stagnation detection (scope movement, peak stuck, progress gaps)
    - Work rate indicators (recent hours, appetite usage)
  - **Risk Classification**: LOW (0-24), MEDIUM (25-49), HIGH (50-69), CRITICAL (70+)
  - **Visual Indicators**: 
    - Critical items: 8px red borders, CRITICAL badges with stripe animation, pulse effects
    - Enhanced progress bars with color-coded budget tracking
    - Gradient warning banners for at-risk items
  - Risk trend indicators (IMPROVING, STABLE, WORSENING)
  - Automated status detection without manual assignment
  - All thresholds and weights customizable per organization with sensible defaults
- **AI-Powered Q&A**: Enhanced RAG-based knowledge retrieval from project documents
  - **Pluggable Vector Store Architecture**: Choose your vector database via configuration
    - **Qdrant** (production recommended): High-performance vector DB with filtering & clustering
    - **In-Memory**: For development/testing (no external dependencies)
    - **ChromaDB**: Alternative option for simpler deployments
  - **Pluggable LLM Architecture**: Choose your AI provider via simple configuration
    - **Ollama** (local/self-hosted): Privacy-first, no API costs, runs on your hardware
    - **OpenAI ChatGPT**: Production-grade GPT-4o/GPT-4o-mini for high-quality responses
    - **RunPod** (cloud GPU): Scalable serverless GPU compute with pay-per-use pricing
  - Easy provider switching via environment variables (no code changes required)
  - Extensible plugin system - add new providers by implementing `VectorStoreProvider` or `LLMProvider` interfaces
  - Smart relevance filtering (0.70 threshold)
  - Source citation tracking
  - RAG evaluation metrics (faithfulness, relevance)
  - Semantic caching for faster responses
  - **Async AI Advisor**: Non-blocking AI analysis with cache-first optimization
    - Cache-first pattern: Returns instantly if result is already cached
    - Job-based async execution: Long-running AI analysis runs in background
    - Polling API: Frontend polls for completion with exponential backoff
    - Dedicated thread pool: Prevents AI operations from blocking main threads
- **QA Test Case Generation**: AI-assisted test case generation with validation
  - Works with all supported LLM providers (Ollama, OpenAI, RunPod)
  - Test type-specific prompts (SMOKE, FUNCTIONAL, REGRESSION, INTEGRATION, E2E)
  - Automated quality validation
  - Historical test pattern learning
  - Completeness scoring (0-100)
- **GitHub Integration**: Seamless integration with GitHub repositories
  - **Two Integration Methods**:
    - **GitHub App** (Recommended): Organization-wide OAuth consent for bulk access to 50+ repos
    - **Manual Registration**: Per-repository setup for smaller projects
  - **GitHub App Benefits**:
    - Single authorization grants access to ALL organization repositories
    - Automatic webhook configuration for all repositories
    - New repositories automatically tracked (if "all" selected)
    - No per-repository webhook setup required
  - Auto-link commits and pull requests to tasks and pitches
  - Auto-close tasks when PRs are merged with closing keywords
  - Real-time webhook updates
  - Visual GitHub activity timeline on task/pitch pages
  - Support for multiple repositories
  - See [GitHub Integration Guide](GITHUB_INTEGRATION_GUIDE.md) for setup
- **Slack Integration**: Real-time team notifications
  - Workspace and channel-specific configuration
  - 8 notification types (tasks, cycles, pitches, betting)
  - Granular notification preferences per channel
  - Test notification functionality
  - Complete audit trail of sent notifications
  - Role-based access control (ADMIN/MANAGER only)
- **Microsoft Teams Integration**: Comprehensive Teams notification support
  - **Multiple Flow Types**: Traditional webhooks, Power Automate (post to channel), Power Automate (create thread)
  - **Smart Detection**: Automatic flow type detection based on webhook URL format
  - **Optimized Payloads**: Different message formats optimized for each integration method
  - **Channel-Specific Configuration**: Per-channel flow type and notification preferences
  - **Enhanced Setup Guide**: Dual-path instructions for webhook vs Power Automate setup
  - **Test Functionality**: Built-in test notifications with detailed error handling
  - **Adaptive Card Support**: Rich formatting for both traditional and Power Automate flows
  - **Role-based Access Control**: ADMIN/MANAGER only configuration

## 🔀 How ShipFlow Compares

ShipFlow is the **only project management tool** built specifically for the [Shape Up](https://basecamp.com/shapeup) methodology:

| Feature | ShipFlow | Linear | Asana | Monday.com | Jira | Basecamp |
|---------|----------|--------|-------|------------|------|----------|
| **Native Shape Up** | ✅ | ❌ | ❌ | ❌ | ❌ | Partial |
| **Kanban Mode** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Dual Mode (Shape Up + Kanban)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **6-Week Cycles** | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Hill Charts** | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Betting Table** | ✅ | ❌ | ❌ | ❌ | ❌ | Partial |
| **Circuit Breaker** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI Q&A (RAG)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI Test Generation** | ✅ | ❌ | ❌ | Partial | ❌ | ❌ |
| **GitHub Integration** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Internationalization** | ✅ | Partial | Partial | ✅ | ✅ | Partial |
| **RTL Language Support** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Self-Hosted** | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Open Source** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

**Why Choose ShipFlow?**
- **Purpose-Built**: Designed from the ground up for Shape Up—no customization needed
- **Fixed-Time, Variable-Scope**: Circuit breaker enforces appetite constraints and prevents scope creep
- **Visual Progress**: Hill charts provide intuitive progress visibility (figuring it out → making it happen)
- **AI-Powered**: Pluggable LLM architecture with provider flexibility
  - **Local AI (Ollama)**: Privacy-first, no API costs, perfect for local development or self-hosted deployments
  - **Cloud AI (OpenAI)**: Production-grade GPT-4o/GPT-4o-mini for complex reasoning and high-quality responses
  - **Serverless GPU (RunPod)**: Pay-per-use cloud GPU compute for scalable deployments
  - RAG-based document Q&A and automated test case generation work with all providers
  - Switch providers via configuration—no code changes needed
- **Complete Control**: Self-hosted with full data ownership

[→ View Full Comparison](/compare)

## ♿ Accessibility

ShipFlow is committed to **WCAG 2.1 AA compliance** with a current score of **B+ (88/100)**:

### ✅ Strengths
- **Keyboard Navigation**: Full keyboard support (Tab, Arrow keys, Enter, Escape) with logical tab order
- **Screen Reader Support**: ARIA labels on interactive elements, semantic HTML structure (`<nav>`, `<main>`, `<h1>`-`<h6>`)
- **Focus Indicators**: Visible 2px focus outline on all interactive elements exceeding WCAG requirements
- **Color Contrast**: Primary colors meet 7:1 contrast ratio (exceeds WCAG AA 4.5:1 requirement)
- **Form Accessibility**: 90%+ of inputs properly associated with `<label>` elements
- **Status Communication**: Status badges include both color and text labels
- **Skip Links**: Skip to main content for keyboard users
- **Reduced Motion**: Respects `prefers-reduced-motion` preference

### 🔧 Recent Improvements
- ✅ Added `aria-label` attributes to icon-only buttons (Edit, Delete, View cycles, Archive)
- ✅ Implemented proper labels for search inputs (visible or screen-reader only)
- ✅ Enhanced color picker accessibility with `role="radiogroup"` and `aria-checked`
- ✅ Touch-friendly 44px × 44px minimum button sizes for mobile
- ✅ Responsive navigation with hamburger menu for small screens

### 📋 Known Limitations
- Some icon-only buttons across additional pages may still need `aria-label` attributes
- Complex data tables could benefit from additional ARIA relationships
- Target: **WCAG 2.1 AAA compliance (90%+)** in future releases

## 📱 Mobile Responsive

ShipFlow is fully responsive and works on all device sizes:

- **Adaptive Layouts**: Headers and navigation stack vertically on mobile
- **Touch-Friendly**: All interactive elements meet **44px × 44px** minimum touch target size
- **Collapsible Sidebar**: Hamburger menu with drawer navigation on mobile (< 1024px), persistent sidebar on desktop
- **Responsive Hill Charts**: Touch-enabled drag-and-drop with dynamic canvas sizing for mobile viewports
- **Responsive Tables**: Horizontal scroll for data-heavy views
- **Optimized Forms**: Full-width inputs and filters on small screens
- **Mobile Breakpoints**: Optimized for 375px (iPhone SE), 414px (standard phones), and all tablet sizes

## 🌍 Internationalization (i18n) & RTL Support

ShipFlow provides comprehensive multilingual support with full RTL (Right-to-Left) layout capabilities:

### Supported Languages
- **English** (en) - 4,800+ translation keys
- **Farsi/Persian** (فارسی) - 3,650+ translation keys with full RTL layout

### RTL Features
- **Automatic Direction Switching**: Layout direction changes automatically based on language selection
- **Logical CSS Properties**: Uses Tailwind's logical properties (me-, ms-, start-, end-) for proper RTL rendering
- **Bidirectional Grids**: React Grid Layout configured for RTL with dynamic width calculation
- **Mirrored Components**: Navigation, forms, tables, and charts properly mirrored in RTL mode
- **RTL-Aware Icons**: Directional icons (arrows, chevrons) automatically flip for RTL languages
- **Mixed Content Support**: Handles LTR content (e.g., code, URLs) within RTL documents

### Language Switching
- **Persistent Preference**: Language choice saved to localStorage
- **No Reload Required**: Instant language switching without page refresh
- **Complete Coverage**: All UI components, messages, forms, charts, and tooltips localized
- **Date/Time Localization**: Automatic date and number formatting per locale

### Adding New Languages
1. Create new translation file: `frontend/src/i18n/locales/{language-code}.json`
2. Add language configuration to `frontend/src/i18n/index.ts`
3. Update `isRTLLanguage()` function if adding an RTL language
4. Language will automatically appear in the language switcher

### Keyboard Shortcuts

Press `?` to view all keyboard shortcuts:
- `G` - Go to Dashboard
- `C` - Go to Cycles
- `P` - Go to Pitches
- `B` - Go to Backlog
- `T` - Go to Time (Work Logs)
- `Shift+N` - New Cycle
- `Shift+W` - Log Work

## 🚀 Quick Start

```bash
# 1. Set up environment (choose one option)

# Option A: Ollama (recommended for development - no API keys needed)
cat > .env << 'EOF'
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=mistral:instruct
EOF
brew install ollama && ollama pull mistral:instruct && ollama serve

# Option B: OpenAI ChatGPT (production-ready, requires API key)
cat > .env << 'EOF'
AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4-turbo-preview
EOF

# Option C: RunPod (cloud GPU - requires API key)
cp .env.example .env
# Edit .env with your RunPod credentials

# 2. Start Backend
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Start Frontend (new terminal)
cd frontend && npm install && npm run dev
```

## ⚙️ Configuration

### Redis for Production Caching

ShipFlow supports distributed caching via Redis for production deployments. All AI-powered services (RAG Q&A, Risk Analysis, Feedback Learning, LLM Cache, Conversation Management) share a unified Redis configuration.

**Enable Redis:**
```properties
# application.properties or environment variables
app.ai.cache.provider=redis  # default: in-memory
app.ai.cache.redis.host=localhost
app.ai.cache.redis.port=6379
app.ai.cache.redis.password=your-password
app.ai.cache.redis.database=0
```

**Environment Variables (recommended for production):**
```bash
AI_CACHE_PROVIDER=redis
AI_CACHE_REDIS_HOST=redis.example.com
AI_CACHE_REDIS_PORT=6379
AI_CACHE_REDIS_PASSWORD=secure-password
```

**Services using Redis when configured:**
- `AICacheService` - Risk analysis & Q&A response caching
- `FeedbackLearningService` - User feedback aggregation
- `LLMCacheService` - LLM response caching (40-60% cost reduction)
- `ConversationManager` - Multi-turn Q&A conversation contexts

**Benefits:**
- ✅ Shared cache across multiple application instances
- ✅ Persistent cache survives application restarts
- ✅ Better scalability for distributed deployments
- ✅ Automatic failover to in-memory if Redis unavailable

**Development:** Uses in-memory ConcurrentHashMap by default (no Redis needed)

## 📖 Documentation

- [Contributing Guide](CONTRIBUTING.md)
- [Task Dependencies Guide](TASK_DEPENDENCIES.md)
- [Changelog](CHANGELOG.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)

## � Community

Join our Discord community for support, discussions, and updates:

- **Discord**: [Join our server](https://discord.com/channels/1460971860823904390/1460971861511766060)

## �📄 License

MIT License