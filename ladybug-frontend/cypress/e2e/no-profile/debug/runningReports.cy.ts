describe('Test running reports', () => {
  before(() => cy.resetApp());

  afterEach(() => cy.resetApp());

  it('If no running reports then number of running reports is zero', () => {
    cy.initializeApp();
    cy.contains('Reports in progress: 0');
  });
});

describe('With running reports', () => {
  before(() => {
    cy.resetApp();
    cy.createRunningReport();
    cy.createRunningReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.createRunningReport();
    cy.createRunningReport();
    cy.initializeApp();
  });

  after(() => {
    cy.resetApp();
  })

  it('Open running reports', () => {
    cy.get('[data-cy-debug-in-progress-counter]').should('contain.text', 'Reports in progress: 2');
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(0);
    cy.checkFileTreeLength(0);
    cy.get('[data-cy-debug="openInProgressNo"]').type('{backspace}1');
    cy.get('[data-cy-debug="openInProgress"]').click();
    cy.get('.toast-body').should('contain.text', 'Opened report in progress');
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-debug-tree="root"] app-tree-item .item-name').eq(0).should('contain.text', 'Waiting for thread to start');
  });
});

describe('Test Reports in progress warning', () => {
  beforeEach(() => {
    cy.createRunningReport();
    cy.request(Cypress.env('backendServer') + '/index.jsp?setReportInProgressThreshold=1').then((resp) => {
      expect(resp.status).equal(200);
    });
    cy.initializeApp();
  });

  afterEach(() => {
    cy.request(Cypress.env('backendServer') + '/index.jsp?setReportInProgressThreshold=300000').then((resp) => {
      expect(resp.status).equal(200);
    });
    cy.resetApp();
  });

  it('If threshold time has been met then show warning', () => {
    cy.get('[data-cy-debug="refresh"]').click();
    cy.contains(`[One or more reports are in progress for more than`);
  });
});

