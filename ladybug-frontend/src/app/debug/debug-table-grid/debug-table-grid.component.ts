import { Component, EventEmitter, inject, Input, OnInit, Output, ViewChild } from '@angular/core';
import { catchError, Subscription } from 'rxjs';
import { TableCellShortenerPipe } from '../../shared/pipes/table-cell-shortener.pipe';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { ClientSettingsService } from '../../shared/services/client.settings.service';
import { Column, Filter2Service, TableData } from '../../shared/services/filter2.service';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

const STORAGE_ID_COLUMN_NAME = 'storageId';
const STATUS_COLUMN_NAME = 'status';

interface RowData {
  checked: boolean;
  fields: Record<string, string>;
}

interface WorkingData {
  rows: RowData[];
  columns: Column[];
  numericMetadataNames: Set<string>;
}

@Component({
  selector: 'app-debug-table-grid',
  templateUrl: './debug-table-grid.component.html',
  styleUrls: ['./debug-table-grid.component.css'],
  standalone: true,
  imports: [
    LoadingSpinnerComponent,
    ReactiveFormsModule,
    FormsModule,
    MatSortModule,
    NgClass,
    TableCellShortenerPipe,
    MatTableModule,
    ShortenedTableHeaderPipe,
  ],
})
export class DebugTableGridComponent implements OnInit {
  @Input() selectedStorageId: string | null = null;
  @Output() clickReportWithStorageId: EventEmitter<number> = new EventEmitter<number>();
  @Output() checkedStorageIds = new EventEmitter<string[]>();

  protected dataLoaded = false;
  protected data?: WorkingData;
  protected tableDataSort?: MatSort;
  protected tableDataSource: MatTableDataSource<RowData> = new MatTableDataSource<RowData>();
  protected checkboxSize?: string;
  protected fontSize?: string;
  protected tableSpacing?: string;

  private clientSettingsService = inject(ClientSettingsService);
  private errorHandler = inject(ErrorHandling);
  private subscriptions = new Subscription();
  private filterService = inject(Filter2Service);

  @ViewChild(MatSort)
  protected set matSort(sort: MatSort) {
    this.tableDataSort = sort;
    this.tableDataSource.sort = this.tableDataSort;
    this.tableDataSource.sortingDataAccessor = (row: RowData, header: string): string | number =>
      this.sortingDataAccessor(row, header);
  }

  ngOnInit(): void {
    this.subscribeToObservables();
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
    const tableDataSubscription = this.filterService.tableData$.subscribe((data) => {
      if (data !== undefined) {
        this.setTableData(data);
      }
    });
    this.subscriptions.add(tableDataSubscription);
  }

  // Not called directly in production - available for Karma tests.
  setTableData(argument: TableData): void {
    this.data = {
      rows: argument.rows.map((r) => {
        return { checked: false, fields: r };
      }),
      columns: argument.columns,
      numericMetadataNames: argument.numericMetadataNames,
    };
    const storageIdColumns: Column[] = this.data.columns.filter((c) => c.name === STORAGE_ID_COLUMN_NAME);
    if (storageIdColumns.length !== 1) {
      throw new Error(`DebugTableGridComponent.set tableData(): Expected column ${STORAGE_ID_COLUMN_NAME}`);
    }
    if (!this.data.numericMetadataNames.has(STORAGE_ID_COLUMN_NAME)) {
      throw new Error(
        `DebugTableGridComponentData.set tableData(): Expected column ${STORAGE_ID_COLUMN_NAME} to be numeric`,
      );
    }
    this.tableDataSource.data = this.data.rows;
    this.reportCheckedStorageIds();
    this.dataLoaded = true;
  }

  protected allChecked(): boolean {
    if (this.data) {
      if (this.data.rows.length === 0) {
        return false;
      }
      for (const row of this.data.rows) {
        if (row.checked === false) {
          return false;
        }
      }
      return true;
    } else {
      // Dummy return value
      return true;
    }
  }

  protected noneChecked(): boolean {
    if (this.data) {
      if (this.data.rows.length === 0) {
        return false;
      }
      for (const row of this.data.rows) {
        if (row.checked === true) {
          return false;
        }
      }
      return true;
    } else {
      // Dummy return value
      return true;
    }
  }

  private checkAll(value: boolean): void {
    if (this.data) {
      for (const row of this.data.rows) {
        row.checked = value;
      }
    }
  }

  protected toggleCheckAll(): void {
    if (this.data && this.data.rows.length > 0) {
      if (this.noneChecked()) {
        this.checkAll(true);
      } else {
        this.checkAll(false);
      }
      this.reportCheckedStorageIds();
    }
  }

  protected toggleCheck(row: RowData): void {
    row.checked = !row.checked;
    this.reportCheckedStorageIds();
  }

  protected getShownColumns(): Column[] {
    return this.data?.columns.filter((c) => c.shown === true) ?? [];
  }

  protected getShownColumnNames(): string[] {
    return this.getShownColumns().map((c) => c.name);
  }

  protected getShownColumnLabels(): string[] {
    return this.getShownColumns().map((c) => c.label);
  }

  sortingDataAccessor(row: RowData, columnName: string): string | number {
    if (this.data && this.data.numericMetadataNames.has(columnName)) {
      const result: number = +row.fields[columnName];
      return result;
    } else {
      return row.fields[columnName];
    }
  }

  protected getMetadata(row: RowData, name: string): string {
    return row.fields[name] ?? '';
  }

  protected getStatusClass(row: RowData): string {
    if (row.fields[STATUS_COLUMN_NAME]) {
      const status: string = row.fields[STATUS_COLUMN_NAME];
      if (status.toLowerCase() === 'success') {
        return 'statusSuccess';
      } else if (status.toLowerCase() === 'null') {
        return 'statusNull';
      } else {
        return 'statusError';
      }
    }
    return 'none';
  }

  private reportCheckedStorageIds(): void {
    if (this.data) {
      this.checkedStorageIds.emit(
        this.data.rows.filter((r) => r.checked === true).map((r) => r.fields[STORAGE_ID_COLUMN_NAME]),
      );
    }
  }

  private setCheckBoxSize(value: number): void {
    const defaultCheckBoxSize = 13;
    this.checkboxSize = `${defaultCheckBoxSize + value}px`;
  }

  private setFontSize(value: number): void {
    const fontSizeSpacingModifier = 1.2;
    const defaultFontSize = 8;
    this.fontSize = `${defaultFontSize + value * fontSizeSpacingModifier}pt`;
  }

  private setTableSpacing(value: number): void {
    this.tableSpacing = `${value * 0.25}em 0 ${value * 0.25}em 0`;
  }
}
