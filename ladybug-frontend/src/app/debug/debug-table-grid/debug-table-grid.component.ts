import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { catchError, Subscription } from 'rxjs';
import { TableCellShortenerPipe } from '../../shared/pipes/table-cell-shortener.pipe';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { ClientSettingsService } from '../../shared/services/client.settings.service';
import { Column, FilterService, TableData } from '../../shared/services/filter.service';
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
    TableCellShortenerPipe,
    MatTableModule,
    ShortenedTableHeaderPipe,
  ],
})
export class DebugTableGridComponent implements OnInit, OnDestroy {
  @Input() openedStorageId: number | null = null;
  @Output() clickReportWithStorageId: EventEmitter<number> = new EventEmitter<number>();
  @Output() checkedStorageIds = new EventEmitter<string[]>();

  protected dataLoaded = false;
  protected data?: WorkingData;
  protected tableDataSort?: MatSort;
  protected tableDataSource: MatTableDataSource<RowData> = new MatTableDataSource<RowData>();
  protected allChecked = false;
  protected checkboxSize?: string;
  protected fontSize?: string;
  protected tableSpacing?: string;

  private clientSettingsService = inject(ClientSettingsService);
  private errorHandler = inject(ErrorHandling);
  private subscriptions = new Subscription();
  private filterService = inject(FilterService);

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

  protected isAllChecked(): boolean {
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

  protected isNoneChecked(): boolean {
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
    this.allChecked = value;
  }

  protected toggleCheckAll(beingCheckedEvent: Event): void {
    const beingChecked: boolean = (beingCheckedEvent.target as HTMLInputElement).checked;
    if (this.data && this.data.rows.length > 0) {
      this.checkAll(beingChecked);
      this.reportCheckedStorageIds();
    }
  }

  protected toggleCheck(row: RowData): void {
    row.checked = !row.checked;
    this.allChecked = this.isAllChecked();
    this.reportCheckedStorageIds();
  }

  protected getSelectAndOtherShownColumnNames(): string[] {
    const columnNames: string[] = this.data?.columns.map((c) => c.name) ?? [];
    return ['select', ...columnNames];
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

  protected getStatusOrHighlightClass(row: RowData): string {
    let result = this.getStatusClass(row);
    // Storage id field can be converted to a number. We checked already that storageId
    // is a numeric field according to the backend.
    if (+row.fields[STORAGE_ID_COLUMN_NAME] === this.openedStorageId) {
      result = `${result} highlight`;
    }
    return result;
  }

  private getStatusClass(row: RowData): string {
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
      // Conversion to number is safe because we checked that column storageId
      // exists and that it is numeric according to the server.
      const checkedStorageIdsNumbers: number[] = this.data.rows
        .filter((r) => r.checked === true)
        .map((r) => +r.fields[STORAGE_ID_COLUMN_NAME]);
      checkedStorageIdsNumbers.sort((a, b) => a - b);
      this.checkedStorageIds.emit(checkedStorageIdsNumbers.map((n) => `${n}`));
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
