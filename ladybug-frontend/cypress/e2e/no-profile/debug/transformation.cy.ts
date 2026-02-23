describe('Tests for report transformation', () => {
  before(() => {
    cy.resetApp();
    cy.createOtherReport();
    cy.debugTabBackToFactorySettings();
  });

  beforeEach(() => {
    cy.visit('');
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.get('[data-cy-settings="nav-server"]').as('serverTab').click();
    cy.get('[data-cy-settings-transformation]').type('{selectAll}{del}');
    cy.get('[data-cy-settings-transformation]').within((textArea) => {
      cy.fixture('ignoreName.xslt').then((newText) => cy.wrap(textArea).type(newText));
    })
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-debug="refresh"]').click();
  })

  afterEach(() => {
    cy.debugTabBackToFactorySettings();
  });

  after(() => {
    // When the debug storage is cleared after each test then the page updates while table rows are searched - false negative.
    cy.clearDebugStore();
  })

  it('Should see updated metadata when updating transformation field', () => {
    cy.visit('');
    cy.navigateToDebugTabAndAwaitLoadingSpinner();
    openTheReport();
    cy.get('[data-cy-element-name="reportXmlEditor"]').contains('Name="IGNORED"');
    // Only the XML in the Monaco editor should be affected, not the other fields
    cy.get('[data-cy-element-name="name"]').should('have.value', 'Another simple report');
  });

  it('When user chooses not to apply report transformation then transformation not applied to opened report', () => {
    cy.visit('');
    cy.navigateToDebugTabAndAwaitLoadingSpinner();
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.get('[data-cy-settings="nav-client"]').click();
    cy.get('[data-cy-settings-transformation-enabled]').uncheck();
    cy.get('[data-cy-settings="saveChanges"]').click();
    openTheReport();
    cy.get('[data-cy-element-name="reportXmlEditor"]').contains('Name="IGNORED"').should('not.exist');
    cy.get('[data-cy-element-name="name"]').should('have.value', 'Another simple report');
  })
});

function openTheReport() {
  cy.assertDebugTableLength(1).click();
  cy.checkFileTreeLength(1);
  cy.clickRootNodeInFileTree();
}