describe('Checkpoint value labels', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    const url = Cypress.config('baseUrl') + '/api/nullAndEmpty'
    cy.request('GET', url, null).then((resp) => {
      expect(resp.status).to.equal(200)
    })
  })

  it('When message is null then a label null is shown', () => {
    openReport('NullAndEmpty')
    cy.selectTreeNode([
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipeline NullAndEmpty/NullAndEmpty'
    ]).click()
    cy.checkNumCheckpointValueLabels(2)
    cy.checkpointValueLabel(0)
      .trimmedText()
      .should('equal', 'Read only')
    cy.checkpointValueLabel(1)
      .trimmedText()
      .should('equal', 'Message is null')
  })

  it('When message is empty character stream then label empty', () => {
    openReport('NullAndEmpty')
    cy.selectTreeNode([
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipe emptyCharacterStream',
      { seq: 1, text: 'Pipe emptyCharacterStream' }
    ]).click()
    cy.checkNumCheckpointValueLabels(2)
    cy.checkpointValueLabel(0)
      .trimmedText()
      .should('equal', 'Read only')
    cy.checkpointValueLabel(1)
      .trimmedText()
      .should('equal', 'Message is empty string')
  })

  it('When message is empty binary stream then three labels read only, empty and encoding', () => {
    openReport('NullAndEmpty')
    cy.selectTreeNode([
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipe emptyBinaryStream',
      { seq: 1, text: 'Pipe emptyBinaryStream' }
    ]).click()
    cy.checkNumCheckpointValueLabels(3)
    cy.checkpointValueLabel(0)
      .trimmedText()
      .should('equal', 'Read only')
    cy.checkpointValueLabel(1)
      .trimmedText()
      .should('equal', 'Message is empty string')
    cy.checkpointValueLabel(2)
      .should('contain.text', 'encoded')
      .should('contain.text', 'UTF-8')
  })

  it('When message is not-streamed string value then no labels except read only', () => {
    openReport('NullAndEmpty')
    cy.selectTreeNode([
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipe normal',
      { seq: 1, text: 'normal' }
    ]).click()
    cy.checkNumCheckpointValueLabels(1)
    cy.checkpointValueLabel(0)
      .trimmedText()
      .should('equal', 'Read only')
  })

  it('When message is non-empty character stream then no labels except read only', () => {
    openReport('NullAndEmpty')
    cy.selectTreeNode([
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipeline NullAndEmpty/NullAndEmpty',
      'Pipe stream',
      { seq: 1, text: 'stream' }
    ]).click()
    cy.checkNumCheckpointValueLabels(1)
    cy.checkpointValueLabel(0)
      .trimmedText()
      .should('equal', 'Read only')
  })
})

function openReport (expectedName: string): void {
  cy.visit('')
  cy.getNumLadybugReports()
  cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1).as('reportRow')
  // Status column.
  // TODO: Test exact value of status column if possible.
  cy.get('@reportRow').find('td:eq(6)').trimmedText().should('equal', 'Success')
  cy.get('@reportRow').contains(expectedName).click()
}

describe('Checkpoint value truncation because of ibistesttool.maxMessageLength', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.visit('')
    // Including the newlines, these are 7 * 56 = 392 characters.
    cy.createReportWithTestPipelineApi('UseTextBlockTestPipe', 'UseTextBlockPipe', '7 55')
  })

  const TOTAL_CHARACTERS_OF_CHECKPOINT = 7 * 56

  it('When maxMessageLength is exceeded by checkpoint value, then the right number of characters is shown', () => {
    // 56 characters including the newline
    // For some reason, Cypress does not come up with the final newline character.
    // It is shown and counted though.
    const row = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRS'
    let expected = ''
    for (let i = 0; i < 5; ++i) {
      const codeOfCharZero = '0'.charCodeAt(0)
      const codeOfFirstChar = codeOfCharZero + i
      const thisRow = row.replace('0', String.fromCharCode(codeOfFirstChar))
      expected += thisRow
    }
    // Have 56 * 5 = 280 characters, adding 20 makes 300.
    expected += '5123456789abcdefghij'
    openReport('UseTextBlockPipe')
    cy.selectTreeNode([
      'Pipeline UseTextBlockTestPipe/UseTextBlockPipe',
      'Pipeline UseTextBlockTestPipe/UseTextBlockPipe',
      'Pipe testPipe',
      { seq: 1, text: 'testPipe' }
    ]).click()
    cy.checkpointValueTrimmedEquals(expected)
  })

  it('When maxMessageLength is exceeded, there is a label showing how many characters are omitted', () => {
    const numOmitted = TOTAL_CHARACTERS_OF_CHECKPOINT - 300
    const omittedText = `${numOmitted}`
    openReport('UseTextBlockPipe')
    cy.selectTreeNode([
      'Pipeline UseTextBlockTestPipe/UseTextBlockPipe',
      'Pipeline UseTextBlockTestPipe/UseTextBlockPipe',
      'Pipe testPipe',
      { seq: 1, text: 'testPipe' }
    ]).click()
    cy.checkNumCheckpointValueLabels(2)
    cy.checkpointValueLabel(0).should('contain.text', 'Read')
    cy.checkpointValueLabel(1)
      .should('contain.text', 'truncated')
      .should('contain.text', omittedText)
  })
})
