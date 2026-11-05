import { execFile } from 'node:child_process';
import path from 'node:path';
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
      on('task', {
        // Runs ladybug-otel-test-companion. Rejects (failing the test) when the jar exits with a
        // non-zero exit code, which it does when Ladybug does not accept the trace.
        runOtelTestCompanion({ endpoint, protocol }: { endpoint: string; protocol: string }): Promise<string> {
          const jar = path.join(
            config.projectRoot,
            '..',
            'ladybug-otel-test-companion',
            'target',
            'ladybug-otel-test-companion.jar',
          );
          const javaArguments = [
            '-jar',
            jar,
            `--ladybug.otel.endpoint=${endpoint}`,
            `--ladybug.otel.protocol=${protocol}`,
            '--ladybug.otel.batching=single-request',
          ];
          return new Promise((resolve, reject) => {
            execFile('java', javaArguments, (error, stdout, stderr) => {
              if (error) {
                reject(new Error([error.message, 'stdout:', stdout, 'stderr:', stderr].join('\n')));
              } else {
                resolve(stdout);
              }
            });
          });
        },
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
