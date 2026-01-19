# Project Type Feature Architecture

## Stakeholder Analysis

### Primary Stakeholders

| Stakeholder | Role | Needs | Impact |
|-------------|------|-------|--------|
| Product Managers | Decision makers | Flexibility to manage projects with different methodologies | High |
| Development Teams | Daily users | Clear UI based on project methodology | High |
| Team Leads | Sprint/Cycle managers | Ability to choose appropriate workflow | Medium |
| Admin Users | System configuration | Easy project setup and management | Medium |

### Stakeholder Requirements

1. **Product Managers**
   - Need to manage both Kanban-style continuous flow and Shape Up cyclic delivery
   - Want seamless switching between project views
   - Need consistent tracking regardless of methodology

2. **Development Teams**
   - Kanban users: Want continuous backlog with board view
   - Shape Up users: Want cycle-based work with betting and pitches
   - Both: Want optional Kanban board visualization

3. **Team Leads**
   - Ability to set project type during creation
   - Control over which features are available per project

---

## Feature Specification

### Project Types

| Type | Description | Primary View | Key Features | Status |
|------|-------------|--------------|--------------|--------|
| `SHAPE_UP` | 6-week cycle methodology | Cycle-based workspace | Pitches, Betting, Hill Charts, Cooldown | ✅ Implemented |
| `KANBAN` | Continuous flow | Backlog Kanban Board | Board view, Continuous backlog, Auto-created cycle | ✅ Implemented |

### Implemented Features

#### Backend (✅ Complete)
- ✅ `ProjectType` enum (SHAPE_UP, KANBAN)
- ✅ Database migration V55 (`project_type` column)
- ✅ Automatic "Continuous Flow" cycle creation for Kanban projects
- ✅ Project DTO includes `projectType` field
- ✅ Comprehensive unit tests for project type logic

#### Frontend (✅ Complete)
- ✅ Project type selection in create/edit dialogs
- ✅ `useProject` context with `isKanbanProject` helper
- ✅ Conditional navigation (cycles hidden for Kanban)
- ✅ Kanban board with drag-and-drop, subtask creation, timer
- ✅ Pitch/scope fields hidden in Kanban task/bug/testcase forms
- ✅ Cycle/pitch filters hidden in Kanban list views
- ✅ Terminology changes: "Feature Tasks" vs "Pitch Tasks"
- ✅ Project-based filtering across all pages

### Menu/Navigation Behavior

#### Shape Up Mode (✅ Implemented)
- ✅ Dashboard
- ✅ Projects
- ✅ Cycles (primary navigation)
- ✅ Cycle Workspace (Pitches, Betting, Health, Retrospectives, Reports)
- ✅ Backlog (list view by default, Kanban toggle available)
- ✅ Work Logs (with cycle selector)
- ✅ Meetings
- ✅ People & Teams
- ✅ Quality (Test Cases, Bug Reports with pitch/cycle filters)

#### Kanban Mode (✅ Implemented)
- ✅ Dashboard
- ✅ Projects
- ❌ Cycles (hidden - no cycle concept for users)
- ❌ Cycle Workspace (hidden)
- ✅ Backlog (Kanban board by default, auto-switched)
- ✅ Work Logs (cycle selector hidden)
- ✅ Meetings
- ✅ People & Teams
- ✅ Quality (Test Cases, Bug Reports without pitch/cycle filters)

---

## Technical Architecture

### Backend Changes

#### 1. New Enum: ProjectType
```java
public enum ProjectType {
    SHAPE_UP,    // Default - 6-week cycles
    KANBAN       // Continuous flow
}
```

#### 2. Updated Project Entity
```java
@Column(nullable = false)
@Enumerated(EnumType.STRING)
@Builder.Default
private ProjectType projectType = ProjectType.SHAPE_UP;
```

#### 3. Database Migration (V55)
- Add `project_type` column to `projects` table
- Default value: 'SHAPE_UP' for backward compatibility

#### 4. Updated DTOs
- `ProjectDTO`: Add `projectType` field
- `CreateProjectRequest`: Add `projectType` field

### Frontend Changes

#### 1. Type Definitions
```typescript
export type ProjectType = 'SHAPE_UP' | 'KANBAN';

export interface Project {
  // ... existing fields
  projectType: ProjectType;
}
```

#### 2. ProjectContext Enhancement
- Track current project type
- Expose `isKanbanProject` computed property

#### 3. Layout Navigation
- Conditional rendering based on project type
- Hide/show menu sections dynamically

#### 4. BacklogPage Enhancement
- Add view mode toggle: List | Kanban Board
- Default view based on project type

#### 5. New KanbanBoard Component
- Drag-and-drop columns by status
- Visual task cards
- Swimlanes by assignee (optional)

---

## Implementation Plan

### Phase 1: Backend (Priority: High)
1. Create `ProjectType` enum
2. Update `Project` entity
3. Create database migration V55
4. Update `ProjectDTO` and `CreateProjectRequest`
5. Update `ProjectService.create()` and `toDTO()`

### Phase 2: Frontend - Core (Priority: High)
1. Update TypeScript types
2. Update ProjectContext
3. Add project type selection to Projects dialog

### Phase 3: Frontend - Navigation (Priority: High)
1. Update Layout.tsx for conditional menu
2. Pass project type context to navigation

### Phase 4: Frontend - Kanban View (Priority: Medium)
1. Create KanbanBoard component
2. Add view toggle to BacklogPage
3. Implement drag-and-drop functionality

---

## Data Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Project        │     │  Project        │     │  Layout         │
│  Creation       │────>│  Context        │────>│  Navigation     │
│  (with type)    │     │  (stores type)  │     │  (conditional)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                │
                                ▼
                        ┌─────────────────┐
                        │  Backlog Page   │
                        │  (view toggle)  │
                        └─────────────────┘
```

---

## API Changes

### Create Project
```http
POST /api/projects
{
  "name": "My Project",
  "projectKey": "MP",
  "projectType": "KANBAN",  // NEW FIELD
  ...
}
```

### Get Project Response
```json
{
  "id": 1,
  "name": "My Project",
  "projectType": "KANBAN",  // NEW FIELD
  ...
}
```

---

## Migration Strategy

1. **Backward Compatibility**: All existing projects default to `SHAPE_UP`
2. **No Data Loss**: Kanban projects can still create tasks (just no cycles)
3. **Gradual Adoption**: Teams can switch project type if needed

---

## Testing Considerations

1. ✅ Unit tests for ProjectService with new projectType field - `ProjectTypeTest.java`
2. ✅ Tests for automatic Continuous Flow cycle creation (Kanban)
3. ✅ Tests for no auto-cycle creation (Shape Up)
4. ✅ Tests for backward compatibility (default to SHAPE_UP)
5. ⏳ Integration tests for API endpoints
6. ⏳ Frontend component tests for conditional rendering
7. ⏳ E2E tests for navigation behavior per project type

---

## Implementation Summary (January 2026)

### ✅ Completed Features

#### Backend
1. **Database Schema** (Migration V55)
   - Added `project_type` VARCHAR(20) column to `projects` table
   - Default value: 'SHAPE_UP' for backward compatibility
   - NOT NULL constraint

2. **Domain Model**
   - Created `ProjectType` enum (SHAPE_UP, KANBAN)
   - Updated `Project` entity with `@Builder.Default` for SHAPE_UP
   - Updated DTOs: `ProjectDTO`, `CreateProjectRequest`

3. **Business Logic**
   - `ProjectService.create()`: Auto-creates "Continuous Flow" cycle for KANBAN projects
   - Cycle configuration: name="Continuous Flow", startDate=now, endDate=2099-12-31, phase=BUILD
   - `ProjectService.toDTO()`: Includes projectType in response
   - Comprehensive unit tests in `ProjectTypeTest.java`

#### Frontend
1. **Type System**
   ```typescript
   export type ProjectType = 'SHAPE_UP' | 'KANBAN';
   ```

2. **Context Management**
   - `ProjectContext`: Added `isKanbanProject` computed property
   - Automatic derivation from `currentProject.projectType`

3. **UI Adaptations**
   - **Layout Navigation**: Cycles menu hidden for Kanban projects
   - **BacklogPage**: 
     - Auto-switches to Kanban view for Kanban projects
     - Hides pitch/scope fields in task forms
     - Hides cycle selector
     - Changes labels: "Feature Tasks" vs "Pitch Tasks"
   - **KanbanBoard Component**:
     - Drag-and-drop status columns
     - Add subtask functionality
     - Start timer functionality
     - Visual task cards with priority indicators
   - **WorkLogsPage**: Cycle selector hidden for Kanban
   - **TestCasesPage**: Pitch/cycle filters hidden for Kanban
   - **BugReportsPage**: Pitch/cycle filters hidden for Kanban
   - **BugReportModal**: Scope field hidden for Kanban
   - **TestCaseFormPage**: Pitch/scope fields hidden for Kanban

4. **Project Filtering**
   - All pages now filter data by currently selected project
   - "All Projects" selection shows data from all projects
   - Consistent filtering across: BacklogPage, WorkLogsPage, TestCasesPage, BugReportsPage, MeetingList

5. **Translations**
   - Added `featureTasks` and `featureScope` keys
   - English and Farsi translations

### 🎯 Design Decisions

1. **Hidden Cycle for Kanban**: The "Continuous Flow" cycle is created automatically but never shown to users. This maintains database consistency while providing a Kanban experience.

2. **Shape Up Concepts Hidden**: Pitches, scopes, cycles, and betting are Shape Up-specific. These are completely hidden in Kanban project UI.

3. **Backward Compatibility**: All existing projects default to SHAPE_UP, ensuring no breaking changes.

4. **Consistent Filtering**: Project-based filtering ensures users only see data relevant to their currently selected project.

### 📊 Test Coverage

**Backend Tests** (`ProjectTypeTest.java`):
- ✅ Default to SHAPE_UP when projectType not specified
- ✅ SHAPE_UP projects do NOT auto-create cycles
- ✅ KANBAN projects auto-create "Continuous Flow" cycle
- ✅ Cycle properties validated (name, dates, phase, active status)
- ✅ Project type preserved during updates
- ✅ DTO correctly includes projectType field

**Frontend Tests**: ⏳ To be added
- Component tests for conditional rendering
- Integration tests for project switching
- E2E tests for Kanban workflow

---

## Future Enhancements

1. **Kanban-Specific Features**
   - WIP (Work In Progress) limits per column
   - Swimlane customization
   - Cumulative flow diagram
   - Cycle time analytics

2. **Migration Tools**
   - Convert Shape Up project to Kanban (and vice versa)
   - Export/import project configurations
   
3. **Hybrid Mode**
   - Allow some Shape Up features in Kanban (e.g., retrospectives)
   - Configurable feature toggles per project
