describe('Tests for Debug tab table', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

  it('Should sort by column naming when clicking on table column header', () => {
    cy.getDebugTableRows().first().contains("Simple report");
    cy.get('[data-cy-debug="metadataLabel"]').eq(3).click();
    cy.getDebugTableRows().first().contains("Another simple report");
    cy.get('[data-cy-debug="metadataLabel"]').eq(3).click();
    cy.getDebugTableRows().first().contains("Simple report");
  });
});
