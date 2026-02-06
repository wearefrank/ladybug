describe('Basic tests', () => {
  it('Basic test', () => {
    cy.visit('')
    cy.getNumLadybugReports().then(numReports => {
      cy.createReportWithTestPipelineApi('Example1a', 'Adapter1a', 'xxx')
      cy.getNumLadybugReports().should('equal', numReports + 1)
    })
  })

  it('Filter no regex', () => {
    cy.visit('')
    cy.createReportWithTestPipelineApi('Example1a', 'Adapter1a', 'xxx')
    cy.createReportWithTestPipelineApi('Example1b', 'Adapter1b', 'xxx')
    cy.getNumLadybugReports().should('at.least', 2).then(total => {
      cy.getNumLadybugReportsForNameFilter('Adapter1a').then(reportsA => {
        expect(reportsA).not.to.be.undefined
        expect(reportsA).to.be.lessThan(total as number)
        cy.getNumLadybugReportsForNameFilter('Adapter1b').then(reportsB => {
          expect(reportsB).to.be.lessThan(total as number)
          expect(reportsA + reportsB).to.equal(total)
        })
      })
    })
  })

  it('Rerun in debug tab', () => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.visit('')
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
          cy.contains('Pipeline Example1a/Adapter1a').click()
        })
      cy.awaitLoadingSpinner()
      cy.inIframeBody('.rerun-result').should('not.exist')
      cy.inIframeBody('[data-cy-report="rerun"]').click()
      cy.inIframeBody(':contains(Report):contains(rerun):contains(successful)')
      cy.inIframeBody('.rerun-result').should('contain', 'stubbed')
      cy.getNumLadybugReports().should('equal', 2)
    })
  })
})
