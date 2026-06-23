/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HelperService } from '../../shared/services/helper.service';
import { HttpService } from '../../shared/services/http.service';
import { TableSettingsModalComponent } from './table-settings-modal/table-settings-modal.component';
import { TableSettings } from '../../shared/interfaces/table-settings';
import { catchError, Subscription } from 'rxjs';
import { Report } from '../../shared/interfaces/report';
import { ToastService } from '../../shared/services/toast.service';
import { TabService } from '../../shared/services/tab.service';
import { AppVariablesService } from '../../shared/services/app.variables.service';
import { Filter2Service } from '../../shared/services/filter2.service';
import { ActiveFiltersComponent } from '../active-filters/active-filters.component';
import { FormControl, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
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
import { SortableTable } from '../sortable-table/sortable-table';
import { FilterSideDrawer2Component } from '../filter-side-drawer/filter-side-drawer2.component';

@Component({
  selector: 'app-table2',
  templateUrl: './table2.component.html',
  styleUrls: ['./table2.component.css'],
  standalone: true,
  imports: [
    FilterSideDrawer2Component,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    ReactiveFormsModule,
    FormsModule,
    ActiveFiltersComponent,
    TableSettingsModalComponent,
    MatTableModule,
    ViewDropdownComponent,
    DeleteModalComponent,
    SortableTable,
  ],
})
export class TableComponent2 implements OnInit, OnDestroy {
  private readonly defaultDisplayAmount: number = 10;
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
  @Output() openReportEvent: EventEmitter<HierarchicalReport> = new EventEmitter<HierarchicalReport>();

  @ViewChild(TableSettingsModalComponent)
  protected tableSettingsModal!: TableSettingsModalComponent;
  @ViewChild(DeleteModalComponent) protected deleteModal!: DeleteModalComponent;

  public helperService = inject(HelperService);

  protected metadataCount = 0;
  protected checkedStorageIds: number[] = [];
  protected hasTimedOut = false;
  protected tableDataSource: MatTableDataSource<Report> = new MatTableDataSource<Report>();
  protected tableSettings: TableSettings = {
    reportMetadata: [],
    tableLoaded: false,
    displayAmount: this.defaultDisplayAmount,
    showFilter: false,
    currentFilters: new Map<string, string>(),
    numberOfReportsInProgress: 0,
    estimatedMemoryUsage: '',
    uniqueValues: new Map<string, string[]>(),
  };

  protected reportsInProgressThreshold?: number;
  protected selectedReportStorageId?: number;
  protected tableSpacing?: string;
  protected fontSize?: string;
  protected checkboxSize?: string;
  protected openInProgress: FormControl = new FormControl(1, this.defaultReportInProgressValidators);
  protected appVariablesService = inject(AppVariablesService);
  protected currentUploadFile = '';

  protected selectedStorageId: string | null = null;
  private reportsInProgress: Record<string, number> = {};

  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private toastService = inject(ToastService);
  private tabService = inject(TabService);
  private filterService = inject(Filter2Service);
  private errorHandler = inject(ErrorHandling);
  private debugTab = inject(DebugTabService);

  ngOnInit(): void {
    this.subscribeToObservables();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToObservables(): void {
    const refreshAll = this.debugTab.refreshAll$.subscribe((condition?: RefreshCondition) => this.refresh(condition));
    this.subscriptions.add(refreshAll);
    const refreshTable = this.debugTab.refreshTable$.subscribe((condition?: RefreshCondition) =>
      this.refresh(condition),
    );
    this.subscriptions.add(refreshTable);
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
    this.viewChange.next(this.currentView);
    // Filter2Service already triggers retrieving the records when the view is changed.
    this.update();
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
          this.tableSettings.numberOfReportsInProgress = settings.reportsInProgress;
          this.tableSettings.estimatedMemoryUsage = settings.estMemory;
          this.loadReportInProgressDates();
          this.openInProgress.setValue(1);
          this.openInProgress.setValidators([
            ...this.defaultReportInProgressValidators,
            Validators.max(this.tableSettings.numberOfReportsInProgress),
          ]);
        },
      });
  }

  loadReportInProgressDates(): void {
    let hasChanged = false;
    for (let index = 1; index <= this.tableSettings.numberOfReportsInProgress; index++) {
      this.httpService
        .getReportInProgress(index)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: (report: HierarchicalReport) => {
            this.reportsInProgress[report.correlationId] ??= report.startTime;
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
      Date.now() - new Date(this.reportsInProgress[report.correlationId]).getTime() >
      (this.reportsInProgressThreshold ?? 0)
    );
  }

  toggleFilter(): void {
    this.filterService.shouldShowFilterDrawer = !this.filterService.shouldShowFilterDrawer;
  }

  openReportInTab(): void {
    for (const storageId of this.checkedStorageIds) {
      this.tabService.openReportTab(this.currentView.storageName, storageId, 'Loading...');
    }
  }

  openSelected(): void {
    if (this.checkedStorageIds.length === 1) {
      this.selectedReportStorageId = this.checkedStorageIds[0];
      this.openReport(this.checkedStorageIds[0]);
    } else {
      this.toastService.showWarning('You can only open one report at a time in the debug tab');
    }
  }

  openDeleteModal(): void {
    if (this.checkedStorageIds.length > 0) {
      this.deleteModal.open(true);
    } else {
      this.toastService.showWarning('No reports to be deleted!');
    }
  }

  deleteSelected(): void {
    if (this.checkedStorageIds.length > 0) {
      this.httpService
        .deleteReport(this.checkedStorageIds, this.currentView.storageName)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: () => this.loadData(),
        });
    }
  }

  deleteAll(): void {
    this.httpService
      .deleteAllReports(this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: () => this.loadData(),
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
    this.httpService
      .getHierarchicalReports([storageId], this.currentView.storageName, this.currentView.name)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: HierarchicalReport[]): void => {
          this.openReportEvent.next(data[0]);
        },
      });
  }

  openReportInProgress(index: number): void {
    this.httpService
      .getReportInProgress(index)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (report: HierarchicalReport) => {
          this.openReportEvent.next(report);
          this.toastService.showSuccess(`Opened report in progress with index [${index}]`);
        },
      });
  }

  deleteReportInProgress(index: number): void {
    this.httpService
      .deleteReportInProgress(index)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (): void => {
          this.loadReportInProgressSettings();
          this.toastService.showSuccess(`Deleted report in progress with index [${index}]`);
        },
      });
  }

  downloadReports(exportBinary: boolean, exportXML: boolean): void {
    const selectedReports = this.tableSettings.reportMetadata.filter((report) => report.checked);

    if (selectedReports.length > 0) {
      let queryString = '';
      for (let report of selectedReports) {
        queryString += `id=${report.storageId}&`;
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
}
