/// <reference types="cypress" />
// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

import Chainable = Cypress.Chainable;
import JQueryWithSelector = Cypress.JQueryWithSelector;
import { Interception } from 'cypress/types/net-stubbing';

const OBSERVER_USER = 'IbisObserver';
const OBSERVER_PWD = 'IbisObserver';
const TESTER_USER = 'IbisTester';
const TESTER_PWD = 'IbisTester';

const TREE_ITEM_SELECTED_CLASS = 'sft-item-selected';

type TabType = 'debug' | 'test';

declare global {
  namespace Cypress {
    interface Chainable {
      initializeApp(): Chainable;

      initializeAppAsObserver(): Chainable;

      initializeAppAsTester(): Chainable;

      resetApp(): Chainable;

      clearTestReports(): Chainable;

      clearDatabaseStorage(): Chainable;

      navigateToTestTabAndAwaitLoadingSpinner(): Chainable;

      navigateToDebugTabAndAwaitLoadingSpinner(): Chainable;

      navigateToTestTab(): Chainable;

      navigateToDebugTab(): Chainable;

      createReport(): Chainable;

      createOtherReport(): Chainable;

      createReportInDatabaseStorage(): Chainable;

      createRunningReport(): Chainable;

      createReportWithLabelNull(): Chainable;

      createReportWithLabelEmpty(): Chainable;

      createReportWithInfopoint(): Chainable;

      createReportWithMultipleStartpoints(): Chainable;

      createJsonReport(): Chainable;

      createReportWithMessageContext(): Chainable;

      createReportOnlyLF(): Chainable;

      createReportOnlyCR(): Chainable;

      createReportWithStatusError(): Chainable;

      clearDebugStore(): Chainable;

      clearReportsInProgress(): Chainable;

      selectIfNotSelected(): Chainable;

      checkTestTableNumRows(length: number): Chainable;

      checkTestTableReportsAre(reportNames: string[]): Chainable;

      debugTreeGuardedCopyReport(
        reportName: string,
        numExpandedNodes: number,
        aliasSuffix: string,
      ): Chainable;

      clickRootNodeInFileTree(): Chainable;

      clickEndCheckpointOfThreeNodeReport(): Chainable;

      getShownNodesOfReportTreeWithText(text: string): Chainable;

      checkShownNodeWithTextSelected(reportName: string, index: number, selected: boolean): Chainable;

      checkShownNodeWithTextSearched(reportName: string, searched: boolean): Chainable;

      collapseNode(text: string, index: number):Chainable;

      expandNode(text: string, index: number): Chainable;

      clickRowInTable(index: number): Chainable;

      checkFileTreeLength(length: number): Chainable;

      refreshApp(): Chainable;

      getDebugTableRows(): Chainable;

      checkDebugTableRowsAre(reportNames: string[]): Chainable;

      getTestTableRows(): Chainable;

      assertDebugTableLength(length: number): Chainable;

      selectRowInDebugTable(index: number): Chainable;

      selectRowInTestTable(index: number): Chainable;

      copyReportsToTestTab(names: string[]): Chainable;

      editCheckpointValue(value: string): Chainable;

      debugTabBackToFactorySettings(): Chainable;

      enterSettingsDialogAndExpectReportGenerator(text: string): Chainable;

      checkNavTab(index: number, text: string, selected: boolean): Chainable;

      windowSendPostReportEvent(storageName: string, storageId: number): Chainable;

      uploadTwoReportsAndCheckTabs(): Chainable;
    }
  }
}

Cypress.Commands.add('initializeApp' as keyof Chainable, (): void => {
  //Custom command to initialize app and wait for all api requests
  cy.visit('');
  awaitLoadingSpinner();
});

Cypress.Commands.add('initializeAppAsObserver' as keyof Chainable, (): void => {
  cy.visit('', {
    auth: {
      username: OBSERVER_USER,
      password: OBSERVER_PWD,
    }
  });
  awaitLoadingSpinner();
})

Cypress.Commands.add('initializeAppAsTester' as keyof Chainable, (): void => {
  cy.visit('', {
    auth: {
      username: TESTER_USER,
      password: TESTER_PWD,
    }
  });
  awaitLoadingSpinner();
})

Cypress.Commands.add('resetApp' as keyof Chainable, (): void => {
  cy.clearDebugStore();
  cy.clearTestReports();
  cy.clearReportsInProgress();
  cy.clearDatabaseStorage();
});

Cypress.Commands.add('clearTestReports' as keyof Chainable, (): void => {
  cy.request({ method: 'DELETE', url: '/api/report/all/Test' }).then(
    (resp: Cypress.Response<ApiResponse>) => {
      expect(resp.status).equal(200);
    },
  );
});

Cypress.Commands.add('clearDatabaseStorage' as keyof Chainable, (): void => {
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?clearDatabaseStorage=true`,
  ).then((resp: Cypress.Response<ApiResponse>): void => {
    expect(resp.status).equal(200);
  });
})

Cypress.Commands.add(
  'navigateToTestTabAndAwaitLoadingSpinner' as keyof Chainable,
  (): void => {
    navigateToTabAndAwaitLoadingSpinner('test');
  },
);

Cypress.Commands.add(
  'navigateToDebugTabAndAwaitLoadingSpinner' as keyof Chainable,
  (): void => {
    navigateToTabAndAwaitLoadingSpinner('debug');
  },
);

Cypress.Commands.add('navigateToTestTab' as keyof Chainable, () => {
  navigateToTab('test');
});
Cypress.Commands.add('navigateToDebugTab' as keyof Chainable, () => {
  navigateToTab('debug');
});

Cypress.Commands.add('createReport' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Simple%20report`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add('createOtherReport' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Another%20simple%20report`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add(
  'createReportInDatabaseStorage' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Add%20report%20to%20database%20storage`,
    ).then((resp: Cypress.Response<ApiResponse>) => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add('createRunningReport' as keyof Chainable, (): void => {
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Waiting%20for%20thread%20to%20start`,
  ).then((resp: Cypress.Response<ApiResponse>): void => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add(
  'createReportWithLabelNull' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Message%20is%20null`,
    ).then((resp: Cypress.Response<ApiResponse>) => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add(
  'createReportWithLabelEmpty' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Message%20is%20an%20empty%20string`,
    ).then((resp: Cypress.Response<ApiResponse>): void => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add(
  'createReportWithInfopoint' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Hide%20a%20checkpoint%20in%20blackbox%20view`,
    ).then((resp: Cypress.Response<ApiResponse>): void => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add(
  'createReportWithMultipleStartpoints' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Multiple%20startpoints`,
    ).then((resp: Cypress.Response<ApiResponse>): void => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add(
  'createJsonReport' as keyof Chainable,
  (): void => {
    // No cy.visit because then the API call can happen multiple times.
    cy.request(
      `${Cypress.env('backendServer')}/index.jsp?createReport=Json%20checkpoint`,
    ).then((resp: Cypress.Response<ApiResponse>): void => {
      expect(resp.status).equal(200);
    });
  },
);

Cypress.Commands.add('createReportWithMessageContext' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Report%20with%20message%20context`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add('createReportOnlyLF' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Add%20report%20with%20checkpoints%20having%20only%20LF%200x0A`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add('createReportOnlyCR' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Add%20report%20with%20checkpoints%20having%20only%20CR%200x0D`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add('createReportWithStatusError' as keyof Chainable, (): void => {
  // No cy.visit because then the API call can happen multiple times.
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?createReport=Complex%20error%20report`,
  ).then((resp: Cypress.Response<ApiResponse>) => {
    expect(resp.status).equal(200);
  });
})

Cypress.Commands.add('clearDebugStore' as keyof Chainable, (): void => {
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?clearDebugStorage=true`,
  ).then((resp: Cypress.Response<ApiResponse>): void => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add('clearReportsInProgress' as keyof Chainable, (): void => {
  cy.request(
    `${Cypress.env('backendServer')}/index.jsp?removeReportsInProgress`,
  ).then((resp: Cypress.Response<ApiResponse>): void => {
    expect(resp.status).equal(200);
  });
});

Cypress.Commands.add(
  'selectIfNotSelected' as keyof Chainable,
  { prevSubject: 'element' },
  (node: JQueryWithSelector<HTMLElement>): void => {
    if (!node[0].classList.contains('selected')) {
      cy.wrap(node).click();
    }
  },
);

Cypress.Commands.add(
  'checkTestTableNumRows' as keyof Chainable,
  (length: number): void => {
    cy.getTestTableRows().should('have.length', length);
  },
);

//Will not work with duplicate report names
Cypress.Commands.add(
  'checkTestTableReportsAre' as keyof Chainable,
  (reportNames: string[]): void => {
    cy.checkTestTableNumRows(reportNames.length);
    cy.get('[data-cy-test="tableRow"]').should($rows => {
      // Check each report name exists in the rows
      for (let reportName of reportNames){
        expect($rows.text()).to.include(reportName);
        // Please mind that we don't want null. There was an issue about this in the past.
        expect($rows.text()).not.to.include('null');
      }
    });
  },
);

Cypress.Commands.add(
  'debugTreeGuardedCopyReport' as keyof Chainable,
  (reportName: string, numExpandedNodes: number, aliasSuffix: string): void => {
    const alias = `debugTreeGuardedCopyReport_${aliasSuffix}`;
    cy.get('[data-cy-debug-tree="root"]')
      .find(`app-tree-item .item-name:contains(${reportName})`)
      .should('have.length', numExpandedNodes);
    cy.intercept({
      method: 'PUT',
      hostname: 'localhost',
      url: /\/api\/report\/store\/*?/g,
      times: 1,
    }).as(alias);
    cy.get('[data-cy-debug-editor="copy"]').click();
    cy.wait(`@${alias}`).then((res: Interception): void => {
      cy.wrap(res).its('request.url').should('contain', 'Test');
      cy.wrap(res).its('request.body').as('requestBody');
      cy.get('@requestBody').its('Debug').should('have.length', 1);
      cy.wrap(res).its('response.statusCode').should('equal', 200);
      cy.log('Api call to copy report has been completed');
    });
  },
);

Cypress.Commands.add('clickRootNodeInFileTree' as keyof Chainable, (): void => {
  cy.wait(200);
  cy.get('[data-cy-debug-tree="root"] > app-tree-item')
    .eq(0)
    .find('.sft-item')
    .eq(0)
    .click();
});

Cypress.Commands.add('clickEndCheckpointOfThreeNodeReport' as keyof Chainable, (): void => {
  cy.get('[data-cy-debug-tree="root"] > app-tree-item')
    .eq(0)
    .find('app-tree-item')
    .eq(0)
    .find('app-tree-item')
    .eq(0)
    .find('.sft-item')
    .eq(0)
    .click();
});

Cypress.Commands.add('getShownNodesOfReportTreeWithText' as keyof Chainable, (text): void => {
  cy.get('[data-cy-debug-tree="root"] app-tree-icon:visible').parent().find(`:contains(${text})`);
});

Cypress.Commands.add('checkShownNodeWithTextSelected' as keyof Chainable, (reportName: string, index: number, selected: boolean): void => {
  const predicate: string = selected === true ? 'have.class' : 'not.have.class';
  cy.getShownNodesOfReportTreeWithText(reportName).eq(index).parent().should(predicate, TREE_ITEM_SELECTED_CLASS);
})

Cypress.Commands.add('checkShownNodeWithTextSearched' as keyof Chainable, (reportName: string, searched: boolean): void => {
  const predicate: string = searched === true ? 'have.css' : 'not.have.css';
  cy.getShownNodesOfReportTreeWithText(reportName).parent().should(predicate, 'color', 'rgb(255, 0, 0)');
})

Cypress.Commands.add('collapseNode' as keyof Chainable, (text, index): void => {
  cy.getShownNodesOfReportTreeWithText(text)
    .eq(index)
    .parent()
    .find('.sft-chevron-container')
    .should('have.length', 1)
    .click();
})

Cypress.Commands.add('expandNode' as keyof Chainable, (text, index): void => {
  cy.getShownNodesOfReportTreeWithText(text)
    .eq(index)
    .parent()
    .find('.bi-chevron-right')
    .should('have.length', 1)
    .click();
})

Cypress.Commands.add(
  'clickRowInTable' as keyof Chainable,
  (index: number): void => {
    cy.getDebugTableRows().eq(index).click();
  },
);

Cypress.Commands.add(
  'checkFileTreeLength' as keyof Chainable,
  (length: number): void => {
    cy.get('[data-cy-debug-tree="root"] > app-tree-item').should(
      'have.length',
      length,
    );
  },
);

Cypress.Commands.add('refreshApp' as keyof Chainable, (): void => {
  cy.get('[data-cy-debug="refresh"]').click();
  awaitLoadingSpinner();
});

Cypress.Commands.add('getDebugTableRows' as keyof Chainable, (): Chainable => {
  return cy.get('[data-cy-debug="tableRow"]');
});

Cypress.Commands.add('checkDebugTableRowsAre' as keyof Chainable, (reportNames: string[]): Chainable => {
  const NAME_COLUMN_INDEX = 4;
  cy.getDebugTableRows().should('have.length', reportNames.length);
  for (let rowIndex = 0; rowIndex < reportNames.length; ++rowIndex) {
    cy.getDebugTableRows()
      .eq(rowIndex)
      .find('td')
      .eq(NAME_COLUMN_INDEX)
      .invoke('text')
      .should('contain', reportNames[rowIndex]);
  }
})

Cypress.Commands.add('getTestTableRows' as keyof Chainable, (): Chainable => {
  return cy.get('[data-cy-test="tableRow"]');
});

Cypress.Commands.add(
  'assertDebugTableLength' as keyof Chainable,
  (length: number): void => {
    length === 0
      ? cy.getDebugTableRows().should('not.exist')
      : cy.getDebugTableRows().should('have.length', length);
  },
);

Cypress.Commands.add(
  'selectRowInDebugTable' as keyof Chainable,
  (index: number): Chainable => {
    cy.get('[data-cy-debug="selectOne"]').eq(index).click();
  },
);

Cypress.Commands.add(
  'selectRowInTestTable' as keyof Chainable,
  (index: number): Chainable => {
    cy.get('[data-cy-test="selectOne"]').eq(index).click();
  },
);

Cypress.Commands.add('copyReportsToTestTab' as keyof Chainable, (names: string[]): Chainable => {
  for (let name of names) {
    cy.get('[data-cy-debug="tableRow"]').find(`td:contains(${name})`).click()
    cy.get('[data-cy-debug-editor="copy"]').click();
  }
})

Cypress.Commands.add('editCheckpointValue' as keyof Chainable, (value: string): Chainable => {
  // Some slack, give app time to recognize item as type-able.
  cy.wait(500);
  cy.window().then(win => {
    const editor = win.monaco.editor.getEditors()[0];
    editor.executeEdits('', [{
      range: editor.getModel().getFullModelRange(),
      text: value
    }]);
  });
});

Cypress.Commands.add('enterSettingsDialogAndExpectReportGenerator' as keyof Chainable, (text: string): Chainable => {
  cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
  cy.get('[data-cy-settings="nav-server"]').click();
  cy.get('[data-cy-settings="generatorEnabled"] option:selected').should('have.text', text);
});

Cypress.Commands.add('debugTabBackToFactorySettings' as keyof Chainable, (): Chainable => {
  cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
  cy.get('[data-cy-settings="factoryReset"]').click();
})

Cypress.Commands.add('checkNavTab' as keyof Chainable, (index: number, text: string, selected: boolean) => {
  cy.get(`[data-cy-nav-tab]:eq(${index})`).should('contain.text', text);
  if (selected) {
    cy.get(`[data-cy-nav-tab]:eq(${index})`).find('.active').should('be.visible');
  } else {
    cy.get(`[data-cy-nav-tab]:eq(${index})`).find('.active').should('not.exist');
  }
})

Cypress.Commands.add('windowSendPostReportEvent' as keyof Chainable, (storageName: string, storageId: number) => {
  cy.window().then(win => {
      win.postMessage({ action: 'ladybug-openReport', storageName: storageName, storageId: storageId }, '*');
  });
})

Cypress.Commands.add('uploadTwoReportsAndCheckTabs', () => {
  cy.fixture('twoReports.zip', 'binary')
    .then(Cypress.Blob.binaryStringToBlob)
    .then((fileContent) => {
      cy.get('[data-cy-debug="upload"]').find('input').attachFile({
        fileContent,
        fileName: 'twoReports.zip',
      });
    });
  cy.get('[data-cy-nav-tab]').should('have.length', 4);
  cy.checkNavTab(0, 'Debug', false);
  cy.checkNavTab(1, 'Test', false);
  cy.checkNavTab(2, 'Adapter1a', false);
  cy.checkNavTab(3, 'Adapter1b', true);
})

function awaitLoadingSpinner(): void {
  cy.get('[data-cy-loading-spinner]', { timeout: 10000 }).should('not.exist');
}

//More string values can be added for each tab that can be opened
function navigateToTabAndAwaitLoadingSpinner(tab: TabType): void {
  cy.visit('');
  cy.get(`[data-cy-nav-tab="${tab}"]`).click();
  awaitLoadingSpinner();
}

function navigateToTab(tab: TabType): void {
  cy.get(`[data-cy-nav-tab="${tab}"]`).click();
}

interface ApiResponse {
  status: boolean;
}
