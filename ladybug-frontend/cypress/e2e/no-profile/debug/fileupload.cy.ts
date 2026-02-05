describe('Debug file upload', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

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
});
