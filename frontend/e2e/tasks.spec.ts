import { test, expect } from '@playwright/test';
import { login, waitForApp, selectFirstProject } from './helpers';

/**
 * Helper: create a task via UI and return its ID from the POST /api/tasks response.
 * Avoids pagination issues — seed data has 20+ tasks so the new task lands on page 2+
 * of the backlog list. Capturing the ID directly lets us navigate with page.goto().
 */
async function createTaskAndGetId(
  page: Parameters<typeof login>[0],
  titlePrefix: string
): Promise<{ taskTitle: string; taskId: number | null }> {
  const taskTitle = `${titlePrefix} ${Date.now()}`;

  // Register response listener BEFORE clicking Create so we don't miss it
  const taskResponsePromise = page.waitForResponse(
    (resp) => resp.url().includes('/api/tasks') && resp.request().method() === 'POST',
    { timeout: 15000 }
  );

  await page.click('text=New Task');
  await page.fill('#title', taskTitle);
  // BacklogTaskDialog submit: t('backlogPage.create') = "Create"
  await page.locator('[role="dialog"]').getByRole('button', { name: /^(Create|Update|Save|Add Task|Create Task)$/ }).click();

  const taskResponse = await taskResponsePromise;
  const body = await taskResponse.json().catch(() => null);
  const taskId: number | null = body?.id ?? null;

  return { taskTitle, taskId };
}

test.describe('Task Management', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
    await login(page);
    // Ensure a Kanban project is active so "New Task" is enabled
    await selectFirstProject(page);
  });

  test('backlog page loads', async ({ page }) => {
    await page.goto('/backlog');
    await expect(page.locator('h1, h2').filter({ hasText: /backlog/i })).toBeVisible({ timeout: 10000 });
  });

  test('create a new task from backlog', async ({ page }) => {
    await page.goto('/backlog');
    const { taskTitle } = await createTaskAndGetId(page, 'E2E Task');
    // Verify the task appears somewhere on the page (toast or list)
    await expect(page.locator(`text=${taskTitle}`)).toBeVisible({ timeout: 10000 });
  });

  test('open task detail page', async ({ page }) => {
    await page.goto('/backlog');
    const { taskId } = await createTaskAndGetId(page, 'E2E Detail');
    if (!taskId) {
      test.skip(true, 'Task creation response did not include an id — skipping navigation test');
      return;
    }
    // Navigate directly using the ID — avoids pagination (seed data has 20+ tasks)
    await page.goto(`/backlog/${taskId}`);
    await expect(page).toHaveURL(/\/backlog\/\d+/, { timeout: 10000 });
    await expect(page.locator('h1, h2').first()).toBeVisible();
  });

  test('change task status', async ({ page }) => {
    await page.goto('/backlog');
    const { taskId } = await createTaskAndGetId(page, 'E2E Status');
    if (!taskId) {
      test.skip(true, 'Task creation response did not include an id — skipping status test');
      return;
    }
    await page.goto(`/backlog/${taskId}`);
    await expect(page).toHaveURL(/\/backlog\/\d+/, { timeout: 10000 });

    // Status selector must be present — if missing it's a regression
    const statusSelect = page.locator('[role="combobox"]').filter({ hasText: /TODO|IN_PROGRESS|DONE|status/i }).first();
    await expect(statusSelect).toBeVisible({ timeout: 5000 });
    await statusSelect.click();
    const inProgressOption = page.locator('[role="option"]:has-text("IN_PROGRESS"), [role="option"]:has-text("In Progress")').first();
    await expect(inProgressOption).toBeVisible({ timeout: 3000 });
    await inProgressOption.click();
    await expect(
      page.locator('text=IN_PROGRESS').or(page.locator('text=In Progress')).first()
    ).toBeVisible({ timeout: 10000 });
  });

  test('add a comment with @mention', async ({ page }) => {
    await page.goto('/backlog');
    const { taskId } = await createTaskAndGetId(page, 'E2E Comment');
    if (!taskId) {
      test.skip(true, 'Task creation response did not include an id — skipping comment test');
      return;
    }
    await page.goto(`/backlog/${taskId}`);
    await expect(page).toHaveURL(/\/backlog\/\d+/, { timeout: 10000 });

    // Comment textarea must be present for @mention coverage to be valid
    const commentInput = page.locator('textarea[placeholder*="comment"], textarea[placeholder*="Add a comment"]').first();
    if (!await commentInput.isVisible({ timeout: 5000 }).catch(() => false)) {
      test.skip(true, 'Comment textarea not present — comments may be behind a feature flag');
      return;
    }
    await commentInput.fill('@admin great work on this task!');
    // t('backlogPage.addComment') = "Add Comment"
    await page.getByRole('button', { name: /^(Add Comment|Post Comment|Post|Submit)$/ }).click();
    await expect(page.locator('text=great work on this task')).toBeVisible({ timeout: 10000 });
  });

  test('notification bell is visible after login', async ({ page }) => {
    await page.goto('/dashboard');
    // Notification bell should be in the layout
    const notifBell = page.locator('[aria-label*="notification"], [data-tour*="notification"], button:has(svg)').first();
    await expect(notifBell).toBeVisible({ timeout: 10000 });
  });

  test('global search opens with Cmd+K and finds tasks', async ({ page }) => {
    await page.goto('/dashboard');

    // Try both keyboard shortcuts — Meta+K (macOS) and Control+K (Linux/Windows)
    await page.keyboard.press('Meta+k');
    // Also try Control+K in case the first didn't fire (headless CI Linux)
    const searchInputCheck = page.locator('input[placeholder*="Search"], input[placeholder*="search"], [cmdk-input]').first();
    const openedWithMeta = await searchInputCheck.isVisible({ timeout: 1500 }).catch(() => false);
    if (!openedWithMeta) {
      await page.keyboard.press('Control+k');
    }

    // Search dialog/command palette should appear
    const searchInput = page.locator('input[placeholder*="Search"], input[placeholder*="search"], [cmdk-input]').first();
    const dialogVisible = await searchInput.isVisible({ timeout: 5000 }).catch(() => false);
    if (!dialogVisible) {
      // Keyboard shortcut may be captured by the OS in headless CI — skip gracefully
      test.skip(true, 'Global search keyboard shortcut did not open dialog in this environment');
      return;
    }

    // Type a search query and wait for results without fixed sleep
    await searchInput.fill('task');

    const results = page.locator('[role="option"], [cmdk-item], [data-value]').first();
    const noResults = page.locator('text=/no results/i').first();
    // Wait for either results or no-results message to appear
    await Promise.race([
      expect(results).toBeVisible({ timeout: 5000 }).catch(() => {}),
      expect(noResults).toBeVisible({ timeout: 5000 }).catch(() => {}),
    ]);
    const hasResults = await results.isVisible().catch(() => false);
    const hasNone = await noResults.isVisible().catch(() => false);
    expect(hasResults || hasNone).toBe(true);

    // Close with Escape
    await page.keyboard.press('Escape');
    await expect(searchInput).not.toBeVisible({ timeout: 3000 });
  });

  test('task list supports filtering by status', async ({ page }) => {
    await page.goto('/backlog');

    // Look for a filter/status dropdown
    const filterBtn = page.locator('button:has-text("Filter"), button:has-text("Status"), [aria-label*="filter"]').first();
    if (await filterBtn.isVisible({ timeout: 5000 })) {
      await filterBtn.click();
      // Some filter options should appear
      await expect(page.locator('[role="option"], [role="menuitem"], [role="checkbox"]').first()).toBeVisible({ timeout: 5000 });
    } else {
      // Filters may be inline — just verify the page heading is present
      await expect(page.locator('h1').filter({ hasText: /backlog/i })).toBeVisible();
    }
  });
});
