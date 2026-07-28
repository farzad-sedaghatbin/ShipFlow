import { expect, test } from '@playwright/test';
import { login, waitForApp } from './helpers';

test.describe('Onboarding tour', () => {
  test('close control presents an actionable confirmation and dismisses the tour', async ({ page }) => {
    await waitForApp(page);
    await login(page);

    await page.getByRole('button', { name: /restart guided tour/i }).click();

    const popover = page.locator('.driver-popover');
    await expect(popover).toBeVisible();
    await expect(popover.locator('.driver-popover-progress-text')).toContainText(/1 of \d+/);

    await popover.locator('.driver-popover-close-btn').click();

    const dialog = page.getByRole('alertdialog');
    const overlay = page.locator('[data-radix-alert-dialog-overlay]');
    await expect(dialog).toBeVisible();
    await expect(overlay).toBeVisible();

    const layers = await page.evaluate(() => ({
      dialog: Number.parseInt(
        getComputedStyle(document.querySelector('[role="alertdialog"]')!).zIndex,
        10,
      ),
      overlay: Number.parseInt(
        getComputedStyle(document.querySelector('[data-radix-alert-dialog-overlay]')!).zIndex,
        10,
      ),
      popover: Number.parseInt(
        getComputedStyle(document.querySelector('.driver-popover')!).zIndex,
        10,
      ),
    }));

    expect(layers.dialog).toBeGreaterThan(layers.popover);
    expect(layers.overlay).toBeGreaterThan(layers.popover);

    await dialog.getByRole('button', { name: /skip tour/i }).click();

    await expect(dialog).toBeHidden();
    await expect(popover).toBeHidden();
    await expect
      .poll(() => page.evaluate(() => localStorage.getItem('shipflow_tour_completed')))
      .toBe('true');
  });
});
