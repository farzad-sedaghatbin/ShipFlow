/**
 * COMPREHENSIVE TRANSLATION COMPLETION GUIDE
 * 
 * This document tracks the full translation implementation for all 17 pages.
 * 
 * STATUS SUMMARY:
 * ✅ Translation keys added to en.json and fa.json for 9 page sections
 * ✅ Import statements exist in all target pages
 * ⏳ Need to add remaining translation keys for 8 more sections
 * ⏳ Need to replace ~1000+ hardcoded strings with t() calls
 * 
 * ========================================
 * PHASE 1: ADD REMAINING TRANSLATION KEYS
 * ========================================
 */

// MISSING SECTIONS (need to add to both en.json and fa.json):
//
// 1. healthOverview section
// 2. backlogPage section  
// 3. pitchBoard section
// 4. peopleManagement section
// 5. profilePage section
// 6. userManagement section
// 7. organizationSettings section
// 8. Additional reportsPage keys

/**
 * ==========================================
 * SECTION 1: healthOverview - HealthOverview.tsx
 * ==========================================
 */
const healthOverviewKeys = {
  en: {
    "healthOverview": {
      "title": "Stakeholder Health Overview",
      "allCycles": "All Active Cycles",
      "singleCycle": "Single Cycle",
      "selectCycle": "Select a cycle",
      "loading": "Loading health data...",
      "noActiveCycles": "No Active Cycles",
      "noActiveCyclesDesc": "There are no currently active cycles. Start a new cycle to track stakeholder health.",
      "noPitches": "No Pitches Found",
      "noPitchesDesc": "This cycle has no pitches yet. Add pitches to track their health status.",
      "cycleName": "Cycle",
      "pitchCount": "Pitches",
      "overallHealth": "Overall Health",
      "atRisk": "At Risk",
      "healthy": "Healthy",
      "needsAttention": "Needs Attention",
      "excellent": "Excellent",
      "good": "Good",
      "fair": "Fair",
      "poor": "Poor",
      "critical": "Critical"
    }
  },
  fa: {
    "healthOverview": {
      "title": "نمای کلی سلامت ذینفعان",
      "allCycles": "همه چرخه‌های فعال",
      "singleCycle": "تک چرخه",
      "selectCycle": "یک چرخه انتخاب کنید",
      "loading": "در حال بارگذاری داده‌های سلامت...",
      "noActiveCycles": "هیچ چرخه فعالی وجود ندارد",
      "noActiveCyclesDesc": "در حال حاضر هیچ چرخه فعالی وجود ندارد. یک چرخه جدید شروع کنید تا سلامت ذینفعان را پیگیری کنید.",
      "noPitches": "هیچ پیچی یافت نشد",
      "noPitchesDesc": "این چرخه هنوز هیچ پیچی ندارد. پیچ اضافه کنید تا وضعیت سلامت آنها را پیگیری کنید.",
      "cycleName": "چرخه",
      "pitchCount": "پیچ‌ها",
      "overallHealth": "سلامت کلی",
      "atRisk": "در معرض خطر",
      "healthy": "سالم",
      "needsAttention": "نیاز به توجه",
      "excellent": "عالی",
      "good": "خوب",
      "fair": "متوسط",
      "poor": "ضعیف",
      "critical": "بحرانی"
    }
  }
};

/**
 * ==========================================
 * SECTION 2: backlogPage - BacklogPage.tsx
 * ==========================================
 */
const backlogPageKeys = {
  en: {
    "backlogPage": {
      "title": "Task Backlog",
      "createTask": "Create Task",
      "editTask": "Edit Task",
      "deleteTask": "Delete Task",
      "deleteConfirm": "Are you sure you want to delete this task?",
      "allTasks": "All Tasks",
      "myTasks": "My Tasks",
      "filterByStatus": "Filter by Status",
      "filterByPriority": "Filter by Priority",
      "filterByCycle": "Filter by Cycle",
      "filterByPitch": "Filter by Pitch",
      "filterByAssignee": "Filter by Assignee",
      "clearFilters": "Clear Filters",
      "search": "Search tasks...",
      "taskName": "Task Name",
      "description": "Description",
      "status": "Status",
      "priority": "Priority",
      "cycle": "Cycle",
      "pitch": "Pitch",
      "assignee": "Assignee",
      "dueDate": "Due Date",
      "estimatedHours": "Estimated Hours",
      "actualHours": "Actual Hours",
      "progress": "Progress",
      "category": "Category",
      "tags": "Tags",
      "attachments": "Attachments",
      "dependencies": "Dependencies",
      "subtasks": "Subtasks",
      "comments": "Comments",
      "activity": "Activity",
      "hill chart": "Hill Chart Position",
      "created": "Created",
      "updated": "Updated",
      "completedAt": "Completed At",
      "startTimer": "Start Timer",
      "stopTimer": "Stop Timer",
      "logWork": "Log Work",
      "addComment": "Add Comment",
      "uploadFile": "Upload File",
      "addSubtask": "Add Subtask",
      "addDependency": "Add Dependency",
      "emptyTitle": "No Tasks Found",
      "emptyDesc": "Create your first task to get started with work tracking.",
      "statistics": "Statistics",
      "totalTasks": "Total Tasks",
      "completedTasks": "Completed Tasks",
      "inProgressTasks": "In Progress Tasks",
      "blockedTasks": "Blocked Tasks",
      "overduefileTasks": "Overdue Tasks",
      "completionRate": "Completion Rate",
      "avgHoursPerTask": "Avg Hours per Task",
      "selectCycle": "Select Cycle",
      "selectPitch": "Select Pitch",
      "selectAssignee": "Select Assignee",
      "selectPriority": "Select Priority",
      "selectStatus": "Select Status",
      "selectCategory": "Select Category",
      "saveTask": "Save Task",
      "cancel": "Cancel",
      "delete": "Delete",
      "confirmDelete": "Confirm Delete",
      "loadingTasks": "Loading tasks...",
      "loadingCycles": "Loading cycles...",
      "loadingPitches": "Loading pitches...",
      "loadingPeople": "Loading people...",
      "taskCreated": "Task created successfully",
      "taskUpdated": "Task updated successfully",
      "taskDeleted": "Task deleted successfully",
      "taskLoadFailed": "Failed to load tasks",
      "taskSaveFailed": "Failed to save task",
      "taskDeleteFailed": "Failed to delete task",
      "statusBacklog": "Backlog",
      "statusTodo": "To Do",
      "statusInProgress": "In Progress",
      "statusBlocked": "Blocked",
      "statusInReview": "In Review",
      "statusDone": "Done",
      "statusCancelled": "Cancelled",
      "priorityLow": "Low",
      "priorityMedium": "Medium",
      "priorityHigh": "High",
      "priorityUrgent": "Urgent",
      "categoryFiguring": "Figuring Things Out",
      "categoryMakingProgress": "Making Progress",
      "categoryNiceTohave": "Nice to Have",
      "categoryMustHave": "Must Have"
    }
  },
  fa: {
    "backlogPage": {
      "title": "باکلاگ تسک‌ها",
      "createTask": "ایجاد تسک",
      "editTask": "ویرایش تسک",
      "deleteTask": "حذف تسک",
      "deleteConfirm": "آیا مطمئن هستید که می‌خواهید این تسک را حذف کنید؟",
      "allTasks": "همه تسک‌ها",
      "myTasks": "تسک‌های من",
      "filterByStatus": "فیلتر بر اساس وضعیت",
      "filterByPriority": "فیلتر بر اساس اولویت",
      "filterByCycle": "فیلتر بر اساس چرخه",
      "filterByPitch": "فیلتر بر اساس پیچ",
      "filterByAssignee": "فیلتر بر اساس مسئول",
      "clearFilters": "پاک کردن فیلترها",
      "search": "جستجوی تسک‌ها...",
      "taskName": "نام تسک",
      "description": "توضیحات",
      "status": "وضعیت",
      "priority": "اولویت",
      "cycle": "چرخه",
      "pitch": "پیچ",
      "assignee": "مسئول",
      "dueDate": "تاریخ سررسید",
      "estimatedHours": "ساعت تخمینی",
      "actualHours": "ساعت واقعی",
      "progress": "پیشرفت",
      "category": "دسته‌بندی",
      "tags": "برچسب‌ها",
      "attachments": "پیوست‌ها",
      "dependencies": "وابستگی‌ها",
      "subtasks": "زیرتسک‌ها",
      "comments": "نظرات",
      "activity": "فعالیت",
      "hillChart": "موقعیت نمودار تپه",
      "created": "ایجاد شده",
      "updated": "بروزرسانی شده",
      "completedAt": "تکمیل شده در",
      "startTimer": "شروع تایمر",
      "stopTimer": "توقف تایمر",
      "logWork": "ثبت کار",
      "addComment": "افزودن نظر",
      "uploadFile": "آپلود فایل",
      "addSubtask": "افزودن زیرتسک",
      "addDependency": "افزودن وابستگی",
      "emptyTitle": "هیچ تسکی یافت نشد",
      "emptyDesc": "اولین تسک خود را ایجاد کنید تا پیگیری کار را شروع کنید.",
      "statistics": "آمار",
      "totalTasks": "کل تسک‌ها",
      "completedTasks": "تسک‌های تکمیل شده",
      "inProgressTasks": "تسک‌های در حال انجام",
      "blockedTasks": "تسک‌های مسدود شده",
      "overdueTasks": "تسک‌های عقب افتاده",
      "completionRate": "نرخ تکمیل",
      "avgHoursPerTask": "میانگین ساعت به ازای هر تسک",
      "selectCycle": "انتخاب چرخه",
      "selectPitch": "انتخاب پیچ",
      "selectAssignee": "انتخاب مسئول",
      "selectPriority": "انتخاب اولویت",
      "selectStatus": "انتخاب وضعیت",
      "selectCategory": "انتخاب دسته‌بندی",
      "saveTask": "ذخیره تسک",
      "cancel": "لغو",
      "delete": "حذف",
      "confirmDelete": "تایید حذف",
      "loadingTasks": "در حال بارگذاری تسک‌ها...",
      "loadingCycles": "در حال بارگذاری چرخه‌ها...",
      "loadingPitches": "در حال بارگذاری پیچ‌ها...",
      "loadingPeople": "در حال بارگذاری افراد...",
      "taskCreated": "تسک با موفقیت ایجاد شد",
      "taskUpdated": "تسک با موفقیت بروزرسانی شد",
      "taskDeleted": "تسک با موفقیت حذف شد",
      "taskLoadFailed": "بارگذاری تسک‌ها ناموفق بود",
      "taskSaveFailed": "ذخیره تسک ناموفق بود",
      "taskDeleteFailed": "حذف تسک ناموفق بود",
      "statusBacklog": "باکلاگ",
      "statusTodo": "انجام شود",
      "statusInProgress": "در حال انجام",
      "statusBlocked": "مسدود شده",
      "statusInReview": "در حال بررسی",
      "statusDone": "انجام شده",
      "statusCancelled": "لغو شده",
      "priorityLow": "پایین",
      "priorityMedium": "متوسط",
      "priorityHigh": "بالا",
      "priorityUrgent": "فوری",
      "categoryFiguring": "در حال فهمیدن موارد",
      "categoryMakingProgress": "در حال پیشرفت",
      "categoryNiceToHave": "خوب است داشته باشیم",
      "categoryMustHave": "باید داشته باشیم"
    }
  }
};

/**
 * ==========================================
 * SECTION 3: pitchBoard - PitchBoard.tsx
 * ==========================================
 */
const pitchBoardKeys = {
  en: {
    "pitchBoard": {
      "title": "Pitch Board",
      "createPitch": "Create Pitch",
      "editPitch": "Edit Pitch",
      "deletePitch": "Delete Pitch",
      "movePitch": "Move Pitch",
      "viewDetails": "View Details",
      "column Idea": "Idea",
      "columnDraft": "Draft",
      "columnReady": "Ready to Bet",
      "columnBetting": "In Betting Table",
      "columnApproved": "Approved",
      "columnInProgress": "In Progress",
      "columnCompleted": "Completed",
      "columnRejected": "Rejected",
      "emptyColumn": "No pitches in this stage",
      "dragToMove": "Drag to move between stages",
      "appetite": "Appetite",
      "smallBatch": "Small Batch (1-2 weeks)",
      "bigBatch": "Big Batch (6 weeks)",
      "assignedTo": "Assigned To",
      "createdBy": "Created By",
      "lastUpdated": "Last Updated",
      "noDescription": "No description provided",
      "pitchCount": "{{count}} pitches",
      "filterByCycle": "Filter by Cycle",
      "filterByPerson": "Filter by Person",
      "showAll": "Show All",
      "sortBy": "Sort By",
      "sortByDate": "Date",
      "sortByName": "Name",
      "sortByAppetite": "Appetite",
      "confirmDelete": "Are you sure you want to delete this pitch?",
      "deleteSuccess": "Pitch deleted successfully",
      "moveSuccess": "Pitch moved successfully",
      "loadFailed": "Failed to load pitches"
    }
  },
  fa: {
    "pitchBoard": {
      "title": "تابلوی پیچ",
      "createPitch": "ایجاد پیچ",
      "editPitch": "ویرایش پیچ",
      "deletePitch": "حذف پیچ",
      "movePitch": "جابجایی پیچ",
      "viewDetails": "مشاهده جزئیات",
      "columnIdea": "ایده",
      "columnDraft": "پیش‌نویس",
      "columnReady": "آماده برای شرط‌بندی",
      "columnBetting": "در میز شرط‌بندی",
      "columnApproved": "تایید شده",
      "columnInProgress": "در حال انجام",
      "columnCompleted": "تکمیل شده",
      "columnRejected": "رد شده",
      "emptyColumn": "هیچ پیچی در این مرحله نیست",
      "dragToMove": "برای جابجایی بین مراحل بکشید",
      "appetite": "اشتها",
      "smallBatch": "دسته کوچک (۱-۲ هفته)",
      "bigBatch": "دسته بزرگ (۶ هفته)",
      "assignedTo": "اختصاص داده شده به",
      "createdBy": "ایجاد شده توسط",
      "lastUpdated": "آخرین بروزرسانی",
      "noDescription": "هیچ توضیحی ارائه نشده است",
      "pitchCount": "{{count}} پیچ",
      "filterByCycle": "فیلتر بر اساس چرخه",
      "filterByPerson": "فیلتر بر اساس شخص",
      "showAll": "نمایش همه",
      "sortBy": "مرتب‌سازی بر اساس",
      "sortByDate": "تاریخ",
      "sortByName": "نام",
      "sortByAppetite": "اشتها",
      "confirmDelete": "آیا مطمئن هستید که می‌خواهید این پیچ را حذف کنید؟",
      "deleteSuccess": "پیچ با موفقیت حذف شد",
      "moveSuccess": "پیچ با موفقیت جابجا شد",
      "loadFailed": "بارگذاری پیچ‌ها ناموفق بود"
    }
  }
};

/**
 * ==========================================
 * SECTION 4: peopleManagement - People.tsx
 * ==========================================
 */
const peopleManagementKeys = {
  en: {
    "peopleManagement": {
      "title": "People",
      "addPerson": "Add Person",
      "editPerson": "Edit Person",
      "deletePerson": "Delete Person",
      "viewHistory": "View History",
      "viewActivity": "View Activity",
      "name": "Name",
      "email": "Email",
      "skills": "Skills",
      "avatar": "Avatar URL",
      "role": "Role",
      "department": "Department",
      "joinedDate": "Joined Date",
      "activeAssignments": "Active Assignments",
      "totalWorkLogs": "Total Work Logs",
      "totalHours": "Total Hours Logged",
      "search": "Search people...",
      "emptyTitle": "No People Found",
      "emptyDesc": "Add team members to start building your organization.",
      "confirmDelete": "Are you sure you want to delete this person?",
      "deleteWarning": "This will remove all team assignments and work logs.",
      "personCreated": "Person added successfully",
      "personUpdated": "Person updated successfully",
      "personDeleted": "Person deleted successfully",
      "loadFailed": "Failed to load people",
      "saveFailed": "Failed to save person",
      "deleteFailed": "Failed to delete person",
      "teamHistory": "Team Assignment History",
      "workLogActivity": "Work Log Activity",
      "cycle": "Cycle",
      "pitch": "Pitch",
      "team": "Team",
      "role": "Role",
      "startDate": "Start Date",
      "endDate": "End Date",
      "active": "Active",
      "completed": "Completed",
      "task": "Task",
      "date": "Date",
      "hours": "Hours",
      "description": "Description",
      "noHistory": "No team assignment history",
      "noActivity": "No work log activity",
      "loading": "Loading...",
      "cancel": "Cancel",
      "save": "Save",
      "close": "Close"
    }
  },
  fa: {
    "peopleManagement": {
      "title": "افراد",
      "addPerson": "افزودن فرد",
      "editPerson": "ویرایش فرد",
      "deletePerson": "حذف فرد",
      "viewHistory": "مشاهده تاریخچه",
      "viewActivity": "مشاهده فعالیت",
      "name": "نام",
      "email": "ایمیل",
      "skills": "مهارت‌ها",
      "avatar": "آدرس تصویر",
      "role": "نقش",
      "department": "بخش",
      "joinedDate": "تاریخ پیوستن",
      "activeAssignments": "تخصیص‌های فعال",
      "totalWorkLogs": "کل گزارش‌های کار",
      "totalHours": "کل ساعات ثبت شده",
      "search": "جستجوی افراد...",
      "emptyTitle": "هیچ فردی یافت نشد",
      "emptyDesc": "اعضای تیم را اضافه کنید تا ساخت سازمان خود را شروع کنید.",
      "confirmDelete": "آیا مطمئن هستید که می‌خواهید این فرد را حذف کنید؟",
      "deleteWarning": "این کار همه تخصیص‌های تیم و گزارش‌های کار را حذف خواهد کرد.",
      "personCreated": "فرد با موفقیت اضافه شد",
      "personUpdated": "فرد با موفقیت بروزرسانی شد",
      "personDeleted": "فرد با موفقیت حذف شد",
      "loadFailed": "بارگذاری افراد ناموفق بود",
      "saveFailed": "ذخیره فرد ناموفق بود",
      "deleteFailed": "حذف فرد ناموفق بود",
      "teamHistory": "تاریخچه تخصیص تیم",
      "workLogActivity": "فعالیت گزارش کار",
      "cycle": "چرخه",
      "pitch": "پیچ",
      "team": "تیم",
      "role": "نقش",
      "startDate": "تاریخ شروع",
      "endDate": "تاریخ پایان",
      "active": "فعال",
      "completed": "تکمیل شده",
      "task": "تسک",
      "date": "تاریخ",
      "hours": "ساعت",
      "description": "توضیحات",
      "noHistory": "بدون تاریخچه تخصیص تیم",
      "noActivity": "بدون فعالیت گزارش کار",
      "loading": "در حال بارگذاری...",
      "cancel": "لغو",
      "save": "ذخیره",
      "close": "بستن"
    }
  }
};

/**
 * ==========================================
 * SECTION 5: profilePage - Profile.tsx
 * ==========================================
 */
const profilePageKeys = {
  en: {
    "profilePage": {
      "title": "My Profile",
      "personalInfo": "Personal Information",
      "accountSettings": "Account Settings",
      "edit": "Edit Profile",
      "save": "Save Changes",
      "cancel": "Cancel",
      "changePassword": "Change Password",
      "username": "Username",
      "email": "Email",
      "fullName": "Full Name",
      "bio": "Bio",
      "skills": "Skills",
      "department": "Department",
      "avatar": "Profile Picture",
      "uploadAvatar": "Upload Avatar",
      "currentPassword": "Current Password",
      "newPassword": "New Password",
      "confirmPassword": "Confirm New Password",
      "passwordRequirements": "Password must be at least 6 characters",
      "passwordMismatch": "Passwords do not match",
      "accountInfo": "Account Information",
      "memberSince": "Member Since",
      "lastLogin": "Last Login",
      "role": "Role",
      "status": "Status",
      "active": "Active",
      "updateSuccess": "Profile updated successfully",
      "updateFailed": "Failed to update profile",
      "passwordChangeSuccess": "Password changed successfully",
      "passwordChangeFailed": "Failed to change password",
      "loadFailed": "Failed to load profile",
      "showPassword": "Show password",
      "hidePassword": "Hide password",
      "saving": "Saving..."
    }
  },
  fa: {
    "profilePage": {
      "title": "پروفایل من",
      "personalInfo": "اطلاعات شخصی",
      "accountSettings": "تنظیمات حساب",
      "edit": "ویرایش پروفایل",
      "save": "ذخیره تغییرات",
      "cancel": "لغو",
      "changePassword": "تغییر رمز عبور",
      "username": "نام کاربری",
      "email": "ایمیل",
      "fullName": "نام کامل",
      "bio": "بیوگرافی",
      "skills": "مهارت‌ها",
      "department": "بخش",
      "avatar": "تصویر پروفایل",
      "uploadAvatar": "آپلود تصویر",
      "currentPassword": "رمز عبور فعلی",
      "newPassword": "رمز عبور جدید",
      "confirmPassword": "تایید رمز عبور جدید",
      "passwordRequirements": "رمز عبور باید حداقل ۶ کاراکتر باشد",
      "passwordMismatch": "رمزهای عبور مطابقت ندارند",
      "accountInfo": "اطلاعات حساب",
      "memberSince": "عضویت از",
      "lastLogin": "آخرین ورود",
      "role": "نقش",
      "status": "وضعیت",
      "active": "فعال",
      "updateSuccess": "پروفایل با موفقیت بروزرسانی شد",
      "updateFailed": "بروزرسانی پروفایل ناموفق بود",
      "passwordChangeSuccess": "رمز عبور با موفقیت تغییر کرد",
      "passwordChangeFailed": "تغییر رمز عبور ناموفق بود",
      "loadFailed": "بارگذاری پروفایل ناموفق بود",
      "showPassword": "نمایش رمز عبور",
      "hidePassword": "پنهان کردن رمز عبور",
      "saving": "در حال ذخیره..."
    }
  }
};

/**
 * ==========================================
 * SECTION 6: userManagement - UserManagement.tsx  
 * ==========================================
 */
const userManagementKeys = {
  en: {
    "userManagement": {
      "title": "User Management",
      "addUser": "Add User",
      "editUser": "Edit User",
      "deleteUser": "Delete User",
      "resetPassword": "Reset Password",
      "activateUser": "Activate User",
      "deactivateUser": "Deactivate User",
      "username": "Username",
      "email": "Email",
      "fullName": "Full Name",
      "role": "Role",
      "status": "Status",
      "createdAt": "Created At",
      "lastLogin": "Last Login",
      "actions": "Actions",
      "search": "Search users...",
      "filterByRole": "Filter by Role",
      "filterByStatus": "Filter by Status",
      "allRoles": "All Roles",
      "allStatuses": "All Statuses",
      "emptyTitle": "No Users Found",
      "emptyDesc": "Add users to manage access to your organization.",
      "confirmDelete": "Are you sure you want to delete this user?",
      "confirmDeactivate": "Are you sure you want to deactivate this user?",
      "confirmActivate": "Are you sure you want to activate this user?",
      "userCreated": "User created successfully",
      "userUpdated": "User updated successfully",
      "userDeleted": "User deleted successfully",
      "userActivated": "User activated successfully",
      "userDeactivated": "User deactivated successfully",
      "passwordReset": "Password reset email sent",
      "loadFailed": "Failed to load users",
      "saveFailed": "Failed to save user",
      "deleteFailed": "Failed to delete user",
      "roleAdmin": "Administrator",
      "roleManager": "Manager",
      "roleUser": "User",
      "roleGuest": "Guest",
      "statusActive": "Active",
      "statusInactive": "Inactive",
      "statusPending": "Pending",
      "cancel": "Cancel",
      "save": "Save",
      "totalUsers": "Total Users",
      "activeUsers": "Active Users",
      "pendingUsers": "Pending Users"
    }
  },
  fa: {
    "userManagement": {
      "title": "مدیریت کاربران",
      "addUser": "افزودن کاربر",
      "editUser": "ویرایش کاربر",
      "deleteUser": "حذف کاربر",
      "resetPassword": "بازنشانی رمز عبور",
      "activateUser": "فعال‌سازی کاربر",
      "deactivateUser": "غیرفعال‌سازی کاربر",
      "username": "نام کاربری",
      "email": "ایمیل",
      "fullName": "نام کامل",
      "role": "نقش",
      "status": "وضعیت",
      "createdAt": "ایجاد شده در",
      "lastLogin": "آخرین ورود",
      "actions": "اقدامات",
      "search": "جستجوی کاربران...",
      "filterByRole": "فیلتر بر اساس نقش",
      "filterByStatus": "فیلتر بر اساس وضعیت",
      "allRoles": "همه نقش‌ها",
      "allStatuses": "همه وضعیت‌ها",
      "emptyTitle": "هیچ کاربری یافت نشد",
      "emptyDesc": "کاربران را اضافه کنید تا دسترسی به سازمان خود را مدیریت کنید.",
      "confirmDelete": "آیا مطمئن هستید که می‌خواهید این کاربر را حذف کنید؟",
      "confirmDeactivate": "آیا مطمئن هستید که می‌خواهید این کاربر را غیرفعال کنید؟",
      "confirmActivate": "آیا مطمئن هستید که می‌خواهید این کاربر را فعال کنید؟",
      "userCreated": "کاربر با موفقیت ایجاد شد",
      "userUpdated": "کاربر با موفقیت بروزرسانی شد",
      "userDeleted": "کاربر با موفقیت حذف شد",
      "userActivated": "کاربر با موفقیت فعال شد",
      "userDeactivated": "کاربر با موفقیت غیرفعال شد",
      "passwordReset": "ایمیل بازنشانی رمز عبور ارسال شد",
      "loadFailed": "بارگذاری کاربران ناموفق بود",
      "saveFailed": "ذخیره کاربر ناموفق بود",
      "deleteFailed": "حذف کاربر ناموفق بود",
      "roleAdmin": "مدیر سیستم",
      "roleManager": "مدیر",
      "roleUser": "کاربر",
      "roleGuest": "مهمان",
      "statusActive": "فعال",
      "statusInactive": "غیرفعال",
      "statusPending": "در انتظار",
      "cancel": "لغو",
      "save": "ذخیره",
      "totalUsers": "کل کاربران",
      "activeUsers": "کاربران فعال",
      "pendingUsers": "کاربران در انتظار"
    }
  }
};

/**
 * ==========================================
 * SECTION 7: organizationSettings - OrganizationSettings.tsx
 * ==========================================
 */
const organizationSettingsKeys = {
  en: {
    "organizationSettings": {
      "title": "Organization Settings",
      "general": "General",
      "branding": "Branding",
      "integrations": "Integrations",
      "security": "Security",
      "organizationName": "Organization Name",
      "description": "Description",
      "website": "Website",
      "logo": "Logo URL",
      "primaryColor": "Primary Color",
      "accentColor": "Accent Color",
      "timezone": "Timezone",
      "dateFormat": "Date Format",
      "timeFormat": "Time Format",
      "language": "Language",
      "currency": "Currency",
      "save": "Save Settings",
      "cancel": "Cancel",
      "slack": "Slack Integration",
      "teams": "Microsoft Teams",
      "github": "GitHub",
      "jira": "Jira",
      "connected": "Connected",
      "notConnected": "Not Connected",
      "connect": "Connect",
      "disconnect": "Disconnect",
      "configure": "Configure",
      "twoFactorAuth": "Two-Factor Authentication",
      "enable": "Enable",
      "disable": "Disable",
      "passwordPolicy": "Password Policy",
      "minLength": "Minimum Length",
      "requireUppercase": "Require Uppercase",
      "requireLowercase": "Require Lowercase",
      "requireNumbers": "Require Numbers",
      "requireSymbols": "Require Symbols",
      "sessionTimeout": "Session Timeout (minutes)",
      "ipWhitelist": "IP Whitelist",
      "updateSuccess": "Settings updated successfully",
      "updateFailed": "Failed to update settings",
      "loadFailed": "Failed to load settings",
      "confirmDisconnect": "Are you sure you want to disconnect this integration?",
      "slackWorkspace": "Slack Workspace",
      "teamsOrganization": "Teams Organization",
      "githubOrganization": "GitHub Organization",
      "jiraProject": "Jira Project",
      "notificationSettings": "Notification Settings",
      "emailNotifications": "Email Notifications",
      "slackNotifications": "Slack Notifications",
      "teamsNotifications": "Teams Notifications",
      "dataRetention": "Data Retention",
      "retentionPeriod": "Retention Period (days)",
      "autoBackup": "Auto Backup",
      "backupFrequency": "Backup Frequency",
      "daily": "Daily",
      "weekly": "Weekly",
      "monthly": "Monthly"
    }
  },
  fa: {
    "organizationSettings": {
      "title": "تنظیمات سازمان",
      "general": "عمومی",
      "branding": "برندینگ",
      "integrations": "یکپارچه‌سازی‌ها",
      "security": "امنیت",
      "organizationName": "نام سازمان",
      "description": "توضیحات",
      "website": "وب‌سایت",
      "logo": "آدرس لوگو",
      "primaryColor": "رنگ اصلی",
      "accentColor": "رنگ تاکیدی",
      "timezone": "منطقه زمانی",
      "dateFormat": "فرمت تاریخ",
      "timeFormat": "فرمت زمان",
      "language": "زبان",
      "currency": "ارز",
      "save": "ذخیره تنظیمات",
      "cancel": "لغو",
      "slack": "یکپارچه‌سازی Slack",
      "teams": "Microsoft Teams",
      "github": "GitHub",
      "jira": "Jira",
      "connected": "متصل شده",
      "notConnected": "متصل نشده",
      "connect": "اتصال",
      "disconnect": "قطع اتصال",
      "configure": "پیکربندی",
      "twoFactorAuth": "احراز هویت دو مرحله‌ای",
      "enable": "فعال‌سازی",
      "disable": "غیرفعال‌سازی",
      "passwordPolicy": "سیاست رمز عبور",
      "minLength": "حداقل طول",
      "requireUppercase": "نیاز به حروف بزرگ",
      "requireLowercase": "نیاز به حروف کوچک",
      "requireNumbers": "نیاز به اعداد",
      "requireSymbols": "نیاز به نمادها",
      "sessionTimeout": "زمان انقضای نشست (دقیقه)",
      "ipWhitelist": "لیست سفید IP",
      "updateSuccess": "تنظیمات با موفقیت بروزرسانی شد",
      "updateFailed": "بروزرسانی تنظیمات ناموفق بود",
      "loadFailed": "بارگذاری تنظیمات ناموفق بود",
      "confirmDisconnect": "آیا مطمئن هستید که می‌خواهید این یکپارچه‌سازی را قطع کنید؟",
      "slackWorkspace": "فضای کاری Slack",
      "teamsOrganization": "سازمان Teams",
      "githubOrganization": "سازمان GitHub",
      "jiraProject": "پروژه Jira",
      "notificationSettings": "تنظیمات اعلان",
      "emailNotifications": "اعلان‌های ایمیل",
      "slackNotifications": "اعلان‌های Slack",
      "teamsNotifications": "اعلان‌های Teams",
      "dataRetention": "نگهداری داده",
      "retentionPeriod": "دوره نگهداری (روز)",
      "autoBackup": "پشتیبان‌گیری خودکار",
      "backupFrequency": "فرکانس پشتیبان‌گیری",
      "daily": "روزانه",
      "weekly": "هفتگی",
      "monthly": "ماهانه"
    }
  }
};

/**
 * ==========================================
 * SECTION 8: Additional reportsPage Keys
 * ==========================================
 */
const additionalReportsPageKeys = {
  en: {
    "reportsPage": {
      // ... existing keys ...
      "varianceHours": "Variance (Hours)",
      "variancePercent": "Variance (%)",
      "teamMembers": "Team Members",
      "avgHoursPerMember": "Avg Hours/Member",
      "outOfScopeWork": "Out-of-Scope Work (Tasks)",
      "totalTasks": "Total Tasks",
      "completedTasks": "Completed Tasks",
      "estHours": "Est. Hours",
      "appetiteVsActual": "Appetite vs Actual Hours by Pitch",
      "pitchStatusDistribution": "Pitch Status Distribution",
      "appetite": "Appetite (h)",
      "actual": "Actual (h)",
      "pitch": "Pitch",
      "variance": "Variance"
    }
  },
  fa: {
    "reportsPage": {
      // ... existing keys ...
      "varianceHours": "واریانس (ساعت)",
      "variancePercent": "واریانس (%)",
      "teamMembers": "اعضای تیم",
      "avgHoursPerMember": "میانگین ساعت/عضو",
      "outOfScopeWork": "کار خارج از محدوده (تسک‌ها)",
      "totalTasks": "کل تسک‌ها",
      "completedTasks": "تسک‌های تکمیل شده",
      "estHours": "ساعت تخمینی",
      "appetiteVsActual": "اشتها در مقابل ساعات واقعی بر اساس پیچ",
      "pitchStatusDistribution": "توزیع وضعیت پیچ",
      "appetite": "اشتها (ساعت)",
      "actual": "واقعی (ساعت)",
      "pitch": "پیچ",
      "variance": "واریانس"
    }
  }
};

console.log("=== COMPREHENSIVE TRANSLATION COMPLETION GUIDE ===");
console.log("");
console.log("NEXT STEPS:");
console.log("1. Copy the translation keys from this file");
console.log("2. Add them to en.json and fa.json respectively");
console.log("3. Update each of the 17 pages to use t() calls");
console.log("4. Test language switching between English and Persian");
console.log("5. Verify RTL (right-to-left) display for Persian");
console.log("");
console.log("Translation keys are ready for all 17 pages!");

export { 
  healthOverviewKeys,
  backlogPageKeys,
  pitchBoardKeys,
  peopleManagementKeys,
  profilePageKeys,
  userManagementKeys,
  organizationSettingsKeys,
  additionalReportsPageKeys
};
