# Roadmap Planning Guide

## Overview

ShipFlow provides a comprehensive roadmap planning system that integrates strategic planning with the Shape Up methodology. The roadmap hierarchy consists of three levels:

1. **Initiatives** - Strategic themes spanning multiple quarters
2. **Epics** - Large feature groupings within initiatives
3. **Releases** - Time-bounded delivery milestones

These roadmap entities complement the core Shape Up workflow (Pitches → Cycles → Tasks) by providing strategic context and long-term planning capabilities.

## Initiatives

### What is an Initiative?

An **Initiative** is a high-level strategic theme that represents a major area of investment for the organization. Initiatives typically span multiple quarters (6-18 months) and contain multiple epics.

**Examples:**
- "Mobile Experience 2026" - A year-long initiative to revamp mobile applications
- "Platform Modernization" - Upgrading infrastructure and technology stack
- "International Expansion" - Building features for global market entry
- "Performance & Scalability" - Improving system performance across all products

### Initiative Properties

- **Name**: Clear, strategic theme name
- **Description**: Detailed explanation of goals, scope, and expected outcomes
- **Status**: DRAFT, ACTIVE, ON_HOLD, COMPLETED, CANCELLED
- **Owner**: Executive or senior leader responsible for the initiative
- **Target Dates**: Start and end dates for the initiative timeline
- **Color**: Visual identifier for roadmap views
- **Epics**: Collection of epics that support this initiative

### When to Use Initiatives

Create initiatives when you need to:
- Communicate strategic direction to stakeholders
- Align multiple teams around a common goal
- Track progress on multi-quarter investments
- Provide context for portfolio planning
- Connect epics and pitches to business strategy

### Best Practices

1. **Limit Active Initiatives**: Keep 3-5 active initiatives per product to maintain focus
2. **Clear Ownership**: Assign a single executive sponsor to each initiative
3. **Measurable Outcomes**: Define success criteria and key results
4. **Regular Reviews**: Review quarterly to assess progress and adjust course
5. **Strategic Alignment**: Ensure all epics ladder up to an initiative

## Epics

### What is an Epic?

An **Epic** is a large feature or body of work that groups related pitches together. Epics are more concrete than initiatives but broader than individual pitches. They typically span 1-3 quarters.

**Examples:**
- "Mobile Checkout Redesign" - Complete overhaul of mobile checkout flow
- "Search Performance Optimization" - Improving search speed across the platform
- "Admin Dashboard v2" - Next generation administrative interface
- "Real-time Notifications System" - Building push notification infrastructure

### Epic Properties

- **Name**: Specific feature or capability name
- **Description**: Detailed scope, user stories, and technical approach
- **Status**: DRAFT, PLANNED, IN_PROGRESS, AT_RISK, COMPLETED, CANCELLED
- **Initiative**: Parent initiative (optional but recommended)
- **Owner**: Product manager or technical lead
- **Target Dates**: Expected start and completion dates
- **Color**: Visual identifier (inherits from initiative if not set)
- **Pitches**: Shape Up pitches that implement this epic

### Relationship to Shape Up

Epics bridge strategic planning and tactical execution:

```
Initiative (Strategic)
    └── Epic (Feature Set)
        └── Pitch (Shaped Work)
            └── Cycle (6-week delivery)
                └── Tasks (Daily work)
```

An epic might be broken down into:
- Multiple pitches across different cycles
- Both "big batch" and "small batch" pitches
- Sequential or parallel delivery streams

### When to Use Epics

Create epics when you need to:
- Group related pitches for easier tracking
- Communicate a larger feature to stakeholders
- Plan work that spans multiple cycles
- Show dependencies between related work streams
- Provide context for shaping sessions

### Best Practices

1. **Right-Sizing**: Epics should take 1-3 quarters; smaller = pitch, larger = initiative
2. **Clear Scope**: Define what's included and explicitly call out what's excluded
3. **Progressive Elaboration**: Don't over-plan; shape pitches as you learn
4. **Risk Management**: Mark epics AT_RISK when blockers or scope issues emerge
5. **Pitch Connection**: Every epic should have at least one pitch

## Releases

### What is a Release?

A **Release** is a time-bounded delivery milestone that can contain work from multiple epics and initiatives. Releases represent coordinated deployments or product launches.

**Examples:**
- "Q2 2026 Release" - Quarterly product release
- "Holiday Feature Drop" - Special seasonal release
- "v3.0 Major Update" - Major version upgrade
- "Enterprise Launch" - Initial enterprise product release

### Release Properties

- **Name**: Clear milestone or version identifier
- **Version**: Semantic version or release identifier (e.g., "v2.4.0", "2026.Q2")
- **Description**: What's included in this release
- **Status**: PLANNING, IN_PROGRESS, TESTING, RELEASED, CANCELLED
- **Risk Level**: LOW, MEDIUM, HIGH, CRITICAL
- **Target Date**: Planned release date
- **Release Date**: Actual release date (set when released)
- **Release Notes**: User-facing documentation in markdown
- **Linked Cycles**: Shape Up cycles contributing to this release

### Relationship to Cycles

Unlike Shape Up's cycle-based delivery, releases are **cross-cutting milestones**:

- Multiple cycles can contribute to one release
- One cycle can contribute to multiple releases
- Releases coordinate work across teams and initiatives
- Cycles maintain Shape Up's shaped work and betting table process

**Example Timeline:**
```
Release: Q2 2026 Release (April 15, 2026)
    ├── Cycle 1 (Jan 6 - Feb 16)  → Pitches A, B, C
    ├── Cycle 2 (Feb 24 - Apr 6)  → Pitches D, E
    └── Cycle 3 (Apr 14 - May 25) → Final testing, bug fixes
```

### When to Use Releases

Create releases when you need to:
- Coordinate a major product launch
- Align with external deadlines (conferences, sales cycles)
- Bundle features for marketing announcements
- Track multi-team delivery milestones
- Communicate delivery plans to customers

### Best Practices

1. **Clear Scope**: Define must-have vs. nice-to-have features early
2. **Risk Assessment**: Update risk level weekly during execution
3. **Release Notes**: Maintain user-facing documentation as you go
4. **Buffer Time**: Include testing and stabilization cycles before release
5. **Flexible Scope**: Use Shape Up's appetite concept - be willing to cut scope to hit dates

## Roadmap Workflow

### Strategic Planning Process

1. **Annual/Quarterly Planning**
   - Define 3-5 major initiatives for the period
   - Assign executive sponsors
   - Set high-level goals and success metrics

2. **Epic Definition**
   - Break initiatives into concrete epics
   - Assign product/technical owners
   - Define rough timeframes (quarters)

3. **Release Planning**
   - Identify key delivery milestones
   - Assign target dates
   - Link relevant cycles

4. **Shaping & Betting**
   - Shape individual pitches within epics
   - Run betting table to select work for cycles
   - Maintain connection to parent epic/initiative

### Integration with Shape Up

The roadmap system **complements** Shape Up without disrupting its core principles:

✅ **DO:**
- Use initiatives/epics for strategic communication
- Link pitches to epics for context and tracking
- Use releases for external milestone coordination
- Shape work at the pitch level, not epic level
- Maintain 6-week cycle cadence

❌ **DON'T:**
- Treat epics as detailed requirements documents
- Pre-commit cycles to epics (maintain betting table autonomy)
- Let roadmap planning override shaping quality
- Turn initiatives into multi-month project plans
- Abandon Shape Up's appetite-driven approach

### Example: Mobile Experience Initiative

**Initiative**: Mobile Experience 2026 (Jan 2026 - Dec 2026)
- Owner: VP Product
- Status: ACTIVE
- Goal: Achieve feature parity with desktop and improve mobile conversion by 30%

**Epics within Initiative**:
1. Mobile Checkout Redesign (Q1-Q2)
   - Pitches: One-page checkout flow, Touch ID integration, Cart optimization
2. Mobile Performance (Q2-Q3)
   - Pitches: Bundle size reduction, Image optimization, Offline support
3. Mobile Notifications (Q3-Q4)
   - Pitches: Push notification system, In-app messaging, Notification preferences

**Releases**:
- Q2 2026 Release: Checkout redesign + performance improvements
- Q4 2026 Release: Notifications + final polish

**Cycles**:
- Each epic broken into 2-4 shaped pitches
- Pitches selected at betting table for 6-week cycles
- Some cycles contribute to multiple epics
- Cool-down weeks used for bug fixes and polish

## Q&A and Knowledge Integration

All roadmap entities (initiatives, epics, releases) are automatically ingested into the AI Q&A knowledge base. This enables questions like:

- "What are our active initiatives for 2026?"
- "Which epics are at risk?"
- "What's included in the Q2 release?"
- "Show me all pitches under the Mobile Experience initiative"
- "What's the status of the Platform Modernization epic?"

The system ingests comprehensive information including:
- Names, descriptions, and status
- Owners and stakeholders
- Target dates and timelines
- Related pitches, epics, and initiatives
- Progress indicators

## Permissions

Roadmap entities follow ShipFlow's permission model:

- **ADMIN**: Full access to all roadmap entities
- **MANAGER**: Full access to roadmap entities within their scope
- **MEMBER**: Read-only access to roadmap
- **GUEST**: No access to roadmap planning

Required permissions by action:
- View roadmap: READ permission on INITIATIVE/EPIC/RELEASE
- Create/Edit: CREATE/UPDATE permission
- Delete: DELETE permission (soft delete only)

## Reporting and Analytics

The roadmap system provides several views:

1. **Timeline View**: Gantt-style visualization of initiatives, epics, and releases
2. **Initiative Dashboard**: Rollup of epic status and completion percentage
3. **Release Planning Board**: Kanban view of work planned for releases
4. **Risk Report**: At-risk epics and releases with mitigation plans

## Summary

ShipFlow's roadmap planning system provides strategic context without sacrificing Shape Up's tactical agility. Use initiatives and epics for communication and alignment, while maintaining shaped pitches and 6-week cycles as the unit of delivery. Connect roadmap entities to enable powerful reporting and AI-powered Q&A about your strategic plans.
