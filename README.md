# ShipFlow

A modern project management application implementing the [Shape Up](https://basecamp.com/shapeup) methodology by Basecamp.

🌐 **Live Demo**: [shipflow.dev](https://shipflow.dev)

## ✨ Features

- **Cycles**: 6-week development cycles with betting table
- **Pitches**: Shape work with appetite, problem definition, and solution
- **Hill Charts**: Visual progress tracking with drag-and-drop dots
- **Tasks**: Independent work management during cycles
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
- **Health Overview**: Automated risk detection and health monitoring
  - Real-time risk analysis based on bugs, scope completion, budget, and timeline
  - Risk trend indicators (IMPROVING, STABLE, WORSENING)
  - Visual priority sorting with pulsing animations for critical items
  - Automated status detection without manual assignment
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
- [Changelog](CHANGELOG.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)

## 📄 License

MIT License