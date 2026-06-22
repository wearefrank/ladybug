import { ErrorHandler, inject, Injectable } from '@angular/core';
import { View } from '../interfaces/view';
import { FilterFromUrl } from './tab.service';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { HttpService } from './http.service';
import { ClientSettingsService } from './client.settings.service';

export interface Column {
  name: string;
  label: string;
  // The status and storageId columns should be present in the table cells,
  // but they are not necessarily shown.
  shown: boolean;
}

export interface TableData {
  rows: Record<string, string>[];
  columns: Column[];
  numericMetadataNames: Set<string>;
}

export interface FixedFilters {
  urlFilters: FilterFromUrl[];
  viewFilters: FilterFromUrl[];
}

// TODO: Should replace FilterService.
@Injectable({
  providedIn: 'root',
})
export class Filter2Service {
  private currentView?: View;
  private urlFilters: FilterFromUrl[] = [];
  private viewFilters: FilterFromUrl[] = [];
  private notShownMetadataNames = new Set<string>();
  private numericMetadataNames = new Set<string>();
  private columns: Column[] = [];
  private userFilters: Map<string, string> = new Map<string, string>();
  private tableDataSubject = new BehaviorSubject<TableData | undefined>(undefined);
  private userFilterColumnsSubject = new BehaviorSubject<Column[] | undefined>(undefined);
  private userFilterChoicesSubject = new BehaviorSubject<Map<string, string[]> | undefined>(undefined);
  private fixedFiltersSubject = new BehaviorSubject<FixedFilters | undefined>(undefined);

  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private errorHandling = inject(ErrorHandler);

  tableData$: Observable<TableData | undefined> = this.tableDataSubject.asObservable();
  userFilterColumns$: Observable<Column[] | undefined> = this.userFilterColumnsSubject.asObservable();
  userFilterChoices$: Observable<Map<string, string[]> | undefined> = this.userFilterChoicesSubject.asObservable();
  fixedFilters$: Observable<FixedFilters | undefined> = this.fixedFiltersSubject.asObservable();

  setCurrentView(currentView: View): void {
    this.currentView = currentView;
    this.initialize();
  }

  setUrlFilters(urlFilters: FilterFromUrl[]): void {
    this.urlFilters = urlFilters;
    this.initialize();
  }

  updateUserFilterContext(filterName: string, filterContext: string): void {
    if (filterContext.length > 0) {
      this.userFilters.set(filterName, filterContext);
    } else {
      this.userFilters.delete(filterName);
    }
    if (this.currentView) {
      this.update();
    }
  }

  private initialize(): void {
    if (this.currentView !== undefined) {
      this.setViewFilters();
      this.setNotShownMetadataNames();
      this.setNumericMetadataNames();
      this.setColumns();
      this.fixedFiltersSubject.next({
        urlFilters: this.urlFilters,
        viewFilters: this.viewFilters,
      });
      this.userFilterColumnsSubject.next(this.columns.filter((c) => c.shown === true));
      this.update();
    }
  }

  private setViewFilters(): void {
    this.viewFilters = [];
    if (this.currentView!.metadataFilter) {
      for (const metadataName of Object.keys(this.currentView!.metadataFilter)) {
        this.viewFilters.push({
          metadataName,
          value: this.currentView!.metadataFilter![metadataName],
        });
      }
    }
  }

  private setNotShownMetadataNames(): void {
    this.notShownMetadataNames.clear();
    for (const metadataName of [...this.urlFilters, ...this.viewFilters].map((f) => f.metadataName)) {
      this.notShownMetadataNames.add(metadataName);
    }
  }

  private setNumericMetadataNames(): void {
    this.numericMetadataNames.clear();
    const metadataTypesMap: Map<string, string> = new Map<string, string>(
      Object.entries(this.currentView!.metadataTypes),
    );
    for (const metadataName of this.currentView!.metadataNames) {
      const metadataType: string = metadataTypesMap.get(metadataName) ?? '';
      if (metadataType === 'int' || metadataType === 'long') {
        this.numericMetadataNames.add(metadataName);
      }
    }
  }

  private setColumns(): void {
    this.columns = [];
    for (let index = 0; index < this.currentView!.metadataNames.length; ++index) {
      const metadataName = this.currentView!.metadataNames[index];
      this.columns.push({
        name: metadataName,
        label: this.currentView!.metadataLabels[index],
        shown: !this.notShownMetadataNames.has(metadataName),
      });
    }
  }

  private update(): void {
    const userFilters: FilterFromUrl[] = [];
    for (const [key, value] of this.userFilters.entries()) {
      userFilters.push({
        metadataName: key,
        value,
      });
    }
    const allFilters: FilterFromUrl[] = [...this.urlFilters, ...this.viewFilters, ...userFilters];
    firstValueFrom(
      this.httpService.getMetadata(this.currentView!, {
        metadataNames: this.currentView!.metadataNames,
        filterHeader: allFilters.map((f) => f.metadataName),
        filter: allFilters.map((f) => f.value),
        limit: this.clientSettingsService.getAmountOfRecordsInTable(),
      }),
    )
      .then((metadata) => {
        this.tableDataSubject.next({
          rows: metadata,
          columns: this.columns,
          numericMetadataNames: this.numericMetadataNames,
        });
        this.userFilterChoicesSubject.next(this.getUniqueOptions(metadata));
      })
      .catch((error) => {
        this.errorHandling.handleError(error);
      });
  }

  private getUniqueOptions(rows: Record<string, string>[]): Map<string, string[]> {
    const result: Map<string, string[]> = new Map<string, string[]>();
    for (const column of this.columns.filter((c) => c.shown)) {
      let uniqueValuesSet: Set<string> = new Set<string>();
      for (const row of rows) {
        uniqueValuesSet.add(row[column.name]);
      }
      const MAX_AMOUNT_OF_FILTER_SUGGESTIONS = 15;
      const uniqueValues: string[] =
        uniqueValuesSet.size < MAX_AMOUNT_OF_FILTER_SUGGESTIONS ? this.sortUniqueValues(uniqueValuesSet) : [];
      result.set(column.name, uniqueValues);
    }
    return result;
  }

  private sortUniqueValues(values: Set<string>): string[] {
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
}
