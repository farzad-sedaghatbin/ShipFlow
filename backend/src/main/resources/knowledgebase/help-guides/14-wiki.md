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

1. Open a page and expand the **History** section at the bottom.
2. A list of revisions appears with revision number, title, timestamp, and editor.
3. Click **Compare & restore** on any revision to open the compare view.

### Compare before restoring

The compare view shows the **current version and the selected revision side by side**, with changes highlighted line by line:

- **Green** — content added by the revision (present on the right, absent on the left).
- **Red** — content removed by the revision (present on the left, absent on the right).
- A **changed** line shows the old text on the left (red) paired with the new text on the right (green).
- If the page **title** changed between versions, the old and new titles are shown at the top.

Review the differences, then click **Restore this version** to make the selected revision the current content — or **Cancel** to back out. If there are no differences, the view tells you the revision matches the current page.

The current published version is never overwritten — restoring just creates a new revision at the top, so you can always compare and roll back again.

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

Click the **Upload Attachment** button in the page's Attachments section to attach files to a page. Attachments are stored via the configured [object storage backend](15-object-storage.md) (S3, MinIO, or local disk).

The attachment list appears at the bottom of the page:

- **Image attachments** show an inline thumbnail preview — click it to open the full-size image in a new tab.
- **Download** any attachment with the download button (it streams through the authenticated session, so files in private spaces stay protected).
- **Delete** an attachment with the trash button (uploader, or an admin/manager).

---

## Linking a Wiki Page to a Pitch or Task

File attachments aren't always the right fit — sometimes what a Pitch or Task needs is a *reference* to existing research or documentation, not another uploaded copy of it. Pitches and Tasks each have a **Linked wiki pages** section (next to their file attachments) for exactly this:

1. Open a Pitch or Task, scroll to **Linked wiki pages**.
2. Use the search box to find an existing wiki page by title.
3. Select it to link it — it appears in the list with its space, and when it was linked.
4. Click the external-link icon to jump to the page, or the **×** to unlink it (the wiki page itself is untouched).

This is available on Pitches and Tasks only — Bug Reports keep file attachments (screenshots, logs) as their primary evidence type, since reproduction artifacts aren't documentation.

---

## Comments and Discussion

Every wiki page has a **comments thread** at the bottom — the same discussion experience used on tasks and bug reports.

- **Add a comment**: type in the comment box below the page content and click **Post**. Comments support Markdown (bold, lists, code, links, tables).
- **@mention teammates**: type `@` and pick a name from the autocomplete. The mentioned person gets an in-app notification (and email, if enabled) that deep-links straight back to the page.
- **React with emoji**: hover a comment and pick a reaction (👍 ❤️ 🚀 …). Reaction counts are shown inline.
- **Edit or delete**: authors can edit or delete their own comments from the **⋯** menu; admins and managers can delete any comment. Deletes are soft (recoverable), consistent with the rest of ShipFlow.

Comments are a great place for review feedback, open questions, or change proposals without editing the page body itself. They are **not** included in PDF exports.

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
