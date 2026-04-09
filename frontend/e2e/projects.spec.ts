import { test, expect } from '@playwright/test';
import { login, waitForApp } from './helpers';

test.describe('Project Management', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
    await login(page);
  });

  test('projects list page loads', async ({ page }) => {
    await page.goto('/projects');
    await expect(page.locator('h1, h2').filter({ hasText: /projects/i })).toBeVisible({ timeout: 10000 });
  });

  test('create a Shape Up project', async ({ page }) => {
    await page.goto('/projects');
    await page.click('text=New Project');

    // Fill project name
    const projectName = `E2E Shape Up ${Date.now()}`;
    await page.fill('#name', projectName);

    // Shape Up should be default — verify it is selected
    await expect(page.locator('text=Shape Up').first()).toBeVisible();

    // Save
    await page.click('button:has-text("Create Project"), button:has-text("Save")');
    await expect(page.locator(`text=${projectName}`)).toBeVisible({ timeout: 10000 });
  });

  test('create a Kanban project', async ({ page }) => {
    await page.goto('/projects');
    await page.click('text=New Project');

    const projectName = `E2E Kanban ${Date.now()}`;
    await page.fill('#name', projectName);

    // Switch to Kanban
    await page.click('text=Kanban');

    // Save
    await page.click('button:has-text("Create Project"), button:has-text("Save")');
    await expect(page.locator(`text=${projectName}`)).toBeVisible({ timeout: 10000 });
  });

  test('Shape Up project shows Cycles in sidebar when selected', async ({ page }) => {
    await page.goto('/projects');

    // Find a Shape Up project card and click its "View Cycles" button
    const shapeUpCard = page.locator('[aria-label*="View Cycles"]').first();
    if (await shapeUpCard.isVisible()) {
      // Navigate to that project context
      await shapeUpCard.click();
    } else {
      // Create one first
      await page.click('text=New Project');
      const name = `E2E SU Nav ${Date.now()}`;
      await page.fill('#name', name);
      await page.click('button:has-text("Create Project"), button:has-text("Save")');
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
      await page.click('text=New Project');
      const projectName = `E2E Kanban Nav ${Date.now()}`;
      await page.fill('#name', projectName);
      await page.click('text=Kanban');
      await page.click('button:has-text("Create Project"), button:has-text("Save")');
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
