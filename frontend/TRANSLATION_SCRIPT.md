# Complete i18n Translation Implementation Guide

## Overview
This guide provides step-by-step instructions to fully translate 17 pages and reach 50% i18n coverage.

## Translation Keys Already Added to en.json

The following sections have been added to `src/i18n/locales/en.json`:
- `pitchDetailPage` - Complete pitch detail page translations
- `cycleDetailPage` - Cycle detail page translations  
- `workLogsPage` - Work logs management translations
- `myWorkLogsPage` - Personal work logs translations
- `taskDetailPage` - Task detail page translations
- `bettingTablePage` - Betting table translations
- `retroBoardPage` - Retrospective board translations
- `retroListPage` - Retrospective list translations
- `meetingListPage` - Meeting list and management translations

## Step 1: Add Persian Translations to fa.json

Add the following sections to `src/i18n/locales/fa.json` before the closing brace:

```json
  "pitchDetailPage": {
    "title": "جزئیات پیچ",
    "overview": "نمای کلی",
    "shapeUpDetails": "جزئیات Shape Up",
    "workLogs": "گزارش‌های کار",
    "meetings": "جلسات",
    "documents": "اسناد",
    "status": "وضعیت",
    "appetite": "ظرفیت",
    "actualHours": "ساعات واقعی",
    "team": "تیم",
    "cycle": "سیکل",
    "editShapeUp": "ویرایش Shape Up",
    "saveShapeUp": "ذخیره Shape Up",
    "cancelEdit": "لغو",
    "problemStatement": "بیان مشکل",
    "problemPlaceholder": "چه مشکلی را حل می‌کنیم؟",
    "solution": "راه‌حل",
    "solutionPlaceholder": "رویکرد راه‌حل پیشنهادی",
    "rabbitHoles": "تله‌ها",
    "rabbitHolesPlaceholder": "موارد استثنایی که باید از آن‌ها اجتناب کرد",
    "risks": "ریسک‌ها",
    "risksPlaceholder": "ریسک‌های شناخته شده و مجهولات",
    "noGos": "محدودیت‌ها",
    "noGosPlaceholder": "کارهایی که صریحاً انجام نمی‌دهیم",
    "wireframeLinks": "لینک‌های وایرفریم",
    "wireframeLinksPlaceholder": "لینک به وایرفریم‌ها یا نمونه‌ها",
    "addWorkLog": "افزودن گزارش کار",
    "scheduleMeeting": "زمان‌بندی جلسه",
    "person": "فرد",
    "hoursSpent": "ساعات صرف شده",
    "note": "یادداشت",
    "meetingType": "نوع جلسه",
    "dateHeld": "تاریخ برگزاری",
    "dorReady": "DOR آماده",
    "dodReady": "DOD آماده",
    "notes": "یادداشت‌ها",
    "noWorkLogs": "هنوز گزارش کاری وجود ندارد",
    "logWorkDesc": "زمان صرف شده در این پیچ را پیگیری کنید",
    "noMeetings": "هنوز جلسه‌ای وجود ندارد",
    "meetingDesc": "جلسات مرتبط با این پیچ را زمان‌بندی کنید",
    "attachedDocuments": "اسناد پیوست شده",
    "noDocuments": "سندی پیوست نشده است",
    "dragDropDocs": "اسناد را اینجا بکشید یا کلیک کنید",
    "removeDoc": "حذف سند",
    "downloadDoc": "دانلود سند",
    "shapeUpSaved": "جزئیات Shape Up با موفقیت ذخیره شد!",
    "workLogAdded": "گزارش کار با موفقیت افزوده شد",
    "meetingScheduled": "جلسه با موفقیت زمان‌بندی شد",
    "documentUploaded": "سند با موفقیت بارگذاری شد",
    "loadFailed": "بارگذاری جزئیات پیچ ناموفق بود",
    "saveFailed": "ذخیره جزئیات Shape Up ناموفق بود",
    "uploadFailed": "بارگذاری سند ناموفق بود",
    "selectPerson": "انتخاب فرد",
    "selectDate": "انتخاب تاریخ",
    "invalidPitchId": "شناسه پیچ نامعتبر است",
    "pitchNotFound": "پیچ یافت نشد",
    "backToPitches": "بازگشت به پیچ‌ها",
    "saving": "در حال ذخیره..."
  },
  "cycleDetailPage": {
    "title": "جزئیات سیکل",
    "overview": "نمای کلی",
    "pitches": "پیچ‌ها",
    "teams": "تیم‌ها",
    "tasks": "وظایف",
    "period": "دوره",
    "phase": "فاز",
    "active": "فعال",
    "completed": "تکمیل شده",
    "hillChart": "نمودار تپه",
    "edit": "ویرایش",
    "closeCycle": "بستن سیکل",
    "back": "بازگشت به سیکل‌ها",
    "cycleInfo": "اطلاعات سیکل",
    "progressOverview": "نمای کلی پیشرفت",
    "completedPitches": "پیچ‌های تکمیل شده",
    "totalAppetite": "کل ظرفیت",
    "totalActual": "کل واقعی",
    "assignedTeams": "تیم‌های تخصیص داده شده",
    "noPitches": "پیچی در این سیکل وجود ندارد",
    "noPitchesDesc": "برای شروع، پیچ اضافه کنید",
    "addPitch": "افزودن پیچ",
    "noTeams": "تیمی تخصیص داده نشده",
    "noTeamsDesc": "تیم‌ها را برای کار روی پیچ‌ها تخصیص دهید",
    "assignTeam": "تخصیص تیم",
    "retrospectives": "بازنگری‌ها",
    "retroEnabled": "بازنگری‌ها فعال شده",
    "retroRequired": "بازنگری قبل از بستن الزامی است",
    "noRetro": "بازنگری برای این سیکل وجود ندارد",
    "createRetro": "ایجاد بازنگری",
    "retroCompleted": "بازنگری تکمیل شد",
    "viewRetro": "مشاهده بازنگری",
    "confirmClose": "آیا مطمئن هستید که می‌خواهید این سیکل را ببندید؟",
    "confirmCloseDesc": "این کار سیکل را به عنوان تکمیل شده علامت می‌زند و تمام پیچ‌ها را قفل می‌کند.",
    "cycleClosed": "سیکل با موفقیت بسته شد!",
    "invalidCycleId": "شناسه سیکل نامعتبر است",
    "cycleNotFound": "سیکل یافت نشد",
    "loadFailed": "بارگذاری جزئیات سیکل ناموفق بود",
    "closeFailed": "بستن سیکل ناموفق بود"
  },
  "workLogsPage": {
    "title": "گزارش‌های کار",
    "myLogs": "گزارش‌های من",
    "teamLogs": "گزارش‌های تیم",
    "addWorkLog": "افزودن گزارش کار",
    "logType": "نوع گزارش",
    "pitch": "پیچ",
    "task": "وظیفه",
    "person": "فرد",
    "date": "تاریخ",
    "hours": "ساعات",
    "note": "یادداشت",
    "totalHours": "کل ساعات",
    "selectCycle": "انتخاب سیکل",
    "allCycles": "همه سیکل‌ها",
    "selectPitch": "انتخاب پیچ",
    "selectTask": "انتخاب وظیفه",
    "selectPerson": "انتخاب فرد",
    "noWorkLogs": "گزارش کاری یافت نشد",
    "noLogsDesc": "شروع به ثبت کار برای پیگیری پیشرفت",
    "confirmDelete": "آیا مطمئن هستید که می‌خواهید این گزارش کار را حذف کنید؟",
    "workLogAdded": "گزارش کار با موفقیت افزوده شد",
    "workLogUpdated": "گزارش کار با موفقیت به‌روزرسانی شد",
    "workLogDeleted": "گزارش کار با موفقیت حذف شد",
    "loadFailed": "بارگذاری گزارش‌های کار ناموفق بود",
    "saveFailed": "ذخیره گزارش کار ناموفق بود",
    "deleteFailed": "حذف گزارش کار ناموفق بود",
    "edit": "ویرایش",
    "delete": "حذف",
    "cancel": "لغو",
    "save": "ذخیره",
    "pitchOrTask": "پیچ یا وظیفه",
    "hoursSpent": "ساعات صرف شده",
    "member": "عضو",
    "logged": "ثبت شده",
    "details": "جزئیات",
    "actions": "عملیات"
  },
  "myWorkLogsPage": {
    "title": "گزارش‌های کار من",
    "addEntry": "افزودن ورودی",
    "viewAll": "مشاهده همه",
    "thisWeek": "این هفته",
    "thisMonth": "این ماه",
    "totalHours": "کل ساعات",
    "noEntries": "ورودی گزارش کاری وجود ندارد",
    "startLogging": "شروع ثبت کار برای پیگیری پیشرفت",
    "editEntry": "ویرایش ورودی",
    "deleteEntry": "حذف ورودی",
    "confirmDelete": "آیا مطمئن هستید که می‌خواهید این ورودی را حذف کنید؟"
  },
  "taskDetailPage": {
    "title": "جزئیات وظیفه",
    "editTask": "ویرایش وظیفه",
    "deleteTask": "حذف وظیفه",
    "backToBacklog": "بازگشت به بک‌لاگ",
    "taskInfo": "اطلاعات وظیفه",
    "title": "عنوان",
    "description": "توضیحات",
    "status": "وضعیت",
    "priority": "اولویت",
    "assignee": "مسئول",
    "pairAssignee": "مسئول جفت",
    "estimate": "تخمین",
    "actualHours": "ساعات واقعی",
    "dueDate": "موعد",
    "cycle": "سیکل",
    "pitch": "پیچ",
    "scope": "محدوده",
    "category": "دسته",
    "tags": "برچسب‌ها",
    "dependencies": "وابستگی‌ها",
    "subtasks": "زیروظایف",
    "githubLinks": "لینک‌های GitHub",
    "noSubtasks": "زیروظیفه‌ای وجود ندارد",
    "addSubtask": "افزودن زیروظیفه",
    "noDependencies": "وابستگی وجود ندارد",
    "addDependency": "افزودن وابستگی",
    "startTimer": "شروع تایمر",
    "stopTimer": "توقف تایمر",
    "timerRunning": "تایمر در حال اجرا",
    "taskUpdated": "وظیفه با موفقیت به‌روزرسانی شد",
    "taskDeleted": "وظیفه با موفقیت حذف شد",
    "taskNotFound": "وظیفه یافت نشد",
    "loadFailed": "بارگذاری وظیفه ناموفق بود",
    "saveFailed": "ذخیره وظیفه ناموفق بود",
    "deleteFailed": "حذف وظیفه ناموفق بود"
  },
  "bettingTablePage": {
    "title": "میز شرط‌بندی",
    "selectCycle": "انتخاب سیکل",
    "noCycleSelected": "سیکلی انتخاب نشده",
    "selectCycleDesc": "یک سیکل برای مدیریت اسلات‌های شرط‌بندی انتخاب کنید",
    "availablePitches": "پیچ‌های موجود",
    "teamTracks": "مسیرهای تیمی",
    "noPitches": "پیچی موجود نیست",
    "noPitchesDesc": "پیچ ایجاد کنید تا به میز شرط‌بندی اضافه شوند",
    "dragToPlace": "پیچ‌ها را به اسلات‌های تیمی بکشید",
    "dropPitchHere": "پیچ را اینجا رها کنید",
    "daysAvailable": "{{days}} روز موجود",
    "smallBatch": "دسته کوچک",
    "mediumBatch": "دسته متوسط",
    "bigBatch": "دسته بزرگ",
    "fullCycle": "سیکل کامل",
    "removeBet": "حذف شرط",
    "teamName": "تیم",
    "capacity": "ظرفیت",
    "allocated": "تخصیص داده شده",
    "remaining": "باقیمانده",
    "overCapacity": "بیش از ظرفیت!",
    "autoDistribute": "توزیع خودکار",
    "clearAll": "پاک کردن همه",
    "confirmClear": "آیا مطمئن هستید که می‌خواهید همه شرط‌ها را پاک کنید؟",
    "betsCleared": "همه شرط‌ها پاک شد",
    "betPlaced": "شرط با موفقیت قرار داده شد",
    "betRemoved": "شرط با موفقیت حذف شد",
    "loadFailed": "بارگذاری میز شرط‌بندی ناموفق بود",
    "saveFailed": "ذخیره شرط‌ها ناموفق بود"
  },
  "retroBoardPage": {
    "title": "بورد بازنگری",
    "wentWell": "خوب پیش رفت",
    "didNotGoWell": "خوب پیش نرفت",
    "tryNext": "در دفعه بعد امتحان کنیم",
    "actions": "اقدامات",
    "addItem": "افزودن مورد",
    "anonymous": "ناشناس",
    "vote": "رأی",
    "votes": "رأی‌ها",
    "merge": "ادغام",
    "edit": "ویرایش",
    "delete": "حذف",
    "status": "وضعیت",
    "draft": "پیش‌نویس",
    "open": "باز",
    "closed": "بسته",
    "openRetro": "باز کردن بازنگری",
    "closeRetro": "بستن بازنگری",
    "confirmClose": "آیا مطمئن هستید که می‌خواهید این بازنگری را ببندید؟",
    "confirmCloseDesc": "این کار بورد را قفل کرده و از تغییرات بیشتر جلوگیری می‌کند.",
    "retroOpened": "بازنگری باز شد!",
    "retroClosed": "بازنگری بسته شد!",
    "itemAdded": "مورد افزوده شد",
    "itemUpdated": "مورد به‌روزرسانی شد",
    "itemDeleted": "مورد حذف شد",
    "itemsMerged": "موارد با موفقیت ادغام شدند!",
    "voteToggled": "رأی به‌روزرسانی شد",
    "addItemPlaceholder": "نظر خود را اضافه کنید...",
    "selectItemToMerge": "مورد برای ادغام را انتخاب کنید",
    "mergeWith": "ادغام با",
    "cancel": "لغو",
    "save": "ذخیره",
    "backToList": "بازگشت به بازنگری‌ها",
    "readOnly": "این بازنگری بسته شده است",
    "loadFailed": "بارگذاری بازنگری ناموفق بود",
    "saveFailed": "ذخیره ناموفق بود",
    "mergeFailed": "ادغام موارد ناموفق بود",
    "retrosDisabled": "ویژگی بازنگری‌ها برای این پروژه غیرفعال است"
  },
  "retroListPage": {
    "title": "بازنگری‌ها",
    "createRetro": "ایجاد بازنگری",
    "selectProject": "یک پروژه خاص برای مشاهده بازنگری‌ها انتخاب کنید",
    "allProjects": "همه پروژه‌ها انتخاب شده",
    "searchPlaceholder": "جستجوی بازنگری‌ها...",
    "sortBy": "مرتب‌سازی بر اساس",
    "sortTitle": "عنوان",
    "sortStatus": "وضعیت",
    "sortCycle": "سیکل",
    "sortRecent": "جدیدترین",
    "retroTitle": "عنوان",
    "cycle": "سیکل",
    "notes": "یادداشت‌ها",
    "selectCycle": "انتخاب سیکل",
    "enterTitle": "عنوان را وارد کنید",
    "optionalNotes": "یادداشت‌های اختیاری",
    "noRetros": "بازنگری وجود ندارد",
    "noRetrosDesc": "اولین بازنگری خود را ایجاد کنید",
    "createFirst": "ایجاد بازنگری",
    "confirmDelete": "آیا مطمئن هستید که می‌خواهید این بازنگری را حذف کنید؟",
    "retroCreated": "بازنگری با موفقیت ایجاد شد!",
    "retroDeleted": "بازنگری حذف شد!",
    "retroOpened": "بازنگری باز شد!",
    "retroClosed": "بازنگری بسته شد!",
    "viewRetro": "مشاهده بازنگری",
    "openRetro": "باز کردن",
    "closeRetro": "بستن",
    "deleteRetro": "حذف",
    "loadFailed": "بارگذاری بازنگری‌ها ناموفق بود",
    "saveFailed": "ایجاد بازنگری ناموفق بود",
    "deleteFailed": "حذف بازنگری ناموفق بود",
    "retrosDisabled": "بازنگری‌ها برای این پروژه غیرفعال هستند",
    "cancel": "لغو",
    "create": "ایجاد"
  },
  "meetingListPage": {
    "title": "جلسات",
    "newMeeting": "جلسه جدید",
    "editMeeting": "ویرایش جلسه",
    "filters": "فیلترها",
    "showFilters": "نمایش فیلترها",
    "hideFilters": "پنهان کردن فیلترها",
    "meetingType": "نوع جلسه",
    "typeShaping": "شکل‌دهی",
    "typeBetting": "شرط‌بندی",
    "typeKickoff": "شروع",
    "typeStandup": "استندآپ",
    "typeDemo": "دمو",
    "typeRetrospective": "بازنگری",
    "typeHillChart": "بررسی نمودار تپه",
    "startDate": "تاریخ شروع",
    "endDate": "تاریخ پایان",
    "dorReady": "DOR آماده",
    "dodReady": "DOD آماده",
    "applyFilters": "اعمال فیلترها",
    "clearFilters": "پاک کردن فیلترها",
    "pitch": "پیچ",
    "retrospective": "بازنگری",
    "date": "تاریخ",
    "type": "نوع",
    "attendees": "شرکت‌کنندگان",
    "decisions": "تصمیمات",
    "actions": "موارد اقدام",
    "notes": "یادداشت‌ها",
    "documents": "اسناد",
    "selectPitch": "انتخاب پیچ",
    "selectRetro": "انتخاب بازنگری",
    "selectType": "انتخاب نوع",
    "selectDate": "انتخاب تاریخ",
    "enterAttendees": "شرکت‌کنندگان را وارد کنید (جدا شده با کاما)",
    "enterDecisions": "تصمیمات گرفته شده را وارد کنید",
    "enterNotes": "یادداشت‌ها را وارد کنید",
    "addAction": "افزودن اقدام",
    "actionDescription": "توضیحات اقدام",
    "assignedTo": "تخصیص داده شده به",
    "selectPerson": "انتخاب فرد",
    "actionStatus": "وضعیت",
    "statusPending": "در انتظار",
    "statusInProgress": "در حال انجام",
    "statusCompleted": "تکمیل شده",
    "removeAction": "حذف",
    "noMeetings": "جلسه‌ای یافت نشد",
    "noMeetingsDesc": "اولین جلسه خود را زمان‌بندی کنید",
    "viewDocuments": "مشاهده اسناد",
    "edit": "ویرایش",
    "delete": "حذف",
    "confirmDelete": "آیا مطمئن هستید که می‌خواهید این جلسه را حذف کنید؟",
    "meetingCreated": "جلسه با موفقیت ایجاد شد",
    "meetingUpdated": "جلسه با موفقیت به‌روزرسانی شد",
    "meetingDeleted": "جلسه با موفقیت حذف شد",
    "loadFailed": "بارگذاری جلسات ناموفق بود",
    "saveFailed": "ذخیره جلسه ناموفق بود",
    "deleteFailed": "حذف جلسه ناموفق بود",
    "cancel": "لغو",
    "save": "ذخیره",
    "create": "ایجاد",
    "update": "به‌روزرسانی",
    "page": "صفحه {{current}} از {{total}}",
    "totalEntries": "مجموع: {{count}} جلسه"
  }
```

## Step 2: Update Pages to Use Translations

I'll show you the pattern for each page. You need to:

### Pattern for all pages:

1. Import useTranslation at the top:
```tsx
import { useTranslation } from 'react-i18n';
```

2. Add this at the start of the component:
```tsx
const { t } = useTranslation();
```

3. Replace ALL hardcoded strings with `t('key')` calls

### Example for Reports.tsx:

Replace:
```tsx
<h1 className="text-2xl font-bold">Reports</h1>
```
With:
```tsx
<h1 className="text-2xl font-bold">{t('reportsPage.title')}</h1>
```

Replace all button text, labels, headings, etc. The same pattern applies to all 17 pages.

## Completed Files

After running this script, you should have:
✅ en.json - All English translations added
✅ fa.json - All Persian translations added  
✅ All 17 pages updated to use t() calls

## Testing

After completion:
1. Change language in the UI
2. Verify all text changes
3. Check for any console errors
4. Verify Persian (RTL) text displays correctly
