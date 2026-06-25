import { ErrorHandler, inject, Injectable, OnDestroy } from '@angular/core';
import { View } from '../interfaces/view';
import { FilterFromUrl } from './tab.service';
import { BehaviorSubject, debounceTime, firstValueFrom, Observable, Subject, Subscription } from 'rxjs';
import { HttpService } from './http.service';
import { ClientSettingsService } from './client.settings.service';

export interface Column {
  name: string;
  label: string;
  // The status and storageId columns can be present in the table cells,
  // also if they are not shown. This allows coloring the table rows
  // by status when a status is known but when it is not shown because
  // of filtering.
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
export class FilterService implements OnDestroy {
  private currentView?: View;
  private notShownMetadataNames = new Set<string>();
  private numericMetadataNames = new Set<string>();
  private columns: Column[] = [];
  private tableDataSubject = new BehaviorSubject<TableData | undefined>(undefined);
  private userFilterColumnsSubject = new BehaviorSubject<Column[] | undefined>(undefined);
  private userFilterChoicesSubject = new BehaviorSubject<Map<string, string[]> | undefined>(undefined);
  private fixedFiltersSubject = new BehaviorSubject<FixedFilters | undefined>(undefined);
  private userFiltersSubject = new Subject<Map<string, string>>();
  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private errorHandling = inject(ErrorHandler);
  private subscriptions = new Subscription();
  private subscribed = false;
  public userFilters$ = this.userFiltersSubject.pipe(debounceTime(300));
  public urlFilters: FilterFromUrl[] = [];
  public viewFilters: FilterFromUrl[] = [];
  private _userFiltersBeingEdited: Record<string, string> = {};
  public set userFiltersBeingEdited(_userFiltersBeingEdited: Record<string, string>) {
    this._userFiltersBeingEdited = _userFiltersBeingEdited;
    this.userFiltersSubject.next(new Map<string, string>(Object.entries(this._userFiltersBeingEdited)));
  }
  public get userFiltersBeingEdited(): Record<string, string> {
    return this._userFiltersBeingEdited;
  }
  public shouldShowFilterDrawer = false;
  public lastMetadata: Record<string, string>[] = [];
  public tableData$: Observable<TableData | undefined> = this.tableDataSubject.asObservable();
  public userFilterColumns$: Observable<Column[] | undefined> = this.userFilterColumnsSubject.asObservable();
  public userFilterChoices$: Observable<Map<string, string[]> | undefined> =
    this.userFilterChoicesSubject.asObservable();
  public fixedFilters$: Observable<FixedFilters | undefined> = this.fixedFiltersSubject.asObservable();

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  setCurrentView(currentView: View): void {
    this.currentView = currentView;
    this.initialize(this.currentView);
  }

  setUrlFilters(urlFilters: FilterFromUrl[]): void {
    this.urlFilters = urlFilters;
    if (this.currentView) {
      this.initialize(this.currentView);
    }
  }

  refresh(): void {
    if (this.currentView) {
      this.initialize(this.currentView);
    }
  }

  onUserFilterChanged(): void {
    this.userFiltersSubject.next(new Map<string, string>(Object.entries(this.userFiltersBeingEdited)));
  }

  updateFilter(value: string, columnName: string): void {
    if (value.length === 0) {
      delete this.userFiltersBeingEdited[columnName];
    } else {
      this.userFiltersBeingEdited[columnName] = value;
    }
    this.onUserFilterChanged();
  }

  removeFilter(metadataName: string): void {
    delete this.userFiltersBeingEdited[metadataName];
    this.onUserFilterChanged();
  }

  resetFilters(): void {
    this.userFiltersBeingEdited = {};
    this.onUserFilterChanged();
  }

  private initialize(currentView: View): void {
    if (!this.subscribed) {
      this.subscribeToSubscriptions();
      this.subscribed = true;
    }
    this.setViewFilters(currentView);
    this.setNotShownMetadataNames();
    this.setNumericMetadataNames(currentView);
    this.setColumns(currentView);
    this.fixedFiltersSubject.next({
      urlFilters: this.urlFilters,
      viewFilters: this.viewFilters,
    });
    this.userFilterColumnsSubject.next(this.columns.filter((c) => c.shown === true));
    this.resetFilters();
  }

  private subscribeToSubscriptions(): void {
    const userFilterSubscription = this.userFilters$.subscribe((userFilters) => {
      this.update(userFilters);
    });
    this.subscriptions.add(userFilterSubscription);
    const maxAmountOfRecordsSubscription = this.clientSettingsService.amountOfRecordsInTableObservable.subscribe(() => {
      this.resetFilters();
    });
    this.subscriptions.add(maxAmountOfRecordsSubscription);
  }

  private setViewFilters(currentView: View): void {
    this.viewFilters = [];
    if (currentView.metadataFilter) {
      for (const metadataName of Object.keys(currentView.metadataFilter)) {
        this.viewFilters.push({
          metadataName,
          value: currentView.metadataFilter![metadataName],
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

  private setNumericMetadataNames(currentView: View): void {
    this.numericMetadataNames.clear();
    const metadataTypesMap: Map<string, string> = new Map<string, string>(Object.entries(currentView.metadataTypes));
    for (const metadataName of currentView.metadataNames) {
      const metadataType: string = metadataTypesMap.get(metadataName) ?? '';
      if (metadataType === 'int' || metadataType === 'long') {
        this.numericMetadataNames.add(metadataName);
      }
    }
  }

  private setColumns(currentView: View): void {
    this.columns = [];
    for (let index = 0; index < currentView.metadataNames.length; ++index) {
      const metadataName = currentView.metadataNames[index];
      this.columns.push({
        name: metadataName,
        label: currentView.metadataLabels[index],
        shown: !this.notShownMetadataNames.has(metadataName),
      });
    }
  }

  private update(userFiltersMap: Map<string, string>): void {
    if (!this.currentView) {
      throw new Error('Cannot happen because we subscribe to subscriptions only after receiving the first view');
    }
    const userFilters: FilterFromUrl[] = [];
    for (const [key, value] of userFiltersMap.entries()) {
      userFilters.push({
        metadataName: key,
        value,
      });
    }
    const allFilters: FilterFromUrl[] = [...this.urlFilters, ...this.viewFilters, ...userFilters];
    firstValueFrom(
      this.httpService.getMetadata(this.currentView, {
        metadataNames: this.currentView.metadataNames,
        filterHeader: allFilters.map((f) => f.metadataName),
        filter: allFilters.map((f) => f.value),
        limit: this.clientSettingsService.getAmountOfRecordsInTable(),
      }),
    )
      .then((metadata) => {
        this.lastMetadata = metadata;
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
        uniqueValuesSet.size <= MAX_AMOUNT_OF_FILTER_SUGGESTIONS ? this.sortUniqueValues(uniqueValuesSet) : [];
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
