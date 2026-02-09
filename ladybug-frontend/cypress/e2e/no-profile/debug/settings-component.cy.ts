describe('Tests for settings component', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

  it('Should alter spacing when spacing setting is altered', () => {
    cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
    cy.get('[data-cy-settings="nav-client"]').as('clientTab').click();
    cy.get('[data-cy-settings="spacingDropdown"]').select('1x');
    cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
      'have.text',
      '1x',
    );
    cy.get('[data-cy-settings="saveChanges"]').as('saveButton').click();
    cy.get('[data-cy-record-table-index="0"]')
      .find('td')
      .first()
      .as('tableCell');
    cy.get('@tableCell').should('have.attr', 'style', 'padding: 0.25em 0px;');
    cy.get('@openSettingsModal').click();
    cy.get('@clientTab').click();
    cy.get('[data-cy-settings="spacingDropdown"]').select('0x');
    cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
      'have.text',
      '0x',
    );
    cy.get('@saveButton').click();
    cy.get('@tableCell').should('have.attr', 'style', 'padding: 0em 0px;');
  });

  it('Should allow multiple files to be opened in debug tree when setting is enabled and close all but one report when setting is disabled', () => {
    cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
    cy.get('[data-cy-settings="nav-client"]').as('clientTab').click();
    cy.get('[data-cy-settings="showAmount"]').should('not.be.checked');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-record-table-index="0"]').click();
    cy.get('[data-cy-record-table-index="1"]').click();
    cy.checkFileTreeLength(1);
    cy.get('@openSettingsModal').click();
    cy.get('@clientTab').click();
    cy.get('[data-cy-settings="showAmount"]').click();
    cy.get('[data-cy-settings="showAmount"]').should('be.checked');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-record-table-index="0"]').click();
    cy.checkFileTreeLength(2);
    cy.get('@openSettingsModal').click();
    cy.get('@clientTab').click();
    cy.get('[data-cy-settings="showAmount"]').click();
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.checkFileTreeLength(1);
  });

  describe('Restore, factory reset, save', () => {
    beforeEach(() => {
      cy.debugTabBackToFactorySettings();
      cy.get('@openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').click();
      cy.get('[data-cy-settings="openLatestReports"]').invoke('val').should('equal', '10');
      cy.get('[data-cy-settings="showAmount"]').should('not.be.checked');
      cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
        'have.text',
        '1x',
      );
      cy.get('[data-cy-settings="nav-server"]').click();
      cy.get('[data-cy-settings="generatorEnabled"] option:selected').should(
        'have.text',
        'Enabled'
      )
      cy.get('[data-cy-settings-transformation]').invoke('val').should('have.length.gt', 10);
      cy.get('[data-cy-settings="regexFilter"]').invoke('val').should('equal', '.*');
      cy.get('[data-cy-settings="close"]').click();
      cy.get('[data-cy-settings="close"]').should('not.exist');
    })

    it('Dummy test', () => {})
  })
});
