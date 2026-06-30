# Pluggable Object Storage

ShipFlow stores **all** persistent file uploads through a configurable object storage backend. This covers task attachments, wiki page attachments, pitch/meeting/cycle/note documents, and bug-report media (images and video). Admins can choose between local disk, AWS S3, and MinIO without changing application code.

> Every upload path in the application routes through the object-storage abstraction — no feature writes files to the server disk directly. This means a single backend choice (and a single migration) covers documents and bug attachments alongside task and wiki files. **Knowledge Center file uploads are stored too** — the original file is persisted to the active backend (not just its extracted text), and the storage key is tracked in the source configuration.

---

## Folder structure

Objects are grouped into a consistent, category-based key layout so a bucket (or local directory) is browsable by what the file belongs to:

| Prefix | Contains |
|--------|----------|
| `attachments/task/{taskId}/…` | Files attached to a task |
| `attachments/bug/{bugId}/…` | Images and video attached to a bug report |
| `attachments/wiki/{pageId}/…` | Files attached to a wiki page |
| `documents/{type}/{entityId}/…` | Uploaded documents, grouped by entity type (`pitch`, `meeting`, `cycle`, `note`) |
| `knowledge/{sourceId}/…` | Original files uploaded to a Knowledge Center source |

> The structure applies to **new** uploads. Files uploaded before this layout was introduced keep their original keys and continue to download correctly; running **Migrate attachments** re-stores them under the new structure.

---

## Storage Backends

| Backend | When to use |
|---------|------------|
| `LOCAL_FS` | Development or single-node self-hosted deployments. Files are stored on the server filesystem. Not suitable for multi-node or containerised setups. |
| `S3` | AWS-hosted or S3-compatible cloud storage. Requires an AWS account and IAM credentials. Best for production cloud deployments. |
| `MINIO` | Self-hosted S3-compatible object store. Ideal for teams that need object storage on-premise or in an air-gapped environment. |

---

## Configuration via Org Settings

All storage configuration is managed through **Organization Settings → Storage**.

### Selecting a Backend

1. Go to **Org Settings** → **Storage**.
2. Under **Storage Backend**, select `LOCAL_FS`, `S3`, or `MinIO` from the dropdown.
3. Fill in the required fields for the chosen backend (see below).
4. Click **Test Connection** to verify the credentials and network access.
5. Click **Save** to apply.

> Changes take effect immediately for new uploads. Existing files are not automatically migrated — use the **Migrate attachments** action to move them.

---

## Backend-Specific Settings

### LOCAL_FS

| Field | Description |
|-------|-------------|
| **Base path** | Absolute filesystem path where files are stored (e.g. `/data/shipflow/attachments`). The directory is created on first write if it does not exist. Ensure the application process has read/write permission. |

### S3

| Field | Description |
|-------|-------------|
| **Bucket name** | The S3 bucket. Must already exist; ShipFlow does not create buckets. |
| **Region** | AWS region code (e.g. `us-east-1`). |
| **Access key ID** | AWS IAM access key ID. Masked in the UI after saving and never returned by the API. |
| **Secret access key** | AWS IAM secret key. Masked in the UI after saving and never returned by the API. |
| **Path prefix** | Optional prefix prepended to all object keys (e.g. `shipflow/prod/`). Useful for sharing a bucket across multiple environments. |

Minimum IAM policy required:
```json
{
  "Effect": "Allow",
  "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
  "Resource": ["arn:aws:s3:::YOUR_BUCKET", "arn:aws:s3:::YOUR_BUCKET/*"]
}
```

### MinIO

| Field | Description |
|-------|-------------|
| **Endpoint URL** | Full URL of the MinIO server (e.g. `http://minio.internal:9000`). |
| **Bucket name** | The MinIO bucket. Must exist before saving. |
| **Access key** | MinIO access key (username). Masked in the UI after saving and never returned by the API. |
| **Secret key** | MinIO secret key. Masked in the UI after saving and never returned by the API. |
| **Path prefix** | Optional prefix (same behavior as S3). |

---

## Connection Test

Click **Test Connection** before saving to verify:

1. The backend is reachable at the configured endpoint.
2. The credentials are valid.
3. The bucket exists and the credentials have sufficient permissions (read + write).

A green **Connection successful** toast confirms readiness. If the test fails, the error message indicates whether it is a network, authentication, or permission issue.

---

## Credential Handling

- Credentials (access keys, secret keys) are stored as plaintext in the database — consistent with ShipFlow's other integration secrets (GitHub, Figma, Notion, SMTP). There is no at-rest encryption subsystem yet; protect the database accordingly.
- As compensating controls, credentials are **never returned by the API** after the first save (the UI shows only a masked `••••••••` and a "secret is set" flag) and are **never written to logs**.
- To rotate credentials: enter the new values and click **Save**. The new credentials are used for all subsequent uploads immediately.

---

## Migrating Between Backends

When you switch from one backend to another, existing attachments remain on the old backend. ShipFlow provides a one-click migration:

1. Configure and save the **new** backend settings.
2. Click **Test Connection** to confirm the new backend is reachable.
3. Click **Migrate attachments** in the Storage settings panel.
4. ShipFlow copies all stored objects — **task attachments, wiki attachments, pitch/meeting/cycle/note documents, and bug-report media** — to the new backend. Legacy documents that predate the storage abstraction (stored as raw files on disk) are copied across in the same pass. Each object is read back from the new backend and verified before its source copy is touched.
5. When the migration finishes, a summary reports how many objects were **migrated, skipped (already on the active backend), and failed**.
6. Once complete, all download links resolve from the new backend. The old files are left in place and can be deleted manually after confirming the migration.

> Do not switch the active backend while a migration is in progress — this will interrupt the job.

---

## Download URL Behavior

- **All backends** (`LOCAL_FS`, `S3`, `MinIO`): downloads stream through ShipFlow's own attachment/document endpoints, protected by JWT authentication — the object is read from the active backend and streamed to the client. This keeps every download behind the app's access checks regardless of backend.
- Pre-signed-URL generation is implemented for S3/MinIO at the provider layer but is **not yet wired into the download flow**, so downloads are currently proxied through the application for all backends.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Upload fails with "Storage not configured" | No backend selected or Save not clicked | Go to Org Settings → Storage, select a backend, and save. |
| Test connection fails with "Connection refused" | MinIO/S3 endpoint unreachable | Check network access; verify the endpoint URL includes the port. |
| Test connection fails with "Access denied" | Invalid credentials or missing bucket permissions | Verify the access key/secret and IAM policy. |
| Files lost after switching backend | Migration not run | Run **Migrate attachments** before switching, or restore files from the old backend. |
| Local disk full | `LOCAL_FS` base path on a volume with no space | Move the base path to a larger volume and update the setting; re-run the migration. |
