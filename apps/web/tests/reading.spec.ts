import { test, expect, Page } from '@playwright/test';
async function login(page: Page) {
  const email = process.env['E2E_EMAIL'], password = process.env['E2E_PASSWORD'];
  if (!email || !password) throw new Error('Use scripts/e2e.sh to inject local test credentials securely.');
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Your progress, one practice at a time' })).toBeVisible();
}
async function save(page: Page) {
  await page.getByRole('button', { name: 'Save answer', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('Saved on the server');
}
test('all three Reading tasks persist, score securely and update real history', async ({ page }) => {
  const errors: string[] = []; page.on('pageerror', error => errors.push(error.message)); page.on('console', message => { if(message.type() === 'error') errors.push(message.text()); });
  await login(page);
  const before = Number(await page.getByTestId('completed-count').innerText());
  const created = page.waitForResponse(response => response.url().endsWith('/api/v1/sessions') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Start Reading practice' }).click();
  const active = await (await created).json();
  expect(JSON.stringify(active)).not.toMatch(/correctAnswers|explanation|rubricVersion/);
  await expect(page.getByRole('heading', { name: 'A campus library' })).toBeVisible();
  await page.getByLabel('Missing letters g1').fill('row');
  await page.getByLabel('Missing letters g2').fill('turn');
  await save(page);
  const url = page.url(); await page.reload();
  await expect(page.getByLabel('Missing letters g1')).toHaveValue('row');
  await expect(page.getByLabel('Missing letters g2')).toHaveValue('turn');
  expect(page.url()).toBe(url);
  await page.getByRole('button', { name: '2. Daily Life' }).click();
  await page.getByRole('radio', { name: 'The location of the repair desk.' }).check(); await save(page);
  await page.getByRole('button', { name: '3. Academic Passage' }).click();
  await page.getByRole('radio', { name: 'Their canopy sizes and access to water may differ.' }).check(); await save(page);
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Submit practice', exact: true }).click();
  await expect(page.getByTestId('objective-score')).toHaveText('4 / 4');
  await expect(page.getByText('Borrow means take something temporarily.', { exact: false })).toBeVisible();
  await page.reload(); await expect(page.getByTestId('objective-score')).toHaveText('4 / 4');
  await page.getByRole('link', { name: 'Back to dashboard', exact: true }).click();
  await expect(page.getByTestId('completed-count')).toHaveText(String(before + 1));
  await expect(page.getByRole('link', { name: 'View results' }).first()).toBeVisible();
  expect(errors).toEqual([]);
});
test('lost save acknowledgement survives refresh and can be retried without duplication', async ({ page }) => {
  await login(page); await page.getByRole('button', { name: 'Start Reading practice' }).click();
  await expect(page.getByRole('heading', { name: 'A campus library' })).toBeVisible();
  let interrupted = false;
  await page.route('**/api/v1/sessions/*/items/*/response', async route => {
    if (!interrupted) { interrupted = true; await route.fetch(); await route.abort('failed'); }
    else await route.continue();
  });
  await page.getByLabel('Missing letters g1').fill('row'); await page.getByLabel('Missing letters g2').fill('turn');
  await page.getByRole('button', { name: 'Save answer', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Retry saving' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('button', { name: 'Retry saving' })).toBeVisible();
  await page.getByRole('button', { name: 'Retry saving' }).click();
  await expect(page.getByRole('status')).toHaveText('Saved on the server');
  await expect(page.getByRole('button', { name: 'Retry saving' })).toHaveCount(0);
  await page.reload(); await expect(page.getByLabel('Missing letters g2')).toHaveValue('turn');
});
