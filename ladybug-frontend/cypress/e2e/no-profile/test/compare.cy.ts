describe('Tests for compare button in test tab', () => {
  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.createJsonReport();
    cy.initializeApp();
    cy.copyReportsToTestTab(['Simple report', 'Another simple report', 'Json checkpoint']);
    cy.navigateToTestTabAndAwaitLoadingSpinner();
  })

  afterEach(() => {
    cy.resetApp();
  })

  it('should show compare button if exactly two reports are selected', () => {
    selectFirstTwoReportsInTestTab()
    cy.get('[data-cy-test="compare"]').should('be.visible');
  });

  it('should not show compare button if one or more than 2 reports are selected', () => {
    cy.get('[data-cy-test="toggleSelectAll"]').uncheck()
    cy.get('[data-cy-test="compare"]').should('not.exist');
    cy.get('[data-cy-test="toggleSelectAll"]').check()
    cy.get('[data-cy-test="compare"]').should('not.exist');
  });

  it('should open a compare tab when clicking the compare button in the test tab', () => {
    selectFirstTwoReportsInTestTab(true)
    cy.get('[data-cy-nav-tab="Compare"]').should('exist');
    cy.get('app-compare').should('exist');
  });

  it('should open the correct reports in compare tab', () => {
    cy.get('[data-cy-test="settings"]').click();
    cy.get('[data-cy-test-settings="showStorageIds"]').check();
    cy.get('[data-cy-test-settings="save"]').click();
    cy.get('[data-cy-test-table="storageId"]').eq(0).invoke('text').then((firstStorageId) => {
      cy.get('[data-cy-test-table="storageId"]').eq(1).invoke('text').then((secondStorageId) => {
        selectFirstTwoReportsInTestTab(true)
        cy.get('[data-cy-nav-tab="Compare"]').should('exist');
        cy.get('app-compare').should('exist');
        cy.url().should('include', firstStorageId).and('include', secondStorageId);
      });
    });
  });
});

function selectFirstTwoReportsInTestTab(navigateToCompareTab: boolean= false) {
  cy.get('[data-cy-test="toggleSelectAll"]').uncheck()
  cy.get('[data-cy-test="selectOne"]').eq(0).check()
  cy.get('[data-cy-test="selectOne"]').eq(1).check()
  if (navigateToCompareTab) {
    cy.get('[data-cy-test="compare"]').click();
  }
}
