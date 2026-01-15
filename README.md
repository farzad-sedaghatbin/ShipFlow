# <img src="frontend/public/icon.png" alt="ShipFlow Logo" width="32" height="32" style="vertical-align: middle;"> ShipFlow

A modern project management application implementing the [Shape Up](https://basecamp.com/shapeup) methodology by Basecamp.

🌐 **Live Demo**: [shipflow.dev](https://shipflow.dev)


## 📸 Screenshots & Demo

![ShipFlow Demo](screenshots/demo.webp)

| Dashboard | Hill Charts | Pitch Board |
|-----------|-------------|-------------|
| ![Dashboard](screenshots/dashboard.png) | ![Hill Chart](screenshots/hill-chart.png) | ![Pitch Board](screenshots/pitch-board.png) |

## ✨ Features

- **Cycles**: 6-week development cycles with betting table
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
  - **Task Dependencies**: Lightweight dependency tracking to identify blockers
    - Three dependency types: BLOCKS, DEPENDS_ON, RELATED_TO
    - Automatic circular dependency detection using depth-first search
    - Visual blocker indicators in task lists and detail pages
    - Same-cycle validation for dependency relationships
    - Clean UI for adding/removing dependencies
    - See [Task Dependencies Guide](TASK_DEPENDENCIES.md) for details
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
  - Out-of-scope work (tasks) tracking
  - PDF and CSV export functionality
- **Retrospectives**: Team retros with voting and merging
  - **Anonymous Submissions**: Post feedback anonymously for psychological safety
  - Checkbox option to hide author attribution on sensitive items
  - Standard columns: Went Well, Needs Improvement, Action Items
  - Real-time collaboration and voting
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
  - Smart relevance filtering (0.70 threshold)
  - Source citation tracking
  - RAG evaluation metrics (faithfulness, relevance)
  - Semantic caching for faster responses
- **QA Test Case Generation**: AI-assisted test case generation with validation
  - Test type-specific prompts (SMOKE, FUNCTIONAL, REGRESSION, INTEGRATION, E2E)
  - Automated quality validation
  - Historical test pattern learning
  - Completeness scoring (0-100)
- **GitHub Integration**: Seamless integration with GitHub repositories
  - Auto-link commits and pull requests to tasks and pitches
  - Auto-close tasks when PRs are merged with closing keywords
  - Real-time webhook updates
  - Visual GitHub activity timeline on task/pitch pages
  - Support for multiple repositories
- **Slack Integration**: Real-time team notifications
  - Workspace and channel-specific configuration
  - 8 notification types (tasks, cycles, pitches, betting)
  - Granular notification preferences per channel
  - Test notification functionality
  - Complete audit trail of sent notifications
  - Role-based access control (ADMIN/MANAGER only)

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

# Option B: RunPod (cloud AI - requires API key)
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