/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      // injectManifest (not generateSW) — src/sw.ts hand-writes a custom
      // background-sync `onSync` callback (see that file's header comment).
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      registerType: 'autoUpdate',
      injectRegister: false, // registered manually from src/lib/pwa.ts
      manifest: {
        name: 'ShipFlow — Project Management',
        short_name: 'ShipFlow',
        description:
          'Open-source, AI-native project management platform supporting Shape Up, Kanban, and Scrum in one workspace.',
        theme_color: '#1976d2',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          { src: '/android-chrome-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: '/android-chrome-512x512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/android-chrome-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      injectManifest: {
        // App-shell assets only (JS/CSS/HTML + small icons) — PNGs are
        // deliberately excluded: `public/icon.png` alone is ~8MB (a
        // marketing-page source image, not an app icon) and precaching would
        // bloat SW install time for every user on every deploy. Images are
        // still cached, just lazily via the runtime CacheFirst route in
        // src/sw.ts on first use instead of eagerly at install time.
        globPatterns: ['**/*.{js,css,html,svg,ico,woff2}'],
      },
      devOptions: {
        // The dev server rewrites/HMR-injects modules in ways that don't
        // round-trip cleanly through a precaching SW; verify PWA behavior
        // against `npm run build && npm run preview` instead (see
        // PWA_GUIDE.md).
        enabled: false,
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
    // Exclude Playwright E2E tests — those are run separately via `npm run test:e2e`
    exclude: [
      '**/node_modules/**',
      '**/dist/**',
      'e2e/**',
      '**/*.e2e.{ts,tsx}',
    ],
    pool: 'forks',
    poolOptions: {
      forks: {
        // singleFork: true accumulates ALL jsdom environments in one process → OOM.
        // maxForks: 1 + singleFork: false gives each file its own fresh process
        // while running only one at a time → low peak memory, full isolation.
        singleFork: false,
        maxForks: 1,
        minForks: 1,
      },
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'src/test/',
        'e2e/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/types/**',
      ],
    },
  },
})
