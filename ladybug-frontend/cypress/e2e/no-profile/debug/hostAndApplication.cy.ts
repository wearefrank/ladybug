describe('Tests about host and application', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.setHostA();
    cy.setApplicationX();
    cy.createReport();
  })

  afterEach(() => {
    cy.clearDebugStore();
  });

  it('When default Ladybug names are applied then host and application do not appear in debug table', () => {
    cy.visit('');
    cy.get('[data-cy-debug="refresh"]').click();
    cy.checkDebugTableRowsAre(['Simple report']);
    cy.checkNoHostAndNoApplication(0);
  })

  it('Report metadata holds host and application', () => {
    cy.visit('');
    cy.get('[data-cy-debug="refresh"]').click();
    cy.clickRowInTable(0);
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-open-metadata-table]').check();
    cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Host A');
    cy.get('[data-cy-metadata-table="table"]').should('contain.text', 'Application X');
  })
})