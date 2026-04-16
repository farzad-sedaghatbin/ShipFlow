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
 * Strategy:
 * 1. Go to /projects and find a "View Backlog" button — its aria-label
 *    includes the Kanban project name (e.g. "View Backlog for DevOps Platform")
 * 2. Extract the project name and use the sidebar project selector dropdown
 *    (role="menuitem") to select it by name — this calls selectProject() in
 *    ProjectContext, persisting the choice to localStorage.
 * 3. Navigate to /backlog so the page context is active.
 */
export async function selectFirstProject(page: Page) {
  // Step 1 — find a Kanban project name from the projects listing
  await page.goto('/projects');
  const viewBacklogBtn = page.locator('[aria-label*="View Backlog"]').first();
  const kanbanVisible = await viewBacklogBtn.isVisible({ timeout: 8000 }).catch(() => false);
  if (!kanbanVisible) {
    // No Kanban project visible — tests will fail naturally on missing button
    return;
  }

  // Extract project name from aria-label: "View backlog for <name>"
  const ariaLabel = (await viewBacklogBtn.getAttribute('aria-label')) ?? '';
  // i18n key: "viewBacklogFor": "View backlog for {{name}}"
  const projectName = ariaLabel.replace(/^view backlog for /i, '').trim();
  if (!projectName) return;

  // Step 2 — select the project via the sidebar dropdown
  await page.goto('/dashboard');
  await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 15000 });

  const trigger = page.locator('[data-tour="project-selector"]');
  await trigger.waitFor({ timeout: 5000 });
  await trigger.click();

  // Radix DropdownMenuItem uses role="menuitem" (not role="option")
  const projectItem = page.locator(`[role="menuitem"]:has-text("${projectName}")`).first();
  if (await projectItem.isVisible({ timeout: 3000 }).catch(() => false)) {
    await projectItem.click();
  } else {
    // Fallback: click the second menuitem (skip "All Projects" at index 0)
    await page.keyboard.press('Escape');
    await trigger.click();
    const items = page.locator('[role="menuitem"]');
    const count = await items.count();
    if (count > 1) {
      await items.nth(1).click();
    }
  }

  // Step 3 — navigate to backlog with project context active
  await page.goto('/backlog');
  await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });
}
