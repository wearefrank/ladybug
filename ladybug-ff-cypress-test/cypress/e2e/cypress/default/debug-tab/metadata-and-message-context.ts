describe('Metadata and message context', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.wrap(Cypress.config('fixturesFolder')).then((fixturesFolder) => {
      const rawInputMessage = fixturesFolder + '/ConclusionInputs/valid'
      const inputMessage = rawInputMessage.replace(/\\/g, '/')
      cy.log('Input message for Conclusion/IngestDocument: ' + inputMessage)
      const url = Cypress.config('baseUrl') + '/api/ingestdocument'
      cy.request('POST', url, inputMessage).then(resp => {
        expect(resp.status).to.equal(200)
      })
    })
  })

  it('Check that configuration Conclusion/IngestDocument ran successfully', () => {
    cy.visit('')
    cy.getNumLadybugReports()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1).as('reportRow')
    // Query fresh from the live DOM instead of clicking through the '@reportRow' alias
    // (also below, in the other tests in this file): a reload can land between locating
    // the row above and clicking it here, which would detach the aliased node and make
    // cy.click() fail with "the page updated as a result of this command". Querying at
    // click time lets Cypress's retry re-locate the row if it gets recreated meanwhile.
    cy.inIframeBody('[data-cy-debug="tableRow"]').contains('Conclusion').click()
    cy.get('@reportRow').checkStatusFromRow('Success');
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe readXmlFile'
    ]).click()
    cy.wait(1000)
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe checkXml'
    ]).click()
    cy.wait(1000)
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe readTxtFile'
    ]).click()
    cy.wait(1000)
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe encodeBody'
    ]).click()
    cy.wait(1000)
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe composeMundoMessage'
    ]).click()
    cy.wait(1000)
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument',
      'Pipe sendToMundo',
      { seq: 1, text: 'Pipe sendToMundo' }
    ]).click()
    cy.wait(1000)
    // Do not check for equality because there is a line number and something
    // other text the user does not see.
    cy.checkpointValueEquals('ok')
  })

  it('Hide and show message context', () => {
    cy.visit('')
    cy.getNumLadybugReports()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1)
    cy.inIframeBody('[data-cy-debug="tableRow"]').contains('Conclusion').click()
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument'
    ]).click()
    cy.inIframeBody('app-messagecontext-table').should('not.exist')
    // TODO: when data-cy tags are updated, update this Cypress query.
    // Also do this for other tests in this file.
    cy.inIframeBody('[data-cy-open-messagecontext-table]')
      .should('contain', 'Show messagecontext')
      .click()
    cy.inIframeBody('app-messagecontext-table').contains('Header.user-agent')
    cy.inIframeBody('[data-cy-open-messagecontext-table]')
      .should('contain', 'Hide messagecontext')
      .click()
    cy.inIframeBody('app-messagecontext-table').should('not.exist')
  })

  it('Hide and show metadata', () => {
    cy.visit('')
    cy.getNumLadybugReports()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1)
    cy.inIframeBody('[data-cy-debug="tableRow"]').contains('Conclusion').click()
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument'
    ]).click()
    cy.inIframeBody('app-metadata-table').should('not.exist')
    cy.inIframeBody('[data-cy-open-metadata-table]')
      .should('contain', 'Show metadata')
      .click()
    cy.inIframeBody('app-metadata-table').contains('Source class name')
    cy.inIframeBody('[data-cy-open-metadata-table]')
      .should('contain', 'Hide metadata')
      .click()
    cy.inIframeBody('app-metadata-table').should('not.exist')
  })

  it('Show metadata and message context together', () => {
    cy.visit('')
    cy.getNumLadybugReports()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1)
    cy.inIframeBody('[data-cy-debug="tableRow"]').contains('Conclusion').click()
    cy.selectTreeNode([
      'Pipeline Conclusion/IngestDocument',
      'Pipeline Conclusion/IngestDocument'
    ]).click()
    cy.inIframeBody('app-messagecontext-table').should('not.exist')
    cy.inIframeBody('app-metadata-table').should('not.exist')
    cy.inIframeBody('[data-cy-open-messagecontext-table]')
      .should('contain', 'Show messagecontext')
      .click()
    cy.inIframeBody('[data-cy-open-metadata-table]')
      .should('contain', 'Show metadata')
      .click()
    cy.inIframeBody('app-messagecontext-table').contains('Header.user-agent')
    cy.inIframeBody('app-metadata-table').contains('Source class name')
    cy.inIframeBody('[data-cy-open-messagecontext-table]')
      .should('contain', 'Hide messagecontext')
      .click()
    cy.inIframeBody('[data-cy-open-metadata-table]')
      .should('contain', 'Hide metadata')
      .click()
    cy.inIframeBody('app-messagecontext-table').should('not.exist')
    cy.inIframeBody('app-metadata-table').should('not.exist')
  })
})

describe('Test contents of message context', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    const url = Cypress.config('baseUrl') + '/api/service1a'
    cy.request({
      method: 'GET',
      url,
      headers: { 'Content-Type': 'application/json' },
      body: { hello: 'world' }
    }).then(resp => {
      expect(resp.status).to.equal(200)
    })
  })

  it('Mime type is a meaningful string', () => {
    cy.visit('')
    cy.getNumLadybugReports()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1)
    cy.inIframeBody('[data-cy-debug="tableRow"]').click()
    cy.selectTreeNode([
      'Pipeline Example1a/Adapter1a',
      'Pipeline Example1a/Adapter1a'
    ]).click()
    cy.inIframeBody('app-messagecontext-table').should('not.exist')
    cy.inIframeBody('[data-cy-open-messagecontext-table]')
      .should('contain', 'Show messagecontext')
      .click()
    cy.inIframeBody('app-messagecontext-table')
      .contains('.key', 'Metadata.MimeType')
      .siblings('[data-cy-messagecontext-table="value"]')
      .invoke('text')
      .should('equal', 'application/json')
  })
})