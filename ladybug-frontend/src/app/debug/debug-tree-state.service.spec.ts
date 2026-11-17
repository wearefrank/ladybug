import { TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { DebugTreeStateService, TreeCommand } from './debug-tree-state.service';
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

describe('DebugTreeStateService', () => {
  const whiteBox = view('White box', 'Debug');
  const blackBox = view('Black box', 'Debug');
  const fileStorage = view('File storage', 'fileStorage');

  let service: DebugTreeStateService;
  let http: FakeHttpService;
  let toast: jasmine.SpyObj<ToastService>;
  let commands: TreeCommand[];

  function shownNames(): string[] {
    return commands.map((c) => (c.command === 'close' ? 'close' : `${c.command} ${c.report.name}`));
  }

  beforeEach(() => {
    http = new FakeHttpService();
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['showWarning', 'showDanger', 'showSuccess']);
    TestBed.configureTestingModule({
      providers: [
        DebugTreeStateService,
        { provide: HttpService, useValue: http },
        { provide: ToastService, useValue: toast },
      ],
    });
    service = TestBed.inject(DebugTreeStateService);
    commands = [];
    service.treeCommands$.subscribe((c) => commands.push(c));
    service.changeView(whiteBox);
  });

  it('opens a report and links it to the table', () => {
    service.openStoredReport(2);
    expect(service.linkedStorageId).toBe(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    expect(shownNames()).toEqual(['new r2 white']);
  });

  it('updates the linked report when the view changes within the storage', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.changeView(blackBox);
    respond(http, 'Debug/2/Black box', 'r2 black');
    expect(shownNames()).toEqual(['new r2 white', 'update r2 black']);
    expect(service.linkedStorageId).toBe(2);
  });

  it('keeps the report but unlinks it when the view changes to another storage', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.changeView(fileStorage);
    expect(service.linkedStorageId).toBeNull();
    service.changeView(whiteBox);
    expect(http.requests.length).toBe(1);
    expect(shownNames()).toEqual(['new r2 white']);
  });

  it('opens a clicked report for the view chosen while it was fetched', () => {
    service.openStoredReport(2);
    service.changeView(blackBox);
    respond(http, 'Debug/2/White box', 'r2 white');
    respond(http, 'Debug/2/Black box', 'r2 black');
    expect(shownNames()).toEqual(['new r2 black']);
  });

  it('does not open a clicked report when the view changes to another storage meanwhile', () => {
    service.openStoredReport(2);
    service.changeView(fileStorage);
    respond(http, 'Debug/2/White box', 'r2 white');
    expect(shownNames()).toEqual([]);
    expect(toast.showWarning).toHaveBeenCalled();
  });

  it('ignores the view change fetch of a report replaced meanwhile', () => {
    service.changeView(blackBox);
    service.openStoredReport(2);
    respond(http, 'Debug/2/Black box', 'r2 black');
    service.changeView(whiteBox);
    service.openStoredReport(0);
    respond(http, 'Debug/0/White box', 'r0 white');
    respond(http, 'Debug/2/White box', 'r2 white');
    expect(shownNames()).toEqual(['new r2 black', 'new r0 white']);
    expect(service.linkedStorageId).toBe(0);
  });

  it('ignores responses after the report was closed', () => {
    service.openStoredReport(2);
    service.closed();
    respond(http, 'Debug/2/White box', 'r2 white');
    expect(shownNames()).toEqual([]);
    expect(service.linkedStorageId).toBeNull();
  });

  it('opens a report in progress without linking it and keeps it on view changes', () => {
    service.openReportInProgress(1);
    respond(http, 'inProgress/1', 'in progress');
    service.changeView(blackBox);
    expect(http.requests.length).toBe(1);
    expect(service.linkedStorageId).toBeNull();
    expect(shownNames()).toEqual(['new in progress']);
  });

  it('keeps the report but unlinks it when fetching for a new view fails', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.changeView(blackBox);
    http.latest('Debug/2/Black box').response.error(new Error('failed'));
    expect(service.linkedStorageId).toBeNull();
    expect(toast.showDanger).toHaveBeenCalled();
    expect(shownNames()).toEqual(['new r2 white']);
  });

  it('closes the linked report when it is deleted', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.deleted('Debug', [1, 2]);
    expect(shownNames()).toEqual(['new r2 white', 'close']);
    expect(service.linkedStorageId).toBeNull();
    expect(toast.showWarning).toHaveBeenCalled();
  });

  it('warns when a report in progress is deleted while the tree shows one', () => {
    service.openReportInProgress(1);
    respond(http, 'inProgress/1', 'in progress');
    service.reportInProgressDeleted(2);
    expect(toast.showWarning).toHaveBeenCalled();
    expect(shownNames()).toEqual(['new in progress']);
  });

  it('treats reopening the same index of a report in progress as a new report', () => {
    service.openReportInProgress(1);
    respond(http, 'inProgress/1', 'first');
    service.openReportInProgress(1);
    respond(http, 'inProgress/1', 'second');
    expect(shownNames()).toEqual(['new first', 'new second']);
  });

  it('warns when a report in progress is deleted while one is being opened', () => {
    service.openReportInProgress(1);
    service.reportInProgressDeleted(1);
    expect(toast.showWarning).toHaveBeenCalled();
  });

  it('does not warn when a report in progress is deleted while the tree shows a stored report', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.reportInProgressDeleted(1);
    expect(toast.showWarning).not.toHaveBeenCalled();
  });

  it('does not close the report when other reports or other storages are deleted', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.deleted('Debug', [1]);
    service.deleted('fileStorage', null);
    expect(shownNames()).toEqual(['new r2 white']);
  });

  it('refetches the linked report after saving, without warning about edits', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.saved(report('r2 saved'));
    respond(http, 'Debug/2/White box', 'r2 refetched');
    expect(shownNames()).toEqual(['new r2 white', 'update r2 refetched']);
    expect((commands[1] as { warnIfEdited: boolean }).warnIfEdited).toBeFalse();
  });

  it('shows the saved report as given when the tree is not linked', () => {
    service.openStoredReport(2);
    respond(http, 'Debug/2/White box', 'r2 white');
    service.changeView(fileStorage);
    service.saved(report('r2 saved'));
    expect(shownNames()).toEqual(['new r2 white', 'update r2 saved']);
  });
});
