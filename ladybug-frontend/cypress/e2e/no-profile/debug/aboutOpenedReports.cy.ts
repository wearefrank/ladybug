describe('About opened reports', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

  it('When we open multiple reports simultaneously from the table then they appear all in the tree', () => {
    cy.enableShowMultipleInDebugTree();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    // Each of the two reports has three lines.
    cy.checkFileTreeLength(2);
    cy.get('[data-cy-debug-tree="root"] app-tree-item > div').should('contain.text', 'Simple report');
    cy.get('[data-cy-debug-tree="root"] app-tree-item > div > div:contains(Simple report)')
      .first()
      .selectIfNotSelected();
    cy.get('[data-cy-debug-tree="root"] > app-tree-item .item-name')
      .should('contain.text', 'Another simple report')
      .eq(0)
      .click();
    cy.get('[data-cy-debug-tree="closeAll"]').click();
    cy.get('[data-cy-debug-tree="root"] app-tree-item').should('not.exist');
  });

  it('When we open reports sequentially with multiple allowed in the debug tree then they appear next to each other', () => {
    cy.enableShowMultipleInDebugTree();
    cy.getDebugTableRows().find('td:contains(Simple report)').first().click();
    cy.checkFileTreeLength(1);
    cy.getDebugTableRows().find('td:contains("Another simple report")').first().click();
    cy.checkFileTreeLength(2);
    // Check sequence of opened reports. We expect "Simple report" first, then "Another simple report".
    cy.get('[data-cy-debug-tree="root"] > app-tree-item:nth-child(1) > div > .sft-item > .item-name').should(
      'contain.text',
      'Simple report',
    );
    cy.get('[data-cy-debug-tree="root"] > app-tree-item:nth-child(2) > div > .sft-item > .item-name')
      .eq(0)
      .should('contain.text', 'Another simple report');
    cy.get('[data-cy-debug-tree="closeAll"]').click();
    cy.get('[data-cy-debug-tree="root"] app-tree-item').should('not.exist');
  });

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
