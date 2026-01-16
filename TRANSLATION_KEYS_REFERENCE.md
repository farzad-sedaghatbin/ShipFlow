# 🔑 TRANSLATION KEYS QUICK REFERENCE
## Organized by Page for Easy Copy-Paste

---

## ✅ **TRANSLATION FILES STATUS**

- **English**: `frontend/src/i18n/locales/en.json` (2,373+ lines) ✅
- **Persian**: `frontend/src/i18n/locales/fa.json` (1,669+ lines) ✅
- **All keys added and ready to use!**

---

## 📄 **PAGE-BY-PAGE KEY MAPPING**

### 1. **reportsPage** → Reports.tsx
```typescript
// Common Report Strings
t('reportsPage.title')               // "Reports"
t('reportsPage.selectCycle')         // "Select cycle"
t('reportsPage.exportPDF')           // "Export PDF"
t('reportsPage.exportCSV')           // "Export CSV"

// Empty States
t('reportsPage.noCyclesFound')       // "No cycles found"
t('reportsPage.noCyclesDesc')        // Description
t('reportsPage.selectCycleToView')   // "Select a cycle to view reports"
t('reportsPage.selectCycleDesc')     // Description

// Metrics
t('reportsPage.totalPitches')        // "Total Pitches"
t('reportsPage.completed')           // "Completed"
t('reportsPage.inProgress')          // "In Progress"
t('reportsPage.appetiteHours')       // "Appetite (h)"
t('reportsPage.actualHours')         // "Actual (h)"
t('reportsPage.efficiency')          // "Efficiency"

// Analysis Sections
t('reportsPage.varianceAnalysis')    // "Variance Analysis"
t('reportsPage.varianceHours')       // "Variance (Hours)"
t('reportsPage.variancePercent')     // "Variance (%)"
t('reportsPage.teamMembers')         // "Team Members"
t('reportsPage.avgHoursPerMember')   // "Avg Hours/Member"

// Charts
t('reportsPage.pitchDistribution')   // "Pitch Distribution"
t('reportsPage.appetiteVsActual')    // "Appetite vs Actual Hours by Pitch"
t('reportsPage.appetite')            // "Appetite"
t('reportsPage.actual')              // "Actual"
t('reportsPage.variance')            // "Variance"

// Table Headers
t('reportsPage.pitch')               // "Pitch"
t('reportsPage.status')              // "Status"
t('reportsPage.team')                // "Team"
```

---

### 2. **backlogPage** → BacklogPage.tsx
```typescript
// Page Header
t('backlogPage.title')               // "Task Backlog"
t('backlogPage.createTask')          // "Create Task"

// Tabs
t('backlogPage.allTasks')            // "All Tasks"
t('backlogPage.myTasks')             // "My Tasks"

// Filters
t('backlogPage.filterByStatus')      // "Filter by Status"
t('backlogPage.filterByPriority')    // "Filter by Priority"
t('backlogPage.filterByCycle')       // "Filter by Cycle"
t('backlogPage.filterByPitch')       // "Filter by Pitch"
t('backlogPage.clearFilters')        // "Clear Filters"
t('backlogPage.search')              // "Search tasks..."

// Form Fields
t('backlogPage.taskName')            // "Task Name"
t('backlogPage.description')         // "Description"
t('backlogPage.status')              // "Status"
t('backlogPage.priority')            // "Priority"
t('backlogPage.assignee')            // "Assignee"
t('backlogPage.dueDate')             // "Due Date"
t('backlogPage.estimatedHours')      // "Estimated Hours"
t('backlogPage.actualHours')         // "Actual Hours"

// Status Options
t('backlogPage.statusBacklog')       // "Backlog"
t('backlogPage.statusTodo')          // "To Do"
t('backlogPage.statusInProgress')    // "In Progress"
t('backlogPage.statusBlocked')       // "Blocked"
t('backlogPage.statusInReview')      // "In Review"
t('backlogPage.statusDone')          // "Done"
t('backlogPage.statusCancelled')     // "Cancelled"

// Priority Options
t('backlogPage.priorityLow')         // "Low"
t('backlogPage.priorityMedium')      // "Medium"
t('backlogPage.priorityHigh')        // "High"
t('backlogPage.priorityUrgent')      // "Urgent"

// Category Options
t('backlogPage.categoryFiguring')    // "Figuring Things Out"
t('backlogPage.categoryMakingProgress') // "Making Progress"
t('backlogPage.categoryNiceToHave')  // "Nice to Have"
t('backlogPage.categoryMustHave')    // "Must Have"

// Actions
t('backlogPage.startTimer')          // "Start Timer"
t('backlogPage.stopTimer')           // "Stop Timer"
t('backlogPage.logWork')             // "Log Work"

// Messages
t('backlogPage.taskCreated')         // "Task created successfully"
t('backlogPage.taskUpdated')         // "Task updated successfully"
t('backlogPage.taskDeleted')         // "Task deleted successfully"
```

---

### 3. **pitchBoard** → PitchBoard.tsx
```typescript
// Page Header
t('pitchBoard.title')                // "Pitch Board"
t('pitchBoard.createPitch')          // "Create Pitch"

// Column Headers (Kanban)
t('pitchBoard.columnIdea')           // "Idea"
t('pitchBoard.columnDraft')          // "Draft"
t('pitchBoard.columnReady')          // "Ready to Bet"
t('pitchBoard.columnBetting')        // "In Betting Table"
t('pitchBoard.columnApproved')       // "Approved"
t('pitchBoard.columnInProgress')     // "In Progress"
t('pitchBoard.columnCompleted')      // "Completed"
t('pitchBoard.columnRejected')       // "Rejected"

// Actions
t('pitchBoard.editPitch')            // "Edit Pitch"
t('pitchBoard.deletePitch')          // "Delete Pitch"
t('pitchBoard.movePitch')            // "Move Pitch"
t('pitchBoard.viewDetails')          // "View Details"

// Pitch Details
t('pitchBoard.appetite')             // "Appetite"
t('pitchBoard.smallBatch')           // "Small Batch (1-2 weeks)"
t('pitchBoard.bigBatch')             // "Big Batch (6 weeks)"
t('pitchBoard.assignedTo')           // "Assigned To"
t('pitchBoard.createdBy')            // "Created By"

// Empty States
t('pitchBoard.emptyColumn')          // "No pitches in this stage"
t('pitchBoard.noDescription')        // "No description provided"

// Filters
t('pitchBoard.filterByCycle')        // "Filter by Cycle"
t('pitchBoard.filterByPerson')       // "Filter by Person"
t('pitchBoard.showAll')              // "Show All"
t('pitchBoard.sortBy')               // "Sort By"
```

---

### 4. **pitchDetailPage** → PitchDetail.tsx
```typescript
// Page Header
t('pitchDetailPage.title')           // "Pitch Details"
t('pitchDetailPage.edit')            // "Edit"
t('pitchDetailPage.delete')          // "Delete"

// Tabs
t('pitchDetailPage.overview')        // "Overview"
t('pitchDetailPage.shapeUpDetails')  // "Shape Up Details"
t('pitchDetailPage.teams')           // "Teams"
t('pitchDetailPage.tasks')           // "Tasks"

// Shape Up Fields
t('pitchDetailPage.problemStatement') // "Problem Statement"
t('pitchDetailPage.solution')        // "Solution"
t('pitchDetailPage.appetite')        // "Appetite"
t('pitchDetailPage.rabbithHoles')    // "Rabbit Holes"
t('pitchDetailPage.noGos')           // "No-Gos"
t('pitchDetailPage.fatMarkerSketches') // "Fat Marker Sketches"
t('pitchDetailPage.breadboarding')   // "Breadboarding"

// Metadata
t('pitchDetailPage.createdBy')       // "Created By"
t('pitchDetailPage.createdAt')       // "Created At"
t('pitchDetailPage.lastUpdated')     // "Last Updated"
t('pitchDetailPage.status')          // "Status"
t('pitchDetailPage.cycle')           // "Cycle"

// Actions
t('pitchDetailPage.save')            // "Save"
t('pitchDetailPage.cancel')          // "Cancel"
t('pitchDetailPage.addTeam')         // "Add Team"
t('pitchDetailPage.addTask')         // "Add Task"
```

---

### 5. **cycleDetailPage** → CycleDetail.tsx
```typescript
// Page Header
t('cycleDetailPage.title')           // "Cycle Details"
t('cycleDetailPage.cycleInformation') // "Cycle Information"

// Fields
t('cycleDetailPage.cycleName')       // "Cycle Name"
t('cycleDetailPage.startDate')       // "Start Date"
t('cycleDetailPage.endDate')         // "End Date"
t('cycleDetailPage.duration')        // "Duration"
t('cycleDetailPage.weeks')           // "weeks"
t('cycleDetailPage.status')          // "Status"

// Status Options
t('cycleDetailPage.planning')        // "Planning"
t('cycleDetailPage.active')          // "Active"
t('cycleDetailPage.cooldown')        // "Cooldown"
t('cycleDetailPage.completed')       // "Completed"

// Sections
t('cycleDetailPage.pitches')         // "Pitches"
t('cycleDetailPage.teams')           // "Teams"
t('cycleDetailPage.progress')        // "Progress"

// Stats
t('cycleDetailPage.totalPitches')    // "Total Pitches"
t('cycleDetailPage.approvedPitches') // "Approved Pitches"
t('cycleDetailPage.completedPitches') // "Completed Pitches"
```

---

### 6. **healthOverview** → HealthOverview.tsx
```typescript
// Page Header
t('healthOverview.title')            // "Stakeholder Health Overview"

// Tabs
t('healthOverview.allCycles')        // "All Active Cycles"
t('healthOverview.singleCycle')      // "Single Cycle"

// Selects
t('healthOverview.selectCycle')      // "Select a cycle"

// Empty States
t('healthOverview.noActiveCycles')   // "No Active Cycles"
t('healthOverview.noActiveCyclesDesc') // Description
t('healthOverview.noPitches')        // "No Pitches Found"
t('healthOverview.noPitchesDesc')    // Description

// Health Status
t('healthOverview.healthy')          // "Healthy"
t('healthOverview.atRisk')           // "At Risk"
t('healthOverview.needsAttention')   // "Needs Attention"
t('healthOverview.excellent')        // "Excellent"
t('healthOverview.good')             // "Good"
t('healthOverview.fair')             // "Fair"
t('healthOverview.poor')             // "Poor"
t('healthOverview.critical')         // "Critical"
```

---

### 7. **peopleManagement** → People.tsx
```typescript
// Page Header
t('peopleManagement.title')          // "People"
t('peopleManagement.addPerson')      // "Add Person"

// Form Fields
t('peopleManagement.name')           // "Name"
t('peopleManagement.email')          // "Email"
t('peopleManagement.skills')         // "Skills"
t('peopleManagement.avatar')         // "Avatar URL"
t('peopleManagement.department')     // "Department"

// Actions
t('peopleManagement.editPerson')     // "Edit Person"
t('peopleManagement.deletePerson')   // "Delete Person"
t('peopleManagement.viewHistory')    // "View History"
t('peopleManagement.viewActivity')   // "View Activity"

// Search
t('peopleManagement.search')         // "Search people..."

// Empty State
t('peopleManagement.emptyTitle')     // "No People Found"
t('peopleManagement.emptyDesc')      // "Add team members..."

// Messages
t('peopleManagement.personCreated')  // "Person added successfully"
t('peopleManagement.personUpdated')  // "Person updated successfully"
t('peopleManagement.personDeleted')  // "Person deleted successfully"

// History
t('peopleManagement.teamHistory')    // "Team Assignment History"
t('peopleManagement.workLogActivity') // "Work Log Activity"
t('peopleManagement.noHistory')      // "No team assignment history"
t('peopleManagement.noActivity')     // "No work log activity"
```

---

### 8. **profilePage** → Profile.tsx
```typescript
// Page Header
t('profilePage.title')               // "My Profile"

// Sections
t('profilePage.personalInfo')        // "Personal Information"
t('profilePage.accountSettings')     // "Account Settings"

// Fields
t('profilePage.username')            // "Username"
t('profilePage.email')               // "Email"
t('profilePage.fullName')            // "Full Name"
t('profilePage.bio')                 // "Bio"
t('profilePage.skills')              // "Skills"
t('profilePage.department')          // "Department"
t('profilePage.avatar')              // "Profile Picture"

// Password Change
t('profilePage.changePassword')      // "Change Password"
t('profilePage.currentPassword')     // "Current Password"
t('profilePage.newPassword')         // "New Password"
t('profilePage.confirmPassword')     // "Confirm New Password"
t('profilePage.passwordMismatch')    // "Passwords do not match"
t('profilePage.passwordRequirements') // "Password must be at least 6 characters"

// Actions
t('profilePage.edit')                // "Edit Profile"
t('profilePage.save')                // "Save Changes"
t('profilePage.cancel')              // "Cancel"

// Messages
t('profilePage.updateSuccess')       // "Profile updated successfully"
t('profilePage.passwordChangeSuccess') // "Password changed successfully"
```

---

### 9. **userManagement** → UserManagement.tsx
```typescript
// Page Header
t('userManagement.title')            // "User Management"
t('userManagement.addUser')          // "Add User"

// Table Headers
t('userManagement.username')         // "Username"
t('userManagement.email')            // "Email"
t('userManagement.fullName')         // "Full Name"
t('userManagement.role')             // "Role"
t('userManagement.status')           // "Status"
t('userManagement.createdAt')        // "Created At"
t('userManagement.lastLogin')        // "Last Login"
t('userManagement.actions')          // "Actions"

// Actions
t('userManagement.editUser')         // "Edit User"
t('userManagement.deleteUser')       // "Delete User"
t('userManagement.resetPassword')    // "Reset Password"
t('userManagement.activateUser')     // "Activate User"
t('userManagement.deactivateUser')   // "Deactivate User"

// Roles
t('userManagement.roleAdmin')        // "Administrator"
t('userManagement.roleManager')      // "Manager"
t('userManagement.roleUser')         // "User"
t('userManagement.roleGuest')        // "Guest"

// Status
t('userManagement.statusActive')     // "Active"
t('userManagement.statusInactive')   // "Inactive"
t('userManagement.statusPending')    // "Pending"

// Filters
t('userManagement.search')           // "Search users..."
t('userManagement.filterByRole')     // "Filter by Role"
t('userManagement.filterByStatus')   // "Filter by Status"
```

---

### 10. **organizationSettings** → OrganizationSettings.tsx
```typescript
// Page Header
t('organizationSettings.title')      // "Organization Settings"

// Tabs
t('organizationSettings.general')    // "General"
t('organizationSettings.branding')   // "Branding"
t('organizationSettings.integrations') // "Integrations"
t('organizationSettings.security')   // "Security"

// General Settings
t('organizationSettings.organizationName') // "Organization Name"
t('organizationSettings.description')      // "Description"
t('organizationSettings.website')          // "Website"
t('organizationSettings.timezone')         // "Timezone"
t('organizationSettings.language')         // "Language"

// Integrations
t('organizationSettings.slack')      // "Slack Integration"
t('organizationSettings.teams')      // "Microsoft Teams"
t('organizationSettings.github')     // "GitHub"
t('organizationSettings.jira')       // "Jira"
t('organizationSettings.connected')  // "Connected"
t('organizationSettings.notConnected') // "Not Connected"
t('organizationSettings.connect')    // "Connect"
t('organizationSettings.disconnect') // "Disconnect"

// Security
t('organizationSettings.twoFactorAuth') // "Two-Factor Authentication"
t('organizationSettings.passwordPolicy') // "Password Policy"
t('organizationSettings.sessionTimeout') // "Session Timeout (minutes)"

// Actions
t('organizationSettings.save')       // "Save Settings"
t('organizationSettings.cancel')     // "Cancel"
```

---

## 🎨 **USAGE PATTERNS**

### Simple Text Replacement
```tsx
// ❌ Before
<h1>Reports</h1>

// ✅ After
<h1>{t('reportsPage.title')}</h1>
```

### Button Labels
```tsx
// ❌ Before
<Button>Save Changes</Button>

// ✅ After
<Button>{t('profilePage.save')}</Button>
```

### Form Labels
```tsx
// ❌ Before
<Label>Email</Label>

// ✅ After
<Label>{t('profilePage.email')}</Label>
```

### Placeholders
```tsx
// ❌ Before
<Input placeholder="Search people..." />

// ✅ After
<Input placeholder={t('peopleManagement.search')} />
```

### Select Options
```tsx
// ❌ Before
<SelectItem value="en">English</SelectItem>

// ✅ After
<SelectItem value="en">{t('common.english')}</SelectItem>
```

### Toast Messages
```tsx
// ❌ Before
showToast('Profile updated successfully', 'success')

// ✅ After
showToast(t('profilePage.updateSuccess'), 'success')
```

### Empty States
```tsx
// ❌ Before
<EmptyState
  title="No People Found"
  description="Add team members to start..."
/>

// ✅ After
<EmptyState
  title={t('peopleManagement.emptyTitle')}
  description={t('peopleManagement.emptyDesc')}
/>
```

### Conditional Text
```tsx
// ❌ Before
{status === 'active' ? 'Active' : 'Inactive'}

// ✅ After
{status === 'active' ? t('userManagement.statusActive') : t('userManagement.statusInactive')}
```

---

## ✅ **VERIFICATION**

After implementing, test with:
```tsx
// In browser console
localStorage.setItem('i18nextLng', 'fa') // Switch to Persian
window.location.reload()

localStorage.setItem('i18nextLng', 'en') // Switch to English
window.location.reload()
```

---

**All translation keys are ready! Start implementing page by page using this reference.** 🚀
