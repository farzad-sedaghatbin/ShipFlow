# Plugin Menu Items & Pages

Some plugins add their own entry to the sidebar and a page that opens from it. This guide covers what you see as a user, how an administrator turns a plugin on or off, and how a plugin author builds one.

---

## For everyone

When an administrator turns on a plugin that has a page, a **Plugins** section appears in the sidebar (and in the **More** menu on a phone). Click the entry to open the plugin's page.

A plugin page is made of simple parts that ShipFlow draws for you:

- **Text** (formatted with Markdown)
- **Numbers** shown as cards
- **Tables**
- **Links** to other ShipFlow pages, or to outside websites that start with `https://`
- **Notices** (information, success, warning or error)

A plugin page cannot run its own code in your browser, so opening one is as safe as opening any other ShipFlow page.

If you are working in a project, the page can use it. If you cannot see an entry that a colleague can, the plugin limits that page to a higher role such as Manager or Admin.

**If a page shows an error**, the plugin reported a problem. The rest of ShipFlow is not affected. Ask an administrator to check the plugin.

---

## For administrators

1. Open **Organization Settings → Plugins**.
2. Plugins that add menu items are listed under **Menu & Pages**.
3. Use the switch to turn one on or off. The sidebar updates straight away.

Plugins that state a version show it next to their name (for example `v1.2.0`), so after an upgrade you can check which build is running.

Your choice is **saved**, so it is still in place after ShipFlow restarts and after a plugin is upgraded to a newer version (the choice follows the plugin's id). A plugin you have never switched either way starts in the state its author chose. The built-in **Plugin Showcase** starts **off**; turn it on to see an example page that uses every part listed above.

A disabled plugin disappears from the menu and its page address stops working.

---

## For plugin authors

Implement `UiContributionPlugin` from `shipflow-plugin-api` **1.1.0** or later:

- `getMenuItems()` returns the entries to add. Each has a key, a label per language (English is required), an icon name, and the lowest role allowed to see it (`ANY`, `MANAGER` or `ADMIN`).
- `getPage(pageKey, context)` returns the page for one of those keys. The context holds the user's name, role, language and the current project (if any). Return `null` for an unknown key.
- `isEnabledByDefault()` is optional and defaults to `true`.

Allowed icons: `puzzle`, `chart`, `list`, `book`, `calendar`, `shield`, `rocket`, `flag`, `users`, `settings`, `file`, `link`, `star`, `bell`. Anything else shows `puzzle`.

ShipFlow checks everything a plugin returns: ids must use lowercase letters, digits and hyphens; link targets must be an in-app path (starting with `/`) or an `https://` address; and content over the limits (50 blocks, 500 table rows, 100,000 characters of Markdown) is cut. Role rules are enforced by ShipFlow itself, not only hidden in the menu.

Keep the plugin id the same in every version: the administrator's on/off choice is saved under it, and a changed id is treated as a brand-new plugin. Override `getVersion()` and raise it with each release. If two plugins on the classpath share an id, ShipFlow refuses to start and names both, so make sure only one build of a plugin is included.

Like every ShipFlow plugin, this one is compiled into the backend. See the Plugin SDK README for the build steps.
