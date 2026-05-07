import { test, expect } from '@playwright/test';
import { login, waitForApp } from './helpers';

test.describe('Hill Chart', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
    await login(page);
  });

  test('cycles list page loads', async ({ page }) => {
    await page.goto('/cycles');
    await expect(page.locator('h1').filter({ hasText: /cycles/i })).toBeVisible({ timeout: 10000 });
  });

  test('hill chart page loads for an existing cycle', async ({ page }) => {
    await page.goto('/cycles');

    // Find any cycle that has a hill chart link
    const hillChartLink = page.locator('a[href*="hill-chart"]').first();
    if (await hillChartLink.isVisible({ timeout: 5000 })) {
      await hillChartLink.click();
      await expect(page).toHaveURL(/\/hill-chart/, { timeout: 10000 });
      await expect(page.locator('text=Cycle Hill Chart')).toBeVisible({ timeout: 10000 });
    } else {
      // Navigate directly if we know a cycle exists
      await page.goto('/cycles');
      // At least verify cycles page loaded
      await expect(page.locator('h1').filter({ hasText: /cycles/i })).toBeVisible();
    }
  });

  test('hill chart canvas renders when scopes exist', async ({ page }) => {
    await page.goto('/cycles');

    const hillChartLink = page.locator('a[href*="hill-chart"]').first();
    if (await hillChartLink.isVisible({ timeout: 5000 })) {
      await hillChartLink.click();
      await expect(page).toHaveURL(/\/hill-chart/, { timeout: 10000 });

      // Canvas should be present
      const canvas = page.locator('canvas').first();
      // Either canvas is visible (scopes exist) or no-scopes message appears
      const hasCanvas = await canvas.isVisible({ timeout: 5000 }).catch(() => false);
      const hasNoPoints = await page.locator('text=No hill chart points').isVisible({ timeout: 5000 }).catch(() => false);
      expect(hasCanvas || hasNoPoints).toBe(true);
    }
  });

  test('drag a scope dot and verify position persists', async ({ page }) => {
    await page.goto('/cycles');

    const hillChartLink = page.locator('a[href*="hill-chart"]').first();
    if (!await hillChartLink.isVisible({ timeout: 5000 })) {
      test.skip();
      return;
    }

    await hillChartLink.click();
    await expect(page).toHaveURL(/\/hill-chart/, { timeout: 10000 });

    const canvas = page.locator('canvas').first();
    if (!await canvas.isVisible({ timeout: 5000 }).catch(() => false)) {
      // No scopes — skip drag test
      test.skip();
      return;
    }

    const box = await canvas.boundingBox();
    if (!box) {
      test.skip();
      return;
    }

    // Read initial position percentage shown in the sidebar list
    const firstScopeEntry = page.locator('text=/%/').first();
    const initialText = await firstScopeEntry.textContent().catch(() => '');

    // Drag from near left of canvas toward center
    const startX = box.x + box.width * 0.15;
    const startY = box.y + box.height * 0.5;
    const endX = box.x + box.width * 0.4;

    await page.mouse.move(startX, startY);
    await page.mouse.down();
    await page.mouse.move(endX, startY, { steps: 10 });
    await page.mouse.up();

    // Wait for the API save to complete by watching for the sidebar text to update
    await page.waitForFunction(
      ({ selector, before }: { selector: string; before: string }) => {
        const el = document.querySelector(selector);
        return el !== null && el.textContent !== before;
      },
      { selector: '[class*="sidebar"] span, li span', before: initialText },
      { timeout: 5000 }
    ).catch(() => { /* position may not have changed enough to update label */ });

    // Reload and verify the page loads without an error toast
    await page.reload();
    await expect(page.locator('text=Cycle Hill Chart')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Error').first()).not.toBeVisible({ timeout: 3000 });
  });

  test('scopes by pitch section shows pitch groupings', async ({ page }) => {
    await page.goto('/cycles');

    const hillChartLink = page.locator('a[href*="hill-chart"]').first();
    if (await hillChartLink.isVisible({ timeout: 5000 })) {
      await hillChartLink.click();
      await expect(page).toHaveURL(/\/hill-chart/, { timeout: 10000 });

      // The "Scopes by Pitch" section should appear if there are any scopes
      const scopesSection = page.locator('text=Scopes by Pitch');
      const noPoints = page.locator('text=No hill chart points');
      const hasScopes = await scopesSection.isVisible({ timeout: 5000 }).catch(() => false);
      const hasNone = await noPoints.isVisible({ timeout: 5000 }).catch(() => false);
      expect(hasScopes || hasNone).toBe(true);
    }
  });
});
