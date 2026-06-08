describe('Tests about host and application', () => {
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
  })

  afterEach(() => {
    cy.clearDebugStore();
  });

  it('When host and application are changed then reports have new host and new application', () => {
    cy.visit('');
    cy.get('[data-cy-debug="refresh"]').click();
    cy.checkDebugTableRowsAre(['Simple report', 'Another simple report']);
    cy.checkHostOfDebugTableRow(0, 'Host A');
    cy.checkApplicationOfDebugTableRow(0, 'Application X');
    cy.checkHostOfDebugTableRow(1, 'Host B');
    cy.checkApplicationOfDebugTableRow(1, 'Application Y');
  })

  it('Report metadata holds host and application', () => {
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
})