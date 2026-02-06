import { defineConfig } from 'cypress'

module.exports = defineConfig({
  e2e: {
    setupNodeEvents (on, config) {
      // implement node event listeners here
    },
    supportFile: 'cypress/e2e/cypress/support/e2e.ts',
    baseUrl: 'http://localhost',
    video: true
  }
})
