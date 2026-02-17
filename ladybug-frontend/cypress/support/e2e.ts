// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

Cypress.on('uncaught:exception', (err, runnable) => {
  // Ignore Identifier '_amdLoaderGlobal' has already been declared
  if (err.name === 'SyntaxError' && err.message.indexOf('_amdLoaderGlobal') >= 0) {
    return false;
  }
  if (err.message.indexOf('WorkerGlobalScope') >= 0) {
    return false;
  }
  return;
})

import 'cypress-file-upload';

// Import commands.js using ES2015 syntax:
import './commands';
