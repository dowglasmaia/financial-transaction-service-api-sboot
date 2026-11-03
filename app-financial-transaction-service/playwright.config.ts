import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests against the real stack: Angular app (ng serve, dev proxy to the API on 8088), the API in Docker and the MOCK
 * identity provider. Start the stack first (see e2e/README.md). The same tests run on three viewports.
 * The browser reaches the mock IdP as `mock-idp` (the name the API container uses for the issuer): it is mapped to 127.0.0.1.
 */
const launchOptions = { args: ['--host-resolver-rules=MAP mock-idp 127.0.0.1'] };

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: { baseURL: 'http://localhost:4200', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  webServer: { command: 'npm start', url: 'http://localhost:4200', reuseExistingServer: true, timeout: 120_000 },
  projects: [
    { name: 'desktop-1280', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 }, launchOptions } },
    { name: 'tablet-768', use: { ...devices['Desktop Chrome'], viewport: { width: 768, height: 1024 }, launchOptions } },
    { name: 'mobile-390', use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 }, launchOptions } },
  ],
});
