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
 */
export async function logout(page: Page) {
  // Open user menu (bottom of sidebar)
  await page.click('[data-tour="user-menu"]');
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
