/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HelperService } from '../../shared/services/helper.service';
import { HttpService } from '../../shared/services/http.service';
import { TableSettingsModalComponent } from '../table-settings-modal/table-settings-modal.component';
import { catchError, Subscription } from 'rxjs';
import { Report } from '../../shared/interfaces/report';
import { ToastService } from '../../shared/services/toast.service';
import { TabService } from '../../shared/services/tab.service';
import { AppVariablesService } from '../../shared/services/app.variables.service';
import { FilterService } from '../../shared/services/filter.service';
import { ActiveFiltersComponent } from '../active-filters/active-filters.component';
import { FormControl, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { View } from '../../shared/interfaces/view';
import { OptionsSettings } from '../../shared/interfaces/options-settings';
import { ErrorHandling } from 'src/app/shared/classes/error-handling.service';
import { CompareReport } from '../../shared/interfaces/compare-reports';
import { DebugTabService } from '../debug-tab.service';
import { ViewDropdownComponent } from '../../shared/components/view-dropdown/view-dropdown.component';
import { DeleteModalComponent } from '../../shared/components/delete-modal/delete-modal.component';
import { RefreshCondition } from '../../shared/interfaces/refresh-condition';
import { ClientSettingsService } from 'src/app/shared/services/client.settings.service';
import { HierarchicalReport } from '../../shared/interfaces/hierarchical-report';
import { CompareData } from '../../compare/compare-data';
import { DebugTableGridComponent } from '../debug-table-grid/debug-table-grid.component';
import { FilterSideDrawerComponent } from '../filter-side-drawer/filter-side-drawer.component';
import { DebugTreeStateService } from '../debug-tree-state.service';

@Component({
  selector: 'app-debug-table-with-controls',
  templateUrl: './debug-table-with-controls.component.html',
  styleUrls: ['./debug-table-with-controls.component.css'],
  standalone: true,
  imports: [
    FilterSideDrawerComponent,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    ReactiveFormsModule,
    FormsModule,
    ActiveFiltersComponent,
    TableSettingsModalComponent,
    ViewDropdownComponent,
    DeleteModalComponent,
    DebugTableGridComponent,
  ],
})
export class DebugTableWithControlsComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();
  private readonly defaultReportInProgressValidators: ValidatorFn[] = [
    Validators.min(1),
    Validators.pattern('^[0-9]*$'),
    Validators.required,
  ];

  // The debug tab waits until the views are known before creating this component.
  @Input({ required: true }) views!: View[];
  @Input({ required: true }) currentView!: View;
  @Output() viewChange: EventEmitter<View> = new EventEmitter<View>();

  @ViewChild(TableSettingsModalComponent)
  protected tableSettingsModal!: TableSettingsModalComponent;
  @ViewChild(DeleteModalComponent) protected deleteModal!: DeleteModalComponent;

  public helperService = inject(HelperService);

  protected metadataCount = 0;
  protected checkedStorageIds: number[] = [];
  protected hasTimedOut = false;
  protected displayAmount = 0;
  protected numberOfReportsInProgress = 0;
  protected estimatedMemoryUsage = '';
  protected reportsInProgressThreshold?: number;
  protected openInProgress: FormControl = new FormControl(1, this.defaultReportInProgressValidators);
  protected appVariablesService = inject(AppVariablesService);
  protected currentUploadFile = '';

  protected treeState = inject(DebugTreeStateService);
  private reportsInProgress: Record<string, number> = {};

  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private toastService = inject(ToastService);
  private tabService = inject(TabService);
  private filterService = inject(FilterService);
  private errorHandler = inject(ErrorHandling);
  private debugTab = inject(DebugTabService);

  ngOnInit(): void {
    this.displayAmount = this.clientSettingsService.getAmountOfRecordsInTable();
    this.subscribeToObservables();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToObservables(): void {
    const refreshTable = this.debugTab.refreshTable$.subscribe((condition?: RefreshCondition) =>
      this.refresh(condition),
    );
    this.subscriptions.add(refreshTable);
    const displayAmountSubscription = this.clientSettingsService.amountOfRecordsInTableObservable.subscribe(
      (amount) => (this.displayAmount = amount),
    );
    this.subscriptions.add(displayAmountSubscription);
  }

  loadData(): void {
    this.filterService.refresh();
    this.update();
  }

  private update(): void {
    this.loadMetadataCount();
    this.loadReportInProgressThreshold();
    this.loadReportInProgressSettings();
  }

  changeView(view: View): void {
    this.currentView = view;
    // FilterService already triggers retrieving the records when the view is changed.
    this.update();
    // The parent passes the view on to DebugTreeStateService.
    this.viewChange.next(view);
  }

  loadMetadataCount(): void {
    this.httpService
      .getMetadataCount(this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (count: number) => (this.metadataCount = count),
      });
  }

  loadReportInProgressThreshold(): void {
    this.httpService
      .getReportsInProgressThresholdTime()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (time: number) => (this.reportsInProgressThreshold = time),
      });
  }

  loadReportInProgressSettings(): void {
    this.httpService
      .getSettings()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (settings: OptionsSettings) => {
          this.numberOfReportsInProgress = settings.reportsInProgress;
          this.estimatedMemoryUsage = settings.estMemory;
          this.loadReportInProgressDates();
          this.openInProgress.setValue(1);
          this.openInProgress.setValidators([
            ...this.defaultReportInProgressValidators,
            Validators.max(this.numberOfReportsInProgress),
          ]);
        },
      });
  }

  loadReportInProgressDates(): void {
    let hasChanged = false;
    for (let index = 1; index <= this.numberOfReportsInProgress; index++) {
      this.httpService
        .getReportInProgress(index)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: (report: HierarchicalReport) => {
            this.reportsInProgress[this.getCorrelationIdKey(report.correlationId)] ??= report.startTime;
            if (this.reportsInProgressMetThreshold(report)) {
              this.hasTimedOut = true;
              hasChanged = true;
            }
          },
        });
    }
    if (!hasChanged) {
      this.hasTimedOut = false;
    }
  }

  reportsInProgressMetThreshold(report: HierarchicalReport): boolean {
    return (
      Date.now() - new Date(this.reportsInProgress[this.getCorrelationIdKey(report.correlationId)]).getTime() >
      (this.reportsInProgressThreshold ?? 0)
    );
  }

  toggleFilter(): void {
    // TODO: Impacts the toast component. Do we need to detect changes?
    this.filterService.shouldShowFilterDrawer = !this.filterService.shouldShowFilterDrawer;
  }

  openReportInTab(): void {
    for (const storageId of this.checkedStorageIds) {
      this.tabService.openReportTab(this.currentView.storageName, storageId, 'Loading...');
    }
  }

  openSelected(): void {
    if (this.checkedStorageIds.length === 1) {
      this.openReport(this.checkedStorageIds[0]);
    } else {
      this.toastService.showWarning('You can open only one report at a time!');
    }
  }

  openDeleteModal(): void {
    if (this.metadataCount > 0) {
      this.deleteModal.open(true);
    } else {
      this.toastService.showWarning('No reports to be deleted!');
    }
  }

  deleteSelected(): void {
    if (this.checkedStorageIds.length > 0) {
      const storageName = this.currentView.storageName;
      const storageIds = [...this.checkedStorageIds];
      this.httpService.deleteReport(storageIds, storageName).subscribe({
        next: () => {
          this.treeState.reportsDeleted(storageIds);
          this.loadData();
        },
        error: (error: unknown) => this.errorHandler.handleUnknownError(error),
      });
    }
  }

  deleteAll(): void {
    const storageName = this.currentView.storageName;
    this.httpService.deleteAllReports(storageName).subscribe({
      next: () => {
        this.treeState.allReportsDeleted();
        this.loadData();
      },
      error: (error: unknown) => this.errorHandler.handleUnknownError(error),
    });
  }

  compareTwoReports(): void {
    this.httpService
      .getReports(this.checkedStorageIds, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Record<string, CompareReport>) => {
          const originalReport = this.transformCompareToReport(data[this.checkedStorageIds[0]]);
          const runResultReport = this.transformCompareToReport(data[this.checkedStorageIds[1]]);
          const compareData: CompareData = {
            originalReport: originalReport,
            runResultReport: runResultReport,
            viewName: this.currentView.name,
          };
          this.tabService.openCompareTab(
            this.currentView.storageName,
            this.checkedStorageIds[0],
            this.currentView.storageName,
            this.checkedStorageIds[1],
            compareData,
          );
        },
      });
  }

  transformCompareToReport(compareReport: CompareReport): Report {
    const report = compareReport.report;
    report.xml = compareReport.xml;
    report.storageName = this.currentView.storageName;
    return report;
  }

  changeTableLimit(event: any): void {
    this.clientSettingsService.setAmountOfRecordsInTable(event.target.value);
    // Change of amount of records is posted on subject.
    // Retrieving records is done in a subscription named
    // amountOfRecordsInTableSubscription.
  }

  refresh(refreshCondition?: RefreshCondition): void {
    // TODO: Control displaying toast?
    if (refreshCondition) {
      this.loadData();
    } else {
      this.loadData();
    }
  }

  openReport(storageId: number): void {
    // The table grid emits the storage id as it is in the metadata, which is a string.
    this.treeState.openStoredReport(+storageId);
  }

  openReportInProgress(index: number): void {
    this.treeState.openReportInProgress(index);
  }

  deleteReportInProgress(index: number): void {
    this.httpService.deleteReportInProgress(index).subscribe({
      next: (): void => {
        this.loadReportInProgressSettings();
        this.toastService.showSuccess(`Deleted report in progress with index [${index}]`);
      },
      error: (error: unknown) => this.errorHandler.handleUnknownError(error),
    });
  }

  downloadReports(exportBinary: boolean, exportXML: boolean): void {
    if (this.checkedStorageIds.length > 0) {
      let queryString = '';
      for (const storageId of this.checkedStorageIds) {
        queryString += `id=${storageId}&`;
      }
      this.helperService.download(
        queryString,
        this.currentView.storageName,
        exportBinary,
        exportXML,
        this.clientSettingsService.isForMultipleOmitIfXmlEmpty(),
      );
    } else {
      this.toastService.showWarning('No reports selected to download');
    }
  }

  downloadReportsAsCsv(): void {
    const metadata: Record<string, string>[] = this.filterService.lastMetadata.map((o) => {
      return { ...o };
    });
    if (metadata.length === 0) {
      this.toastService.showWarning('No data to export.');
      return;
    }
    const csv = this.jsonToCsv(metadata);
    this.triggerCsvDownload(csv, 'export.csv');
  }

  private triggerCsvDownload(csv: string, filename: string): void {
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = globalThis.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    document.body.append(a);
    a.click();

    globalThis.URL.revokeObjectURL(url);
    a.remove();
  }

  private escapeCsvValue(value: any): string {
    return `"${String(value ?? '').replaceAll('"', '""')}"`;
  }

  jsonToCsv(items: any[]): string {
    if (items.length === 0) return '';

    const headers = Object.keys(items[0]);
    const rows = items.map((row) => headers.map((field) => this.escapeCsvValue(row[field])).join(','));

    return [headers.join(','), ...rows].join('\n');
  }

  onForMultipleOmitIfXmlEmptyChanged($event: any): void {
    let value = false;
    if ($event?.target.checked) {
      value = true;
    }
    this.clientSettingsService.setForMultipleOmitIfXmlEmpty(value);
  }

  uploadReports(event: Event): void {
    // Allow the same file to be uploaded again.
    this.currentUploadFile = '';
    const eventTarget = event.target as HTMLInputElement;
    const file: File | undefined = eventTarget.files?.[0];
    if (file) {
      const formData: FormData = new FormData();
      formData.append('file', file);
      this.showUploadedReports(formData);
    }
  }

  showUploadedReports(formData: FormData): void {
    this.httpService
      .uploadReport(formData)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: HierarchicalReport[]) => {
          for (let report of data) {
            // Each report was put in a temporary storage on the server
            // that is not accessible anymore. We cache the reports
            // to avoid a vain download attempt.
            this.tabService.openReportTab(report.storageName, report.storageId, report.name, report);
          }
          this.toastService.showSuccess('Report uploaded!');
        },
      });
  }

  processCustomReportAction(): void {
    if (this.checkedStorageIds.length > 0) {
      this.httpService
        .processCustomReportAction(this.currentView.storageName, this.checkedStorageIds)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: (data: Record<string, string>) => {
            if (data.success) {
              this.toastService.showSuccess(data.success);
            }
            if (data.error) {
              this.toastService.showDanger(data.error);
            }
          },
          error: () => {
            this.toastService.showDanger('Failed to process custom report action');
          },
        });
    }
  }

  onCheckedStorageIds(checkedStorageIds: string[]): void {
    // TODO: Add check that really numeric or harmonize types.
    this.checkedStorageIds = checkedStorageIds.map((s) => +s);
  }

  private getCorrelationIdKey(k: string | null): string {
    return k === null ? '@!$null!!' : k;
  }
}
