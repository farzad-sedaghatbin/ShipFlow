# Wiki / Docs Space

ShipFlow's built-in Wiki lets teams write, organize, and search documentation without leaving the tool — no Confluence or Notion required.

---

## Concepts

| Term | Description |
|------|-------------|
| **Space** | Top-level container for a subject area (e.g. "Engineering", "Product", "Onboarding"). Each space has its own page tree and access settings. |
| **Page** | A document inside a space. Pages are nested — a page can have child pages, forming a tree of any depth. |
| **Block** | The atomic unit of content inside a page (paragraph, heading, list item, table, code block, callout, etc.). |

---

## Getting Started

### Create a Space

1. In the sidebar, click **Wiki**.
2. Click **New Space** and give it a name and optional description.
3. Choose visibility — **Private** (space members only) or **Public** (all org members can read).

### Create a Page

1. Open a Space and click **New Page**, or hover over any existing page in the tree and click the **+** icon.
2. A blank page opens in the editor with the cursor in the title field.
3. Type a title and press **Enter** to start writing in the body.

### Organize Pages

Drag a page in the sidebar tree to move it under a different parent or change its order. You can also right-click any page for **Move**, **Duplicate**, or **Delete**.

---

## The Block Editor

The editor is block-based. Each line or element is a block you can manipulate independently.

### Slash Menu

Type `/` anywhere in the body to open the block-type picker:

| Command | Block |
|---------|-------|
| `/heading1` – `/heading3` | H1 / H2 / H3 headings |
| `/bullet` | Unordered list |
| `/numbered` | Ordered list |
| `/checklist` | Checkbox list (to-do items) |
| `/table` | Inserts a table |
| `/code` | Code block (with language selector) |
| `/callout` | Highlighted callout box (info / warning / tip) |
| `/divider` | Horizontal rule |
| `/image` | Upload or paste an image |
| `/file` | Attach a file |
| `/link` | Internal page link |

### Inline Formatting

Use standard Markdown shortcuts in a block:

- `**bold**`, `_italic_`, `` `code` ``
- `[[` to search and link an internal page
- `@name` to mention a team member

### Tables

Click any cell to edit. Use Tab to move to the next cell. Right-click a row or column header for insert/delete options.

---

## Version History

Every save creates a new revision. To browse history:

1. Open a page and click **…** → **Page History**.
2. A list of revisions appears with author name, timestamp, and change summary.
3. Click any revision to preview it in read-only mode.
4. Click **Restore this version** to make it the current page content.

The current published version is never overwritten — restoring just creates a new revision at the top.

---

## Search

Full-text search across all wiki pages is available from the global search bar (**⌘K** / **Ctrl+K**). Type your query and select **Wiki** from the scope filter to restrict results to wiki content.

Within a space, use the search box at the top of the page tree to filter pages by title.

> **Note**: Full-text trigram indexing requires PostgreSQL. H2 (used in tests) covers SQL correctness but not ranked search.

---

## @Mentions and Internal Page Links

- **@mention a user**: type `@` followed by the user's name. The mentioned user receives an in-app notification.
- **Link to a page**: type `[[` and start typing the page title. Select the page from the autocomplete list. The link updates automatically if the page is renamed.
- **Link to a task or pitch**: type `#` followed by the task title or pitch ID.

---

## File Attachments

Click the **Attach file** button in the editor toolbar or use the `/file` slash command to attach files to a page. Attachments are stored via the configured [object storage backend](15-object-storage.md) (S3, MinIO, or local disk).

The attachment list appears at the bottom of the page. Click an attachment to download it.

---

## Breadcrumbs and Table of Contents

- **Breadcrumbs** appear at the top of every page showing the full space → parent → page path. Click any segment to navigate up.
- **Auto table of contents**: when a page has three or more headings, a floating table of contents panel appears on the right margin. Click a heading to jump to it.

---

## Permissions

Access is controlled at the **Space** level.

| Role | Default access |
|------|---------------|
| `ADMIN` | Full access to all spaces |
| `PROJECT_MANAGER` | Read/write to any space in their project |
| `DEVELOPER`, `QA`, `PRODUCT` | Read/write to spaces where they are a member |
| `VIEWER` | Read-only to public spaces |

Space owners can override defaults by adding explicit member entries with **Viewer**, **Editor**, or **Admin** rights. Go to **Space Settings → Members** to manage this.

---

## AI Integration

Wiki pages are automatically ingested into the **Knowledge Center** when saved. This means:

- **AI Q&A** (`/ask`) answers using your wiki content.
- **Wise Architecture** analysis draws on docs in relevant spaces.
- **Risk analysis** and **test generation** benefit from wiki context automatically.

No manual sync is required — ingestion is event-driven on every page save. Attachment text extraction into the Knowledge Center is a planned follow-up; page body content is ingested today.
