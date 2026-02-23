describe('Tests for report transformation', () => {
  before(() => cy.resetApp());


  afterEach(() => {
    cy.clearDebugStore();
    cy.get('[data-cy-debug="openSettings"]').click();
    // Factory reset in settings dialog. Resets
    // transformation to factory value. This
    // also closes the dialog.
    cy.get('[data-cy-settings="factoryReset"]').click();
  });

  it('Should see updated metadata when updating transformation field', () => {
    cy.visit('');
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.get('[data-cy-settings-transformation]').type('{selectAll}{del}');
    cy.get('[data-cy-settings-transformation]').within((textArea) => {
      cy.fixture('ignoreName.xslt').then((newText) => cy.wrap(textArea).type(newText));
    });
    cy.get('[data-cy-settings-transformation-enabled]').check();
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="reportXmlEditor"]').contains('Name="IGNORED"');
    // Only the XML in the Monaco editor should be affected, not the other fields
    cy.get('[data-cy-element-name="name"]').should('have.value', 'Another simple report');
  });
});
