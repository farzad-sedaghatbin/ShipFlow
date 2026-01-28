# Date/Time & Timezone Architecture

## Overview

ShipFlow uses a **consistent UTC backend** with **localized frontend** architecture for handling dates, times, and calendars.

## Architecture Principles

### 🗄️ Backend (Database Layer)
- **Storage**: All dates stored in **UTC** (Universal Time Coordinated)
- **Format**: ISO 8601 format in database: `2026-01-16T12:30:00.000Z`
- **Consistency**: No timezone conversions in backend logic
- **API Response**: Always returns UTC dates in ISO format

### 🎨 Frontend (Presentation Layer)
- **Parsing**: Receives UTC ISO dates from backend
- **Conversion**: Converts to user's browser timezone automatically
- **Display**: Shows dates in user's locale format:
  - **Persian (fa)**: Jalali calendar + Persian digits (۱۴۰۴/۱۰/۲۷)
  - **English (en)**: Gregorian calendar (Jan 16, 2026)
  - **Spanish (es)**: Gregorian calendar (16 ene 2026)

---

## Configuration

### Backend Configuration

**File**: `backend/src/main/resources/application.properties`

```properties
# Jackson JSON Configuration
# Ensure all dates are serialized in ISO-8601 format (UTC)
spring.jackson.time-zone=UTC
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss.SSSXXX
spring.jackson.default-property-inclusion=non_null

# Timezone - All server dates in UTC
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

### Frontend Configuration

**File**: `frontend/src/lib/locale.ts`

The locale utilities handle:
- ✅ Timezone detection via `Intl.DateTimeFormat().resolvedOptions().timeZone`
- ✅ UTC to local timezone conversion
- ✅ Jalali calendar for Persian
- ✅ Persian digit conversion (۰-۹)
- ✅ RTL support for Persian

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     DATABASE (PostgreSQL/H2)                 │
│                 Stores: 2026-01-16 12:30:00 UTC             │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  BACKEND (Spring Boot)                       │
│          Reads UTC, sends ISO: "2026-01-16T12:30:00.000Z"   │
│          Jackson config ensures UTC serialization            │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP/JSON
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                 FRONTEND (React + locale.ts)                 │
│   1. Receives: "2026-01-16T12:30:00.000Z" (UTC ISO)         │
│   2. Detects user timezone: "America/New_York"              │
│   3. Converts: 12:30 UTC → 07:30 EST                        │
│   4. Formats by locale:                                      │
│      • en: "Jan 16, 2026, 7:30 AM"                          │
│      • fa: "۲۷ دی ۱۴۰۴، ۷:۳۰"  (Jalali + Persian digits)   │
│      • es: "16 ene 2026, 7:30"                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Frontend Usage

### Basic Date Formatting

```tsx
import { formatDate, formatDateShort, formatDateTime } from '@/lib/locale';

// Current locale: Persian (fa)
formatDate('2026-01-16T12:30:00.000Z', 'PPP')
// Output: ۲۷ دی ۱۴۰۴

formatDateShort('2026-01-16T12:30:00.000Z')
// Output: ۱۴۰۴/۱۰/۲۷

formatDateTime('2026-01-16T12:30:00.000Z')
// Output: ۲۷ دی ۱۴۰۴، ۱۶:۰۰ (if user is in Asia/Tehran UTC+3:30)
```

### Using LocalizedDate Component

```tsx
import { LocalizedDate } from '@/components/LocalizedDate';

function MyComponent() {
  // Backend sends UTC ISO string
  const createdAt = "2026-01-16T12:30:00.000Z";
  
  return (
    <div>
      {/* Short format with tooltip */}
      <LocalizedDate date={createdAt} format="short" />
      
      {/* Relative time: "2 hours ago" / "۲ ساعت پیش" */}
      <LocalizedDate date={createdAt} format="relative" />
      
      {/* Full datetime */}
      <LocalizedDate date={createdAt} format="datetime" />
      
      {/* Custom format */}
      <LocalizedDate date={createdAt} format="dd MMM yyyy" />
    </div>
  );
}
```

### Timezone Utilities

```tsx
import { getUserTimezone, toUserTimezone, formatInUserTimezone } from '@/lib/locale';

// Get user's timezone
const timezone = getUserTimezone();
// Returns: "America/New_York", "Asia/Tehran", etc.

// Convert UTC to user's local time
const utcDate = new Date("2026-01-16T12:30:00.000Z");
const localDate = toUserTimezone(utcDate);
// localDate is now in user's timezone

// Format in specific timezone
const formatted = formatInUserTimezone(utcDate, 'PPpp');
```

---

## Calendar Systems

### Gregorian Calendar (English, Spanish)

```
Date: 2026-01-16
Format: January 16, 2026
ISO Week: Week 3, 2026
```

### Jalali Calendar (Persian/Farsi)

```
Date: 1404/10/27 (۱۴۰۴/۱۰/۲۷)
Month: Dey (دی)
Format: ۲۷ دی ۱۴۰۴
```

**Jalali Months**:
1. Farvardin (فروردین)
2. Ordibehesht (اردیبهشت)
3. Khordad (خرداد)
4. Tir (تیر)
5. Mordad (مرداد)
6. Shahrivar (شهریور)
7. Mehr (مهر)
8. Aban (آبان)
9. Azar (آذر)
10. Dey (دی) ← Current
11. Bahman (بهمن)
12. Esfand (اسفند)

---

## Number Formatting

### Persian Digits

```tsx
import { formatNumber, toPersianDigits } from '@/lib/locale';

// Auto-converts for Persian locale
formatNumber(1234)
// fa: "۱٬۲۳۴"
// en: "1,234"

// Manual conversion
toPersianDigits(2026)
// Output: "۲۰۲۶"
```

### Currency

```tsx
import { formatCurrency } from '@/lib/locale';

formatCurrency(1500.50, 'USD')
// en: "$1,500.50"
// fa: "۱٬۵۰۰٫۵۰ IRR" (auto-switches to Rial for Persian)
// es: "1.500,50 US$"
```

---

## RTL Support

### Automatic RTL Switching

When user selects Persian:
```html
<html dir="rtl" lang="fa">
  <body class="rtl">
    <!-- All content flows right-to-left -->
  </body>
</html>
```

### CSS RTL Utilities (in `index.css`)

```css
/* Automatic direction switching */
html[dir="rtl"] { direction: rtl; }
html[dir="rtl"] input { text-align: right; }

/* Numbers stay LTR even in RTL */
html[dir="rtl"] input[type="number"] { direction: ltr; }

/* Persian font support */
html[lang="fa"] {
  font-family: "Vazirmatn", "Inter", system-ui, sans-serif;
}
```

---

## Best Practices

### ✅ DO

1. **Always store dates in UTC** in the database
2. **Always send ISO 8601 format** from backend: `"2026-01-16T12:30:00.000Z"`
3. **Use `formatDate()` utilities** for display in frontend
4. **Use `<LocalizedDate>`** component for automatic locale handling
5. **Let the browser detect timezone** - don't hardcode

### ❌ DON'T

1. **Don't store dates in user's local timezone** in database
2. **Don't convert timezones in backend** - keep everything UTC
3. **Don't hardcode date formats** - use locale utilities
4. **Don't use `new Date().toString()`** - use ISO format
5. **Don't assume Gregorian calendar** - support Jalali for Persian

---

## Testing Different Locales

### Test Persian (Jalali Calendar + RTL)

1. Click language selector in app header
2. Select "فارسی" (Persian)
3. Verify:
   - Layout switches to RTL
   - Dates show in Jalali calendar (۱۴۰۴/۱۰/۲۷)
   - Numbers use Persian digits (۱۲۳)
   - Text aligns to right

### Test Timezones

Open browser console:
```javascript
// Check detected timezone
Intl.DateTimeFormat().resolvedOptions().timeZone
// Returns: "America/New_York", "Asia/Tehran", etc.

// Test conversion
const utc = new Date("2026-01-16T12:30:00.000Z");
console.log(utc.toLocaleString('en-US')); // Local time
console.log(utc.toLocaleString('fa-IR')); // Persian format
```

---

## API Examples

### Backend API Response (Java)

```java
@GetMapping("/api/cycles/{id}")
public ResponseEntity<CycleDTO> getCycle(@PathVariable Long id) {
    // LocalDateTime is automatically serialized to UTC ISO by Jackson
    CycleDTO cycle = cycleService.getCycle(id);
    // Returns JSON: { "startDate": "2026-01-16T12:30:00.000Z", ... }
    return ResponseEntity.ok(cycle);
}
```

### Frontend API Call (TypeScript)

```tsx
// Fetch cycle from API
const { data: cycle } = await api.get<Cycle>('/cycles/1');

// cycle.startDate is "2026-01-16T12:30:00.000Z" (UTC ISO string)

// Display with locale formatting
<div>
  <h3>{cycle.name}</h3>
  <LocalizedDate date={cycle.startDate} format="long" />
  {/* Persian: "۲۷ دی ۱۴۰۴" */}
  {/* English: "January 16, 2026" */}
</div>
```

---

## Dependencies

### Backend
- **Spring Boot**: Jackson with UTC timezone configuration
- **Hibernate**: JDBC timezone set to UTC

### Frontend
- **date-fns**: Gregorian calendar formatting
- **date-fns-jalali**: Persian/Jalali calendar support
- **date-fns-tz**: Timezone conversion utilities
- **Intl API**: Browser-native locale and timezone detection

---

## Troubleshooting

### Issue: Dates showing wrong time

**Solution**: Check that backend is sending UTC ISO format:
```json
{
  "createdAt": "2026-01-16T12:30:00.000Z"  // ✅ Correct - UTC with 'Z'
  "createdAt": "2026-01-16T12:30:00"       // ❌ Wrong - missing timezone
}
```

### Issue: Persian dates not showing

**Solution**: Verify `date-fns-jalali` is installed:
```bash
npm list date-fns-jalali
```

### Issue: RTL not working

**Solution**: Check `html[dir]` attribute in browser DevTools. Should be `rtl` for Persian.

---

## Summary

| Aspect | Backend | Frontend |
|--------|---------|----------|
| **Storage** | UTC only | User's timezone |
| **Format** | ISO 8601 | Locale-specific |
| **Calendar** | Gregorian | Jalali for Persian |
| **Conversion** | None | UTC → Local |
| **Configuration** | Jackson UTC | locale.ts utilities |

This architecture ensures **data consistency** in the database while providing **localized user experiences** in different timezones and calendar systems.
