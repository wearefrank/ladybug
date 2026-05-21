// We do not test the external-ladybug-access.html page.
// That page uses window.open() to open Ladybug in a different tab.
// It is difficult to handle that in Cypress tests.
//
// Instead, we test here that Ladybug satisfies the contract
// expected by that page.
describe('External access', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('When we refresh and create two reports, they have storage ids 0 and 1', () => {
    cy.visit('');
    cy.get('[data-cy-debug="tableRow"] td:nth-child(2)').should('have.length', 2).then((items) => {
      cy.wrap(items[0]).invoke('text').should('contain', '0');
      cy.wrap(items[1]).invoke('text').should('contain', '1');
    })
  })

  it('When we enter ladybug through a report URL, we have a tab selected with that report', () => {
    cy.visit('/report/Debug/0');
    cy.checkNavTab(0, 'Debug', false);
    cy.checkNavTab(1, 'Test', false);
    cy.checkNavTab(2, 'Simple report', true);
    cy.get('[data-cy-element-name]').invoke('text').should('contain', 'Hello').should('contain', 'World');
  })

  it('Can open a report by sending a Windows event', () => {
    cy.visit('');
    cy.checkNavTab(0, 'Debug', true);
    cy.checkNavTab(1, 'Test', false).then(() => {
      window.postMessage({ action: 'ladybug-openReport', storageName: 'Debug', storageId: '0' }, '*');
      cy.log('Posted message to open report');
    });
    cy.wait(1000);
    cy.checkNavTab(0, 'Debug', false);
    cy.checkNavTab(1, 'Test', false);
    cy.checkNavTab(2, 'Simple report', true);
    cy.get('[data-cy-element-name]').invoke('text').should('contain', 'Hello').should('contain', 'World');
  })
})
