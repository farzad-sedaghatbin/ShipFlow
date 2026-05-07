import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'ShipFlow',
  description: 'Methodology-agnostic project management — Shape Up, Kanban, and more.',
  base: process.env.NODE_ENV === 'production' ? '/ShipFlow/' : '/',
  ignoreDeadLinks: [/localhost/],

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }],
  ],

  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'ShipFlow Docs',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'Getting Started', link: '/getting-started/installation' },
      { text: 'GitHub', link: 'https://github.com/farzad-sedaghatbin/ShipFlow' },
    ],

    sidebar: [
      {
        text: 'Getting Started',
        items: [
          { text: 'Installation', link: '/getting-started/installation' },
          { text: 'Environment Setup', link: '/getting-started/environment-setup' },
          { text: 'Quick Start (Docker)', link: '/getting-started/quick-start' },
        ],
      },
      {
        text: 'User Guide',
        items: [
          { text: 'Shape Up Workflow', link: '/user-guide/shape-up' },
          { text: 'Kanban Mode', link: '/user-guide/kanban' },
          { text: 'Project Types', link: '/user-guide/project-types' },
          { text: 'Hill Charts', link: '/user-guide/hill-charts' },
          { text: 'Onboarding Tour', link: '/user-guide/tour' },
        ],
      },
      {
        text: 'Admin Guide',
        items: [
          { text: 'GitHub Integration', link: '/admin-guide/github-integration' },
          { text: 'Slack Integration', link: '/admin-guide/slack-integration' },
          { text: 'Teams Integration', link: '/admin-guide/teams-integration' },
          { text: 'MCP Client Setup', link: '/admin-guide/mcp-client-setup' },
          { text: 'Redis Configuration', link: '/admin-guide/redis-configuration' },
          { text: 'Permission Matrix', link: '/admin-guide/permission-matrix' },
        ],
      },
      {
        text: 'Developer Guide',
        items: [
          { text: 'Contributing', link: '/developer-guide/contributing' },
          { text: 'Development Workflow', link: '/developer-guide/development-workflow' },
          { text: 'Architecture Overview', link: '/developer-guide/architecture' },
          { text: 'Claude Code Guide', link: '/developer-guide/claude-code-guide' },
        ],
      },
      {
        text: 'API Reference',
        items: [
          { text: 'REST API', link: '/api-reference/rest-api' },
          { text: 'MCP Server Tools', link: '/api-reference/mcp-tools' },
          { text: 'Webhooks', link: '/api-reference/webhooks' },
        ],
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/farzad-sedaghatbin/ShipFlow' },
    ],

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026 ShipFlow Contributors',
    },

    editLink: {
      pattern: 'https://github.com/farzad-sedaghatbin/ShipFlow/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },

    search: {
      provider: 'local',
    },
  },
})
