# Licensing

Starting with the release after v1.13.1, ShipFlow is distributed under the
**[Elastic License 2.0](./LICENSE)** (ELv2), not the MIT License. ELv2 keeps
ShipFlow free to use, modify, and self-host — it simply prevents third
parties from reselling ShipFlow as a hosted or managed service, or from
circumventing license-key-protected functionality. See `LICENSE` for the
full legal text.

Releases up to and including v1.13.1 remain under the MIT License — see
[`LICENSE-MIT`](./LICENSE-MIT).

## What is free — always

- **Self-hosting** ShipFlow for your own team or organization, on your own
  infrastructure or any cloud account you control
- **Internal commercial use** — your company runs ShipFlow to manage its own
  projects, including for-profit use
- **Modifying and forking** ShipFlow for your own use
- **Building plugins and integrations** — custom risk calculators, report
  generators, integration providers (see `plugin-sdk/README.md`), and tools
  that connect to ShipFlow's API or MCP server
- **Consultants and agencies** installing, configuring, or customizing
  ShipFlow **for a client's own internal deployment** (the client runs and
  owns the instance)

## What is not allowed without a commercial license

- **Providing ShipFlow to third parties as a hosted or managed service** —
  operating an instance that other organizations' users log into, whether as
  a paid SaaS product, a managed offering bundled into another product, or a
  free-tier hosting service
- **White-label resale** — rebranding and selling ShipFlow (or a
  substantially similar derivative) as your own product
- **Removing, disabling, or circumventing license keys**, or any feature
  gated behind one, including modifying the software to bypass license-key
  checks

Examples that require a commercial license:
- A SaaS product where your customers pay to use a hosted ShipFlow
- A managed ShipFlow offering bundled into another product
- A white-labeled version of ShipFlow sold to end users
- Distributing a modified build that disables or bypasses license-key checks

## How to obtain a commercial license

Contact: **farzad@shipflow.dev**

We offer flexible terms for startups, agencies, and enterprises. Revenue-based
pricing is available so early-stage companies can get started without large
upfront costs.

## FAQ

**Does this affect versions I've already deployed?**
No. Every release up to and including v1.13.1 was published under the MIT
License and stays MIT-licensed — nothing is revoked retroactively. See
`LICENSE-MIT`.

**Can I still fork an MIT-licensed version?**
Yes. Forks made from a release at or before v1.13.1 are unaffected and
remain governed by the MIT License terms under which that version was
published. A fork that pulls in code from a release after v1.13.1 takes on
the Elastic License 2.0 for that code.

**What license applies if I upgrade an existing self-hosted install?**
Self-hosting is free either way — upgrading to a post-v1.13.1 release just
means the software you're running is now licensed under ELv2 instead of
MIT. The practical terms for self-hosters (run it, modify it, keep your
data) don't change; what changes is that reselling it as a hosted service
now requires a commercial license, which was never something the MIT
License granted protection against.

**What license do my contributions fall under?**
Contributions made under the MIT era remain MIT-licensed as contributed.
New contributions are accepted under the Elastic License 2.0, and (per
`CONTRIBUTING.md`) require a Developer Certificate of Origin (DCO)
sign-off (`git commit -s`) certifying you have the right to submit the
change under that license.

## Why this policy

ShipFlow is built and maintained by a solo developer. Self-hosting stays
free — that will never change. This policy simply ensures that if someone
builds a business by reselling ShipFlow as a service, the project that makes
it possible is sustainable too.

---

*This document is a plain-language summary. The [Elastic License 2.0](./LICENSE)
text is the legally binding license. For commercial hosting scenarios or any
other use outside what ELv2 permits, a separate commercial license agreement
is required.*
