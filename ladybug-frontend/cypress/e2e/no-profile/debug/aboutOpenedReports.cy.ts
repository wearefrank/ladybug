describe('About opened reports', () => {
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

  it('When multiple reports are selected and open selected pressed then only warning', () => {
    cy.getDebugTableRows().should('have.length', 2);
    // Opening a row fetches the report asynchronously. checkFileTreeLength(1) would not
    // wait for the second fetch, since the tree already has length 1 from the first report;
    // if that fetch is still in flight when "close" is clicked below, its late response
    // re-populates the tree afterwards and makes checkFileTreeLength(0) flaky. Asserting on
    // the report name instead forces Cypress to retry until the correct report has loaded.
    //
    // When we search the opened report we expect three nodes because the tree
    // is expanded by default.
    cy.getDebugTableRows().find('td:contains("Simple report")').first().click();
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 3);
    cy.getDebugTableRows().find('td:contains("Another simple report")').first().click();
    cy.getShownNodesOfReportTreeWithText('Another simple report').should('have.length', 3);
    cy.get('[data-cy-debug-tree="close"]').click();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.contains('You can open only one report at a time!');
    cy.checkFileTreeLength(0);
  })

  it('When we collapse a parent checkpoint then the child becomes invisible', () => {
    cy.createReportWithInfopoint();
    cy.initializeApp();
    cy.getDebugTableRows().find('td:contains("Hide a checkpoint in blackbox view")').first().click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-debug-tree="root"] app-tree-item app-tree-icon .sft-chevron-container').eq(1).click()
    cy.get('[data-cy-debug-tree="root"] app-tree-item > div > div:contains("Hide this checkpoint")').should(
      'be.hidden',
    );
  });

  it('Correct nesting in debug tree for report with multiple startpoints', () => {
    cy.createReportWithMultipleStartpoints();
    cy.initializeApp();
    cy.getDebugTableRows().find('td:contains("Multiple startpoints")').first().click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-debug-tree="root"] app-tree-item app-tree-icon .sft-chevron-container').eq(1).click()
    cy.get('[data-cy-debug-tree="root"] app-tree-item > div > div:contains("Hello infopoint")').should('be.hidden');
    cy.get(
      '[data-cy-debug-tree="root"] app-tree-item > div > div:contains("startpoint 2") > div:contains("Multiple startpoints") > app-tree-item > div > div:contains("Multiple startpoints")',
    )
      .first()
      .selectIfNotSelected()
      .click();
    cy.get('[data-cy-debug-tree="root"] app-tree-item > div > div:contains("startpoint 2")').should('be.hidden');
  });
});
