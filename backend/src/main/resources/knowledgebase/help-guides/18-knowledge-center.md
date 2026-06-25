# Knowledge Center

The Knowledge Center lets you feed your own documents and reference material into ShipFlow's AI. Once a source is ingested, its content becomes searchable context for **AI Q&A**, **Wise Architecture**, **AI test generation**, and **risk analysis** — so the AI answers using *your* documentation, not just general knowledge.

Open it at **Knowledge Center** (`/knowledge`).

## What You Can Add

| Source type | Use it for | Refreshable? |
|-------------|-----------|--------------|
| **File Upload** | PDFs, Word docs, Markdown, plain text, spreadsheets | No — re-upload to update |
| **URL** | A web page or online doc you want ingested | Yes — re-fetches on refresh |

Additional providers (GitHub, Confluence, Notion, Google Drive) appear in the picker as **Coming Soon**.

## How to Add a Knowledge Source

1. Go to **Knowledge Center** and click **Add Source**.
2. Choose a provider: **File Upload** or **URL**.
3. Enter a **Name** (required) and an optional **Description**.
4. Provide the source:
   - **File Upload** — select the file from your computer.
   - **URL** — paste the full address (e.g. `https://docs.example.com/guide`).
5. Click **Create**.

The source then ingests: its text is extracted, split into chunks, and embedded into the vector store.

## Ingestion Status

Each source shows a status while it processes:

- **Pending** — queued for ingestion
- **Ingesting** — fetching and chunking
- **Ready** — embedded and searchable by the AI
- **Failed** — something went wrong (the error is shown on the source)
- **Stale** — the source is out of date or became unreachable

File uploads usually finish immediately; URL sources may process in the background.

## Refreshing a Source

URL sources support **Refresh** — click it to re-fetch the page. If the page hasn't changed (detected via its ETag), ShipFlow skips re-processing. File uploads can't be refreshed; delete the old source and upload the new version instead.

## What the AI Does With It

Ingested content is retrieved by similarity search and passed as context to AI features, so answers and generated artifacts reflect your own docs. Each chunk is tagged with its source, so the system can tell where an answer came from.

## Scope & Permissions

Sources are scoped so the right people see them:

- **Organization** — visible to everyone in the org.
- **Team** — visible to members of that team.
- **Project** — visible to members of that project.

Any signed-in user can add sources within scopes they belong to. You can refresh or delete a source if you have modify rights on it (typically its creator or an admin).
