import { Page, expect } from '@playwright/test';

export const ADMIN = { username: 'admin', password: 'admin123' };
export const BASE_URL = 'http://localhost:3000';

/**
 * Log in with the given credentials and wait for the dashboard to load.
 */
export async function login(page: Page, username = ADMIN.username, password = ADMIN.password) {
  await page.goto('/login');
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
 * Wait for the API to be reachable before running tests.
 */
export async function waitForApp(page: Page) {
  await page.goto('/login');
  await expect(page.locator('#username')).toBeVisible({ timeout: 30000 });
}
