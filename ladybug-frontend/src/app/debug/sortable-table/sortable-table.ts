import { Component, EventEmitter, inject, Input, OnInit, Output, ViewChild } from '@angular/core';
import { catchError, Subscription } from 'rxjs';
import { TableCellShortenerPipe } from '../../shared/pipes/table-cell-shortener.pipe';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { ErrorHandling } from 'src/app/shared/classes/error-handling.service';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { ClientSettingsService } from 'src/app/shared/services/client.settings.service';

const STORAGE_ID_COLUMN_NAME = 'storageId';
const STATUS_COLUMN_NAME = 'status';

export interface Column {
  name: string;
  label: string;
}

export interface TableData {
  rows: Record<string, string>[];
  columns: Column[];
  numericMetadataNames: Set<string>;
}

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
  selector: 'app-table',
  templateUrl: './sortable-table.html',
  styleUrls: ['./sortable-table.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    MatSortModule,
    NgClass,
    TableCellShortenerPipe,
    MatTableModule,
    ShortenedTableHeaderPipe,
  ],
})
export class SortableTable implements OnInit {
  @Input() set tableData(argument: TableData) {
    this.data = {
      rows: argument.rows.map((r) => {
        return { checked: false, fields: r };
      }),
      columns: argument.columns,
      numericMetadataNames: argument.numericMetadataNames,
    };
    const storageIdColumns: Column[] = this.data.columns.filter((c) => c.name === STORAGE_ID_COLUMN_NAME);
    if (storageIdColumns.length !== 1) {
      throw new Error(`SortableTable.set tableData(): Expected column ${STORAGE_ID_COLUMN_NAME}`);
    }
    if (!this.data.numericMetadataNames.has(STORAGE_ID_COLUMN_NAME)) {
      throw new Error(`SortableTableData.set tableData(): Expected column ${STORAGE_ID_COLUMN_NAME} to be numeric`);
    }
    const statusColumns: Column[] = this.data.columns.filter((c) => c.name === STATUS_COLUMN_NAME);
    if (statusColumns.length !== 1) {
      throw new Error(`SortableTableData.set tableData(): Expected column ${STATUS_COLUMN_NAME}`);
    }
  }
  @Input() selectedStorageId: string | null = null;
  @Output() clickReportWithStorageId: EventEmitter<number> = new EventEmitter<number>();

  protected data?: WorkingData;
  protected tableDataSort?: MatSort;
  protected tableDataSource: MatTableDataSource<RowData> = new MatTableDataSource<RowData>();
  protected checkboxSize?: string;
  protected fontSize?: string;
  protected tableSpacing?: string;

  private clientSettingsService = inject(ClientSettingsService);
  private errorHandler = inject(ErrorHandling);
  private subscriptions = new Subscription();

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

  protected selectAll(value: boolean): void {
    if (this.data) {
      for (const row of this.data.rows) {
        row.checked = value;
      }
    }
  }

  protected toggleSelectAll(): void {
    if (this.data && this.data.rows.length > 0) {
      if (this.noneChecked()) {
        this.selectAll(true);
      } else {
        this.selectAll(false);
      }
    }
  }

  protected toggleCheck(row: RowData): void {
    row.checked = !row.checked;
  }

  protected getColumnNames(): string[] {
    return this.data?.columns.map((c) => c.name) ?? [];
  }

  protected getColumnLabels(): string[] {
    return this.data?.columns.map((c) => c.label) ?? [];
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
