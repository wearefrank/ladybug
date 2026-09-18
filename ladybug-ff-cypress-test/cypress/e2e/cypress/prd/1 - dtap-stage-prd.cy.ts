import { AUTHENTICATIONS } from "../support/commands"

// Filename starts with 1 so that these texts are executed before other specs.
// We want to check that the report generator is disabled by default before
// other tests manipulate the report generator state.

describe('dtap.stage=PRD', () => {
  it('Report generator is disabled by default', () => {
    cy.visitAsTester()
    cy.getNumLadybugReports().then(numReports => {
      cy.wrap(numReports).should('equal', 0)
      cy.createReportWithTestPipelineApi('Example1a', 'Adapter1a', 'xxx')
      cy.getNumLadybugReports().should('equal', 0)
    })
  })

  describe('Rerun in debug tab forbidden', () => {
    before(() => {
      // Implicitly logs in
      cy.visitAsTester()
      cy.apiDeleteAllAsTester(Cypress.env('debugStorageName') as string)
      cy.apiDeleteAllAsTester('Test')
      cy.enterLadybug()
      cy.enableReportGenerator()
      cy.createReportInLadybug('Example1a', 'Adapter1a', 'xxx', 'tester', 'IbisTester')
    })

    for (const testUser of Array.from(AUTHENTICATIONS.keys()).filter((user) => user !== 'tester')) {
      it(`Cannot Report rerun as ${testUser}`, () => {
        cy.visitAs(testUser)
        cy.getNumLadybugReports().should('equal', 1)
        cy.inIframeBody('[data-cy-debug="tableRow"]')
          .find('td:nth-child(2)')
          .click()
        cy.inIframeBody('[data-cy-debug-tree="root"]')
          .should('have.length.at.least', 1)
          .contains('Pipeline Example1a/Adapter1a').within(_ => {
            cy.contains('Pipeline Example1a/Adapter1a').click()
          })
        cy.awaitLoadingSpinner()
        cy.inIframeBody('.rerun-result').should('not.exist')
        cy.inIframeBody('[data-cy-report="rerun"]').click()
        cy.inIframeBody(':contains(Not allowed)')
        cy.awaitLoadingSpinner()
        cy.getNumLadybugReports().should('equal', 1)
      })
    }

    it("When logged in as IbisTester then rerun allowed", () => {
      cy.visitAs('tester')
      cy.getNumLadybugReports().should('equal', 1)
      cy.inIframeBody('[data-cy-debug="tableRow"]')
        .find('td:nth-child(2)')
        .click()
      cy.inIframeBody('[data-cy-debug-tree="root"]')
        .should('have.length.at.least', 1)
        .contains('Pipeline Example1a/Adapter1a').within(_ => {
          cy.contains('Pipeline Example1a/Adapter1a').click()
        })
      cy.awaitLoadingSpinner()
      cy.inIframeBody('.rerun-result').should('not.exist')
      cy.inIframeBody('[data-cy-report="rerun"]').click()
      cy.inIframeBody('.rerun-result').trimmedText().should('contain', 'checkpoints')
      cy.getNumLadybugReports().should('equal', 2)
    })
  })
})
