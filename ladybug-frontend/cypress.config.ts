import { defineConfig } from 'cypress';

export default defineConfig({
  trashAssetsBeforeRuns: false,
  expose: {
    backendServer: 'http://localhost:80',
    FILESEP: '\\',
  },
  e2e: {
    setupNodeEvents(on, config) {
      // Allow running as root (e.g. in CI/Docker): Chromium refuses to start
      // as root without --no-sandbox.
      on('before:browser:launch', (browser, launchOptions) => {
        if (browser.family === 'chromium') {
          launchOptions.args.push('--no-sandbox', '--disable-gpu');
        }
        return launchOptions;
      });
      // Cypress 16 only reads values from cypress.env.json and CYPRESS_* environment
      // variables into config.env (accessible through cy.env()). None of our values
      // are secret (the passwords belong to test users), so expose them to the
      // tests through Cypress.expose().
      config.expose = { ...config.expose, ...config.env };
      return config;
    },
    baseUrl: 'http://localhost:4200',
    excludeSpecPattern: ['**/cypress/e2e/1-getting-started/**', '**/cypress/e2e/2-advanced-examples/**'],
    viewportWidth: 1920,
    viewportHeight: 1080,
    experimentalRunAllSpecs: true,
    video: true,
    specPattern: 'cypress/e2e/**/*.ts',
  },
  defaultCommandTimeout: 10_000,
});
