-- V62: Add GitHub App Installation table for organization-wide OAuth consent
-- This enables bulk repository access through a single authorization instead of adding repositories one by one

CREATE TABLE github_app_installations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    installation_id BIGINT NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    account_login VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    repository_selection VARCHAR(20) NOT NULL,
    access_token VARCHAR(500),
    token_expires_at TIMESTAMP,
    permissions TEXT,
    subscribed_events VARCHAR(1000),
    target_id BIGINT,
    target_type VARCHAR(20),
    installed_by_login VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    webhooks_configured BOOLEAN NOT NULL DEFAULT FALSE,
    last_sync_at TIMESTAMP,
    repository_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_github_app_installation_account ON github_app_installations (account_login);
CREATE INDEX idx_github_app_installation_active ON github_app_installations (is_active);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_github_app_installations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_github_app_installations_updated_at
BEFORE UPDATE ON github_app_installations
FOR EACH ROW
EXECUTE FUNCTION update_github_app_installations_updated_at();

-- Add messages for GitHub App integration
INSERT INTO i18n_messages (locale, message_key, message_value) VALUES
('en', 'github.app.not_configured', 'GitHub App is not configured. Please contact your administrator.'),
('en', 'github.app.authorization_initiated', 'GitHub App authorization initiated. Please complete the installation in GitHub.'),
('en', 'github.installation.success', 'Successfully connected {0} with access to {1} repositories!'),
('en', 'github.installation.removed', 'GitHub App installation has been deactivated.'),
('en', 'github.installation.sync_complete', 'Repository sync complete: {0} repositories synced.'),
('en', 'github.bulk.sync_complete', 'Bulk sync complete: {0} repositories discovered from {1} installations.')
ON CONFLICT (locale, message_key) DO UPDATE SET message_value = EXCLUDED.message_value;

INSERT INTO i18n_messages (locale, message_key, message_value) VALUES
('fa', 'github.app.not_configured', 'برنامه گیت‌هاب پیکربندی نشده است. لطفاً با مدیر خود تماس بگیرید.'),
('fa', 'github.app.authorization_initiated', 'مجوز برنامه گیت‌هاب شروع شد. لطفاً نصب را در گیت‌هاب تکمیل کنید.'),
('fa', 'github.installation.success', 'با موفقیت به {0} با دسترسی به {1} مخزن متصل شد!'),
('fa', 'github.installation.removed', 'نصب برنامه گیت‌هاب غیرفعال شد.'),
('fa', 'github.installation.sync_complete', 'همگام‌سازی مخزن کامل شد: {0} مخزن همگام‌سازی شد.'),
('fa', 'github.bulk.sync_complete', 'همگام‌سازی دسته‌ای کامل شد: {0} مخزن از {1} نصب کشف شد.')
ON CONFLICT (locale, message_key) DO UPDATE SET message_value = EXCLUDED.message_value;

INSERT INTO i18n_messages (locale, message_key, message_value) VALUES
('ar', 'github.app.not_configured', 'تطبيق GitHub غير مُهيأ. يرجى الاتصال بالمسؤول.'),
('ar', 'github.app.authorization_initiated', 'بدأ تفويض تطبيق GitHub. يرجى إكمال التثبيت في GitHub.'),
('ar', 'github.installation.success', 'تم الاتصال بنجاح بـ {0} مع الوصول إلى {1} مستودعات!'),
('ar', 'github.installation.removed', 'تم إلغاء تنشيط تثبيت تطبيق GitHub.'),
('ar', 'github.installation.sync_complete', 'اكتملت مزامنة المستودع: تمت مزامنة {0} مستودعات.'),
('ar', 'github.bulk.sync_complete', 'اكتملت المزامنة المجمعة: تم اكتشاف {0} مستودعات من {1} تثبيتات.')
ON CONFLICT (locale, message_key) DO UPDATE SET message_value = EXCLUDED.message_value;
