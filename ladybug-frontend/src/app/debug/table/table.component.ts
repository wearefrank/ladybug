/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HelperService } from '../../shared/services/helper.service';
import { HttpService } from '../../shared/services/http.service';
import { TableSettingsModalComponent } from './table-settings-modal/table-settings-modal.component';
import { TableSettings } from '../../shared/interfaces/table-settings';
import { catchError, Subscription } from 'rxjs';
import { Report } from '../../shared/interfaces/report';
import { SettingsService } from '../../shared/services/settings.service';
import { ToastService } from '../../shared/services/toast.service';
import { TabService } from '../../shared/services/tab.service';
import { AppVariablesService } from '../../shared/services/app.variables.service';
import { FilterService } from '../filter-side-drawer/filter.service';
import { ReportData } from '../../shared/interfaces/report-data';
import { TableCellShortenerPipe } from '../../shared/pipes/table-cell-shortener.pipe';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { ActiveFiltersComponent } from '../active-filters/active-filters.component';
import { FormControl, FormsModule, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { FilterSideDrawerComponent } from '../filter-side-drawer/filter-side-drawer.component';
import { NgClass } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { View } from '../../shared/interfaces/view';
import { OptionsSettings } from '../../shared/interfaces/options-settings';
import { ErrorHandling } from 'src/app/shared/classes/error-handling.service';
import { CompareReport } from '../../shared/interfaces/compare-reports';
import { DebugTabService } from '../debug-tab.service';
import { ViewDropdownComponent } from '../../shared/components/view-dropdown/view-dropdown.component';
import { DeleteModalComponent } from '../../shared/components/delete-modal/delete-modal.component';
import { RefreshCondition } from '../../shared/interfaces/refresh-condition';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css'],
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
    MatSortModule,
    NgClass,
    TableSettingsModalComponent,
    TableCellShortenerPipe,
    MatTableModule,
    ViewDropdownComponent,
    DeleteModalComponent,
    LoadingSpinnerComponent,
    ShortenedTableHeaderPipe,
  ],
})
export class TableComponent implements OnInit, OnDestroy {
  private readonly defaultDisplayAmount: number = 10;
  private readonly subscriptions: Subscription = new Subscription();
  private readonly defaultReportInProgressValidators: ValidatorFn[] = [
    Validators.min(1),
    Validators.pattern('^[0-9]*$'),
    Validators.required,
  ];

  @Input({ required: true }) views!: View[];
  @Input({ required: true }) currentView!: View;
  @Output() viewChange: EventEmitter<View> = new EventEmitter<View>();
  @Output() openReportEvent: EventEmitter<Report> = new EventEmitter<Report>();

  @ViewChild(TableSettingsModalComponent)
  protected tableSettingsModal!: TableSettingsModalComponent;
  @ViewChild(DeleteModalComponent) protected deleteModal!: DeleteModalComponent;

  @ViewChild(MatSort)
  protected set matSort(sort: MatSort) {
    this.tableDataSort = sort;
    this.tableDataSource.sort = this.tableDataSort;
  }

  public helperService = inject(HelperService);

  protected metadataCount = 0;
  protected selectedReports: Report[] = [];
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

  private reportsInProgress: Record<string, number> = {};

  private get selectedReportIds(): number[] {
    return this.selectedReports.map((report: Report): number => report.storageId);
  }

  private showMultipleFiles?: boolean;
  private tableDataSort?: MatSort;

  private httpService = inject(HttpService);
  private settingsService = inject(SettingsService);
  private toastService = inject(ToastService);
  private tabService = inject(TabService);
  private filterService = inject(FilterService);
  private errorHandler = inject(ErrorHandling);
  private debugTab = inject(DebugTabService);

  ngOnInit(): void {
    localStorage.setItem('transformationEnabled', 'true');
    this.filterService.setMetadataTypes(this.currentView.metadataTypes);
    this.subscribeToObservables();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToObservables(): void {
    const tableSpacingSubscription: Subscription = this.settingsService.tableSpacingObservable.subscribe({
      next: (value: number) => {
        this.setTableSpacing(value);
        this.setFontSize(value);
        this.setCheckBoxSize(value);
      },
      error: () => catchError(this.errorHandler.handleError()),
    });
    this.subscriptions.add(tableSpacingSubscription);
    const showMultipleSubscription: Subscription = this.settingsService.showMultipleAtATimeObservable.subscribe({
      next: (value: boolean) => (this.showMultipleFiles = value),
      error: () => catchError(this.errorHandler.handleError()),
    });
    this.subscriptions.add(showMultipleSubscription);
    const showFilterSubscription: Subscription = this.filterService.showFilter$.subscribe({
      next: (show: boolean) => (this.tableSettings.showFilter = show),
      error: () => catchError(this.errorHandler.handleError()),
    });
    this.subscriptions.add(showFilterSubscription);
    const filterContextSubscription: Subscription = this.filterService.filterContext$.subscribe({
      next: (context: Map<string, string>) => this.changeFilter(context),
      error: () => catchError(this.errorHandler.handleError()),
    });
    this.subscriptions.add(filterContextSubscription);
    const refreshAll = this.debugTab.refreshAll$.subscribe((condition?: RefreshCondition) => this.refresh(condition));
    this.subscriptions.add(refreshAll);
    const refreshTable = this.debugTab.refreshTable$.subscribe((condition?: RefreshCondition) =>
      this.refresh(condition),
    );
    this.subscriptions.add(refreshTable);
    const amountOfRecordsInTableSubscription = this.settingsService.amountOfRecordsInTableObservable.subscribe(
      (value) => (this.tableSettings.displayAmount = value),
    );
    this.subscriptions.add(amountOfRecordsInTableSubscription);
  }

  setTableSpacing(value: number): void {
    this.tableSpacing = `${value * 0.25}em 0 ${value * 0.25}em 0`;
  }

  setFontSize(value: number): void {
    const fontSizeSpacingModifier = 1.2;
    const defaultFontSize = 8;
    this.fontSize = `${defaultFontSize + value * fontSizeSpacingModifier}pt`;
  }

  setCheckBoxSize(value: number): void {
    const defaultCheckBoxSize = 13;
    this.checkboxSize = `${defaultCheckBoxSize + value}px`;
  }

  changeFilter(filters: Map<string, string>): void {
    for (let value of filters.values()) {
      if (!value) {
        value = '';
      }
    }
    this.tableSettings.currentFilters = filters;
    this.retrieveRecords();
  }

  loadData(showToast = true): void {
    this.tableSettings.tableLoaded = false;
    this.retrieveRecords(showToast);
    this.loadMetadataCount();
    this.loadReportInProgressThreshold();
    this.loadReportInProgressSettings();
  }

  retrieveRecords(showToast = true): void {
    this.httpService
      .getMetadataReports(this.tableSettings, this.currentView)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (value: Report[]) => {
          this.setUniqueOptions(value);
          this.tableSettings.reportMetadata = value;
          this.tableDataSource.data = value;
          this.tableSettings.tableLoaded = true;
          if (showToast) {
            this.toastService.showSuccess('Data loaded!');
          }
        },
      });
  }

  changeView(view: View): void {
    this.currentView = view;
    this.loadData();
    this.filterService.setMetadataLabels(this.currentView.metadataNames);
    this.viewChange.next(this.currentView);
    this.tableSettings.showFilter = false;
    this.filterService.setShowFilter(this.tableSettings.showFilter);
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
          next: (report: Report) => {
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

  reportsInProgressMetThreshold(report: Report): boolean {
    return (
      Date.now() - new Date(this.reportsInProgress[report.correlationId]).getTime() >
      (this.reportsInProgressThreshold ?? 0)
    );
  }

  toggleFilter(): void {
    this.filterService.setMetadataLabels(this.currentView.metadataNames);
    this.filterService.setMetadataTypes(this.currentView.metadataTypes);
    this.tableSettings.showFilter = !this.tableSettings.showFilter;
    this.filterService.setShowFilter(this.tableSettings.showFilter);
    this.filterService.setCurrentRecords(this.tableSettings.uniqueValues);
  }

  toggleCheck(report: Report, event: MouseEvent): void {
    event.stopPropagation();
    report.checked = !report.checked;
    if (report.checked) {
      this.selectedReports.push(report);
    } else {
      const index = this.selectedReports.indexOf(report);
      this.selectedReports.splice(index, 1);
    }
  }

  toggleSelectAll(): void {
    if (this.selectedReports.length === this.tableSettings.reportMetadata.length) {
      this.setCheckedForAllReports(false);
      this.selectedReports = [];
    } else {
      this.setCheckedForAllReports(true);
      this.selectedReports = [...this.tableSettings.reportMetadata];
    }
  }

  setCheckedForAllReports(value: boolean): void {
    for (const report of this.tableSettings.reportMetadata) {
      report.checked = value;
    }
  }

  getStatusColor(metadata: any): string {
    let statusName = this.currentView.metadataNames.find((name: string) => {
      return name.toLowerCase() === 'status';
    });
    if (statusName && metadata[statusName]) {
      if (metadata[statusName].toLowerCase() === 'success') {
        return '#c3e6cb';
      } else if (metadata[statusName].toLowerCase() === 'null') {
        return '#A9A9A9FF';
      } else {
        return '#f79c9c';
      }
    }
    return 'none';
  }

  openReportInTab(): void {
    const reportTab: Report | undefined = this.tableSettings.reportMetadata.find((report: Report) => report.checked);
    if (!reportTab) {
      this.toastService.showDanger('Could not find report that was selected.');
      return;
    }
    this.httpService
      .getReport(reportTab.storageId, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (report: Report): void => {
          const reportData: ReportData = {
            report: report,
            currentView: this.currentView!,
          };
          this.tabService.openNewTab(reportData);
        },
      });
  }

  openSelected(): void {
    if (this.selectedReports.length > 1 && !this.showMultipleFiles) {
      this.toastService.showWarning(
        'Please enable show multiple files in settings to open multiple files in the debug tree',
      );
      return;
    }
    this.httpService
      .getReports(this.selectedReportIds, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Record<string, CompareReport>) => {
          for (const storageId of this.selectedReportIds) {
            this.openReportEvent.next(data[storageId].report);
          }
        },
      });
  }

  openDeleteModal(): void {
    if (this.tableSettings.reportMetadata.length > 0) {
      this.deleteModal.open(true);
    } else {
      this.toastService.showWarning('No reports to be deleted!');
    }
  }

  deleteSelected(): void {
    const reportIds = this.helperService.getSelectedIds(this.tableSettings.reportMetadata);
    if (reportIds.length > 0) {
      this.httpService
        .deleteReport(reportIds, this.currentView.storageName)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: () => this.retrieveRecords(),
        });
    }
  }

  deleteAll(): void {
    this.httpService
      .deleteAllReports(this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: () => this.retrieveRecords(),
      });
  }

  compareTwoReports(): void {
    this.httpService
      .getReports(this.selectedReportIds, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Record<string, CompareReport>) => {
          const originalReport = this.transformCompareToReport(data[this.selectedReportIds[0]]);
          const runResultReport = this.transformCompareToReport(data[this.selectedReportIds[1]]);

          const id: string = this.tabService.createCompareTabId(originalReport, runResultReport);
          this.tabService.openNewCompareTab({
            id: id,
            originalReport: originalReport,
            runResultReport: runResultReport,
            viewName: this.currentView.name,
          });
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
    const value = event.target.value === '' ? 0 : event.target.value;
    if (this.tableSettings.displayAmount !== value) {
      this.settingsService.setAmountOfRecordsInTable(value);
      this.retrieveRecords();
    }
  }

  refresh(refreshCondition?: RefreshCondition): void {
    if (refreshCondition) {
      this.loadData(refreshCondition.displayToast);
    } else {
      this.loadData();
    }
  }

  openReport(storageId: number): void {
    this.debugTab.setAnyReportsOpen(true);
    this.httpService
      .getReport(storageId, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Report): void => {
          data.storageName = this.currentView.storageName;
          this.openReportEvent.next(data);
        },
      });
  }

  openSelectedReport(storageId: number): void {
    this.selectedReportStorageId = storageId;
    this.openReport(storageId);
  }

  openLatestReports(amount: number): void {
    this.httpService
      .getLatestReports(amount, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Report[]) => {
          for (let report of data) {
            this.openReportEvent.next(report);
          }
          this.toastService.showSuccess(`Latest ${amount} reports opened!`);
        },
      });
  }

  openReportInProgress(index: number): void {
    this.httpService
      .getReportInProgress(index)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (report: Report) => {
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
      this.helperService.download(queryString, this.currentView.storageName, exportBinary, exportXML);
    } else {
      this.toastService.showWarning('No reports selected to download');
    }
  }

  downloadReportsAsCsv(): void {
    this.httpService.getMetadataReports(this.tableSettings, this.currentView).subscribe({
      next: (reports) => {
        if (!reports?.length) {
          this.toastService.showWarning('No data to export.');
          return;
        }

        const csv = this.jsonToCsv(reports);
        this.triggerCsvDownload(csv, 'export.csv');
      },
      error: () => {
        this.toastService.showWarning('Failed to fetch data.');
      },
    });
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
        next: (data: Report[]) => {
          for (let report of data) {
            const reportData: ReportData = {
              report: report,
              currentView: this.currentView,
            };
            this.tabService.openNewTab(reportData);
          }
          this.toastService.showSuccess('Report uploaded!');
        },
      });
  }

  setUniqueOptions(data: any): void {
    for (const headerName of this.currentView.metadataNames as string[]) {
      const lowerHeaderName = headerName.toLowerCase();
      const upperHeaderName = headerName.toUpperCase();
      let uniqueValues: Set<string> = new Set<string>();
      for (let element of data) {
        if (element[lowerHeaderName]) {
          uniqueValues.add(element[lowerHeaderName]);
        }
        if (element[upperHeaderName]) {
          uniqueValues.add(element[upperHeaderName]);
        }
        if (element[headerName]) {
          uniqueValues.add(element[headerName]);
        }
      }
      const MAX_AMOUNT_OF_FILTER_SUGGESTIONS = 15;
      this.tableSettings.uniqueValues.set(
        lowerHeaderName,
        uniqueValues.size < MAX_AMOUNT_OF_FILTER_SUGGESTIONS ? this.sortUniqueValues(uniqueValues) : [],
      );
      this.filterService.setCurrentRecords(this.tableSettings.uniqueValues);
    }
  }

  sortUniqueValues(values: Set<string>): string[] {
    //Sort list alphabetically, if string is actually a number, sort smallest to biggest
    return [...values].toSorted((a, b) => {
      // eslint-disable-next-line unicorn/prefer-number-properties
      const isANumber = !isNaN(Number(a));
      // eslint-disable-next-line unicorn/prefer-number-properties
      const isBNumber = !isNaN(Number(b));
      if (isANumber && isBNumber) {
        return Number(a) - Number(b);
      }
      if (isANumber && !isBNumber) {
        return -1;
      } else if (!isANumber && isBNumber) {
        return 1;
      }
      return a.localeCompare(b);
    });
  }

  getMetadata(report: Report, field: string): string {
    const value = report[field as keyof Report];
    return value !== undefined && value !== null ? String(value) : '';
  }

  getMetadataNameFromHeader(header: string): string {
    const index = this.currentView.metadataLabels.indexOf(header);
    return this.currentView.metadataNames[index];
  }

  getDisplayedColumnNames(labels: string[]): string[] {
    const names: string[] = ['select'];
    for (const header of labels) {
      names.push(this.getMetadataNameFromHeader(header));
    }
    return names;
  }

  processCustomReportAction(): void {
    const reportIds = this.helperService.getSelectedIds(this.tableSettings.reportMetadata);
    if (reportIds.length > 0) {
      this.httpService
        .processCustomReportAction(this.currentView.storageName, reportIds)
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
}
