describe('Debug file upload', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  const reportsToTest: string[] = ['testRerun.ttr', 'testRerunForNlNnReport.ttr']
  for (const report of reportsToTest) {
    it(`Upload report ${report}`, () => {
      cy.fixture(report, 'binary')
        .then(Cypress.Blob.binaryStringToBlob)
        .then((fileContent) => {
          cy.get('[data-cy-debug="upload"]').find('input').attachFile({
            fileContent,
            fileName: 'testRerun.ttr',
          });
        });
      cy.checkFileTreeLength(1);
    });
  }

  it ('When two reports are uploaded then two new tabs opened which are not confused', () => {
    cy.fixture('twoReports.zip', 'binary')
      .then(Cypress.Blob.binaryStringToBlob)
      .then((fileContent) => {
        cy.get('[data-cy-debug="upload"]').find('input').attachFile({
          fileContent,
          fileName: 'twoReports.zip',
        });
      });
    cy.get('[data-cy-nav-tab]').should('have.length', 4);
    cy.checkNavTab(0, 'Debug', false);
    cy.checkNavTab(1, 'Test', false);
    cy.checkNavTab(2, 'Adapter1a', false);
    cy.checkNavTab(3, 'Adapter1b', true);
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1b)').should('be.visible');
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1a)').should('not.exist');
    checkCheckpointMessage('B');
    cy.get(`[data-cy-nav-tab]:eq(2)`).click();
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1a)').should('be.visible');
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1b)').should('not.exist');
    checkCheckpointMessage('A');
    cy.get(`[data-cy-nav-tab]:eq(3)`).click();
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1b)').should('be.visible');
    cy.get('[data-cy-debug-tree]').find(':contains(Adapter1a)').should('not.exist');
    checkCheckpointMessage('B');
  });
});

function checkCheckpointMessage(distinction: string) {
  cy.get('[data-cy-element-name]').invoke('text').should('contain', 'Message').should('contain', distinction);
}
