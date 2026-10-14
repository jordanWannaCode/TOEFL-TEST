import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './tests',
  use: { baseURL: process.env['FRONTEND_URL'] ?? 'http://127.0.0.1:4200', headless: true, launchOptions: { executablePath: process.env['PLAYWRIGHT_EXECUTABLE_PATH'] } },
  reporter: 'list',
});
