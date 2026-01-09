# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
