import { defineConfig } from 'cypress'

// Ignore this red flag by VS Code. This require statement works.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Cypress derives screenshot filenames from the describe/it titles. Long
// titles produce paths that exceed Windows' MAX_PATH once the CI artifact
// zip is extracted there, which makes the zip look "corrupt" to Windows
// tools even though it is valid. Truncate the basename when needed, but
// keep the trailing "(failed)" marker and append a short hash so distinct
// long titles cannot collide. The original title is logged alongside the
// screenshots so it stays traceable from the artifact.
const MAX_SCREENSHOT_BASENAME_LENGTH = 40
const SCREENSHOT_NAME_MAP_FILENAME = 'truncated-screenshot-names.log'

module.exports = defineConfig({
  e2e: {
    setupNodeEvents (on, config) {
      // Allow running as root (e.g. in CI/Docker): Chromium refuses to start
      // as root without --no-sandbox.
      on('before:browser:launch', (browser, launchOptions) => {
        if (browser.family === 'chromium') {
          launchOptions.args.push('--no-sandbox')
          launchOptions.args.push('--disable-gpu')
        }
        return launchOptions
      })
      on('after:screenshot', (details) => {
        const dir = path.dirname(details.path)
        const ext = path.extname(details.path)
        const base = path.basename(details.path, ext)
        if (base.length <= MAX_SCREENSHOT_BASENAME_LENGTH) {
          return
        }
        const failedSuffix = ' (failed)'
        const hasFailedSuffix = base.endsWith(failedSuffix)
        const title = hasFailedSuffix ? base.slice(0, -failedSuffix.length) : base
        const suffix = hasFailedSuffix ? failedSuffix : ''
        const hash = crypto.createHash('md5').update(base).digest('hex').slice(0, 8)
        const keepLength = MAX_SCREENSHOT_BASENAME_LENGTH - suffix.length - hash.length - 1
        const truncatedTitle = title.slice(0, keepLength)
        const newBase = `${truncatedTitle}-${hash}${suffix}`
        const newPath = path.join(dir, `${newBase}${ext}`)
        fs.renameSync(details.path, newPath)
        fs.appendFileSync(
          path.join(dir, SCREENSHOT_NAME_MAP_FILENAME),
          `${newBase}${ext} <- ${base}${ext}\n`
        )
        return { path: newPath }
      })
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
