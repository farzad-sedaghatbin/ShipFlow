# Blog posts — read before editing

**The canonical source for published blog posts is a private repository, not
this one.**

Posts are kept private so that a merged pull request against this open-source
repo cannot publish an article on shipflow.dev. Editorial control over what
appears on the live site stays with the maintainer.

## What that means in practice

The `.md` files sitting next to this README are **copies that get overwritten
at build time** by the private repo's versions, for any post whose filename
exists in both places. Editing one of those files here looks like it works —
`npm run dev` picks it up — and then the change silently disappears on the next
deploy.

If you want to propose a change to an article's *content*, open an issue rather
than a pull request, and the maintainer will apply it in the private repo.

Changes to how posts are **rendered** (`src/pages/Blog.tsx`,
`src/pages/BlogPost.tsx`) or to the SEO plumbing around them
(`src/hooks/useSeo.ts`, `scripts/generate-sitemap.mjs`) are ordinary
contributions and belong here as normal.

## How posts reach a build

```bash
npm run blog:sync     # pull posts from the private repo, rebuild index.json
npm run build         # tsc + vite build + generate sitemap.xml
```

or in one step:

```bash
npm run build:deploy
```

`blog:sync` authenticates with `BLOG_SYNC_TOKEN` if set, otherwise the `gh`
CLI's own credentials. **Without either it exits successfully and leaves the
posts already on disk alone**, so contributors without access to the private
repo can still build the frontend — they just get whatever is committed here.

`.github/workflows/docker.yml` performs the same injection, but only on a
`v*.*.*` tag push. A plain local `docker build` does **not** — which is why
`blog:sync` exists. Three posts written in May 2026 went unpublished for months
because every deploy was a local build that skipped the CI injection step.

## index.json

Generated, not hand-maintained — both `blog:sync` and the CI workflow rebuild
it from the frontmatter `date` of every post on disk, newest first. Don't edit
it by hand; it will be overwritten.

See `SEO_GUIDE.md` in the repo root for the wider contract, including title and
description limits and how a new post gets cross-linked.
