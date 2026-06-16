// This test extends the debug table with columns host and application.
// This can only be reversed by restarting the server which is not feasible
// in cypress tests. This is not an issue as long as all other Cypress tests
// in folder "no-profile" do not check anything about columns host and
// application.
describe('Tests about host and application', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.clearDebugStore();
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
  })
})