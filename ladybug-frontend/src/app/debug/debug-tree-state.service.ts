import { inject, Injectable, OnDestroy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, EMPTY, map, Observable, of, Subject, Subscription, switchMap } from 'rxjs';
import { HttpService } from '../shared/services/http.service';
import { ToastService } from '../shared/services/toast.service';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { View } from '../shared/interfaces/view';

// A report that can be shown in the debug tree.
export type TreeTarget =
  | { kind: 'stored'; storageName: string; storageId: number }
  | { kind: 'inProgress'; index: number };

export type TreeCommand =
  | {
      command: 'new' | 'update';
      report: HierarchicalReport;
      // False when replacing edits is expected, for example after saving them.
      warnIfEdited: boolean;
    }
  | { command: 'close' };

interface RenderRequest {
  target: TreeTarget;
  view: View;
  // When present, show this report instead of fetching it.
  report?: HierarchicalReport;
  warnIfEdited: boolean;
}

type RenderResult = { request: RenderRequest; report: HierarchicalReport } | { request: RenderRequest; error: unknown };

// Owns what the debug tree shows and how that relates to the debug table.
//
// Every user event (change view, open a report, open a report in progress,
// close, save, delete) first updates the desired state synchronously. Then,
// if the tree has to change, a render request is posted. Rendering may need
// HTTP traffic; while that is going on, the tree is out of sync. Render
// requests go through switchMap, so a new request cancels the previous one
// and only the response to the last request is applied. Because the desired
// state was updated before posting, the last request always reflects all
// earlier events, and applying its response brings the tree in sync.
//
// The desired state is changed only by methods that also post a render
// request or a cancellation, except for view changes that keep the storage
// while no stored report is desired; those do not affect what the tree shows.
// Hence a response always belongs to the current desired state.
@Injectable()
export class DebugTreeStateService implements OnDestroy {
  private httpService = inject(HttpService);
  private toastService = inject(ToastService);

  private tableView?: View;
  // The report the tree should show, null when the tree should not follow the table.
  private desired: TreeTarget | null = null;
  // The report the tree shows, null when the tree is empty. When it differs
  // from desired, a render is pending or the tree is no longer linked.
  private shown: TreeTarget | null = null;
  private pending = false;

  // null cancels the pending render.
  private renderRequests = new Subject<RenderRequest | null>();
  private treeCommandSubject = new Subject<TreeCommand>();
  private subscription: Subscription;

  treeCommands$: Observable<TreeCommand> = this.treeCommandSubject.asObservable();

  constructor() {
    this.subscription = this.renderRequests
      .pipe(switchMap((request) => (request === null ? EMPTY : this.render(request))))
      .subscribe((result) => this.apply(result));
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  // The storage id of the report in the table that the tree shows or is about to show.
  get linkedStorageId(): number | null {
    return this.desired?.kind === 'stored' ? this.desired.storageId : null;
  }

  changeView(view: View): void {
    const previousTableView = this.tableView;
    this.tableView = view;
    if (this.desired?.kind !== 'stored') {
      // A report in progress does not depend on the view.
      return;
    }
    if (view.storageName === previousTableView?.storageName) {
      this.post({ target: this.desired, view, warnIfEdited: true });
    } else {
      // The table no longer shows the storage of the desired report. Keep
      // what the tree shows, but stop linking it to the table.
      if (this.pending && !sameTarget(this.desired, this.shown)) {
        this.toastService.showWarning(
          `Did not open report with storage id ${this.desired.storageId}, because the view changed to another storage`,
        );
      }
      this.desired = null;
      this.cancel();
    }
  }

  openStoredReport(storageId: number): void {
    const view = this.requireTableView();
    this.desired = { kind: 'stored', storageName: view.storageName, storageId };
    this.post({ target: this.desired, view, warnIfEdited: true });
  }

  openReportInProgress(index: number): void {
    this.desired = { kind: 'inProgress', index };
    this.post({ target: this.desired, view: this.requireTableView(), warnIfEdited: true });
  }

  // The tree was closed by the user.
  closed(): void {
    this.desired = null;
    this.shown = null;
    this.cancel();
  }

  // The user saved changes to the shown report, which was fetched again as given.
  saved(report: HierarchicalReport): void {
    if (this.desired?.kind === 'stored') {
      // Fetch again, because the view may have changed after saving.
      this.post({ target: this.desired, view: this.requireTableView(), warnIfEdited: false });
    } else if (this.shown !== null) {
      // The tree is not linked to the table, so the view does not matter.
      this.post({ target: this.shown, view: this.requireTableView(), report, warnIfEdited: false });
    }
  }

  // Reports were deleted from the given storage, null storage ids means all.
  deleted(storageName: string, storageIds: number[] | null): void {
    const desired = this.desired;
    if (
      desired?.kind === 'stored' &&
      desired.storageName === storageName &&
      (storageIds === null || storageIds.includes(desired.storageId))
    ) {
      this.toastService.showWarning(`Closed report with storage id ${desired.storageId}, because it was deleted`);
      this.desired = null;
      this.shown = null;
      this.cancel();
      this.treeCommandSubject.next({ command: 'close' });
    }
  }

  // A report in progress was deleted. If the tree shows a report in progress,
  // it cannot tell whether that is the deleted one: indexes are only valid at
  // the moment of fetching, see sameTarget(). So warn when the tree shows or is
  // about to show any report in progress.
  reportInProgressDeleted(index: number): void {
    if (this.desired?.kind === 'inProgress' || this.shown?.kind === 'inProgress') {
      this.toastService.showWarning(
        `Deleted report in progress with index [${index}]. The debug tree shows a report in progress that may be the deleted one. If not, its index may have changed`,
      );
    }
  }

  private post(request: RenderRequest): void {
    this.pending = true;
    this.renderRequests.next(request);
  }

  private cancel(): void {
    this.pending = false;
    this.renderRequests.next(null);
  }

  private requireTableView(): View {
    if (this.tableView === undefined) {
      throw new Error('DebugTreeStateService: no view yet');
    }
    return this.tableView;
  }

  private render(request: RenderRequest): Observable<RenderResult> {
    let report$: Observable<HierarchicalReport>;
    if (request.report !== undefined) {
      report$ = of(request.report);
    } else if (request.target.kind === 'stored') {
      report$ = this.httpService
        .getHierarchicalReports([request.target.storageId], request.target.storageName, request.view.name)
        .pipe(map((reports) => reports[0]));
    } else {
      report$ = this.httpService.getReportInProgress(request.target.index);
    }
    return report$.pipe(
      map((report): RenderResult => ({ request, report })),
      catchError((error: unknown) => of<RenderResult>({ request, error })),
    );
  }

  private apply(result: RenderResult): void {
    this.pending = false;
    const target = result.request.target;
    if ('error' in result) {
      console.warn(result.error);
      // The tree keeps what it shows, but no longer follows the table.
      this.desired = null;
      const detail = result.error instanceof HttpErrorResponse ? result.error.message : String(result.error);
      const consequence =
        this.shown === null ? '' : ', the debug tree keeps its report but no longer follows the table';
      this.toastService.showDanger(`Could not show ${describe(target)}${consequence}`, detail);
      return;
    }
    const command = sameTarget(target, this.shown) ? 'update' : 'new';
    this.shown = target;
    this.treeCommandSubject.next({ command, report: result.report, warnIfEdited: result.request.warnIfEdited });
    if (target.kind === 'inProgress' && result.request.report === undefined) {
      this.toastService.showSuccess(`Opened report in progress with index [${target.index}]`);
    }
  }
}

function sameTarget(a: TreeTarget | null, b: TreeTarget | null): boolean {
  if (a === null || b === null) {
    return false;
  }
  if (a.kind === 'stored' && b.kind === 'stored') {
    return a.storageName === b.storageName && a.storageId === b.storageId;
  }
  // An index identifies a report in progress only at the moment it is fetched.
  // When a report in progress is deleted or finishes, the reports after it get
  // a lower index. So two reports in progress are never known to be the same.
  return false;
}

function describe(target: TreeTarget): string {
  return target.kind === 'stored'
    ? `report with storage id ${target.storageId}`
    : `report in progress with index [${target.index}]`;
}
