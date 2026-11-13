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
    cy.closeDebugTree();
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
    // The second chevron belongs to the startpoint, the first child of the report node.
    cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view="chevron"]').eq(1).click()
    cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view="text"]:contains("Hide this checkpoint")').should(
      'be.hidden',
    );
  });

  it('Correct nesting in debug tree for report with multiple startpoints', () => {
    cy.createReportWithMultipleStartpoints();
    cy.initializeApp();
    cy.getDebugTableRows().find('td:contains("Multiple startpoints")').first().click();
    cy.checkFileTreeLength(1);
    // Expected nesting:
    //
    // Multiple startpoints (report)
    // +- Multiple startpoints (startpoint)
    //    +- startpoint 2
    //    |  +- Hello infopoint
    //    |  +- endpoint 2
    //    +- Multiple startpoints (endpoint)
    cy.getRootNodeOfDebugTree().getChildNodesInDebugTree().should('have.length', 1).as('outerStartpoint');
    cy.get('@outerStartpoint')
      .getChildNodesInDebugTree()
      .should(($children) => {
        expect($children).to.have.length(2);
        expect($children.eq(0)).to.contain.text('startpoint 2');
        expect($children.eq(1)).to.contain.text('Multiple startpoints');
      });
    cy.get('@outerStartpoint')
      .getChildNodesInDebugTree()
      .eq(0)
      .getChildNodesInDebugTree()
      .should(($children) => {
        expect($children).to.have.length(2);
        expect($children.eq(0)).to.contain.text('Hello infopoint');
        expect($children.eq(1)).to.contain.text('endpoint 2');
      });
    // Collapsing the outer startpoint hides everything that is nested in it.
    cy.get('@outerStartpoint').find('[data-cy-tree-view="chevron"]').click();
    cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view="text"]:contains("startpoint 2")').should('be.hidden');
    cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view="text"]:contains("Hello infopoint")').should('be.hidden');
  });
});
