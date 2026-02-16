# Wise Architecture Feature

## Overview

Wise Architecture is an **experimental** AI-powered feature that helps development teams generate comprehensive technical solutions for pitches. It analyzes your codebase to identify reusable services, recommend libraries, and create implementation plans that fit within your appetite (time budget).

## Features

### 1. Tech Stack Detection
- Automatically detects technology stacks in your repositories
- Supports multiple categories:
  - **Mobile**: Kotlin (Android), Swift (iOS), React Native, Flutter
  - **Backend**: Java (Spring), Node.js, Python, Go, .NET
  - **Web**: React, Angular, Vue.js, Next.js
- Shows confidence scores for each detected stack

### 2. Technical Solution Generation
- Generates architecture overviews for each selected stack
- Identifies reusable services from your existing codebase
- Recommends libraries and tools to accelerate development
- Creates step-by-step implementation plans with time estimates
- Provides best practices specific to your tech stack

### 3. Appetite Validation
- Checks if the estimated effort fits within the pitch's appetite
- Converts appetite (in days) to estimated hours
- Provides reduced scope suggestions when appetite is exceeded
- Lists items that could be deferred to fit the timeline

### 4. Follow-up Questions & Copilot Prompts
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
  - Architecture overview
  - Reusable services from your codebase
  - Recommended libraries with purposes
  - Implementation steps with time estimates
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

## Requirements

- **AI Features** must be enabled in organization settings
- **GitHub Integration** must be configured with accessible repositories
- **LLM Configuration** must be set up (OpenAI, Ollama, or RunPod)
- MCP (Model Context Protocol) is used for code analysis when available

## Technical Architecture

### Backend Services
- `WiseArchitectureService`: Main orchestration service
- `TechStackDetectorService`: Detects tech stacks from file patterns
- `TechnicalSolutionGeneratorService`: Generates solutions using LLM
- `WiseArchitectureConversationService`: Manages chat sessions and Copilot prompts

### Frontend Components
- `WiseArchitecturePage`: Multi-step wizard UI
- `wiseArchitectureService`: API client for backend calls
- Step progress indicator with validation

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

*This documentation is for Wise Architecture v1.0 (Experimental)*
