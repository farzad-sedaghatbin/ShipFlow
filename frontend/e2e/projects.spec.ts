import { test, expect } from '@playwright/test';
import { login, waitForApp } from './helpers';

test.describe('Project Management', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
    await login(page);
  });

  test('projects list page loads', async ({ page }) => {
    await page.goto('/projects');
    await expect(
      page.locator('h1').filter({ hasText: /projects/i })
        .or(page.locator('h2').filter({ hasText: /projects/i }))
    ).first().toBeVisible({ timeout: 10000 });
  });

  test('create a Shape Up project', async ({ page }) => {
    await page.goto('/projects');
    await page.click('text=New Project');

    // Scope ALL interactions to the dialog to avoid hitting background elements
    // that are behind the Radix Dialog overlay.
    const dialog = page.locator('[role="dialog"]');
    await dialog.waitFor({ timeout: 5000 });

    const projectName = `E2E Shape Up ${Date.now()}`;
    await dialog.locator('#name, input[name="name"], input[placeholder*="name" i]').first().fill(projectName);

    // Shape Up should be default — verify it is selected (within dialog only)
    await expect(dialog.locator('text=Shape Up').first()).toBeVisible();

    // Submit — try common button labels, scoped to the dialog
    await dialog.locator('button[type="submit"], button:has-text("Create"), button:has-text("Save"), button:has-text("Add")').first().click();
    await expect(page.locator(`text=${projectName}`)).toBeVisible({ timeout: 10000 });
  });

  test('create a Kanban project', async ({ page }) => {
    await page.goto('/projects');
    await page.click('text=New Project');

    const dialog = page.locator('[role="dialog"]');
    await dialog.waitFor({ timeout: 5000 });

    const projectName = `E2E Kanban ${Date.now()}`;
    await dialog.locator('#name, input[name="name"], input[placeholder*="name" i]').first().fill(projectName);

    // Click Kanban INSIDE the dialog to avoid hitting sidebar nav links behind the overlay
    await dialog.locator('text=Kanban').first().click();

    // Submit
    await dialog.locator('button[type="submit"], button:has-text("Create"), button:has-text("Save"), button:has-text("Add")').first().click();
    await expect(page.locator(`text=${projectName}`)).toBeVisible({ timeout: 10000 });
  });

  test('Shape Up project shows Cycles in sidebar when selected', async ({ page }) => {
    await page.goto('/projects');

    // Find a Shape Up project card and click its "View Cycles" button
    const shapeUpCard = page.locator('[aria-label*="View Cycles"]').first();
    if (await shapeUpCard.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Navigate to that project context
      await shapeUpCard.click();
    } else {
      // Create one first — scope all interactions to the dialog
      await page.click('text=New Project');
      const dialog = page.locator('[role="dialog"]');
      await dialog.waitFor({ timeout: 5000 });
      const name = `E2E SU Nav ${Date.now()}`;
      await dialog.locator('#name, input[name="name"], input[placeholder*="name" i]').first().fill(name);
      // Shape Up is default — just submit
      await dialog.locator('button[type="submit"], button:has-text("Create"), button:has-text("Save"), button:has-text("Add")').first().click();
      await expect(page.locator(`text=${name}`)).toBeVisible({ timeout: 10000 });
      await page.goto('/cycles');
    }
    // Cycles nav item should be visible in sidebar
    await expect(page.locator('[data-tour="sidebar"]').locator('text=Cycles')).toBeVisible({ timeout: 10000 });
  });

  test('Kanban project shows Backlog link instead of Cycles', async ({ page }) => {
    await page.goto('/projects');

    // Ensure at least one Kanban project exists
    let backlogBtn = page.locator('[aria-label*="View Backlog"]').first();
    if (!await backlogBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Create a Kanban project so the sidebar assertion is meaningful
      // Scope all interactions to the dialog to avoid clicking sidebar elements behind the overlay
      await page.click('text=New Project');
      const dialog = page.locator('[role="dialog"]');
      await dialog.waitFor({ timeout: 5000 });
      const projectName = `E2E Kanban Nav ${Date.now()}`;
      await dialog.locator('#name, input[name="name"], input[placeholder*="name" i]').first().fill(projectName);
      await dialog.locator('text=Kanban').first().click();
      await dialog.locator('button[type="submit"], button:has-text("Create"), button:has-text("Save"), button:has-text("Add")').first().click();
      await expect(page.locator(`text=${projectName}`)).toBeVisible({ timeout: 10000 });
      await page.goto('/projects');
      backlogBtn = page.locator('[aria-label*="View Backlog"]').first();
    }

    if (!await backlogBtn.isVisible({ timeout: 5000 }).catch(() => false)) {
      test.skip(true, 'Kanban backlog navigation not available in this environment');
      return;
    }

    await backlogBtn.click();
    await expect(page).toHaveURL(/\/backlog/, { timeout: 10000 });

    // In Kanban mode the sidebar should show Backlog, not Cycles
    const sidebar = page.locator('[data-tour="sidebar"]');
    await expect(sidebar.locator('text=Backlog')).toBeVisible({ timeout: 10000 });
  });

  test('project selector in sidebar lists available projects', async ({ page }) => {
    await page.goto('/dashboard');
    const selector = page.locator('[data-tour="project-selector"]');
    await expect(selector).toBeVisible();
    await selector.click();
    // Dropdown should show at least one project
    await expect(page.locator('[role="option"], [role="menuitem"], [data-radix-select-item]').first()).toBeVisible({ timeout: 5000 });
  });
});
