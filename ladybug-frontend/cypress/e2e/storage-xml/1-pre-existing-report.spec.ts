// This test should be run as the first. Other tests
// have to delete all reports from the test tab and
// remove pre-existing reports.
//

import { STORAGE_ID_COLUMN } from 'cypress/support/e2e';

const STORAGE_ID_HEADING = 'Storage Id';

describe('Pre existing report', () => {
  beforeEach(() => {
    cy.initializeApp();
    cy.navigateToTestTabAndAwaitLoadingSpinner();
    showStorageIdColumn();
  })

  it('Reports present in src/test/testtool should be shown', () => {
    cy.getTestTableRows().contains('reportWithoutStorageId').should('have.length', 1)
    cy.getTestTableRows().contains('reportWithStorageId').should('have.length', 1)
  })

  it('When report file has no storage id then negative storage id assigned', () => {
    cy.getTestTableRows().contains('reportWithoutStorageId').should('have.length', 1)
      .parent()
      .find('td')
      .eq(STORAGE_ID_COLUMN)
      .should('contain.text', '-1');
  })

  it('When report file has storage id then taken from file', () => {
    cy.getTestTableRows().contains('reportWithStorageId').should('have.length', 1)
      .parent()
      .find('td')
      .eq(STORAGE_ID_COLUMN)
      .should('contain.text', '3');
  })

  it('Can open the pre-existing report without storage id', () => {
    cy.getTestTableRows()
      .contains('reportWithoutStorageId')
      .parent()
      .find('[data-cy-test="openReport"]').click();
    cy.wait(500);
    cy.get("[data-cy-toast]").should('not.exist');
    // Assume we open the first checkpoint by default
    cy.get('[data-cy-element-name="checkpointEditor"]')
      .invoke('text')
      .should('contain', 'Hello')
      .should('contain', 'World');
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="name"]').invoke('val').should('contain', 'reportWithoutStorageId');
  })

  it('Can open the pre-existing report with storage id and it has host and application', () => {
    cy.getTestTableRows()
      .contains('reportWithStorageId')
      .parent()
      .find('[data-cy-test="openReport"]').click();
    cy.wait(500);
    cy.get("[data-cy-toast]").should('not.exist');
    // Assume we open the first checkpoint by default
    cy.get('[data-cy-element-name="checkpointEditor"]')
      .invoke('text')
      .should('contain', 'Other')
      .should('contain', 'message')
      .should('contain', 'hello');
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="name"]').invoke('val').should('contain', 'reportWithStorageId');
    cy.get('[data-cy-open-metadata-table]').check();
    cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'MyPredefinedHost');
    cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'MyPredefinedApplication');       
  })
})

function showStorageIdColumn() {
  cy.get('[data-cy-test="showHideStorageIds"]').should('contain.text', 'Show');
  cy.get('[data-cy-test="tableHeaderRow"]').contains(STORAGE_ID_HEADING).should('not.exist');
  cy.get('[data-cy-test="showHideStorageIds"]').click();
  cy.get('[data-cy-test="showHideStorageIds"]').should('contain.text', 'Hide');
  cy.get('[data-cy-test="tableHeaderRow"]').contains(STORAGE_ID_HEADING).should('be.visible');
}