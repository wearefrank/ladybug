/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, inject, Input, OnDestroy, Output, ViewChild } from '@angular/core';
import { Report } from '../../shared/interfaces/report';
import { catchError, firstValueFrom, Observable, Subscription } from 'rxjs';
import { HttpService } from '../../shared/services/http.service';
import { SettingsService } from '../../shared/services/settings.service';
import { CreateTreeItem, FileTreeItem, FileTreeOptions, NgSimpleFileTree } from 'ng-simple-file-tree';
import { ReportHierarchyTransformer } from '../../shared/classes/report-hierarchy-transformer';
import { SimpleFileTreeUtil as SimpleFileTreeUtility } from '../../shared/util/simple-file-tree-util';
import { View } from '../../shared/interfaces/view';
import { DebugTabService } from '../debug-tab.service';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { RefreshCondition } from '../../shared/interfaces/refresh-condition';

@Component({
  selector: 'app-debug-tree',
  templateUrl: './debug-tree.component.html',
  styleUrls: ['./debug-tree.component.css'],
  standalone: true,
  imports: [NgSimpleFileTree],
})
export class DebugTreeComponent implements OnDestroy {
  @ViewChild('tree') tree!: NgSimpleFileTree;
  @Input() adjustWidth: Observable<void> = {} as Observable<void>;
  @Output() selectReportEvent = new EventEmitter<Report>();
  @Output() closeEntireTreeEvent = new EventEmitter<any>();

  showMultipleAtATime!: boolean;
  subscriptions: Subscription = new Subscription();
  treeOptions: FileTreeOptions = {
    hierarchyLines: {
      vertical: true,
    },
    highlightOpenFolders: false,
    folderBehaviourOnClick: 'select',
    doubleClickToOpenFolders: false,
    autoOpenCondition: this.conditionalOpenFunction,
    determineIconClass: SimpleFileTreeUtility.conditionalCssClass,
  };

  private _currentView!: View;
  private lastReport?: Report | null;

  private httpService = inject(HttpService);
  private settingsService = inject(SettingsService);
  private debugTab = inject(DebugTabService);
  private errorHandler = inject(ErrorHandling);

  constructor() {
    this.subscribeToSubscriptions();
  }

  @Input({ required: true }) set currentView(value: View) {
    if (this._currentView !== value) {
      // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1125.
      this.hideOrShowCheckpointsBasedOnView(value);
    }
    this._currentView = value;
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToSubscriptions(): void {
    const showMultipleSubscription: Subscription = this.settingsService.showMultipleAtATimeObservable.subscribe({
      next: (value: boolean) => {
        this.showMultipleAtATime = value;
        if (!this.showMultipleAtATime) {
          this.removeAllReportsButOne();
        }
      },
    });
    this.subscriptions.add(showMultipleSubscription);
    const refreshAll: Subscription = this.debugTab.refreshAll$.subscribe((condition?: RefreshCondition) =>
      this.refreshReports(condition),
    );
    this.subscriptions.add(refreshAll);
    const refreshTree: Subscription = this.debugTab.refreshTree$.subscribe((condition?: RefreshCondition) =>
      this.refreshReports(condition),
    );
    this.subscriptions.add(refreshTree);
  }

  hideOrShowCheckpointsBasedOnView(currentView: View): void {
    if (this.tree) {
      this.checkUnmatchedCheckpoints(this.getTreeReports(), currentView);
    }
  }

  checkUnmatchedCheckpoints(reports: Report[], currentView: View): void {
    for (let report of reports) {
      if (report.storageName === currentView.storageName) {
        this.httpService
          .getUnmatchedCheckpoints(report.storageName, report.storageId, currentView.name)
          .pipe(catchError(this.errorHandler.handleError()))
          .subscribe({
            next: (unmatched: string[]) =>
              SimpleFileTreeUtility.hideOrShowCheckpoints(unmatched, this.tree.elements.toArray()),
          });
      }
    }
  }

  getTreeReports(): Report[] {
    const reports: Report[] = [];
    for (const item of this.tree.items) {
      if (item.originalValue.storageId != undefined) {
        reports.push(item.originalValue);
      }
    }
    return reports;
  }

  removeAllReportsButOne(): void {
    if (this.tree) {
      this.tree.clearItems();
    }
    if (this.lastReport) {
      this.addReportToTree(this.lastReport);
    }
  }

  addReportToTree(report: Report): void {
    if (this.selectAndReplaceReportIfPresent(report)) {
      return;
    }
    this.lastReport = report;
    if (!this.showMultipleAtATime) {
      this.tree.clearItems();
    }
    const newReport: CreateTreeItem = new ReportHierarchyTransformer().transform(report);
    const rootNodePath: string = this.tree.addItem(newReport);
    this.selectFirstCheckpoint(rootNodePath);
    if (this._currentView) {
      this.hideOrShowCheckpointsBasedOnView(this._currentView);
    }
  }

  closeEntireTree(): void {
    this.debugTab.setAnyReportsOpen(false);
    this.closeEntireTreeEvent.emit();
    this.tree.clearItems();
    this.lastReport = null;
  }

  changeSearchTerm(event: KeyboardEvent): void {
    const term: string = (event.target as HTMLInputElement).value;
    this.tree.searchTree(term);
  }

  conditionalOpenFunction(item: CreateTreeItem): boolean {
    const type = item['type'];
    return type === undefined || type === 1 || type === 2;
  }

  selectAndReplaceReportIfPresent(report: Report): boolean {
    for (const index in this.tree.items) {
      const treeReport: Report = this.tree.items[index].originalValue;
      if (treeReport.storageId === report.storageId) {
        const transformedReport = new ReportHierarchyTransformer().transform(report);
        this.tree.items[index] = this.tree.createItemToFileItem(transformedReport);
        this.tree.selectItem(this.tree.items[index].path);
        return true;
      }
    }
    return false;
  }

  async refreshReports(condition?: RefreshCondition): Promise<void> {
    const selectedReportId: number = this.tree.getSelected().originalValue.storageId;
    let lastSelectedReport: FileTreeItem | undefined;

    const shouldProcessReport = (reportId: number): boolean =>
      !condition?.reportIds || condition.reportIds.includes(reportId);

    for (const index in this.tree.items) {
      const report: Report = this.tree.items[index].originalValue as Report;

      if (shouldProcessReport(report.storageId)) {
        const fileItem: FileTreeItem = await this.getNewReport(report.storageId);

        if (selectedReportId === report.storageId) {
          lastSelectedReport = fileItem;
        }

        this.tree.items[index] = fileItem;
      }
    }

    if (lastSelectedReport) {
      this.tree.selectItem(lastSelectedReport.path);
    }

    this.hideOrShowCheckpointsBasedOnView(this._currentView);
  }

  async getNewReport(storageId: number): Promise<FileTreeItem> {
    const response: Report = await firstValueFrom(
      this.httpService
        .getReport(storageId, this._currentView.storageName)
        .pipe(catchError(this.errorHandler.handleError())),
    );
    const transformedReport: Report = new ReportHierarchyTransformer().transform(response);
    return this.tree.createItemToFileItem(transformedReport);
  }

  private selectFirstCheckpoint(rootNodePath: string): void {
    const last = this.tree.items.length - 1;
    const lastAdded = this.tree.items[last];
    if (lastAdded.children) {
      const firstCheckpoint = lastAdded.children[0];
      this.tree.selectItem(firstCheckpoint.path);
    } else {
      this.tree.selectItem(rootNodePath);
    }
  }
}
