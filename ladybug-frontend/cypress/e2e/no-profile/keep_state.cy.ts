const UPLOADED_REPORT_NAMES = ['Pipeline Example1a/Adapter1a', 'Pipeline Example1b/Adapter1b']

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

  it('should keep state in separate report tab', () => {
    cy.uploadTwoReportsAndCheckTabs();
    cy.get('[data-cy-debug-tree="expandAll"]').click();
    let reportName = UPLOADED_REPORT_NAMES[1]
    cy.getShownNodesOfReportTreeWithText(reportName).eq(2).click();
    cy.getShownNodesOfReportTreeWithText(reportName).should('have.length', 3);
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(2).click();
    reportName = UPLOADED_REPORT_NAMES[0];
    cy.getShownNodesOfReportTreeWithText(reportName).eq(2).click();
    cy.getShownNodesOfReportTreeWithText(reportName).should('have.length', 3);
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(3).click();
    reportName = UPLOADED_REPORT_NAMES[1];
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(2).click();
    reportName = UPLOADED_REPORT_NAMES[0];
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
  })
});

describe('Tests for keeping state in tabs when switching tabs - with URL filters', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.setHostA();
    cy.setApplicationX();
    cy.createReport();
    cy.setHostB();
    cy.setApplicationY();
    cy.createOtherReport();
    cy.visit('debug?filter-application=Application%20x');
    // Check the URL filter has effect - Another simple report should not be included.
    cy.checkDebugTableRowsAre(['Simple report']);
  });

  afterEach(() => {
    cy.clearDebugStore();
  });

  after(() => {
    cy.clearHostAndApplication();
  });

  it('should reopen the last opened report in debug tab when switching tabs', () => {
    const metadataCellIdentifier = '[data-cy-metadata="storageId"]'
    cy.clickRowInTable(0);
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-open-metadata-table]').click()
    cy.get(metadataCellIdentifier).then((element) => {
      const openedReportUid = element.text()
      cy.navigateToTestTab();
      // cy.navigateToDebugTab() because referenced data-cy attribute is different with filters
      cy.get('[data-cy-nav-tab]').eq(0).click();
      cy.get(metadataCellIdentifier).should('contain.text', openedReportUid);
    })
  });

  it('should keep the same reports selected in debug table', () => {
    cy.selectRowInDebugTable(0)
    cy.navigateToTestTab();
    // cy.navigateToDebugTab() does not work because referenced data-cy attribute is different with filters
    cy.get('[data-cy-nav-tab]').eq(0).click();
    cy.get('[data-cy-debug="selectOne"]').eq(0).should('be.checked')
  });

  it('should keep the same reports selected in test table', () => {
    cy.copyReportsToTestTab(['Simple report'])
    cy.navigateToTestTabAndAwaitLoadingSpinner()
    cy.get('[data-cy-test="toggleSelectAll"]').uncheck();
    cy.get('[data-cy-test="selectOne"]').eq(0).check()
    // cy.navigateToDebugTab() does not work because referenced data-cy attribute is different with filters
    cy.get('[data-cy-nav-tab]').eq(0).click();
    cy.navigateToTestTab();
    cy.get('[data-cy-test="selectOne"]').eq(0).should('be.checked')
  });

  it('should keep state in separate report tab', () => {
    cy.get('[data-cy-debug="selectOne"]').check();
    cy.uploadTwoReportsAndCheckTabs();
    cy.get('[data-cy-debug-tree="expandAll"]').click();
    let reportName = UPLOADED_REPORT_NAMES[1]
    cy.getShownNodesOfReportTreeWithText(reportName).eq(2).click();
    cy.getShownNodesOfReportTreeWithText(reportName).should('have.length', 3);
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(2).click();
    reportName = UPLOADED_REPORT_NAMES[0];
    cy.getShownNodesOfReportTreeWithText(reportName).eq(2).click();
    cy.getShownNodesOfReportTreeWithText(reportName).should('have.length', 3);
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(3).click();
    reportName = UPLOADED_REPORT_NAMES[1];
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    cy.get('[data-cy-nav-tab]').eq(2).click();
    reportName = UPLOADED_REPORT_NAMES[0];
    cy.checkShownNodeWithTextSelected(reportName, 2, true);
    cy.checkShownNodeWithTextSelected(reportName, 1, false);
    // cy.navigateToDebugTab() does not work because referred data-cy attribute is different with filters.
    cy.get('[data-cy-nav-tab]').eq(0).click();
    // Same as command cy.selectRowInDebugTable(). Using that command is less clear because more indirection.
    // We can some day replace that custom command everywhere.
    cy.get('[data-cy-debug="selectOne"]').should('be.checked');
  })
});
