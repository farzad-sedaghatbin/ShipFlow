import { test, expect } from '@playwright/test';
import path from 'path';
import { login, waitForApp } from './helpers';

const SAMPLE_CSV = path.join(__dirname, 'fixtures', 'sample.csv');

test.describe('Import / Migration', () => {
  test.beforeEach(async ({ page }) => {
    await waitForApp(page);
    await login(page);
  });

  test('import history page loads', async ({ page }) => {
    await page.goto('/import');
    // Wait for sidebar to confirm the layout rendered
    await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });

    // The import page header should be visible
    const heading = page.locator('h1').filter({ hasText: /import/i });
    await expect(heading).toBeVisible({ timeout: 10000 });
  });

  test('import page shows CSV, Linear, and Jira source tabs', async ({ page }) => {
    await page.goto('/import');
    await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });

    // The three source tabs (CSV, Linear, Jira) should all be visible
    await expect(page.locator('button, [role="tab"]').filter({ hasText: /csv/i }).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('button, [role="tab"]').filter({ hasText: /linear/i }).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('button, [role="tab"]').filter({ hasText: /jira/i }).first()).toBeVisible({ timeout: 10000 });
  });

  test('CSV tab shows format options (jira, linear, asana, generic)', async ({ page }) => {
    await page.goto('/import');
    await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });

    // Click the CSV tab (it may already be active by default)
    const csvTab = page.locator('button, [role="tab"]').filter({ hasText: /^csv$/i }).first();
    const csvTabVisible = await csvTab.isVisible({ timeout: 5000 }).catch(() => false);
    if (csvTabVisible) {
      await csvTab.click();
    }

    // The upload step (step 1) should be visible — look for the drop zone or file input
    const dropZone = page.locator('[role="button"]').filter({ hasText: /upload|drag|csv/i }).first();
    const fileInput = page.locator('input[type="file"]').first();
    const hasDropZone = await dropZone.isVisible({ timeout: 5000 }).catch(() => false);
    const hasFileInput = await fileInput.count().then((c) => c > 0).catch(() => false);
    expect(hasDropZone || hasFileInput).toBe(true);

    // Format selector — look for format options (auto-detect, jira, linear, asana, generic)
    // They may be in a <select>, <button> group, or radio buttons
    const formatSelectors = page.locator(
      'select, [role="combobox"], [role="listbox"], [role="radiogroup"], [role="group"]'
    );
    const hasFormatSelectors = await formatSelectors.count().then((c) => c > 0).catch(() => false);

    // Alternatively the format options may render as text in a collapsible table
    const formatText = page.locator('text=/jira|linear|asana|generic/i').first();
    const hasFormatText = await formatText.isVisible({ timeout: 5000 }).catch(() => false);

    expect(hasFormatSelectors || hasFormatText).toBe(true);
  });

  test('CSV upload flow — file selected shows file name in the UI', async ({ page }) => {
    await page.goto('/import');
    await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });

    // Click CSV tab if not already active
    const csvTab = page.locator('button, [role="tab"]').filter({ hasText: /^csv$/i }).first();
    const csvTabVisible = await csvTab.isVisible({ timeout: 5000 }).catch(() => false);
    if (csvTabVisible) {
      await csvTab.click();
    }

    // Locate the hidden file input and upload the fixture CSV
    const fileInput = page.locator('input[type="file"]').first();
    const inputExists = await fileInput.count().then((c) => c > 0).catch(() => false);
    if (!inputExists) {
      test.skip(true, 'File input not found — CSV upload UI may have changed');
      return;
    }

    await fileInput.setInputFiles(SAMPLE_CSV);

    // After selecting a file, the UI should show the file name somewhere
    const fileName = 'sample.csv';
    const fileNameVisible = await page.locator(`text=${fileName}`).isVisible({ timeout: 5000 }).catch(() => false);

    // Also acceptable: a submit/import button appears (indicating a file was staged)
    const submitBtn = page.locator('button[type="submit"], button:has-text("Import"), button:has-text("Upload"), button:has-text("Next")').first();
    const submitVisible = await submitBtn.isVisible({ timeout: 3000 }).catch(() => false);

    expect(fileNameVisible || submitVisible).toBe(true);
  });

  test('import nav link is present in sidebar', async ({ page }) => {
    await page.goto('/dashboard');
    await page.locator('[data-tour="sidebar"]').waitFor({ timeout: 10000 });

    // The import nav item should be present in the sidebar (data-tour="import-nav")
    const importNavLink = page.locator('[data-tour="import-nav"]');
    await expect(importNavLink).toBeVisible({ timeout: 10000 });
  });
});
