# SEO Guide

How ShipFlow's public pages get indexed, and the rules to follow when adding
new ones. Read this before touching `robots.txt`, `sitemap.xml`, or any page
under the public routes in `frontend/src/App.tsx`.

---

## The name-collision problem (read this first)

"ShipFlow" is not a distinctive name. At least seven unrelated products use it:
freight and e-commerce shipping platforms, a 3PL fulfilment service, an
enterprise logistics-AI company, a Shopify shipping-rules app, and a
long-established CFD package for ship-hull design.

Consequences that shape every decision in this guide:

- **The bare term "shipflow" is not winnable and not worth winning.** Most
  people searching it want one of the others. Ranking for it delivers visitors
  who bounce.
- **Every title and description must carry a disambiguating qualifier** —
  "Shape Up", "project management", "open source". A search snippet should tell
  the right reader they're in the right place *and* the wrong reader that
  they're not. That filtering is a feature.
- **The winnable ground is the Shape Up vocabulary**: shape up methodology,
  shape up process, hill chart, betting table, appetite, cooldown, circuit
  breaker, shape up vs scrum, shape up software. Nobody else owns these, and
  the people searching them are exactly the intended users.
- The landing page's `SoftwareApplication` schema carries a
  `disambiguatingDescription` — schema.org's dedicated field for separating
  same-named entities. Keep it accurate if the product description changes.

---

## Positioning: three methodologies, one focus

ShipFlow is **methodology-agnostic**. Shape Up, Scrum, and Kanban are all
first-class project modes, and a single workspace can run all three side by
side. Copy must never imply ShipFlow is a Shape-Up-only tool — "does it also do
Scrum?" is the first question a visiting team asks, and answering it late loses
the visitor.

But *focus* and *coverage* are different things, and the search strategy
depends on the difference:

| Term class | Example | Strategy |
|---|---|---|
| **Shape Up vocabulary** | `shape up methodology`, `hill chart`, `betting table`, `shape up software` | **Target directly.** Low competition, exact intent match, and ShipFlow is genuinely one of the few real answers. This is where pages get written. |
| **Bridge terms** | `shape up vs scrum`, `shape up and kanban`, `switching from scrum to shape up` | **Target directly.** High intent, low competition, and they reach teams who have Scrum today — the realistic convert. |
| **Open-source / self-hosted long tail** | `open source scrum tool`, `self-hosted kanban board`, `free jira alternative self hosted` | **Target selectively.** Winnable, and the open-source angle is a real differentiator. Best served by `/compare` and comparison posts. |
| **Head terms** | `scrum software`, `kanban board`, `project management tool` | **Do not target.** Jira, Trello, monday.com and Asana spend real money here. A new site cannot win them, and effort spent trying is effort not spent on the rows above. |

The practical rule for any new page: **lead with Shape Up, but always name all
three.** Titles and descriptions should make the breadth visible without
burying the differentiator. The landing page title —
`ShipFlow — Open-Source Project Management (Shape Up, Kanban, Scrum)` — is the
model: the differentiator disambiguates the brand, the parenthetical reassures
on coverage.

Watch description length when doing this. Google truncates around 160
characters, and the "…and Scrum and Kanban too" reassurance is exactly the part
that gets cut. Put it before the 150-character mark or lose it.

---

## Architecture

| Concern | Where | Notes |
|---|---|---|
| Per-route meta tags | `frontend/src/hooks/useSeo.ts` | Zero dependencies. Title, description, canonical, robots, OG, Twitter, JSON-LD. |
| Crawl policy | `frontend/public/robots.txt` | Allow-by-default, explicit disallow list. |
| Sitemap | `frontend/scripts/generate-sitemap.mjs` | Generated at build time. Never edit the output. |
| Blog content | `frontend/public/blog/posts/*.md` + `index.json` | Markdown with YAML frontmatter. |
| Static fallback meta | `frontend/index.html` | What a non-JS crawler sees. Keep it as a sane default for the landing page. |

### Why a custom hook instead of react-helmet-async

`react-helmet-async` is in maintenance mode, and this need is about sixty lines
of DOM manipulation. Adding a dependency to a public open-source project has a
real supply-chain cost that this feature does not justify.

---

## Adding a new public page

1. Add the route to `frontend/src/App.tsx` **above** the `/*` protected route.
2. Call `useSeo` at the top of the component:

   ```tsx
   useSeo({
     title: 'Betting Table Guide',        // ` · ShipFlow` is appended automatically
     description: '140–160 characters, written for a human reading a SERP.',
     path: '/guides/betting-table',       // canonical path, leading slash
     keywords: ['betting table', 'shape up betting'],
     jsonLd: breadcrumbSchema([
       { name: 'Home', path: '/' },
       { name: 'Betting Table Guide', path: '/guides/betting-table' },
     ]),
   });
   ```

3. Add the path to `STATIC_ROUTES` in `scripts/generate-sitemap.mjs`.
4. If the page must be reachable but not indexed (sign-in, callbacks, thin
   utility pages), pass `noindex: true` and leave it **out** of the sitemap.
5. If the route is part of the authenticated app, add it to the disallow list
   in `robots.txt` instead of doing any of the above.

## Adding a blog post

> **Posts live in the private `ShipFlow-blog` repository, not here.** That is
> deliberate: a merged pull request against this open-source repo must not be
> able to publish an article on shipflow.dev. The `.md` files in
> `frontend/public/blog/posts/` are build-time copies and get overwritten for
> any filename that exists in both places — see the README in that directory.
>
> Write the post in the private repo. Everything below still applies; only the
> location changes.
>
> **Getting posts into a build.** `.github/workflows/docker.yml` injects them,
> but only on a `v*.*.*` tag push — a plain local `docker build` skips it. Use
> `npm run build:deploy` (which is `blog:sync` + `build`) for any local
> production build, or three posts will sit unpublished for months the way
> `cooldown-periods-matter` and friends did.

1. Create `{slug}.md` in the private repo's `posts/` directory with full frontmatter:

   ```yaml
   ---
   title: "Sentence-case title that includes the target phrase"
   slug: the-slug
   date: 2026-08-16
   description: "140–160 characters. This becomes the SERP snippet."
   keywords: ["primary phrase", "close variants"]
   author: farzad
   ---
   ```

2. Add a "Further reading" section linking to two or three related posts, and
   add a link **to** the new post from at least one existing post. A post that
   nothing links to is much slower to get discovered and ranks worse.
3. Use root-relative internal links (`/blog/other-post`), never absolute
   `https://shipflow.dev/...` links.
4. Don't touch `index.json` — it is generated from every post's frontmatter
   `date`, newest first, by both `blog:sync` and the CI workflow.

That's all. `blog:sync` rebuilds the index, the sitemap generator picks the post
up at build time, and `BlogPost.tsx` derives the meta tags and `BlogPosting`
schema from the frontmatter.

**One canonical page per query.** Before adding a post, check that no existing
one already targets its primary phrase. Two pages competing for the same query
split link equity and let Google choose between them semi-arbitrarily — this
already happened once, with `betting-tables-explained` and
`what-is-a-betting-table`, resolved by merging into the single deeper page.

---

## Rules

**Never reintroduce a blanket `Disallow: /` in `robots.txt`.** It was there
once with a four-path allow-list that omitted `/` and `/blog`, which silently
forbade crawling of the landing page and every article while the sitemap kept
advertising them. Use the explicit disallow list.

**Never hand-edit `sitemap.xml`.** It is generated. A hand-maintained copy is
how `lastmod` dates froze five months behind reality and a published post went
unlisted.

**Don't route SEO strings through i18next.** This is a deliberate exception to
the project's i18n rule. Public pages serve one URL per page regardless of the
selected language, so a client-side language toggle rewriting the title or
description would mean the canonical URL and its indexed content disagree.
Meta strings stay in English at the call site. UI strings on the same page
still go through i18next as normal.

**Titles are ~60 characters, descriptions 140–160.** Longer gets truncated in
the SERP, which usually cuts the disambiguating half.

**One canonical per page.** `useSeo` strips trailing slashes for exactly this
reason. `/blog/post` and `/blog/post/` must not both be claimable.

**Keep `noindex` and the sitemap consistent.** Listing a `noindex` page in the
sitemap asks Google to index something that refuses, which shows up as a
coverage error against the whole site.

---

## Verifying a change

```bash
cd frontend && npm run build
```

Then check `dist/sitemap.xml` and `dist/robots.txt`.

For per-route tags, run the dev server and inspect the settled DOM — the tags
are written by an effect, so they are correct after hydration, not in the
initial HTML source:

```bash
cd frontend && npm run dev
```

Unit tests for the hook live in `frontend/src/hooks/useSeo.test.ts`.

After deploying, the authoritative checks are Google Search Console's URL
Inspection tool (does Google see the page, and with which title?) and the
[Rich Results Test](https://search.google.com/test/rich-results) for structured
data.

---

## Known limitation: client-side rendering

Meta tags and structured data are applied by a React effect, so they exist in
the rendered DOM but not in the initial HTML response. Googlebot, Bingbot,
GPTBot and ClaudeBot all execute JavaScript and will see them. Some smaller
crawlers and a few social-preview scrapers will not, and will fall back to the
static tags in `index.html`.

The fix, if this becomes a measurable problem, is build-time prerendering of
the public routes into static HTML. That is a larger change and deliberately
not done yet — the crawl-blocking and missing-meta problems above were the
binding constraints, and they are addressed. Revisit only with Search Console
data showing rendered-content indexing failures, not on principle.

Note that `LinkPreviewController` already server-renders OG tags for
`/preview/{pitch,task,cycle}/{id}` — that pattern is the natural starting point
for a server-side approach if one is ever needed.
