describe('Metadata', () => {
  before(() => cy.resetApp());

  afterEach(() => {
    cy.clearDebugStore();
  });

  it('When root node is selected then metadata can be shown', () => {
    cy.visit('');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.clickRootNodeInFileTree();
    cy.wait(200);
    cy.get('[data-cy-metadata-table="table"]').should('not.exist');
    cy.get(':contains(Show metadata)').should('be.visible');
    cy.get('[data-cy-open-metadata-table]').click();
    cy.get(':contains(Hide metadata)').should('be.visible');
    cy.get('[data-cy-metadata-table="table"]').should('be.visible');
  });

  it('When checkpont node is selected then metadata can be shown', () => {
    cy.visit('');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-metadata-table="table"]').should('not.exist');
    cy.get(':contains(Show metadata)').should('be.visible');
    cy.get('[data-cy-open-metadata-table]').click();
    cy.get(':contains(Hide metadata)').should('be.visible');
    cy.get('[data-cy-metadata-table="table"]').should('be.visible');
  });
});
