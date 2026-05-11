import { Component, EventEmitter, inject, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HttpService } from '../../shared/services/http.service';
import { TableSettingsModalComponent } from './table-settings-modal/table-settings-modal.component';
import { catchError, Subscription } from 'rxjs';
import { Report } from '../../shared/interfaces/report';
import { ToastService } from '../../shared/services/toast.service';
import { TableCellShortenerPipe } from '../../shared/pipes/table-cell-shortener.pipe';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { NgClass } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { ErrorHandling } from 'src/app/shared/classes/error-handling.service';
import { ViewDropdownComponent } from '../../shared/components/view-dropdown/view-dropdown.component';
import { DeleteModalComponent } from '../../shared/components/delete-modal/delete-modal.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { ClientSettingsService } from 'src/app/shared/services/client.settings.service';

@Component({
  selector: 'app-tracing-table',
  templateUrl: './tracing-table.component.html',
  styleUrls: ['./tracing-table.component.css'],
  standalone: true,
  imports: [
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    ReactiveFormsModule,
    FormsModule,
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
export class TracingTableComponent implements OnInit, OnDestroy {
  private readonly subscriptions: Subscription = new Subscription();

  reports: Report[] = [];

  displayedColumns: string[] = ['select', 'traceId', 'duration', 'name', 'spans'];

  protected selectedReports: Report[] = [];
  protected selectedReportCorrelationId?: string;

  @Output() openReportEvent: EventEmitter<Report> = new EventEmitter<Report>();

  @ViewChild(TableSettingsModalComponent)
  protected tableSettingsModal!: TableSettingsModalComponent;
  @ViewChild(DeleteModalComponent) protected deleteModal!: DeleteModalComponent;

  @ViewChild(MatSort)
  protected set matSort(sort: MatSort) {
    this.tableDataSort = sort;
    this.tableDataSource.sort = this.tableDataSort;
  }

  protected tableDataSource: MatTableDataSource<Report> = new MatTableDataSource<Report>();

  protected tableSpacing?: string;
  protected fontSize?: string;
  protected checkboxSize?: string;

  private tableDataSort?: MatSort;

  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private toastService = inject(ToastService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.subscribeToObservables();
    this.loadData();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToObservables(): void {
    const tableSpacingSubscription: Subscription = this.clientSettingsService.tableSpacingObservable.subscribe({
      next: (value: number) => {
        this.setTableSpacing(value);
        this.setFontSize(value);
        this.setCheckBoxSize(value);
      },
      error: () => catchError(this.errorHandler.handleError()),
    });
    this.subscriptions.add(tableSpacingSubscription);
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

  loadData(): void {
    this.httpService
      .getTraceReports()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (traces: Report[]) => {
          this.reports = traces;
          this.tableDataSource.data = traces;
        },
      });
  }

  selectReport(report: Report): void {
    this.selectedReportCorrelationId = report.correlationId;
    this.openReportEvent.next(report);
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
    if (this.selectedReports.length === this.reports.length) {
      this.setCheckedForAllReports(false);
      this.selectedReports = [];
    } else {
      this.setCheckedForAllReports(true);
      this.selectedReports = [...this.reports];
    }
  }

  setCheckedForAllReports(value: boolean): void {
    for (const report of this.reports) {
      report.checked = value;
    }
  }

  openSelected(): void {
    for (const report of this.selectedReports) {
      this.openReportEvent.next(report);
    }
  }

  openDeleteModal(): void {
    if (this.reports.length > 0) {
      this.deleteModal.open(true);
    } else {
      this.toastService.showWarning('No reports to be deleted!');
    }
  }

  deleteSelected(): void {
    if (this.selectedReports.length === 0) {
      this.toastService.showWarning('No reports selected to delete!');
      return;
    }

    const selectedIds = new Set(this.selectedReports.map((report) => report.correlationId));
    this.reports = this.reports.filter((report) => !selectedIds.has(report.correlationId));
    this.tableDataSource.data = this.reports;

    this.selectedReports = [];

    this.toastService.showSuccess('Selected reports removed');
  }

  deleteAll(): void {
    this.reports = [];
    this.selectedReports = [];
    this.tableDataSource.data = this.reports;

    this.toastService.showSuccess('All reports removed');
  }

  getStatusColor(): string {
    return '#c3e6cb';
  }

  refresh(): void {
    this.loadData();
  }
}
