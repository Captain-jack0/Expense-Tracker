import { test, expect } from '@playwright/test';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';

test.describe('Smoke: application startup', () => {
  test('frontend serves the root page', async ({ page }) => {
    const response = await page.goto('/');
    expect(response, 'expected a response from /').not.toBeNull();
    expect(response!.ok()).toBe(true);
  });

  test('frontend routes to /login (SPA fallback)', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveURL(/\/login$/);
    // React takes a moment to hydrate; give the document a chance to render.
    await page.waitForLoadState('domcontentloaded');
  });

  test('backend actuator health reports UP', async ({ request }) => {
    const response = await request.get(`${API_URL}/actuator/health`);
    expect(response.ok()).toBe(true);
    const body = (await response.json()) as { status?: string };
    expect(body.status).toBe('UP');
  });
});
