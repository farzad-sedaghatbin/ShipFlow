#!/usr/bin/env python3
"""
Fix all missing i18n translation keys for en.json and fa.json.
Run from the workspace root: python3 scripts/fix-i18n-keys.py
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent
EN_FILE = ROOT / "frontend/src/i18n/locales/en.json"
FA_FILE = ROOT / "frontend/src/i18n/locales/fa.json"


def set_nested(data: dict, path: str, value, overwrite=False):
    """Set a nested key only if it doesn't already exist (unless overwrite=True)."""
    parts = path.split(".")
    current = data
    for part in parts[:-1]:
        if part not in current:
            current[part] = {}
        current = current[part]
    key = parts[-1]
    if key not in current or overwrite:
        current[key] = value
        return True  # was set
    return False  # already existed


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")  # trailing newline


def fix_en(data):
    changes = 0
    # common section
    for key, val in [
        ("common.pitch", "Pitch"),
        ("common.saving", "Saving..."),
        ("common.noResults", "No results found"),
        ("common.new", "New"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # cycles section
    for key, val in [
        ("cycles.betting", "Betting"),
        ("cycles.build", "Build"),
        ("cycles.cooldown", "Cooldown"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # pitches.status
    if set_nested(data, "pitches.status.inProgress", "In Progress"):
        changes += 1; print("  EN +pitches.status.inProgress")

    # landing
    for key, val in [
        ("landing.wiseArchitecture", "Wise Architecture"),
        ("landing.wiseArchitectureDesc", "AI-powered architecture advisor that analyzes codebases, team skills, and Figma designs to recommend optimal implementation strategies."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # backlogPage
    for key, val in [
        ("backlogPage.hillChartAutoCreate", "Auto-create hill chart scope"),
        ("backlogPage.searchPersons", "Search persons..."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # scopeNarrative
    for key, val in [
        ("scopeNarrative.autoProgress", "Auto Progress"),
        ("scopeNarrative.autoProgressDesc", "Automatically calculates progress based on linked task completion"),
        ("scopeNarrative.linkedTask", "Linked Task"),
        ("scopeNarrative.linkedTaskDesc", "Task linked to this scope"),
        ("scopeNarrative.suggestedPosition", "Suggested Position"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # cooldownActivity
    for key, val in [
        ("cooldownActivity.createError", "Failed to create activity"),
        ("cooldownActivity.updateError", "Failed to update activity"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # organizationSettings
    for key, val in [
        ("organizationSettings.capacityDesc", "Configure team capacity defaults for planning"),
        ("organizationSettings.defaultHoursPerDayNote", "Default hours per day used for capacity calculations across the organization"),
        ("organizationSettings.defaultWorkingDaysPerWeekNote", "Default working days per week used for capacity calculations"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # releases
    for key, val in [
        ("releases.nameVersionRequired", "Name and version are required"),
        ("releases.updated", "Release updated successfully"),
        ("releases.createError", "Failed to create release"),
        ("releases.noCyclesAvailable", "No cycles available"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  EN +{key}")

    # meetingList.table
    if set_nested(data, "meetingList.table.viewTooltip", "View meeting"):
        changes += 1; print("  EN +meetingList.table.viewTooltip")

    # peopleManagement
    if set_nested(data, "peopleManagement.tryDifferentSearch", "Try a different search term"):
        changes += 1; print("  EN +peopleManagement.tryDifferentSearch")

    # pitchBoard
    if set_nested(data, "pitchBoard.cycleRequired", "Please select a cycle"):
        changes += 1; print("  EN +pitchBoard.cycleRequired")

    # pitchHillChart
    if set_nested(data, "pitchHillChart.taskAutoCreate", "Auto-create task from scope"):
        changes += 1; print("  EN +pitchHillChart.taskAutoCreate")

    # top-level conversation
    if set_nested(data, "conversation", "Conversation"):
        changes += 1; print("  EN +conversation")

    # new inboundWebhooks section
    iw_keys = {
        "title": "Inbound Webhook Providers",
        "subtitle": "Configure external services to send events to ShipFlow",
        "addProvider": "Add Provider",
        "howItWorks": "How It Works",
        "howItWorksDesc": "External services send signed HTTP POST requests to ShipFlow. Configure a provider's name, webhook URL, HMAC secret, and signature header without writing any code.",
        "step1": "Copy the webhook URL and configure it in the external service",
        "step2": "Set a shared secret for HMAC signature verification",
        "step3": "Configure the signature header (e.g. X-Hub-Signature-256)",
        "step4": "Enable the provider to start receiving events",
        "configurations": "Configured Providers",
        "configurationsDesc": "Manage your inbound webhook configurations",
        "empty": "No providers configured yet",
        "provider": "Provider",
        "webhookUrl": "Webhook URL",
        "algorithm": "Algorithm",
        "status": "Status",
        "copyUrl": "Copy URL",
        "editProvider": "Edit Provider",
        "providerNameLabel": "Provider Name",
        "providerNameHint": "A unique identifier for this provider (e.g. zendesk, pagerduty)",
        "displayNameLabel": "Display Name",
        "webhookSecretLabel": "Webhook Secret",
        "leaveBlank": "Leave blank to keep existing secret",
        "secretPlaceholder": "Enter webhook secret",
        "secretHint": "The shared secret used to verify HMAC signatures from this provider",
        "signatureHeaderLabel": "Signature Header",
        "algorithmLabel": "Signature Algorithm",
        "descriptionLabel": "Description",
        "descriptionPlaceholder": "Optional description for this provider",
        "enabledLabel": "Enabled",
        "create": "Create Provider",
        "deleteTitle": "Delete Provider",
        "deleteDescription": "Are you sure you want to delete this webhook provider? This action cannot be undone.",
        "fetchError": "Failed to load webhook providers",
        "updated": "Provider updated successfully",
        "created": "Provider created successfully",
        "saveError": "Failed to save provider",
        "toggleError": "Failed to toggle provider status",
        "deleted": "Provider deleted successfully",
        "deleteError": "Failed to delete provider",
        "copied": "Webhook URL copied to clipboard",
    }
    for key, val in iw_keys.items():
        if set_nested(data, f"inboundWebhooks.{key}", val):
            changes += 1; print(f"  EN +inboundWebhooks.{key}")

    # new webhooksGuide section
    wg_keys = {
        "outgoing": "Outgoing Webhooks",
        "outgoingDesc": "Send events from ShipFlow to external services like Slack and Microsoft Teams",
        "outgoingIntro": "ShipFlow can send notifications to Slack and MS Teams when important events occur in your cycles and pitches.",
        "slackSetup": "Setting up Slack Notifications",
        "slackStep1": "Go to Organization Settings → Integrations → Slack",
        "slackStep2": "Generate an Incoming Webhook URL from your Slack workspace",
        "slackStep3": "Paste the URL and configure which events trigger notifications",
        "slackStep4": "Save and test the connection",
        "stagnation": "Stagnation Alerts",
        "stagnationDesc": "Automatic notifications are sent when a scope has not progressed past the peak of the hill chart for an extended period.",
        "supportedEvents": "Supported Events",
        "inbound": "Inbound Webhooks",
        "inboundDesc": "Receive events from external services and trigger actions in ShipFlow",
        "inboundIntro": "Inbound webhooks allow external services like Zendesk, PagerDuty, or custom systems to push events into ShipFlow securely.",
        "howToConnect": "How to Connect an External Service",
        "step1Title": "Create a Provider",
        "step1Desc": "Go to Integrations → Inbound Webhooks and click Add Provider",
        "openSettings": "Open Settings",
        "step2Title": "Configure the External Service",
        "step2Desc": "Copy the webhook URL and paste it into your external service, along with the shared secret",
        "step3Title": "Verify Signatures",
        "step3a": "ShipFlow verifies each request using HMAC signature validation",
        "step3b": "Supported algorithms: SHA-256 (recommended), SHA-1, MD5",
        "step3c": "The signature header name must match what the external service sends",
        "step3d": "Requests with invalid signatures are rejected with a 401 error",
        "step4Title": "Test the Integration",
        "step4Desc": "Send a test event from the external service and check ShipFlow logs",
        "security": "Security",
        "securityDesc": "All inbound webhook requests are verified using HMAC signatures. Each provider has its own unique secret, and the signature is checked before any event is processed.",
        "endpoint": "Webhook Endpoint",
        "endpointExample": "POST /api/webhooks/inbound/{providerName}",
        "listProviders": "List Providers",
        "listProvidersDesc": "GET /api/webhooks/providers — Returns all configured webhook providers for your organization",
        "eventHeaders": "Event Headers",
        "eventHeadersDesc": "Each inbound request includes provider metadata headers for routing and auditing",
        "providers": "Provider Types",
        "providersDesc": "ShipFlow supports any HTTP-based webhook provider that can send HMAC-signed requests",
        "providersIntro": "Common integrations include:",
        "vcsLabel": "VCS & DevOps",
        "notifLabel": "Notifications & Alerting",
        "inboundLabel": "Inbound Webhooks Guide",
    }
    for key, val in wg_keys.items():
        if set_nested(data, f"webhooksGuide.{key}", val):
            changes += 1; print(f"  EN +webhooksGuide.{key}")

    return changes


def fix_fa(data):
    changes = 0

    # common section
    for key, val in [
        ("common.download", "دانلود"),
        ("common.pitch", "پیچ"),
        ("common.saving", "در حال ذخیره..."),
        ("common.noResults", "نتیجه‌ای یافت نشد"),
        ("common.new", "جدید"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # cycles — top-level betting_cooldown key (different from phase.betting_cooldown)
    if set_nested(data, "cycles.betting_cooldown", "ارزیابی و خنک‌شدن"):
        changes += 1; print("  FA +cycles.betting_cooldown")

    # pitches section — add status-less keys
    pitches_keys = {
        "all": "همه",
        "ideas": "ایده‌ها",
        "shaping": "در حال شکل‌دهی",
        "readyForBetting": "آماده برای شرط‌بندی",
        "noIdeas": "هنوز ایده‌ای وجود ندارد — ایده‌های خام خود را اینجا ثبت کنید",
        "noDrafts": "هیچ پیچی در حال شکل‌گیری نیست",
        "noShaped": "هیچ پیچ شکل‌گرفته‌ای برای شرط‌بندی آماده نیست",
        "quickAddIdea": "افزودن سریع ایده",
        "quickAddIdeaDescription": "یک ایده خام را سریع ثبت کنید. بعداً می‌توانید آن را با جزئیات بیشتر شکل دهید.",
        "ideaCreated": "ایده با موفقیت ایجاد شد",
        "addIdea": "افزودن ایده",
        "addIdeaHint": "با افزودن ایده‌ها شروع کنید، سپس آن‌ها را به پیچ تبدیل کنید",
        "titleRequired": "عنوان الزامی است",
        "titlePlaceholder": "یک عنوان کوتاه برای ایده خود وارد کنید",
        "ideaDescriptionPlaceholder": "اختیاری: افکار اولیه یا زمینه را اضافه کنید",
        "shapingStarted": "شکل‌دهی آغاز شد — جزئیات را برای شکل‌دادن به این پیچ اضافه کنید",
        "startShaping": "شروع شکل‌دهی",
        "shape": "شکل‌دادن",
        "noCycleAssigned": "به هیچ چرخه‌ای اختصاص داده نشده",
        "createError": "خطا در ایجاد پیچ",
        "updateError": "خطا در به‌روزرسانی پیچ",
    }
    for key, val in pitches_keys.items():
        if set_nested(data, f"pitches.{key}", val):
            changes += 1; print(f"  FA +pitches.{key}")

    # pitches.status nested keys
    for key, val in [
        ("pitches.status.idea", "ایده"),
        ("pitches.status.draft", "پیش‌نویس"),
        ("pitches.status.in_progress", "در حال انجام"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # pitch.statuses nested keys
    for key, val in [
        ("pitch.statuses.idea", "ایده"),
        ("pitch.statuses.draft", "پیش‌نویس"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # top-level status
    for key, val in [
        ("status.idea", "ایده"),
        ("status.draft", "پیش‌نویس"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # landingPage
    for key, val in [
        ("landingPage.wiseArchitecture", "معماری هوشمند"),
        ("landingPage.wiseArchitectureDesc", "تولیدکننده راه‌حل فنی مبتنی بر هوش مصنوعی که کدبیس، مهارت‌های تیم و طراحی‌های فیگما را تحلیل می‌کند."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # helpGuidesPage
    for key, val in [
        ("helpGuidesPage.wiseArchitecture", "معماری هوشمند"),
        ("helpGuidesPage.wiseArchitectureDesc", "با دستیار معماری هوشمند راه‌حل‌های فنی بهینه برای پیچ‌های خود دریافت کنید."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # landing
    for key, val in [
        ("landing.scopeTaskBridge", "پل محدوده-تسک"),
        ("landing.scopeTaskBridgeDesc", "ارتباط مستقیم بین محدوده‌های نمودار تپه و تسک‌ها با پیشرفت خودکار."),
        ("landing.wiseArchitecture", "معماری هوشمند"),
        ("landing.wiseArchitectureDesc", "تولیدکننده راه‌حل فنی مبتنی بر هوش مصنوعی که کدبیس، مهارت‌های تیم و طراحی‌های فیگما را تحلیل می‌کند تا استراتژی‌های پیاده‌سازی بهینه را توصیه کند."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # workLogForm
    if set_nested(data, "workLogForm.entries", "ورودی‌ها"):
        changes += 1; print("  FA +workLogForm.entries")

    # wiseArchitecture.history
    history_keys = {
        "subtitle": "تمام جلسات مشاوره معماری شما",
        "yourHistory": "تاریخچه شما",
        "yourHistoryDesc": "مشاوره‌های معماری قبلی دریافت شده",
        "noHistoryYet": "هنوز تاریخچه‌ای وجود ندارد",
        "refresh": "بازنشانی",
        "message": "پیام",
        "messages_one": "{{count}} پیام",
        "messages_other": "{{count}} پیام",
        "started": "شروع شد",
        "initialSolution": "راه‌حل اولیه",
        "followUp": "پیگیری",
        "yourQuestion": "سوال شما",
        "yourFeedback": "بازخورد شما",
        "rateAdvice": "ارزیابی مشاوره",
        "continueInWise": "ادامه در Wise",
        "goToWise": "رفتن به Wise",
        "previous": "قبلی",
        "next": "بعدی",
        "pageOf": "صفحه {{current}} از {{total}}",
    }
    for key, val in history_keys.items():
        if set_nested(data, f"wiseArchitecture.history.{key}", val):
            changes += 1; print(f"  FA +wiseArchitecture.history.{key}")

    # wiseArchitecture.history.feedback
    for key, val in [
        ("wiseArchitecture.history.feedback.description", "توضیحات بازخورد"),
        ("wiseArchitecture.history.feedback.helpful", "مفید بود"),
        ("wiseArchitecture.history.feedback.notHelpful", "مفید نبود"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # backlogPage
    for key, val in [
        ("backlogPage.hillChartAutoCreate", "ایجاد خودکار محدوده نمودار تپه"),
        ("backlogPage.searchPersons", "جستجوی افراد..."),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # scopeNarrative
    for key, val in [
        ("scopeNarrative.autoProgress", "پیشرفت خودکار"),
        ("scopeNarrative.autoProgressDesc", "پیشرفت به صورت خودکار بر اساس تکمیل تسک مرتبط محاسبه می‌شود"),
        ("scopeNarrative.linkedTask", "تسک مرتبط"),
        ("scopeNarrative.linkedTaskDesc", "تسک مرتبط با این محدوده"),
        ("scopeNarrative.suggestedPosition", "موقعیت پیشنهادی"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # cooldownActivity
    for key, val in [
        ("cooldownActivity.createError", "خطا در ایجاد فعالیت"),
        ("cooldownActivity.updateError", "خطا در به‌روزرسانی فعالیت"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # organizationSettings
    for key, val in [
        ("organizationSettings.capacityDesc", "تنظیم مقادیر پیش‌فرض ظرفیت تیم برای برنامه‌ریزی"),
        ("organizationSettings.defaultHoursPerDayNote", "ساعات کاری روزانه پیش‌فرض برای محاسبات ظرفیت سازمان"),
        ("organizationSettings.defaultWorkingDaysPerWeekNote", "روزهای کاری پیش‌فرض در هفته برای محاسبات ظرفیت"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # releases
    for key, val in [
        ("releases.nameVersionRequired", "نام و نسخه الزامی هستند"),
        ("releases.updated", "نسخه با موفقیت به‌روزرسانی شد"),
        ("releases.createError", "خطا در ایجاد نسخه"),
        ("releases.noCyclesAvailable", "هیچ چرخه‌ای در دسترس نیست"),
    ]:
        if set_nested(data, key, val):
            changes += 1; print(f"  FA +{key}")

    # meetingList.table
    if set_nested(data, "meetingList.table.viewTooltip", "مشاهده جلسه"):
        changes += 1; print("  FA +meetingList.table.viewTooltip")

    # peopleManagement
    if set_nested(data, "peopleManagement.tryDifferentSearch", "یک عبارت جستجوی دیگر امتحان کنید"):
        changes += 1; print("  FA +peopleManagement.tryDifferentSearch")

    # pitchBoard
    if set_nested(data, "pitchBoard.cycleRequired", "لطفاً یک چرخه انتخاب کنید"):
        changes += 1; print("  FA +pitchBoard.cycleRequired")

    # pitchHillChart
    if set_nested(data, "pitchHillChart.taskAutoCreate", "ایجاد خودکار تسک از محدوده"):
        changes += 1; print("  FA +pitchHillChart.taskAutoCreate")

    # top-level conversation
    if set_nested(data, "conversation", "مکالمه"):
        changes += 1; print("  FA +conversation")

    # new inboundWebhooks section
    iw_fa = {
        "title": "ارائه‌دهندگان وب‌هوک ورودی",
        "subtitle": "سرویس‌های خارجی را برای ارسال رویدادها به ShipFlow پیکربندی کنید",
        "addProvider": "افزودن ارائه‌دهنده",
        "howItWorks": "نحوه عملکرد",
        "howItWorksDesc": "سرویس‌های خارجی درخواست‌های POST HTTP امضاشده را به ShipFlow ارسال می‌کنند. نام ارائه‌دهنده، URL وب‌هوک، رمز HMAC و هدر امضا را بدون نوشتن کد پیکربندی کنید.",
        "step1": "URL وب‌هوک را کپی کرده و در سرویس خارجی پیکربندی کنید",
        "step2": "یک رمز مشترک برای تأیید امضای HMAC تنظیم کنید",
        "step3": "هدر امضا را پیکربندی کنید (مثلاً X-Hub-Signature-256)",
        "step4": "ارائه‌دهنده را فعال کنید تا شروع به دریافت رویدادها کنید",
        "configurations": "ارائه‌دهندگان پیکربندی شده",
        "configurationsDesc": "پیکربندی‌های وب‌هوک ورودی خود را مدیریت کنید",
        "empty": "هنوز ارائه‌دهنده‌ای پیکربندی نشده",
        "provider": "ارائه‌دهنده",
        "webhookUrl": "URL وب‌هوک",
        "algorithm": "الگوریتم",
        "status": "وضعیت",
        "copyUrl": "کپی URL",
        "editProvider": "ویرایش ارائه‌دهنده",
        "providerNameLabel": "نام ارائه‌دهنده",
        "providerNameHint": "یک شناسه منحصر به فرد برای این ارائه‌دهنده (مثلاً zendesk، pagerduty)",
        "displayNameLabel": "نام نمایشی",
        "webhookSecretLabel": "رمز وب‌هوک",
        "leaveBlank": "برای نگه داشتن رمز فعلی خالی بگذارید",
        "secretPlaceholder": "رمز وب‌هوک را وارد کنید",
        "secretHint": "رمز مشترک برای تأیید امضاهای HMAC این ارائه‌دهنده",
        "signatureHeaderLabel": "هدر امضا",
        "algorithmLabel": "الگوریتم امضا",
        "descriptionLabel": "توضیحات",
        "descriptionPlaceholder": "توضیحات اختیاری برای این ارائه‌دهنده",
        "enabledLabel": "فعال",
        "create": "ایجاد ارائه‌دهنده",
        "deleteTitle": "حذف ارائه‌دهنده",
        "deleteDescription": "آیا مطمئن هستید که می‌خواهید این ارائه‌دهنده وب‌هوک را حذف کنید؟ این عمل قابل بازگشت نیست.",
        "fetchError": "خطا در بارگذاری ارائه‌دهندگان وب‌هوک",
        "updated": "ارائه‌دهنده با موفقیت به‌روزرسانی شد",
        "created": "ارائه‌دهنده با موفقیت ایجاد شد",
        "saveError": "خطا در ذخیره ارائه‌دهنده",
        "toggleError": "خطا در تغییر وضعیت ارائه‌دهنده",
        "deleted": "ارائه‌دهنده با موفقیت حذف شد",
        "deleteError": "خطا در حذف ارائه‌دهنده",
        "copied": "URL وب‌هوک در کلیپ‌بورد کپی شد",
    }
    for key, val in iw_fa.items():
        if set_nested(data, f"inboundWebhooks.{key}", val):
            changes += 1; print(f"  FA +inboundWebhooks.{key}")

    # new webhooksGuide section
    wg_fa = {
        "outgoing": "وب‌هوک‌های خروجی",
        "outgoingDesc": "ارسال رویدادها از ShipFlow به سرویس‌های خارجی مانند Slack و Microsoft Teams",
        "outgoingIntro": "ShipFlow می‌تواند اعلان‌ها را به Slack و MS Teams ارسال کند.",
        "slackSetup": "راه‌اندازی اعلان‌های Slack",
        "slackStep1": "به تنظیمات سازمان → یکپارچه‌سازی‌ها → Slack بروید",
        "slackStep2": "یک URL Incoming Webhook از فضای کاری Slack خود ایجاد کنید",
        "slackStep3": "URL را جای‌گذاری کرده و رویدادهای مورد نظر را پیکربندی کنید",
        "slackStep4": "ذخیره کرده و اتصال را آزمایش کنید",
        "stagnation": "هشدارهای رکود",
        "stagnationDesc": "اعلان‌های خودکار ارسال می‌شود زمانی که یک محدوده برای مدت طولانی از قله نمودار تپه عبور نکرده باشد.",
        "supportedEvents": "رویدادهای پشتیبانی شده",
        "inbound": "وب‌هوک‌های ورودی",
        "inboundDesc": "دریافت رویدادها از سرویس‌های خارجی و اجرای عملیات در ShipFlow",
        "inboundIntro": "وب‌هوک‌های ورودی به سرویس‌هایی مانند Zendesk، PagerDuty یا سیستم‌های سفارشی اجازه می‌دهند رویدادها را به صورت امن به ShipFlow ارسال کنند.",
        "howToConnect": "نحوه اتصال سرویس خارجی",
        "step1Title": "ایجاد یک ارائه‌دهنده",
        "step1Desc": "به یکپارچه‌سازی‌ها → وب‌هوک‌های ورودی بروید و روی افزودن ارائه‌دهنده کلیک کنید",
        "openSettings": "باز کردن تنظیمات",
        "step2Title": "پیکربندی سرویس خارجی",
        "step2Desc": "URL وب‌هوک را کپی کرده و همراه با رمز مشترک در سرویس خارجی خود وارد کنید",
        "step3Title": "تأیید امضاها",
        "step3a": "ShipFlow هر درخواست را با استفاده از تأیید امضای HMAC بررسی می‌کند",
        "step3b": "الگوریتم‌های پشتیبانی شده: SHA-256 (توصیه شده)، SHA-1، MD5",
        "step3c": "نام هدر امضا باید با آنچه سرویس خارجی ارسال می‌کند مطابقت داشته باشد",
        "step3d": "درخواست‌هایی با امضاهای نامعتبر با خطای 401 رد می‌شوند",
        "step4Title": "آزمایش یکپارچه‌سازی",
        "step4Desc": "یک رویداد آزمایشی از سرویس خارجی ارسال کرده و لاگ‌های ShipFlow را بررسی کنید",
        "security": "امنیت",
        "securityDesc": "تمام درخواست‌های وب‌هوک ورودی با استفاده از امضاهای HMAC تأیید می‌شوند.",
        "endpoint": "نقطه پایانی وب‌هوک",
        "endpointExample": "POST /api/webhooks/inbound/{providerName}",
        "listProviders": "فهرست ارائه‌دهندگان",
        "listProvidersDesc": "GET /api/webhooks/providers — همه ارائه‌دهندگان وب‌هوک پیکربندی شده سازمان را برمی‌گرداند",
        "eventHeaders": "هدرهای رویداد",
        "eventHeadersDesc": "هر درخواست ورودی شامل هدرهای متادیتای ارائه‌دهنده برای مسیریابی و حسابرسی است",
        "providers": "انواع ارائه‌دهنده",
        "providersDesc": "ShipFlow از هر ارائه‌دهنده وب‌هوک مبتنی بر HTTP که می‌تواند درخواست‌های امضاشده HMAC ارسال کند پشتیبانی می‌کند",
        "providersIntro": "یکپارچه‌سازی‌های رایج عبارتند از:",
        "vcsLabel": "کنترل نسخه و DevOps",
        "notifLabel": "اعلان و هشدار",
        "inboundLabel": "راهنمای وب‌هوک‌های ورودی",
    }
    for key, val in wg_fa.items():
        if set_nested(data, f"webhooksGuide.{key}", val):
            changes += 1; print(f"  FA +webhooksGuide.{key}")

    return changes


def main():
    print("Loading JSON files...")
    en_data = load_json(EN_FILE)
    fa_data = load_json(FA_FILE)

    print("\nFixing en.json...")
    en_changes = fix_en(en_data)

    print("\nFixing fa.json...")
    fa_changes = fix_fa(fa_data)

    print(f"\nSaving en.json ({en_changes} keys added)...")
    save_json(EN_FILE, en_data)

    print(f"Saving fa.json ({fa_changes} keys added)...")
    save_json(FA_FILE, fa_data)

    print(f"\nDone! {en_changes + fa_changes} total keys added.")
    print("Run 'node scripts/validate-i18n.js' to verify.")


if __name__ == "__main__":
    main()
