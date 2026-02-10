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
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').click();
      cy.get('[data-cy-settings="openLatestReports"]').invoke('val').should('equal', '10');
      cy.get('[data-cy-settings="showAmount"]').should('not.be.checked');
      cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
        'have.text',
        '1x',
      );
      cy.get('[data-cy-settings-transformation-enabled]').should('be.checked');
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

    it('When change of number of reports is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="openLatestReports"]').as('numberOfReports').type('{selectAll}8');
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@numberOfReports').invoke('val').should('equal', '10');
      cy.get('@numberOfReports').type('{selectAll}8');
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('@numberOfReports').invoke('val').should('equal', '8');
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('@numberOfReports').invoke('val').should('equal', '10');
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When change of show multiple is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="showAmount"]').as('showMultiple').check();
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@showMultiple').should('not.be.checked');
      cy.get('@showMultiple').check();
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@showMultiple').should('be.checked');      
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('@showMultiple').should('not.be.checked');
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When change of table spacing is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="spacingDropdown"]').select('5x');
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
        'have.text',
        '1x',
      );
      cy.get('[data-cy-settings="spacingDropdown"]').select('5x');
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
        'have.text',
        '5x',
      );
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="spacingDropdown"] option:selected').should(
        'have.text',
        '1x',
      );
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When transformation enabled choice change is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings-transformation-enabled]').as('transformationEnabled').uncheck();
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@transformationEnabled').should('be.checked')
      cy.get('@transformationEnabled').uncheck();
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('@transformationEnabled').should('not.be.checked')
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@transformationEnabled').should('be.checked')
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When generator enabled choice change is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-server"]').as('server').click();
      cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('[data-cy-settings="generatorEnabled"] option:selected').should(
        'have.text',
        'Enabled'
      )
      cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('[data-cy-settings="generatorEnabled"] option:selected').should(
        'have.text',
        'Disabled'
      )
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('[data-cy-settings="generatorEnabled"] option:selected').should(
        'have.text',
        'Enabled'
      )
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When transformation change is discarded then not changed and when saved then changed - valid XSLT', () => {
      cy.fixture('empty.xslt').then((emptyXslt: string) => {
        emptyXslt = emptyXslt.replace('\r\n', '\n');
        const LENGTH_ALTERNATIVE_XSLT = emptyXslt.length;
        const LENGTH_THRESHOLD = LENGTH_ALTERNATIVE_XSLT + 10;
        cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
        cy.get('[data-cy-settings="nav-server"]').as('server').click();
        cy.get('[data-cy-settings-transformation]').as('transformation').invoke('val').should('have.length.gt', LENGTH_THRESHOLD);
        cy.get('@transformation').clear().type(emptyXslt);
        cy.get('[data-cy-settings="close"]').as('close').click();
        cy.get('[data-cy-debug-confirm="discard"]').click();
        cy.get('@openSettingsModal').click();
        cy.get('@server').click();
        cy.get('@transformation').invoke('val').should('have.length.gt', LENGTH_THRESHOLD);
        cy.get('@transformation').clear().type(emptyXslt);
        cy.get('@close').click();
        cy.get('[data-cy-debug-confirm="save"]').click();
        cy.get('@openSettingsModal').click();
        cy.get('@server').click();
        cy.get('@transformation').invoke('val').then((raw) => {
          expect(typeof raw).to.equal('string');
          expect(normalize(raw as string)).to.equal(normalize(emptyXslt));
        });
        cy.get('[data-cy-settings="factoryReset"]').click();
        cy.get('@openSettingsModal').click();
        cy.get('@server').click();
        cy.get('@transformation').invoke('val').should('have.length.gt', LENGTH_THRESHOLD);
        cy.get('[data-cy-settings="close"]').as('close').click();
      })
    })

    it('When regex filter change is discarded then not changed and when saved then changed', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-server"]').as('server').click();
      cy.get('[data-cy-settings="regexFilter"]').as('regexFilter').clear().type('xxx');
      cy.get('[data-cy-settings="close"]').as('close').click();
      cy.get('[data-cy-debug-confirm="discard"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('@regexFilter').invoke('val').should('equal', '.*');
      cy.get('@regexFilter').clear().type('xxx');
      cy.get('@close').click();
      cy.get('[data-cy-debug-confirm="save"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('@regexFilter').invoke('val').should('equal', 'xxx');
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@server').click();
      cy.get('@regexFilter').invoke('val').should('equal', '.*');
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    it('When escape is pressed then dialog closed and when enter is pressed then settings saved', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="openLatestReports"]').as('numberOfReports').type('{esc}');
      cy.get('@openSettingsModal');
      cy.get('@client').click();
      cy.get('@numberOfReports').type('{selectAll}8{enter}');
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@numberOfReports').invoke('val').should('equal', '8');
      cy.get('[data-cy-settings="factoryReset"]').click();
      cy.get('@openSettingsModal').click();
      cy.get('@client').click();
      cy.get('@numberOfReports').invoke('val').should('equal', '10');
      cy.get('[data-cy-settings="close"]').as('close').click();
    })
  })

  describe('Revert', () => {
    beforeEach(() => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="openLatestReports"]').as('numberOfReports').type('{selectAll}8');
      cy.get('[data-cy-settings="showAmount"]').as('showMultiple').check();
      cy.get('[data-cy-settings="spacingDropdown"]').select('5x');
      cy.get('[data-cy-settings-transformation-enabled]').as('transformationEnabled').uncheck();
      cy.get('[data-cy-settings="nav-server"]').as('server').click();
      cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
      cy.fixture('empty.xslt').then((emptyXslt: string) => {
        emptyXslt = normalize(emptyXslt);
        cy.get('[data-cy-settings-transformation]').as('transformation').clear().type(emptyXslt);
      });
      cy.get('[data-cy-settings="regexFilter"]').as('regexFilter').clear().type('xxx');
      cy.get('@regexFilter').invoke('val').should('equal', 'xxx');
      cy.get('@regexFilter').type('{enter}');
      cy.get('@openSettingsModal').click();
      checkSavedModification();
      cy.get('[data-cy-settings="close"]').as('close').click();
    })

    afterEach(() => {
      cy.debugTabBackToFactorySettings();
    })

    it('When changes are reverted then the last-saved settings are restored', () => {
      cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
      cy.get('[data-cy-settings="nav-client"]').as('client').click();
      cy.get('[data-cy-settings="openLatestReports"]').as('numberOfReports').type('{selectAll}6');
      cy.get('@numberOfReports').invoke('val').should('equal', '6');
      cy.get('[data-cy-settings="showAmount"]').as('showMultiple').uncheck();
      cy.get('[data-cy-settings="spacingDropdown"]').select('3x');
      cy.get('[data-cy-settings-transformation-enabled]').as('transformationEnabled').check();
      cy.get('[data-cy-settings="nav-server"]').as('server').click();
      cy.get('[data-cy-settings="generatorEnabled"]').select('Enabled');
      cy.get('[data-cy-settings-transformation]').as('transformation').clear().type('yyy');
      cy.get('[data-cy-settings="regexFilter"]').as('regexFilter').clear().type('yyy');
      cy.get('[data-cy-settings="revertChanges"]').click();
      checkSavedModification();
      cy.get('[data-cy-settings="close"]').as('close').click();
    })
  })
});

function checkSavedModification() {
  cy.get('@client').click();
  cy.get('@numberOfReports').invoke('val').should('equal', '8');
  cy.get('@showMultiple').should('be.checked');
  cy.get('[data-cy-settings="spacingDropdown"] option:selected').should('have.text', '5x');
  cy.get('@transformationEnabled').should('not.be.checked');
  cy.get('@server').click();
  cy.get('[data-cy-settings="generatorEnabled"] option:selected').should(
    'have.text',
    'Disabled'
  )
  cy.fixture('empty.xslt').then((rawExpected) => {
    cy.get('@transformation').invoke('val').then((rawActual) => {
      expect(normalize(rawActual as string)).to.equal(normalize(rawExpected));
    })
  })
  cy.get('@regexFilter').invoke('val').should('equal', 'xxx');
}
// For some reason Cypress adds extra line endings when typing the text
// in the text area. We compensate for this here.
function normalize(s: string): string {
  return s.replaceAll('\r\n', '\n').replaceAll('\n\n', '\n');
}