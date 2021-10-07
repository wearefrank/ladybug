// init.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test

describe('Ladybug simple protractor test', function() {
  it('Confirm title of ladybug app', function() {
    cy.visit('http://localhost:4200');
    cy.title().should('eq', 'Ladybug');
  });
});
