describe('Test user stories about testing with Ladybug', () => {
  beforeEach(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.visit('')
  })

  it('Run report', () => {
    cy.createReportInLadybug('Example1a', 'Adapter1a', 'xxx').then(storageId => {
      cy.wrap('Found report just created, storageId=' + storageId)
      cy.inIframeBody('[data-cy-debug="tableRow"]')
        .find('td:nth-child(2)').each($cell => {
          if (parseInt($cell.text()) === storageId) {
            cy.wrap('Going to click cell with text' + $cell.text())
            cy.wrap($cell).click()
          }
        })
      cy.inIframeBody('[data-cy-debug-tree="root"]')
        .should('have.length.at.least', 1)
        .contains('Pipeline Example1a/Adapter1a').within(_ => {
          cy.contains('Pipeline Example1a/Adapter1a')
        })
      cy.guardedCopyReportToTestTab('apiCopyTheReportToTestTab')
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
      // Martijn October 8 2024: I do not know why I have to do this query again.
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
        .find('[data-cy-test="runReport"]').click()
      // TODO: Use data-cy to find the rerun result.
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
        .find('td:eq(5)').should('contain', 'stubbed')
    })
  })

  it('When the adapter is down then rerunning a related report produces a warning', () => {
    cy.createReportInLadybug('Example1a', 'Adapter1a', 'xxx').then(storageId => {
      cy.wrap('Found report just created, storageId=' + storageId)
      cy.stopAdapter('Example1a', 'Adapter1a');
      cy.inIframeBody('[data-cy-debug="tableRow"]')
        .find('td:nth-child(2)').each($cell => {
          if (parseInt($cell.text()) === storageId) {
            cy.wrap('Going to click cell with text' + $cell.text())
            cy.wrap($cell).click()
          }
        })
      cy.inIframeBody('[data-cy-debug-tree="root"]')
        .should('have.length.at.least', 1)
        .contains('Pipeline Example1a/Adapter1a').within(_ => {
          cy.contains('Pipeline Example1a/Adapter1a')
        })
      cy.guardedCopyReportToTestTab('apiCopyTheReportToTestTab2')
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
      // Martijn October 8 2024: I do not know why I have to do this query again.
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
        .find('[data-cy-test="runReport"]').click()
      cy.inIframeBody('[data-cy-toast="warning"]')
        .should('be.visible')
        .should('contain.text', 'stopped');
      cy.checkTestTabHasReportNamed('Pipeline Example1a_Adapter1a')
        .find('td:eq(5)').should('contain', 'stopped')
      cy.startAdapter('Example1a', 'Adapter1a');
    })
  })
})
