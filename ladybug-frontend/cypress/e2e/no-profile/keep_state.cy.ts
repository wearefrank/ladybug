describe('Tests for keeping state in tabs when switching tabs', () => {

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.clearDebugStore()
    cy.clearTestReports()
  });

  it('should reopen the last opened report in debug tab when switching tabs', () => {
    const metadataCellIdentifier = '[data-cy-metadata="storageId"]'
    cy.clickRowInTable(0);
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-open-metadata-table]').click()
    cy.get(metadataCellIdentifier).then((element) => {
      const openedReportUid = element.text()
      cy.navigateToTestTab();
      cy.navigateToDebugTab();
      cy.get(metadataCellIdentifier).should('contain.text', openedReportUid);
    })
  });

  it('should keep the same reports selected in debug table', () => {
    cy.selectRowInDebugTable(0)
    cy.navigateToTestTab();
    cy.navigateToDebugTab()
    cy.get('[data-cy-debug="selectOne"]').eq(0).should('be.checked')
  });

  it('should keep the same reports selected in test table', () => {
    cy.copyReportsToTestTab(['Simple report', 'Another simple report'])
    cy.navigateToTestTabAndAwaitLoadingSpinner()
    cy.get('[data-cy-test="toggleSelectAll"]').uncheck();
    cy.get('[data-cy-test="selectOne"]').eq(1).check()
    cy.navigateToDebugTab()
    cy.navigateToTestTab();
    cy.get('[data-cy-test="selectOne"]').eq(0).should('not.be.checked')
    cy.get('[data-cy-test="selectOne"]').eq(1).should('be.checked')
  });

});
