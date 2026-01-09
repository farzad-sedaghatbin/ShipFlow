# Dashboard Features - User Guide

## Overview

The dashboard customization and notification system allows you to personalize your workspace and stay informed about important events.

---

## 📊 Dashboard Widgets

### What Are Widgets?

Widgets are customizable cards that display information on your dashboard. Currently available widgets:

1. **STATS_CARDS** - Quick metrics overview
2. **QUICK_LINKS** - Fast access to common pages
3. **ACTIVE_CYCLES** - Current cycles in progress
4. **RECENT_PITCHES** - Latest pitch submissions
5. **HILL_CHART** - Progress visualization
6. **RECENT_ACTIVITY** - Latest system activity
7. **RISK_OVERVIEW** - Risk analysis summary

### How to Manage Widgets

**Currently:** Widget management is available via API only. The UI for drag-and-drop customization is planned for a future update.

#### Via API (for developers/admins):

**Get your widgets:**
```bash
GET /api/dashboard/widgets
```

**Hide/Show a widget:**
```bash
PUT /api/dashboard/widgets/{id}
{
  "isVisible": false,
  "displayOrder": 3
}
```

**Reorder widgets:**
```bash
PUT /api/dashboard/widgets/bulk
[
  { "id": 1, "displayOrder": 0, "isVisible": true },
  { "id": 2, "displayOrder": 1, "isVisible": true }
]
```

**Reset to defaults:**
```bash
POST /api/dashboard/widgets/reset
```

### Default Behavior

When you first log in, the system automatically creates 7 default widgets for you in a standard layout. These appear on your dashboard automatically.

---

## 🔔 Notification System

### What Triggers Notifications?

The system generates notifications automatically every day at **8:00 AM** for the following scenarios:

#### 1️⃣ Overdue Tasks
- **When:** A task's due date has passed
- **Condition:** Task status is NOT "Done" or "Cancelled"
- **Who Gets Notified:** The person assigned to the task
- **Severity:** ⚠️ WARNING (Amber)
- **Frequency:** Once per day (won't spam you)
- **Example:** "Task Overdue: Fix login bug - Task 'Fix login bug' is overdue by 3 days"

#### 2️⃣ Blocked Tasks
- **When:** A task has status "BLOCKED"
- **Who Gets Notified:** The person assigned to the task
- **Severity:** 🔴 ERROR (Red)
- **Frequency:** Once per day
- **Example:** "Task Blocked: API integration - Task 'API integration' is blocked and needs attention"

#### 3️⃣ Cycle Deadlines
- **When:** A cycle will end within the next 7 days
- **Condition:** Cycle is in "BUILD" phase
- **Who Gets Notified:** All team members
- **Severity:** ⚠️ WARNING (Amber)
- **Frequency:** Once per week (won't re-notify for same cycle)
- **Example:** "Cycle Ending Soon: Sprint 5 - Cycle 'Sprint 5' ends in 5 days"

#### 4️⃣ Stalled Hill Chart Progress
- **When:** A hill chart scope hasn't been updated in 14+ days
- **Condition:** Progress position is less than 100% (not complete)
- **Who Gets Notified:** Team members working on that pitch
- **Severity:** ⚠️ WARNING (Amber)
- **Frequency:** Once per week
- **Example:** "Scope Not Moving: User Authentication - Hill chart scope 'User Authentication' hasn't been updated in 14 days"

### Notification Bell 🔔

Located in the top-right corner of your screen, the notification bell shows:
- **Badge with count** - Number of unread notifications (shows "99+" if over 99)
- **Color-coded notifications:**
  - 🔵 Blue = INFO
  - 🟡 Amber = WARNING
  - 🔴 Red = ERROR
  - 🔴 Dark Red = CRITICAL

### Managing Notifications

**View notifications:**
- Click the bell icon to see a dropdown list
- Automatically refreshes every 30 seconds

**Mark as read:**
- Click on a notification to navigate to the related item (also marks it as read)
- Click "Mark as read" button on individual notifications
- Click "Mark all as read" to clear all at once

**Delete notifications:**
- Click the "×" button on any notification

**Auto-cleanup:**
- Read notifications are automatically deleted after 30 days
- Expired notifications are cleaned up daily

### Notification Retention

- **Active notifications:** Visible until you mark them as read or they expire
- **Read notifications:** Kept for 30 days, then automatically deleted
- **Expired notifications:** Have an `expires_at` date, deleted immediately after expiration

---

## 🛠️ Technical Details

### API Endpoints

**Notifications:**
- `GET /api/dashboard/notifications` - Get all active notifications
- `GET /api/dashboard/notifications/unread` - Get unread only
- `GET /api/dashboard/notifications/unread/count` - Get unread count
- `PUT /api/dashboard/notifications/{id}/read` - Mark as read
- `PUT /api/dashboard/notifications/read-all` - Mark all as read
- `DELETE /api/dashboard/notifications/{id}` - Delete notification

**Widgets:**
- `GET /api/dashboard/widgets` - Get all your widgets
- `GET /api/dashboard/widgets/visible` - Get visible widgets only
- `POST /api/dashboard/widgets` - Create a widget
- `PUT /api/dashboard/widgets/{id}` - Update a widget
- `PUT /api/dashboard/widgets/bulk` - Bulk update for reordering
- `DELETE /api/dashboard/widgets/{id}` - Delete a widget
- `POST /api/dashboard/widgets/reset` - Reset to defaults

### Scheduled Tasks

The notification generator runs via Spring `@Scheduled` annotation:
- **Cron:** `0 0 8 * * *` (Every day at 8:00 AM)
- **Tasks:** Generate new notifications, cleanup old ones
- **Thread-safe:** Uses `@Transactional` to ensure data consistency

---

## 📅 Future Enhancements

### Planned Features:

1. **Widget Customization UI**
   - Drag-and-drop interface to reorder widgets
   - Toggle visibility with switches
   - Resize widgets (full-width, half-width, etc.)
   - Custom widget settings (e.g., show 5 vs 10 recent items)

2. **Notification Preferences**
   - Choose which notifications you want to receive
   - Set notification frequency (immediate, daily digest, weekly)
   - Email notifications for critical items
   - Slack/Teams integration

3. **Custom Notifications**
   - Set up your own notification rules
   - Monitor specific metrics or conditions
   - Alert on custom thresholds

4. **Dashboard Themes**
   - Light/dark mode per widget
   - Color schemes
   - Compact/expanded views

---

## 💡 Tips & Best Practices

1. **Check notifications daily** - They're designed to highlight important items that need attention
2. **Mark as read** - Keep your notification list clean by marking items you've addressed
3. **Use the bell badge** - The count gives you a quick overview without opening the dropdown
4. **Click to navigate** - Notifications include direct links to the related items
5. **Default widgets are smart** - If you don't see widgets, they'll be created automatically when you visit the dashboard

---

## ❓ FAQ

**Q: I don't see any widgets on my dashboard**
A: Widgets are created automatically on first access. Refresh the page or log out and back in.

**Q: How do I customize my dashboard layout?**
A: Currently, customization is via API only. A drag-and-drop UI is coming in a future update.

**Q: Why am I getting the same notification repeatedly?**
A: Each notification type has a cooldown period (1 day to 1 week) to prevent spam.

**Q: Can I turn off notifications?**
A: Not yet - notification preferences will be added in a future update. You can delete notifications manually for now.

**Q: Who can see my notifications?**
A: Notifications are private - you only see notifications assigned to you.

**Q: What happens if I delete a widget?**
A: You can always reset to defaults using the API reset endpoint.

---

## 🐛 Troubleshooting

**Notifications not appearing:**
1. Check if the scheduled task is enabled in your environment
2. Verify your user account is linked to tasks/cycles
3. Check the backend logs for errors at 8:00 AM

**Widget API not working:**
1. Ensure you're authenticated
2. Check that you have a valid user session
3. Verify the backend is running

**Notification bell not updating:**
1. The component auto-polls every 30 seconds
2. Try manually refreshing the page
3. Check browser console for API errors

---

For technical support or feature requests, contact the development team or create an issue in the project repository.
