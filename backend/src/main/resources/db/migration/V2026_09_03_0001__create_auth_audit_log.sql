-- Authentication audit trail.
--
-- Added after it proved impossible to determine who changed the admin password
-- on a public demo instance: successful and failed logins were recorded
-- nowhere, and Hibernate Envers does not audit the users table. The only
-- surviving evidence was an updated_at timestamp.
--
-- Records every authentication outcome with the originating IP (resolved
-- through Cloudflare/Caddy via ClientIpService, not the proxy address),
-- country, and a human-readable device summary derived from the User-Agent.
CREATE TABLE IF NOT EXISTS auth_audit_log (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type      VARCHAR(40)  NOT NULL,
    outcome         VARCHAR(16)  NOT NULL,
    -- Username as SUPPLIED by the caller. Deliberately not a foreign key: a
    -- failed login may reference an account that does not exist, and that is
    -- exactly the case worth recording.
    username        VARCHAR(255),
    user_id         BIGINT,
    ip_address      VARCHAR(45),
    country         VARCHAR(2),
    user_agent      VARCHAR(512),
    device_summary  VARCHAR(160),
    failure_reason  VARCHAR(160),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Newest-first listing is the default view.
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_created_at ON auth_audit_log (created_at DESC);
-- "show me everything for this account" and "everything from this address".
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_username ON auth_audit_log (username);
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_ip_address ON auth_audit_log (ip_address);
-- Brute-force hunting: failures for one account, or one IP, over a window.
CREATE INDEX IF NOT EXISTS idx_auth_audit_log_outcome_created ON auth_audit_log (outcome, created_at DESC);
