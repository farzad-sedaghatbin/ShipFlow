import { Page, expect } from '@playwright/test';

export const ADMIN = { username: 'admin', password: 'admin123' };
export const BASE_URL = 'http://localhost:3000';

/**
 * Log in with the given credentials and wait for the dashboard to load.
 * Also suppresses first-visit UI (welcome tour dialog, onboarding tour) so
 * overlays do not block subsequent interactions in tests.
 */
export async function login(page: Page, username = ADMIN.username, password = ADMIN.password) {
  await page.goto('/login');
  // Set localStorage flags BEFORE submitting the form so that when the
  // dashboard mounts, WelcomeTourDialog and TourContext see the keys already
  // set and never schedule the 1500 ms timer.  Setting them on /login works
  // because localStorage is scoped to the origin (same host:port).
  await page.evaluate(() => {
    localStorage.setItem('shipflow_welcome_shown', 'true');
    localStorage.setItem('shipflow_tour_completed', 'true');
  });
  await page.fill('#username', username);
  await page.fill('#password', password);
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
}

/**
 * Log out via the user menu in the sidebar.
 * Defensively clears blocking overlays before clicking:
 *  - Escape dismisses any open dialog/sheet (e.g. WelcomeTourDialog)
 *  - Wait for Sonner success toasts to disappear (they cover the user-menu)
 */
export async function logout(page: Page) {
  // Dismiss any open modal/dialog overlay
  await page.keyboard.press('Escape');
  // Wait for any Sonner toast to vanish (they sit at top-right and intercept clicks)
  await page.waitForFunction(
    () => !document.querySelector('[data-sonner-toast][data-visible="true"]'),
    { timeout: 8000 }
  ).catch(() => {});
  // Open user menu (bottom of sidebar)
  await page.locator('[data-tour="user-menu"]').click();
  await page.click('text=Logout');
  await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
}

/**
 * Wait for the frontend to be reachable and the login page to render.
 * Note: this checks that the frontend dev server is up and the login route
 * is rendered — not backend API health. Use the backend health check in CI.
 */
export async function waitForApp(page: Page) {
  await page.goto('/login');
  await expect(page.locator('#username')).toBeVisible({ timeout: 30000 });
}

/**
 * Select the first Kanban project so that "New Task" is always enabled.
 *
 * The backlog "New Task" button is only enabled when a Kanban project is
 * active (Shape Up also requires a specific cycle to be selected).
 *
 * Strategy: call GET /api/projects, find the first KANBAN project by
 * projectType, then write its id directly into the `shipflow_selected_project_id`
 * localStorage key that ProjectContext reads on mount.  This approach is immune
 * to i18n text changes, Radix dropdown timing, and the SSE notification stream
 * that keeps network activity alive and causes waitForLoadState('networkidle')
 * to time-out indefinitely.
 */
export async function selectFirstProject(page: Page) {
  // AuthContext stores the JWT under this key after a successful login
  const token = await page.evaluate(() => localStorage.getItem('shipflow_token'));
  if (!token) return;

  // GET /api/projects → List<ProjectDTO>  (plain array, fields: id, projectType)
  const projectId = await page.evaluate(async (authToken: string) => {
    try {
      const resp = await fetch('/api/projects', {
        headers: { Authorization: `Bearer ${authToken}` },
      });
      if (!resp.ok) return null;
      const projects: Array<{ id: number; projectType: string }> = await resp.json();
      const kanban = projects.find((p) => p.projectType === 'KANBAN');
      return kanban ? String(kanban.id) : null;
    } catch {
      return null;
    }
  }, token);

  if (!projectId) return;

  // Write the key that ProjectContext reads — avoids any UI interaction
  await page.evaluate((id: string) => {
    localStorage.setItem('shipflow_selected_project_id', id);
  }, projectId);

  // Navigate to backlog; wait for sidebar element rather than networkidle —
  // the SSE notification stream keeps the network permanently active.
  await page.goto('/backlog');
  await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });
}
