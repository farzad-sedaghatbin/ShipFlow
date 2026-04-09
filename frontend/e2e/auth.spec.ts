import { test, expect } from '@playwright/test';
import { login, logout, waitForApp, ADMIN } from './helpers';

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
  });

  test('login page renders correctly', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('login with valid credentials redirects to dashboard', async ({ page }) => {
    await login(page);
    await expect(page).toHaveURL(/\/dashboard/);
    // Sidebar should be visible
    await expect(page.locator('[data-tour="sidebar"]')).toBeVisible();
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'wrongpassword');
    await page.click('button[type="submit"]');
    // Should stay on login page
    await expect(page).toHaveURL(/\/login/);
    // Error message should appear
    await expect(page.locator('text=/invalid|incorrect|wrong|error/i')).toBeVisible({ timeout: 10000 });
  });

  test('protected routes redirect unauthenticated users to login', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });

  test('logout redirects to login page', async ({ page }) => {
    await login(page);
    await logout(page);
    await expect(page).toHaveURL(/\/login/);
  });

  test('after logout, navigating to dashboard redirects to login', async ({ page }) => {
    await login(page);
    await logout(page);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });

  test('remember username persists across page reload', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#username', ADMIN.username);
    await page.fill('#password', ADMIN.password);
    // Remember checkbox must be present for this test to be meaningful
    const rememberCheckbox = page.locator('#remember');
    if (!await rememberCheckbox.isVisible({ timeout: 3000 }).catch(() => false)) {
      test.skip(true, '"Remember me" checkbox not present in current build');
      return;
    }
    await rememberCheckbox.check();
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await logout(page);
    // Username should be pre-filled after logout
    await expect(page.locator('#username')).toHaveValue(ADMIN.username);
  });
});
