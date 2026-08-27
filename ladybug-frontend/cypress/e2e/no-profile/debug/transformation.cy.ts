describe('Tests for report transformation', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
    cy.createOtherReport();
    cy.debugTabBackToFactorySettings();
  });

  beforeEach(() => {
    cy.visit('');
    cy.navigateToDebugTabAndAwaitLoadingSpinner();
    cy.wait(200);
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.wait(200);
    cy.get('[data-cy-settings="nav-server"]').as('serverTab').click();
    cy.wait(200);
    cy.get('[data-cy-settings-transformation]').type('{selectAll}{del}');
    cy.wait(200);
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
    cy.wait(200);
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
    // Saving settings triggers a debounced metadata reload (see FilterService),
    // which re-renders the table rows. Wait for it to finish before interacting
    // with the table, otherwise the row can detach mid-click (flaky failure).
    cy.intercept('GET', '**/api/metadata/**').as('metadataReload');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.wait('@metadataReload');
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