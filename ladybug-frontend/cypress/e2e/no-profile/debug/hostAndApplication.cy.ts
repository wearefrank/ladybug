import { STORAGE_ID_COLUMN } from '../../../support/e2e';

describe('Tests about host and application', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.clearDebugStore();
  });

  after(() => {
    cy.clearHostAndApplication();
  });

  it('When host and application not set on TestTool then host and application do not appear in debug table', () => {
    cy.createReport();
    cy.visit('');
    cy.get('[data-cy-debug="refresh"]').click();
    cy.checkDebugTableRowsAre(['Simple report']);
    cy.checkNoHostAndNoApplication(0);
  })

  describe('When host and application are set on TestTool', () => {
    beforeEach(() => {
      cy.setHostA();
      cy.setApplicationX();
      cy.createReport();
      cy.setHostB();
      cy.setApplicationY();
      cy.createOtherReport();
    })

    it('Then debug table rows have host and application', () => {
      cy.visit('');
      cy.get('[data-cy-debug="refresh"]').click();
      cy.checkDebugTableRowsAre(['Simple report', 'Another simple report']);
      cy.checkHostOfDebugTableRow(0, 'Host A');
      cy.checkApplicationOfDebugTableRow(0, 'Application X');
      cy.checkHostOfDebugTableRow(1, 'Host B');
      cy.checkApplicationOfDebugTableRow(1, 'Application Y');
    })

    it('Then opened report holds host and application', () => {
      cy.visit('');
      cy.get('[data-cy-debug="refresh"]').click();
      cy.clickRowInTable(0);
      cy.clickRootNodeInFileTree();
      cy.get('[data-cy-open-metadata-table]').check();
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Host A');
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Application X');
      cy.clickRowInTable(1);
      cy.clickRootNodeInFileTree();
      cy.get('[data-cy-open-metadata-table]').check();
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Host B');
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Application Y');
    })

    it('Then host and application are still set when report is copied', () => {
      cy.visit('');
      cy.get('[data-cy-debug="refresh"]').click();
      cy.clickRowInTable(0);
      cy.debugTreeGuardedCopyReport('Simple report', 3, '');
      cy.navigateToTestTabAndAwaitLoadingSpinner();
      cy.get('[data-cy-test="showHideStorageIds"]').should('contain.text', 'Show').click();
      cy.get('[data-cy-test="showHideStorageIds"]').should('contain.text', 'Hide');
      cy.get('[data-cy-test="tableRow"]')
        .should('have.length', 1)
        .find('td').eq(STORAGE_ID_COLUMN)
        .should('have.text', '0');
      cy.get('[data-cy-test="tableRow"]').find('td').eq(0).find('input').check();
      cy.get('[data-cy-test="copy"]').click();
      cy.get('[data-cy-test="tableRow"]').should('have.length', 2)
        .eq(1)
        .find('td').eq(STORAGE_ID_COLUMN)
        .should('have.text', 1);
      cy.get('[data-cy-test="tableRow"]').eq(1)
        .find('[data-cy-test="openReport"]')
        .click();
      cy.clickRootNodeInFileTree();
      cy.get('[data-cy-open-metadata-table]').check();
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Host A');
      cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Application X');      
    })
  })
})