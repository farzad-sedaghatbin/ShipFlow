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
 * The app stores the selected project in localStorage under
 * `shipflow_selected_project_id`. On the backlog page, "New Task" is only
 * enabled for Kanban projects (Shape Up requires a cycle to be selected too).
 *
 * Strategy: call the projects API with the JWT from localStorage, find the
 * first Kanban project, write its id into localStorage, then navigate to
 * /backlog so the React ProjectContext picks it up on mount.
 */
export async function selectFirstProject(page: Page) {
  // Ensure we are on an authenticated page so the JWT exists in localStorage
  if (!page.url().includes('localhost')) {
    await page.goto('/dashboard');
    await expect(page.locator('[data-tour="sidebar"]')).toBeVisible({ timeout: 15000 });
  }

  const projectId = await page.evaluate(async () => {
    const token = localStorage.getItem('shipflow_token');
    if (!token) return null;
    try {
      const res = await fetch('/api/projects/active', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return null;
      const projects: Array<{ id: number; projectType: string }> = await res.json();
      // Prefer Kanban — "New Task" is always enabled there (no cycle needed)
      const kanban = projects.find((p) => p.projectType === 'KANBAN');
      const chosen = kanban ?? projects[0] ?? null;
      if (chosen) {
        localStorage.setItem('shipflow_selected_project_id', chosen.id.toString());
        return chosen.id;
      }
    } catch {
      // ignore — tests will fail naturally if the button remains disabled
    }
    return null;
  });

  if (projectId) {
    // Re-navigate so ProjectContext re-reads the updated localStorage key
    await page.goto('/backlog');
    await page.waitForLoadState('networkidle');
  }
}
