// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

const ibisTesterUser = 'tester'
const ibisTesterPwd = 'IbisTester'

declare namespace Cypress {
  interface Chainable<Subject = any> {
    inIframeBody(query: string): Chainable<any>
    enterLadybug(): void
    getNumLadybugReports(): Chainable<any>
    createReportWithTestPipelineApi(config: string, adapter: string, message: string, username?: string, password?: string): Chainable<any>
    getNumLadybugReportsForNameFilter(name: string): Chainable<number>
    createReportInLadybug(config: string, adapter: string, message: string, username?: string, password?: string): Chainable<number>
    createReportAndOpen(config: string, adapter: string, message: string, username?: string, password?: string);
    getAllStorageIdsInTable(): Chainable<number[]>
    guardedCopyReportToTestTab(alias: string)
    checkTestTabHasReportNamed(name: string): Cypress.Chainable<any>
    enterFilter(field: string, filter: string)
    checkActiveFilterSphere(field: string, value: string): Cypress.Chainable<any>
    apiDeleteAll(storageName: string)
    apiDeleteAllAsTester(storageName: string)
    selectTreeNode(path: NodeSelection[]): Cypress.Chainable<any>
    awaitLoadingSpinner(): void
    waitForVideo(): void
    trimmedText(): Chainable<any>
    checkpointValueEquals(expectedValue: string): void
    checkpointValueTrimmedEquals(expectedValue: string): void
    checkpointValueEmpty(): void
    checkNumCheckpointValueLabels(expectedNumLabels: number): void
    checkpointValueLabel(index: number): Chainable<any>
    visitAsTester(): void
    visitAs(username: string, password: string): void
    enableReportGenerator(): void
  }
}

Cypress.Commands.add('inIframeBody', (query) => {
  cy
    .get('iframe')
    .its('0.contentDocument')
    .its('body')
    .then(body => {
      cy.wrap(body).find(query)
    })
})

Cypress.Commands.add('enterLadybug', () => {
  cy.get('[data-cy-nav="adapterStatus"]', { timeout: 10000 }).click()
  cy.get('[data-cy-nav="testingLadybug"]').should('not.be.visible')
  cy.get('[data-cy-nav="testing"]').click()
  cy.get('[data-cy-nav="testingLadybug"]').click()
  cy.awaitLoadingSpinner()
  cy.inIframeBody('[data-cy-nav-tab="debugTab"]').click()
})

Cypress.Commands.add('getNumLadybugReports', () => {
  cy.enterLadybug()
  cy.intercept({
    method: 'GET',
    url: `iaf/ladybug/api/metadata/${Cypress.env('debugStorageName') as string}/count`,
    times: 1
  }).as('apiGetReports_2')
  cy.awaitLoadingSpinner()
  cy.inIframeBody('[data-cy-debug="refresh"]').click()
  cy.wait('@apiGetReports_2').then(interception => {
    const count: number = interception.response.body
    // Uncomment if PR https://github.com/wearefrank/ladybug-frontend/pull/363
    // has been merged and if its frontend is referenced by F!F pom.xml.
    //
    // cy.inIframeBody('[data-cy-debug="amountShown"]')
    // .should('equal', "/" + count);
    cy.inIframeBody('[data-cy-debug="tableRow"]')
      .should('have.length', count)
    return cy.wrap(count)
  })
})

// When the Test a Pipeline UI was used, the empty message was supported.
// Now that the API endpoint is called directly, the empty message does not work.
// An internal server error (500) was observed for that case.
Cypress.Commands.add('createReportWithTestPipelineApi', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  const formData = new FormData();
  formData.append('configuration', config);
  formData.append('adapter', adapter);
  formData.append('message', new Blob([message], { type: 'text/plain' }), 'message');
  const multipartHeader = {
    'Content-Type': 'multipart/form-data'
  }
  let authorizationHeader = {}
  if (username !== undefined) {
    if (password === undefined) {
      throw new Error('When you want to authorize with a username, then a password should be provided')
    }
    // Encode to Base64
    const encodedCredentials = btoa(`${username}:${password}`);
    authorizationHeader = {
      Authorization: `Basic ${encodedCredentials}`,
    }
  }
  const headers = { ...multipartHeader, ...authorizationHeader }
  cy.request({
    method: 'POST',
    url: 'iaf/api/test-pipeline',
    headers,
    body: formData
  }).then((response) => {
    expect(response.status).to.equal(200);
    const dec = new TextDecoder();
    const parsedResponse = JSON.parse(dec.decode(response.body));
    expect(parsedResponse.state).to.equal('SUCCESS');
  })
})

// Only works if some reports are expected to be omitted because of the filter
Cypress.Commands.add('getNumLadybugReportsForNameFilter', (name) => {
  cy.getNumLadybugReports().then(totalNumReports => {
    cy.inIframeBody('[data-cy-debug="filter"]').click()
    cy.enterFilter('Name', name)
    cy.inIframeBody('[data-cy-debug="tableRow"]').its('length')
      .should('be.lessThan', totalNumReports).then(result => {
        cy.inIframeBody('app-filter-side-drawer').find('label:contains(Name)')
          .parent().find('button:contains(Clear)').click()
        cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', totalNumReports)
        cy.inIframeBody('app-filter-side-drawer').find('button:contains(Close)').click()
        cy.inIframeBody('app-filter-side-drawer').find('label').should('not.exist')
        return cy.wrap(result)
      })
  })
})

Cypress.Commands.add('createReportInLadybug', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  cy.getNumLadybugReports().then(numBefore => {
    cy.createReportWithTestPipelineApi(config, adapter, message, username, password)
    cy.getNumLadybugReports().should('equal', numBefore + 1)
    cy.getAllStorageIdsInTable().then(storageIds => {
      const storageId = Math.max.apply(null, storageIds)
      cy.log(`Last created report has storageId ${storageId.toString()}`)
      return cy.wrap(storageId)
    })
  })
})

Cypress.Commands.add('createReportAndOpen', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  cy.createReportInLadybug('Example1a', 'Adapter1a', 'xxx').then(storageId => {
    cy.wrap('Found report just created, storageId=' + storageId)
    cy.inIframeBody('[data-cy-debug="tableRow"]')
      .find('td:nth-child(2)').each($cell => {
        if (parseInt($cell.text()) === storageId) {
          cy.wrap('Going to click cell with text' + $cell.text())
          cy.wrap($cell).click()
        }
      })
  })
})

Cypress.Commands.add('getAllStorageIdsInTable', () => {
  const storageIds: number[] = []
  cy.inIframeBody('[data-cy-debug="tableRow"]').each($row => {
    cy.wrap($row).find('td:eq(1)').invoke('text').then(s => {
      storageIds.push(parseInt(s))
    })
  }).then(() => {
    cy.log(`Ladybug debug tab table has storage ids: ${storageIds.toString()}`)
    return cy.wrap(storageIds)
  })
})

Cypress.Commands.add('guardedCopyReportToTestTab', (alias) => {
  cy.intercept({
    method: 'PUT',
    url: /\/api\/report\/store\/*?/g,
    times: 1
  }).as(alias)
  cy.intercept({
    method: 'GET',
    url: /\/iaf\/ladybug\/api\/metadata\/Test*/g
  }).as('apiGetTestReports')
  cy.inIframeBody('[data-cy-debug-editor="copy"]').click()
  cy.wait(`@${alias}`).then((interception) => {
    cy.wrap(interception).its('request.url').should('contain', 'Test')
    cy.wrap(interception).its('response.statusCode').should('equal', 200)
  })
  cy.wait('@apiGetTestReports', { timeout: 30000 })
})

Cypress.Commands.add('checkTestTabHasReportNamed', (name) => {
  cy.inIframeBody('[data-cy-nav-tab="testTab"]').click()
  cy.inIframeBody('[data-cy-test="table"] tbody tr')
    .should('have.length', 1)
    .as('testtabReportRow')
  // TODO: It would be nice not to trim the text here.
  cy.get('@testtabReportRow').find('td:eq(2)').trimmedText().should('equal', name)
  cy.get('@testtabReportRow').find('td:eq(4)').should('be.empty')
  return cy.get('@testtabReportRow')
})

Cypress.Commands.add('enterFilter', (field: string, filter: string) => {
  const fieldQuery = `label:contains(${field})`
  cy.inIframeBody('app-filter-side-drawer').find(fieldQuery)
    .parent().find('input')
    .type(filter + '{enter}')
})

Cypress.Commands.add('checkActiveFilterSphere', (field: string, value: string) => {
  const expectedText = `${field}: ${value}`
  return cy.inIframeBody('app-active-filters').contains(expectedText)
})

Cypress.Commands.add('apiDeleteAll', (storageName: string) => {
  cy.request({
    method: 'DELETE',
    url: `/iaf/ladybug/api/report/all/${storageName}`
  }).then(response => {
    cy.wrap(response).its('status').should('equal', 200)
  })
})

Cypress.Commands.add('apiDeleteAllAsTester', (storageName: string) => {
  cy.request({
    method: 'DELETE',
    url: `/iaf/ladybug/api/report/all/${storageName}`,
    auth: {
      username: ibisTesterUser,
      password: ibisTesterPwd
    }
  }).then(response => {
    cy.wrap(response).its('status').should('equal', 200)
  })
})

interface TextWithSeq {
  text: string
  seq: number
}

type NodeSelection = TextWithSeq | string

function normalizeNodeSelection (input: NodeSelection): TextWithSeq {
  if (typeof input === 'string') {
    return { text: input, seq: 0 }
  } else {
    return input
  }
}

Cypress.Commands.add('selectTreeNode', (path: NodeSelection[]) => {
  const head = normalizeNodeSelection(path.shift())
  cy.inIframeBody(`[data-cy-debug-tree="root"] > app-tree-item > div > div:nth-child(1):contains(${head.text})`).then((elementsWithTexts) => {
    const chosen = elementsWithTexts[head.seq]
    return cy.wrap(chosen).parent().parent().then((element) => {
      if (path.length === 0) {
        return cy.wrap(element)
      } else {
        return selectTreeNodeImpl(element, path)
      }
    })
  })
})

function selectTreeNodeImpl (subject: JQuery<HTMLElement>, path: NodeSelection[]): Cypress.Chainable<any> | void {
  const head = normalizeNodeSelection(path.shift())
  cy.wrap(subject).find(`> div > div > div > app-tree-item > div > div:nth-child(1):contains(${head.text})`).then((elementsWithTexts) => {
    const chosen = elementsWithTexts[head.seq]
    if (path.length === 0) {
      return cy.wrap(chosen)
    } else {
      cy.wrap(chosen).parent().parent().then((element) => {
        return selectTreeNodeImpl(element, path)
      })
    }
  })
}

Cypress.Commands.add('awaitLoadingSpinner', () => {
  // We do not want to catch the moment that the loading spinner is NOT YET present
  cy.wait(400)
  cy.inIframeBody('[data-cy-loading-spinner]').should('not.exist')
})

// Wait so that the state of the UI is shown more clearly in videos.
Cypress.Commands.add('waitForVideo', () => {
  cy.wait(3000)
})

Cypress.Commands.add('trimmedText', { prevSubject: true }, (subject) => {
  cy.wrap(subject).invoke('text').then((theText) => {
    cy.wrap(trimMonacoText(theText))
  })
})

function trimMonacoText (value: string): string {
  const nbspRegex = /\u00A0/g
  return value.replace(nbspRegex, ' ').trim()
}

Cypress.Commands.add('checkpointValueEquals', { prevSubject: false }, (expectedValue) => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue) => actualValue === expectedValue, 6, 1000)
})

function checkLadybugCheckpointValue (checker: (string) => boolean, numberOfTimes: number, frequencyMS: number): void {
  if (numberOfTimes === 0) {
    throw new Error('checkLadybugCheckpointValue number of tries exceeded')
  }
  cy.log(`Remaining number of tries: ${numberOfTimes}`).then(() => {
    cy.inIframeBody('[data-cy-element-name="checkpointEditor"]').then((appEditor) => {
      const textOfAppEditor: string = appEditor.text()
      cy.log(`Text out of scrollable element: ${trimForLog(textOfAppEditor)}`).then(() => {
        if (checker(textOfAppEditor)) {
          cy.log('Text of app editor already matches')
        } else if (textOfAppEditor.length === 0) {
          cy.log('No text in app editor, next try').then(() => {
            cy.wait(frequencyMS)
            checkLadybugCheckpointValue(checker, numberOfTimes - 1, frequencyMS)
          })
        } else {
          cy.wrap(appEditor).find('.monaco-scrollable-element').then((monacoScrollableElement) => {
            const textOfScrollableElement: string = monacoScrollableElement.text()
            cy.log(`Text inside scrollable element: ${trimForLog(textOfScrollableElement)}`).then(() => {
              if (checker(textOfScrollableElement)) {
                cy.log('Text inside scrollable element matches')
              } else {
                cy.wait(frequencyMS)
                checkLadybugCheckpointValue(checker, numberOfTimes - 1, frequencyMS)
              }
            })
          })
        }
      })
    })
  })
}

function trimForLog (value: string): string {
  if (value.length > 20) {
    return `${value.substring(0, 17)}...`
  } else {
    return value
  }
}

Cypress.Commands.add('checkpointValueTrimmedEquals', { prevSubject: false }, (expectedValue) => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue: string) => trimMonacoText(actualValue) === expectedValue, 6, 1000)
})

Cypress.Commands.add('checkpointValueEmpty', { prevSubject: false }, () => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue: string) => actualValue.length === 0, 6, 1000)
})

Cypress.Commands.add('checkNumCheckpointValueLabels', { prevSubject: false }, (expectedNumLabels: number) => {
  cy.inIframeBody('app-report-alert-message2 > div > div')
    .should('have.length', expectedNumLabels)
})

Cypress.Commands.add('checkpointValueLabel', { prevSubject: false }, (index: number) => {
  cy.inIframeBody(`app-report-alert-message2 > div > div:eq(${index})`)
})

Cypress.Commands.add('visitAsTester', { prevSubject: false }, () => {
  cy.visit('', {
    auth: {
      username: ibisTesterUser,
      password: ibisTesterPwd
    }
  })
})

Cypress.Commands.add('visitAs', { prevSubject: false }, (username, password) => {
  cy.visit('', {
    auth: {
      username,
      password
    }
  })
})

Cypress.Commands.add('enableReportGenerator', { prevSubject: false }, () => {
  cy.inIframeBody('[data-cy-debug="openSettings"]').should('be.visible').click()
  cy.inIframeBody('[role=dialog]').should('be.visible')
  cy.inIframeBody('[data-cy-settings="generatorEnabled"]').select('Enabled').should('have.value', 'Enabled')
  cy.inIframeBody('[data-cy-settings="saveChanges"]').click()
})
