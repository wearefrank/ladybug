import { Subject, Observable, switchMap, of, map, catchError, Subscription, ReplaySubject, EMPTY } from 'rxjs';
import { inject, Injectable, OnDestroy } from '@angular/core';
import { View } from '../shared/interfaces/view';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { HttpService } from '../shared/services/http.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '../shared/services/toast.service';

export type TreeItem =
  | { kind: 'closed' }
  | { kind: 'stored'; storageId: number }
  | { kind: 'in-progress'; index: number; atMoment: number };

export interface TreeTarget {
  readonly view: View;
  readonly treeItem: TreeItem;
}

export type TreeCommand =
  | {
      command: 'new' | 'update';
      report: HierarchicalReport;
    }
  | { command: 'close' };

type ReportFetch = { result: 'success'; report: HierarchicalReport } | { result: 'error'; error: unknown };

@Injectable()
export class DebugTreeStateService2 implements OnDestroy {
  private httpService = inject(HttpService);
  private toastService = inject(ToastService);
  private shown: TreeTarget | null = null;
  private desired: TreeTarget | null = null;

  get openedStorageId(): number | null {
    if (this.desired === null) {
      return null;
    }
    if (this.desired.treeItem.kind === 'stored') {
      return this.desired.treeItem.storageId;
    }
    return null;
  }

  private treeTargetSubject = new Subject<TreeTarget>();
  private treeCommandBase$: Observable<TreeCommand> = this.treeTargetSubject.pipe(
    switchMap((target) => this.render(target)),
  );
  private treeCommandSubject = new ReplaySubject<TreeCommand>();
  treeCommand$ = this.treeCommandSubject.asObservable();

  private static _MOMENT_COUNTER = 0;
  private static NEXT_MOMENT(): number {
    DebugTreeStateService2._MOMENT_COUNTER++;
    return DebugTreeStateService2._MOMENT_COUNTER;
  }

  private subscriptions = new Subscription();

  constructor() {
    this.subscriptions.add(this.treeCommandBase$.subscribe((command) => this.treeCommandSubject.next(command)));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  changeView(view: View): void {
    if (this.desired === null) {
      this.desired = { view, treeItem: { kind: 'closed' } };
    } else if (this.desired.view.storageName === view.storageName || this.desired.treeItem.kind === 'in-progress') {
      this.desired = { view, treeItem: { ...this.desired.treeItem } };
    } else {
      this.desired = { view, treeItem: { kind: 'closed' } };
    }
    this.treeTargetSubject.next(this.desired);
  }

  openStoredReport(storageId: number): void {
    if (this.desired === null) {
      throw new Error(`Cannot open report because there is no view - storageId [${storageId}]`);
    }
    this.desired = { view: this.desired.view, treeItem: { kind: 'stored', storageId } };
    this.treeTargetSubject.next(this.desired);
  }

  openReportInProgress(index: number): void {
    if (this.desired === null) {
      throw new Error(`Cannot open report-in-progress because there is no view - index [${index}]`);
    }
    this.desired = {
      view: this.desired.view,
      treeItem: { kind: 'in-progress', index, atMoment: DebugTreeStateService2.NEXT_MOMENT() },
    };
    this.treeTargetSubject.next(this.desired);
  }

  reportsDeleted(storageIds: number[]): void {
    if (
      this.desired !== null &&
      this.desired.treeItem.kind === 'stored' &&
      storageIds.includes(this.desired.treeItem.storageId)
    ) {
      this.closeReport();
    }
  }

  allReportsDeleted(): void {
    if (this.desired?.treeItem.kind === 'stored') {
      this.closeReport();
    }
  }

  closeReport(): void {
    if (this.desired === null) {
      throw new Error('Cannot close report - a report could not have been opened without a view');
    }
    this.desired = { view: this.desired.view, treeItem: { kind: 'closed' } };
    this.treeTargetSubject.next(this.desired);
  }

  saved(): void {
    if (this.shown === null || this.shown.treeItem.kind === 'closed') {
      throw new Error('Cannot happen - nothing was shown but that nothing was saved');
    }
    if (this.shown !== null && this.shown.treeItem.kind === 'stored') {
      if (this.shown.view.storageName === this.desired?.view.storageName) {
        this.openStoredReport(this.shown.treeItem.storageId);
      } else {
        this.toastService.showWarning(
          `Cannot show saved report [${this.shown.treeItem.storageId}] in storage [${this.shown.view.storageName}] because the view shows storage [${this.desired?.view.storageName}]`,
        );
        this.closeReport();
      }
    } else {
      this.toastService.showDanger('Programming error - a report-in-progress cannot be saved');
      this.closeReport();
      throw new Error('Saving a report-in-progress should not happen');
    }
  }

  private render(newTreeTarget: TreeTarget): Observable<TreeCommand> {
    if (newTreeTarget.treeItem.kind === 'closed') {
      this.updateStateForClose();
      return of({ command: 'close' });
    }
    if (newTreeTarget.treeItem.kind === 'stored') {
      return this.httpService
        .getHierarchicalReports(
          [newTreeTarget.treeItem.storageId],
          newTreeTarget.view.storageName,
          newTreeTarget.view.name,
        )
        .pipe(
          map((reports): ReportFetch => {
            if (reports.length === 0) {
              // Thrown so that catchError below handles it the same way as an HTTP error.
              throw new Error('Server returned no report');
            }
            return { result: 'success', report: reports[0] };
          }),
          catchError((error): Observable<ReportFetch> => of({ result: 'error', error })),
        )
        .pipe(map((reportOrError) => this.onReportReceivedOrError(reportOrError)));
    } else if (this.shown !== null && this.isSameInProgress(this.shown, newTreeTarget)) {
      // A report in progress does not depend on the view, so the tree already shows the right report.
      return EMPTY;
    } else {
      // Also reached when the view changes while a report in progress is being fetched:
      // switchMap cancels that request, so this.shown was never updated and
      // isSameInProgress() above is false. The report is then fetched again with the
      // same index. That is acceptable: an index identifies a report in progress only
      // at the moment of fetching, so the original request had the same risk of
      // hitting a different report; refetching only widens that window slightly.
      return this.httpService
        .getReportInProgress(newTreeTarget.treeItem.index)
        .pipe(
          map((report): ReportFetch => {
            return { result: 'success', report };
          }),
          catchError((error): Observable<ReportFetch> => of({ result: 'error', error })),
        )
        .pipe(map((reportOrError) => this.onReportReceivedOrError(reportOrError)));
    }
  }

  private onReportReceivedOrError(reportFetch: ReportFetch): TreeCommand {
    if (this.desired === null) {
      throw new Error('Cannot happen, report can only be requested when there is a view');
    }
    return reportFetch.result === 'success'
      ? this.onReportReceived(reportFetch.report, this.desired)
      : this.handleError(reportFetch.error);
  }

  private onReportReceived(report: HierarchicalReport, desired: TreeTarget): TreeCommand {
    if (this.shown === null) {
      throw new Error(
        'Cannot have received a report while "shown" is still null. When a view is set then a close command is produced that sets it.',
      );
    }
    const isSameStored: boolean =
      this.shown.treeItem.kind === 'stored' &&
      desired.treeItem.kind === 'stored' &&
      this.shown.view.storageName === desired.view.storageName &&
      this.shown.treeItem.storageId === desired.treeItem.storageId;
    this.desired = desired;
    this.shown = this.copy(this.desired);
    return isSameStored ? { command: 'update', report } : { command: 'new', report };
  }

  private handleError(error: unknown): TreeCommand {
    const detail = error instanceof HttpErrorResponse || error instanceof Error ? error.message : String(error);
    this.toastService.showDanger(`Could not show ${this.describe(this.desired)}: ${detail}`);
    this.updateStateForClose();
    return { command: 'close' };
  }

  private updateStateForClose(): void {
    if (this.desired === null) {
      throw new Error('Cannot happen, reports cannot be requested before there is a view');
    }
    this.desired = { view: this.desired.view, treeItem: { kind: 'closed' } };
    this.shown = this.copy(this.desired);
  }

  private describe(target: TreeTarget | null): string {
    if (target === null) {
      return 'null';
    } else if (target.treeItem.kind === 'stored') {
      return `view: ${target.view.name}, storageId: ${target.treeItem.storageId}`;
    } else if (target.treeItem.kind === 'in-progress') {
      return `view: ${target.view.name}, index: ${target.treeItem.index}`;
    } else {
      throw new Error(`Unknown TreeItem kind [${target.treeItem.kind}]`);
    }
  }

  private copy(target: TreeTarget): TreeTarget {
    return { view: target.view, treeItem: { ...target.treeItem } };
  }

  private isSameInProgress(first: TreeTarget, second: TreeTarget): boolean {
    return (
      first.treeItem.kind === 'in-progress' &&
      second.treeItem.kind === 'in-progress' &&
      first.treeItem.atMoment === second.treeItem.atMoment &&
      first.treeItem.index === second.treeItem.index
    );
  }
}
