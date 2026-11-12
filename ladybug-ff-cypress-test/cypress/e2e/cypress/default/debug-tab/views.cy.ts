// Regression tests for the views White box, Gray box and Black box.
//
// We deliberately check only a few checkpoints per report. The exact set of
// checkpoints created by the Frank!Framework tends to vary over versions, so
// matching all of them would make these tests brittle.

const GRAY_BOX_REPORT = 'Pipeline GrayBox/Caller'
const CONCLUSION_REPORT = 'Pipeline Conclusion/IngestDocument'

describe('Views', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.expose('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.visitLadybug()
    cy.createReportWithTestPipelineApi('GrayBox', 'Caller', 'xxx')
    cy.wrap(Cypress.config('fixturesFolder')).then((fixturesFolder) => {
      const rawInputMessage = fixturesFolder + '/ConclusionInputs/valid'
      const inputMessage = rawInputMessage.replace(/\\/g, '/')
      cy.log('Input message for Conclusion/IngestDocument: ' + inputMessage)
      const url = Cypress.config('baseUrl') + '/api/ingestdocument'
      cy.request('POST', url, inputMessage).then(resp => {
        expect(resp.status).to.equal(200)
      })
    })
    cy.getNumLadybugReports().should('equal', 2)
  })

  describe('Configuration GrayBox', () => {
    it('White box view shows pipes, call to other adapter and end checkpoint', () => {
      openReportInView('White box', GRAY_BOX_REPORT)
      tree().should('contain', 'Pipe FixedMessage')
      tree().should('contain', 'Sender CallOtherAdapterSender')
      tree().should('contain', 'Pipeline GrayBox/Called')
      endCheckpointShouldExist(GRAY_BOX_REPORT)
    })

    it('Gray box view hides pipes but shows call to other adapter and end checkpoint', () => {
      openReportInView('Gray box', GRAY_BOX_REPORT)
      tree().should('not.contain', 'Pipe FixedMessage')
      tree().should('not.contain', 'Pipe CallOtherAdapter')
      tree().should('contain', 'Sender CallOtherAdapterSender')
      endCheckpointShouldExist(GRAY_BOX_REPORT)
    })

    it('Black box view shows only the end checkpoint', () => {
      openReportInView('Black box', GRAY_BOX_REPORT)
      tree().should('not.contain', 'Pipe FixedMessage')
      tree().should('not.contain', 'CallOtherAdapter')
      tree().should('not.contain', 'Pipeline GrayBox/Called')
      endCheckpointShouldExist(GRAY_BOX_REPORT)
      // The start checkpoint has only one child: the end checkpoint.
      childrenOfStartCheckpoint(GRAY_BOX_REPORT).should('have.length', 1)
    })
  })

  describe('Configuration Conclusion', () => {
    it('White box view shows all checkpoints', () => {
      openReportInView('White box', CONCLUSION_REPORT)
      tree().should('contain', 'Pipe readXmlFile')
      tree().should('contain', 'Pipe checkXml')
      tree().should('contain', 'Pipe composeMundoMessage')
      tree().should('contain', 'Pipe sendToMundo')
      tree().should('contain', 'Sender sendToMundo')
      endCheckpointShouldExist(CONCLUSION_REPORT)
    })

    // Gray box and Black box should produce the same limited output.
    for (const view of ['Gray box', 'Black box']) {
      it(`${view} view shows only the call to Mundo and the end checkpoint`, () => {
        openReportInView(view, CONCLUSION_REPORT)
        tree().should('not.contain', 'Pipe readXmlFile')
        tree().should('not.contain', 'Pipe checkXml')
        tree().should('not.contain', 'Pipe composeMundoMessage')
        tree().should('not.contain', 'Pipe sendToMundo')
        tree().should('contain', 'Sender sendToMundo')
        endCheckpointShouldExist(CONCLUSION_REPORT)
      })
    }
  })
})

function openReportInView (view: string, reportName: string): void {
  cy.visitLadybug()
  cy.getNumLadybugReports().should('equal', 2)
  cy.inLadybug('[data-cy-change-view-dropdown]').select(view)
  cy.awaitLoadingSpinner()
  // Query fresh from the live DOM instead of using an alias, because a reload
  // can detach a previously found row.
  cy.inLadybug('[data-cy-debug="tableRow"]').contains(reportName).click()
  cy.awaitDebugTree()
}

function tree (): Cypress.Chainable<any> {
  return cy.inLadybug('[data-cy-debug-tree="root"]')
}

// Report node -> start checkpoint -> end checkpoint. The end checkpoint of the
// pipeline has the same name as the start checkpoint and is its last child.
function endCheckpointShouldExist (reportName: string): void {
  cy.selectTreeNode([reportName, reportName, reportName]).should('exist')
}

function childrenOfStartCheckpoint (reportName: string): Cypress.Chainable<any> {
  // selectTreeNode yields the label of the start checkpoint. Go up to its
  // app-tree-item, see selectTreeNode in commands.ts for the DOM structure.
  return cy.selectTreeNode([reportName, reportName])
    .parent().parent()
    .find('> div > div > div > app-tree-item')
}
