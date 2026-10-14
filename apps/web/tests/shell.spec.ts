import { test, expect } from '@playwright/test';
test('unauthenticated dashboard redirects to login and health crosses the frontend proxy', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  expect(await (await request.get('/system/health')).json()).toEqual({ status: 'UP' });
  expect((await request.get('/api/v1/dashboard')).status()).toBe(401);
});
