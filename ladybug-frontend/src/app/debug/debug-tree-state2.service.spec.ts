import { TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { DebugTreeStateService2, TreeCommand } from './debug-tree-state2.service';
import { HttpService } from '../shared/services/http.service';
import { ToastService } from '../shared/services/toast.service';
import { View } from '../shared/interfaces/view';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';

interface PendingRequest {
  description: string;
  response: Subject<HierarchicalReport[] | HierarchicalReport>;
}

// Each request gets a subject, so the test decides when and in which order responses arrive.
class FakeHttpService {
  requests: PendingRequest[] = [];

  getHierarchicalReports(
    storageIds: number[],
    storageName: string,
    viewName: string,
  ): Observable<HierarchicalReport[]> {
    return this.add(`${storageName}/${storageIds[0]}/${viewName}`) as Observable<HierarchicalReport[]>;
  }

  getReportInProgress(index: number): Observable<HierarchicalReport> {
    return this.add(`inProgress/${index}`) as Observable<HierarchicalReport>;
  }

  latest(description: string): PendingRequest {
    const request = this.requests.findLast((r) => r.description === description);
    if (request === undefined) {
      throw new Error(`No request ${description}, have ${this.requests.map((r) => r.description).join(', ')}`);
    }
    return request;
  }

  private add(description: string): Observable<HierarchicalReport[] | HierarchicalReport> {
    const response = new Subject<HierarchicalReport[] | HierarchicalReport>();
    this.requests.push({ description, response });
    return response;
  }
}

function view(name: string, storageName: string): View {
  return { name, storageName } as View;
}

function report(name: string): HierarchicalReport {
  return { name } as HierarchicalReport;
}

function respond(http: FakeHttpService, description: string, name: string): void {
  const request = http.latest(description);
  request.response.next(description.startsWith('inProgress') ? report(name) : [report(name)]);
  request.response.complete();
}

function fail(http: FakeHttpService, description: string): void {
  http.latest(description).response.error(new Error('failed'));
}

describe('DebugTreeStateService2', () => {
  const whiteBox = view('White box', 'Debug');
  const blackBox = view('Black box', 'Debug');
  const fileStorage = view('File storage', 'fileStorage');

  let service: DebugTreeStateService2;
  let http: FakeHttpService;
  let toast: jasmine.SpyObj<ToastService>;
  let commands: TreeCommand[];

  function shownNamesOf(commandsStore: TreeCommand[]): string[] {
    return commandsStore.map((c) => (c.command === 'close' ? c.command : `${c.command} ${c.report.name}`));
  }

  function createService(): DebugTreeStateService2 {
    http = new FakeHttpService();
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['showWarning', 'showDanger', 'showSuccess']);
    TestBed.configureTestingModule({
      providers: [
        DebugTreeStateService2,
        { provide: HttpService, useValue: http },
        { provide: ToastService, useValue: toast },
      ],
    });
    return TestBed.inject(DebugTreeStateService2);
  }

  describe('before a view is chosen', () => {
    beforeEach(() => {
      service = createService();
      commands = [];
      service.treeCommand$.subscribe((c) => commands.push(c));
    });

    it('closes the tree when the first view is chosen', () => {
      service.changeView(whiteBox);
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(service.openedStorageId).toBeNull();
    });

    it('refuses to open a stored report', () => {
      expect(() => service.openStoredReport(2)).toThrowError();
      expect(http.requests.length).toBe(0);
    });

    it('refuses to open a report in progress', () => {
      expect(() => service.openReportInProgress(1)).toThrowError();
      expect(http.requests.length).toBe(0);
    });

    it('refuses to close a report', () => {
      expect(() => service.closeReport()).toThrowError();
    });
  });

  describe('with a view', () => {
    beforeEach(() => {
      service = createService();
      commands = [];
      service.treeCommand$.subscribe((c) => commands.push(c));
      service.changeView(whiteBox);
      commands = [];
    });

    it('links the table to a clicked report before the response arrives', () => {
      service.openStoredReport(2);
      expect(service.openedStorageId).toBe(2);
      expect(shownNamesOf(commands)).toEqual([]);
      respond(http, 'Debug/2/White box', 'r2 white');
      expect(shownNamesOf(commands)).toEqual(['new r2 white']);
      expect(service.openedStorageId).toBe(2);
    });

    it('shows only the last clicked report when responses arrive in order', () => {
      service.openStoredReport(2);
      service.openStoredReport(3);
      respond(http, 'Debug/2/White box', 'r2 white');
      respond(http, 'Debug/3/White box', 'r3 white');
      expect(shownNamesOf(commands)).toEqual(['new r3 white']);
      expect(service.openedStorageId).toBe(3);
    });

    it('shows only the last clicked report when responses cross', () => {
      service.openStoredReport(2);
      service.openStoredReport(3);
      respond(http, 'Debug/3/White box', 'r3 white');
      respond(http, 'Debug/2/White box', 'r2 white');
      expect(shownNamesOf(commands)).toEqual(['new r3 white']);
      expect(service.openedStorageId).toBe(3);
    });

    it('updates the tree when the same report is clicked again', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'first');
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'second');
      expect(shownNamesOf(commands)).toEqual(['new first', 'update second']);
    });

    it('updates the tree when going back to the shown report while another one was fetched', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 first');
      service.openStoredReport(3);
      service.openStoredReport(2);
      respond(http, 'Debug/3/White box', 'r3 white');
      respond(http, 'Debug/2/White box', 'r2 second');
      expect(shownNamesOf(commands)).toEqual(['new r2 first', 'update r2 second']);
      expect(service.openedStorageId).toBe(2);
    });

    it('closes the report', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.closeReport();
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'close']);
      expect(service.openedStorageId).toBeNull();
    });

    it('ignores a response that arrives after the report was closed', () => {
      service.openStoredReport(2);
      service.closeReport();
      respond(http, 'Debug/2/White box', 'r2 white');
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(service.openedStorageId).toBeNull();
    });

    it('opens a new report after closing one', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.closeReport();
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 again');
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'close', 'new r2 again']);
    });

    it('closes the tree, unlinks the table and shows an error when fetching fails', () => {
      service.openStoredReport(2);
      fail(http, 'Debug/2/White box');
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(service.openedStorageId).toBeNull();
      expect(toast.showDanger).toHaveBeenCalledTimes(1);
      expect(toast.showDanger.calls.mostRecent().args[0]).toContain('storageId: 2');
    });

    it('keeps working after a failed fetch', () => {
      service.openStoredReport(2);
      fail(http, 'Debug/2/White box');
      service.openStoredReport(3);
      respond(http, 'Debug/3/White box', 'r3 white');
      expect(shownNamesOf(commands)).toEqual(['close', 'new r3 white']);
      expect(service.openedStorageId).toBe(3);
    });

    it('ignores a failure of a request that was replaced', () => {
      service.openStoredReport(2);
      service.openStoredReport(3);
      fail(http, 'Debug/2/White box');
      respond(http, 'Debug/3/White box', 'r3 white');
      expect(shownNamesOf(commands)).toEqual(['new r3 white']);
      expect(toast.showDanger).not.toHaveBeenCalled();
    });

    it('closes the tree, unlinks the table and shows an error when the server returns no report', () => {
      service.openStoredReport(2);
      const request = http.latest('Debug/2/White box');
      request.response.next([]);
      request.response.complete();
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(service.openedStorageId).toBeNull();
      expect(toast.showDanger).toHaveBeenCalledTimes(1);
      expect(toast.showDanger.calls.mostRecent().args[0]).toBe(
        'Could not show view: White box, storageId: 2: Server returned no report',
      );
    });

    it('keeps working after the server returned no report', () => {
      service.openStoredReport(2);
      const request = http.latest('Debug/2/White box');
      request.response.next([]);
      request.response.complete();
      service.openStoredReport(3);
      respond(http, 'Debug/3/White box', 'r3 white');
      expect(shownNamesOf(commands)).toEqual(['close', 'new r3 white']);
      expect(service.openedStorageId).toBe(3);
    });

    it('updates the report when the view changes within the storage', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.changeView(blackBox);
      expect(service.openedStorageId).toBe(2);
      respond(http, 'Debug/2/Black box', 'r2 black');
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'update r2 black']);
      expect(service.openedStorageId).toBe(2);
    });

    it('fetches a clicked report with the view chosen while it was fetched', () => {
      service.openStoredReport(2);
      service.changeView(blackBox);
      respond(http, 'Debug/2/White box', 'r2 white');
      respond(http, 'Debug/2/Black box', 'r2 black');
      expect(shownNamesOf(commands)).toEqual(['new r2 black']);
    });

    it('opens reports with the new view after the view changed', () => {
      service.changeView(blackBox);
      commands = [];
      service.openStoredReport(2);
      respond(http, 'Debug/2/Black box', 'r2 black');
      expect(shownNamesOf(commands)).toEqual(['new r2 black']);
    });

    it('closes the report when the view changes to another storage', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.changeView(fileStorage);
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'close']);
      expect(service.openedStorageId).toBeNull();
      expect(http.requests.length).toBe(1);
    });

    it('does not reopen the report when changing back to the original storage', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.changeView(fileStorage);
      service.changeView(whiteBox);
      expect(http.requests.length).toBe(1);
      expect(service.openedStorageId).toBeNull();
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'close', 'close']);
    });

    it('drops a clicked report when the view changes to another storage meanwhile', () => {
      service.openStoredReport(2);
      service.changeView(fileStorage);
      respond(http, 'Debug/2/White box', 'r2 white');
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(service.openedStorageId).toBeNull();
    });

    it('opens reports from the new storage after the storage changed', () => {
      service.changeView(fileStorage);
      commands = [];
      service.openStoredReport(2);
      respond(http, 'fileStorage/2/File storage', 'r2 file');
      expect(shownNamesOf(commands)).toEqual(['new r2 file']);
      expect(service.openedStorageId).toBe(2);
    });

    it('treats the same storage id in another storage as a new report', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 debug');
      service.changeView(fileStorage);
      service.openStoredReport(2);
      respond(http, 'fileStorage/2/File storage', 'r2 file');
      expect(shownNamesOf(commands)).toEqual(['new r2 debug', 'close', 'new r2 file']);
    });

    it('opens a report in progress without linking the table', () => {
      service.openReportInProgress(1);
      expect(service.openedStorageId).toBeNull();
      respond(http, 'inProgress/1', 'in progress');
      expect(shownNamesOf(commands)).toEqual(['new in progress']);
      expect(service.openedStorageId).toBeNull();
    });

    it('unlinks the table when a report in progress replaces a stored report', () => {
      service.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.openReportInProgress(1);
      expect(service.openedStorageId).toBeNull();
      respond(http, 'inProgress/1', 'in progress');
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'new in progress']);
    });

    it('treats reopening the same report in progress as a new report', () => {
      service.openReportInProgress(1);
      respond(http, 'inProgress/1', 'first');
      service.openReportInProgress(1);
      respond(http, 'inProgress/1', 'second');
      expect(shownNamesOf(commands)).toEqual(['new first', 'new second']);
    });

    it('ignores a report in progress replaced by a clicked stored report', () => {
      service.openReportInProgress(1);
      service.openStoredReport(2);
      respond(http, 'inProgress/1', 'in progress');
      respond(http, 'Debug/2/White box', 'r2 white');
      expect(shownNamesOf(commands)).toEqual(['new r2 white']);
      expect(service.openedStorageId).toBe(2);
    });

    it('shows an error with the index when fetching a report in progress fails', () => {
      service.openReportInProgress(1);
      fail(http, 'inProgress/1');
      expect(shownNamesOf(commands)).toEqual(['close']);
      expect(toast.showDanger.calls.mostRecent().args[0]).toContain('index: 1');
    });

    it('when view changes to same storage then report in progress not reopened', () => {
      service.openReportInProgress(1);
      respond(http, 'inProgress/1', 'first');
      service.changeView(blackBox);
      expect(http.requests.length).toBe(1);
      expect(shownNamesOf(commands)).toEqual(['new first']);
    });

    it('when the view changes to another storage then a report in progress is not reopened', () => {
      service.openReportInProgress(1);
      respond(http, 'inProgress/1', 'in progress');
      service.changeView(fileStorage);
      expect(http.requests.length).toBe(1);
      expect(shownNamesOf(commands)).toEqual(['new in progress']);
    });

    it('sends each command once to every subscriber and fetches once', () => {
      const other: TreeCommand[] = [];
      service.treeCommand$.subscribe((c) => other.push(c));
      service.openStoredReport(2);
      expect(http.requests.length).toBe(1);
      respond(http, 'Debug/2/White box', 'r2 white');
      service.openStoredReport(2);
      expect(http.requests.length).toBe(2);
      respond(http, 'Debug/2/White box', 'r2 again');
      expect(shownNamesOf(commands)).toEqual(['new r2 white', 'update r2 again']);
      // other gets every command, also the close that was thrown away for commands.
      expect(other[0].command).toEqual('close');
      other.shift();
      expect(other).toEqual(commands);
    });

    it('keeps history of report updates for when subscriber subscribes late', () => {
      const unsubscribed = createServiceWithoutSubscriber();
      unsubscribed.changeView(whiteBox);
      unsubscribed.openStoredReport(2);
      respond(http, 'Debug/2/White box', 'r2 white');
      unsubscribed.openStoredReport(2);
      const late: TreeCommand[] = [];
      unsubscribed.treeCommand$.subscribe((c) => late.push(c));
      respond(http, 'Debug/2/White box', 'r2 again');
      expect(shownNamesOf(late)).toEqual(['close', 'new r2 white', 'update r2 again']);
    });

    it('stops fetching and emitting after it is destroyed', () => {
      service.openStoredReport(2);
      service.ngOnDestroy();
      respond(http, 'Debug/2/White box', 'r2 white');
      service.openStoredReport(3);
      expect(shownNamesOf(commands)).toEqual([]);
      expect(http.requests.length).toBe(1);
    });
  });

  function createServiceWithoutSubscriber(): DebugTreeStateService2 {
    TestBed.resetTestingModule();
    return createService();
  }
});
