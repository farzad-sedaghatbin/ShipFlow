# Wise Architecture Feature

## Overview

Wise Architecture is an **experimental** AI-powered feature that helps development teams generate comprehensive technical solutions for pitches. It leverages multiple context sources—your codebase, team skills, design files, and product roadmap—to identify reusable services, recommend libraries, and create implementation plans that fit within your appetite (time budget).

## Features

### 1. Tech Stack Detection
- Automatically detects technology stacks in your repositories
- Supports multiple categories:
  - **Mobile**: Kotlin (Android), Swift (iOS), React Native, Flutter
  - **Backend**: Java (Spring), Node.js, Python, Go, .NET
  - **Web**: React, Angular, Vue.js, Next.js
- Shows confidence scores for each detected stack

### 2. Async Processing & Performance (v0.5.4)

For large repositories, Wise Architecture uses asynchronous processing to prevent timeouts:

- **Async Job Infrastructure**: Long-running operations execute in background
  - `AsyncWiseArchitectureService` with job-based execution pattern  
  - Dedicated `aiTaskExecutor` thread pool for AI operations
  - Request deduplication prevents duplicate jobs for same parameters
  - Automatic job cleanup after 30-minute TTL

- **Granular Progress Tracking**: Real-time updates with descriptive messages
  - 0-10%: Initialization - "Loading repository information..."
  - 10-55%: File listing - "Listing files for repository 1/3 (kixy-mobile)..."
  - 55-95%: Processing - "Detected 3 stacks: React Native, Node.js, Kotlin"
  - 95-100%: Finalization - "Solution complete: 3 stacks, 48 hours, appetite passed"

- **Performance Optimizations**:
  - File list caching with 10-minute TTL (avoids repeated MCP calls)
  - Pre-indexed file pattern matching by extension for O(1) lookups
  - Quick config file detection for common frameworks
  - Parallel repository processing (up to 4 concurrent scans)
  - Batch file reads using CompletableFuture for code context

- **Frontend Polling**: Automatic progress tracking
  - 2-second polling interval with exponential backoff
  - Real-time progress bar and status messages
  - Graceful timeout handling after 10 minutes

### 3. Multi-Source Context Integration (v0.5.3)

Wise Architecture now considers multiple context sources for smarter recommendations:

- **Team Skills Integration**: Extracts unique skills from assigned team members (~25-35 tokens)
  - Suggests technologies matching team expertise for faster implementation
  - Prioritizes libraries and tools the team already knows
  
- **Figma MCP Integration**: Analyzes Figma design files linked in pitches
  - Extracts design context from wireframeLinks via Figma MCP server (~100-200 tokens)
  - Per-organization Figma access token storage in Organization Settings
  - Environment-variable driven: `MCP_FIGMA_ENABLED`, `MCP_FIGMA_SERVER_URL`

- **GitHub MCP Integration**: Repository code analysis via Model Context Protocol servers
  - Full HTTP implementation for file listing, reading, and searching
  - Batch file read support with fallback to individual reads
  - Automatic service discovery based on tech stack patterns
  - Graceful fallback to default patterns when MCP not configured

- **Roadmap Context Integration**: Considers Epic/Initiative relationships for extensibility
  - Extracts roadmap context from assigned Epic (name, status, description)
  - Includes parent Initiative information when available
  - Lists related pitches in the same Epic for cohesive design recommendations
  - Generates architecture suggestions optimized for future extension

- **Context Availability Warnings**: Frontend displays alerts when context sources are missing
  - Shows which sources were used (code analysis, team skills, design context, roadmap context)
  - Warns users that recommendations may be less accurate without full context
  - Tip: "Assign pitches to epics to enable roadmap-aware recommendations"

### 4. Advice History & Feedback (v0.5.5)

All AI-generated solutions are automatically saved for review and follow-up:

- **Conversation Persistence**: Solutions and follow-ups are stored for later review
  - Conversation ID groups related messages into threads
  - Message types: `INITIAL_SOLUTION` and `FOLLOW_UP`
  - Processing time tracked for performance monitoring

- **Context Tracking**: Each advice entry records which context sources were used
  - Figma design context, GitHub code context, roadmap context flags
  - Tech stacks and repository IDs stored for reference

- **Feedback System**: Users can rate advice quality
  - Mark advice as helpful or not helpful
  - Optional text feedback for improvement suggestions
  - Feedback timestamp for analytics

- **History API**: Full REST API for accessing advice history
  - Paginated user history with conversation summaries
  - Pitch-specific conversation lists
  - Full conversation thread retrieval

### 5. Agent-Ready Markdown Output (v0.9.0)

After every analysis, Wise Architecture generates a set of **Markdown files** designed to be read
directly by AI coding agents (Claude Code, Cursor, GitHub Copilot Workspace) to drive implementation
without additional context.

**Files produced per analysis run:**

| File | Purpose |
|------|---------|
| `architecture-overview.md` | Problem statement, appetite, stack summary, context source status |
| `{stack-id}-implementation-guide.md` | Full guide per stack — components, API contracts, data model, reusable services, libraries, implementation steps, risks |
| `api-design.md` | Consolidated REST API contracts across all stacks (omitted when no APIs are defined) |
| `implementation-plan.md` | Combined step-by-step plan across all phases with dependency graph |

**Suggested repository layout:**

```
your-repo/
└── .wise/
    ├── architecture-overview.md
    ├── java-spring-implementation-guide.md
    ├── react-native-implementation-guide.md
    ├── api-design.md
    └── implementation-plan.md
```

**How agents consume the files:**

- **Claude Code**: `@.wise/architecture-overview.md` — agent reads context before writing code.
- **Cursor**: Add `.wise/` to your Cursor workspace context index.
- **GitHub Copilot Workspace**: Reference files in the task description using `#file:.wise/api-design.md`.

Files are:
- Available in the **Generated Agent Files** panel at the bottom of the Step 4 solution view.
- Downloadable individually or all at once with **Download All**.
- Persisted in advice history (`GET /api/wise-architecture/history/{adviceId}/files`) so they can be retrieved later without re-running the analysis.

### 6. Technical Solution Generation (v1.3 — Structured & Actionable)

Solutions are now structured with concrete, actionable detail rather than generic overviews:

- **Architecture Detail Breakdown**: Each stack solution includes:
  - **Summary**: Concise architecture overview rendered as markdown
  - **Components**: Named architectural components with responsibilities and interaction maps
  - **API Contracts**: Concrete endpoint definitions with method, path, request/response shapes
  - **Data Model**: Entity definitions with fields and relationships
  - **Configuration Changes**: Specific config keys/values to add or modify

- **Enriched Implementation Steps**: Steps now include:
  - Concrete sub-tasks with acceptance criteria (checklist-style)
  - Method signatures to implement (code font in UI)
  - Step dependencies (`dependsOnSteps`) for execution ordering
  - Files to create (green `+` badges) and modify (purple `~` badges)

- **Enriched Reusable Services**: Each identified service now shows:
  - Specific methods to call (`methodsToCall`) as badges
  - Import/injection statement (`importStatement`) in code format
  - Usage instructions (`howToUse`)

- **Enriched Libraries**: Each recommendation now shows:
  - Version number and documentation URL with link
  - "In project" badge when the library is already a dependency

- **Project Convention Pre-pass**: Before generating solutions, a lightweight LLM call analyzes code context to detect naming conventions, patterns, and project structure preferences

- **Cross-Stack Coordination**: When generating solutions for multiple stacks, API contracts from previous stacks are passed to subsequent ones to ensure interface consistency

- **Risk Factors Section**: Dedicated UI section with warning icon for implementation risks

- **History Alignment**: The enriched markdown format is persisted to advice history, so past solutions display with full structured detail (components, API contracts, data model, implementation steps with sub-tasks, etc.)

### 7. Appetite Validation
- Checks if the estimated effort fits within the pitch's appetite
- Converts appetite (in days) to estimated hours
- Provides reduced scope suggestions when appetite is exceeded
- Lists items that could be deferred to fit the timeline

### 8. Follow-up Questions & Copilot Prompts
- Chat interface for asking clarifying questions about the solution
- Automatically detects code requests (keywords like "generate", "create", "implement")
- Generates ready-to-use prompts for GitHub Copilot or other AI assistants
- Copy prompts directly to use in your IDE

## Enabling the Feature

Wise Architecture is an experimental feature that must be enabled by an administrator:

1. Go to **Organization Settings** → **Features** tab
2. Enable **AI Features** (required)
3. Enable **Wise Architecture** (Experimental badge)
4. Save settings

## Using Wise Architecture

### Step 1: Select a Pitch
- Choose from your available pitches
- The pitch's problem statement and appetite are displayed
- Appetite is shown in weeks (calculated from days)

### Step 2: Select Repositories
- Choose one or more GitHub repositories to analyze
- Multiple repositories can be selected for multi-service architectures
- Repositories must be connected via GitHub integration

### Step 3: Confirm Tech Stacks
- Review automatically detected stacks with confidence scores
- Select/deselect stacks to include in the solution
- High-confidence stacks (≥70%) are pre-selected

### Step 4: Review Solution
- **Appetite Check**: See if the estimated effort fits your timeline
- **Stack Solutions**: Expand each stack to see:
  - Architecture overview (rendered as markdown)
  - Architecture components with responsibilities and interaction maps
  - API contracts with method, endpoint, request/response shapes
  - Data model entities with fields and relationships
  - Configuration changes with key=value pairs
  - Reusable services with import statements, methods to call, and usage instructions
  - Recommended libraries with version, docs link, and "in project" badge
  - Implementation steps with:
    - Time estimates and step dependencies
    - Files to create (green) and modify (purple)
    - Method signatures in code font
    - Sub-tasks with acceptance criteria
  - Risk factors with warning indicators
  - Best practices
- **Reduced Scope**: If appetite is exceeded, see suggestions for what to defer

### Step 5: Follow-up Questions
- Ask questions about the generated solution
- Request code snippets (triggers Copilot prompt generation)
- Copy Copilot prompts to use in your development environment

## API Endpoints

### Check Feature Status
```
GET /api/wise-architecture/status
```
Returns whether the feature is enabled for the organization.

### Detect Tech Stacks
```
POST /api/wise-architecture/detect-stacks
{
  "pitchId": 123,
  "repositoryIds": [1, 2, 3]
}
```
Analyzes repositories and returns detected technology stacks.

### Analyze and Generate Solution
```
POST /api/wise-architecture/analyze
{
  "pitchId": 123,
  "repositoryIds": [1, 2, 3],
  "selectedStacks": ["BACKEND_JAVA", "WEB_REACT"]
}
```
Generates a complete technical solution with implementation plan.

### Follow-up Question
```
POST /api/wise-architecture/follow-up
{
  "sessionId": "uuid-session-id",
  "question": "How should I structure the API endpoints?"
}
```
Answers questions about the generated solution; detects code requests and generates Copilot prompts.

### Async Operations (Recommended for Large Repos)

#### Start Async Stack Detection
```
POST /api/wise-architecture/async/detect-stacks
{
  "pitchId": 123,
  "repositoryIds": [1, 2, 3]
}
```
Returns immediately with a job ID for polling.

#### Start Async Solution Analysis
```
POST /api/wise-architecture/async/analyze
{
  "pitchId": 123,
  "repositoryIds": [1, 2, 3],
  "selectedStacks": ["BACKEND_JAVA", "WEB_REACT"]
}
```
Returns immediately with a job ID for polling.

#### Poll Job Status
```
GET /api/wise-architecture/jobs/{jobId}/status
```
Returns:
```json
{
  "jobId": "abc12345",
  "status": "PROCESSING",
  "progress": 45,
  "progressMessage": "Listing files for repository 2/3 (backend-api)..."
}
```

#### Get Job Result
```
GET /api/wise-architecture/jobs/{jobId}/result
```
Returns the complete result when job status is COMPLETED.

#### Cancel Job
```
DELETE /api/wise-architecture/jobs/{jobId}
```
Cancels a running job.

### Advice History Endpoints

Save and retrieve AI-generated solutions for review and follow-up:

#### Get User's Conversation History
```
GET /api/wise-architecture/history?page=0&size=10
```
Returns paginated list of conversation summaries for the current user.

#### Get Pitch Conversations
```
GET /api/wise-architecture/history/pitch/{pitchId}
```
Returns all conversations related to a specific pitch.

#### Get Full Conversation
```
GET /api/wise-architecture/history/conversation/{conversationId}
```
Returns all messages in a conversation thread, ordered chronologically.

#### Get Single Advice Entry
```
GET /api/wise-architecture/history/{adviceId}
```
Returns a specific advice entry by ID.

#### Submit Feedback
```
POST /api/wise-architecture/history/{adviceId}/feedback
{
  "helpful": true,
  "feedbackText": "This solution worked well for our architecture."
}
```
Submit feedback on an advice entry (helpful/not helpful with optional comment).

#### Get Generated Markdown Files
```
GET /api/wise-architecture/history/{adviceId}/files
```
Returns the agent-consumable Markdown files generated during the analysis. Files include
architecture overview, per-stack implementation guides, API design, and implementation plan.
Returns an empty array for FOLLOW_UP messages or entries created before v0.9.0.

## Requirements

- **AI Features** must be enabled in organization settings
- **GitHub Integration** must be configured with accessible repositories
- **LLM Configuration** must be set up (OpenAI, Ollama, or RunPod)
- MCP (Model Context Protocol) is used for code analysis when available

## Technical Architecture

### Backend Services
- `WiseArchitectureService`: Main orchestration service with progress callbacks
- `AsyncWiseArchitectureService`: Job management and request deduplication
- `WiseArchitectureExecutor`: Async execution on `aiTaskExecutor` thread pool
- `WiseArchitectureHistoryService`: Persists and retrieves advice history with feedback; serialises generated files to JSON
- `WiseArchitectureMarkdownService`: **New in v0.9.0** — converts structured solutions to agent-consumable Markdown files
- `TechStackDetectorService`: Detects tech stacks with pre-indexed pattern matching
- `TechnicalSolutionGeneratorService`: Generates solutions using LLM with JSON schema and retry logic
- `WiseArchitectureConversationService`: Manages chat sessions and Copilot prompts
- `GitHubMcpProvider`: File list caching with 10-minute TTL
- `FigmaMcpProvider`: Figma design context extraction with node-id support

### Frontend Components
- `WiseArchitecturePage`: Multi-step wizard UI with polling; includes `GeneratedFilesPanel` in Step 4
- `wiseArchitectureService`: API client with async job support; `downloadMarkdownFile` / `downloadAllMarkdownFiles` helpers
- Step progress indicator with real-time status updates
- Debounced repository search (300ms)
- Memoized computed values for performance

## Limitations

- This is an **experimental feature** and may change significantly
- Solutions are AI-generated suggestions and should be reviewed by developers
- Code detection relies on file patterns and may miss some configurations
- Copilot prompts are suggestions; actual code generation depends on your AI assistant

## Feedback

As an experimental feature, we welcome feedback to improve Wise Architecture:
- Report issues via the bug reporting system
- Suggest improvements through the feedback mechanism
- Share successful use cases with your team

---

*This documentation is for Wise Architecture v1.4 (Experimental) - Updated April 2026*
