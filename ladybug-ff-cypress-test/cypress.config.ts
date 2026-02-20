import { defineConfig } from 'cypress'

// Ignore this red flag by VS Code. This require statement works.
const fs = require('fs');

module.exports = defineConfig({
  e2e: {
    setupNodeEvents (on, config) {
      // implement node event listeners here
      on('task', {
        // deconstruct the individual properties
        fileExists (fname: string) {
          if (fs.existsSync(fname)) {
            return true
          } else {
            return false
          }
        }
      })
    },
    supportFile: 'cypress/e2e/cypress/support/e2e.ts',
    baseUrl: 'http://localhost',
    video: true
  }
})
