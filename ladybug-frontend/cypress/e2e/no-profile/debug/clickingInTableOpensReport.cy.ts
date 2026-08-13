describe('Clicking a report', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.createRunningReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('Selecting report should show a tree', () => {
    cy.get('[data-cy-debug-tree="buttons"]').should('not.exist');
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('not.have.class', 'highlight');
    })
    cy.openReportByClickInDebugTable(0);
    cy.get('[data-cy-debug-tree="buttons"]').should('be.visible');
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('have.class', 'highlight');
    })
  });

  it('Selecting report should show display', () => {
    cy.get('[data-cy-debug-editor="buttons"]').should('not.exist');
    cy.get('[data-cy-element-name="checkpointEditor"]').should('not.exist');
    cy.openReportByClickInDebugTable(0);
    cy.get('[data-cy-debug-editor="buttons"]').should('be.visible');
    cy.get('[data-cy-element-name="checkpointEditor"]').should('be.visible');
  });

  it('When running report is opened then report that was openend before is no longher highlighted', () => {
    cy.openReportByClickInDebugTable(0);
    cy.get('[data-cy-debug-tree="buttons"]').should('be.visible');
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('have.class', 'highlight');
    })
    cy.get('[data-cy-debug-in-progress-counter]').should('contain.text', 'Reports in progress: 1');
    cy.get('[data-cy-debug="openInProgress"]').click();
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('not.have.class', 'highlight');
    })
  })

  it('When report is closed then no longer highlighted in table', () => {
    cy.openReportByClickInDebugTable(0);
    cy.get('[data-cy-debug-tree="buttons"]').should('be.visible');
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('have.class', 'highlight');
    });
    cy.get('[data-cy-debug-tree="close"]').click();
    cy.get('[data-cy-debug-tree="buttons"]').should('not.exist');
    cy.getDebugTableRows().first().find('td').each((cell) => {
      cy.wrap(cell).should('not.have.class', 'highlight');
    });
  })

  it('When report is checked then not yet opened', () => {
    cy.get('[data-cy-debug="selectOne"]').eq(0).should('not.be.checked');
    cy.get('[data-cy-debug="selectOne"]').eq(0).check();
    cy.get('[data-cy-debug="selectOne"]').eq(0).should('be.checked');
    // Check / uncheck is also tested by Karma tests.
    cy.get('[data-cy-debug-tree="buttons"]').should('not.exist');
    cy.openReportByClickInDebugTable(0);
    cy.get('[data-cy-debug-tree="buttons"]').should('be.visible');
    cy.get('[data-cy-debug="selectOne"]').eq(0).should('be.checked');
  })
});
