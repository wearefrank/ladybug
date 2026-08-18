import { ErrorHandler, inject, Injectable, OnDestroy } from '@angular/core';
import { View } from '../interfaces/view';
import { MetadataFilter, TabService } from './tab.service';
import { BehaviorSubject, debounceTime, firstValueFrom, Observable, Subscription } from 'rxjs';
import { HttpService } from './http.service';
import { ClientSettingsService } from './client.settings.service';
import { ToastService } from './toast.service';

export interface Column {
  name: string;
  label: string;
  userFilterable: boolean;
}

export interface TableData {
  rows: Record<string, string>[];
  columns: Column[];
  numericMetadataNames: Set<string>;
}

@Injectable({
  providedIn: 'root',
})
export class FilterService implements OnDestroy {
  private currentView?: View;
  private notUserFilterableMetadataNames = new Set<string>();
  private numericMetadataNames = new Set<string>();
  private columns: Column[] = [];
  private tableDataSubject = new BehaviorSubject<TableData | undefined>(undefined);
  private userFilterColumnsSubject = new BehaviorSubject<Column[] | undefined>(undefined);
  private userFilterChoicesSubject = new BehaviorSubject<Map<string, string[]> | undefined>(undefined);
  private userFiltersSubject = new BehaviorSubject<MetadataFilter[]>([]);
  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private errorHandling = inject(ErrorHandler);
  private toastService = inject(ToastService);
  private tabService = inject(TabService);
  private subscriptions = new Subscription();
  private subscribed = false;
  public userFilters$ = this.userFiltersSubject.pipe(debounceTime(300));
  public urlFilters: MetadataFilter[] = [];
  public viewFilters: MetadataFilter[] = [];
  private _userFiltersBeingEdited: Record<string, string> = {};
  public set userFiltersBeingEdited(_userFiltersBeingEdited: Record<string, string>) {
    this._userFiltersBeingEdited = _userFiltersBeingEdited;
    this.userFiltersSubject.next(this.parseUserFilters(this._userFiltersBeingEdited));
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

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  setCurrentView(currentView: View): void {
    this.currentView = currentView;
    this.initialize(this.currentView);
  }

  setUrlFilters(urlFilters: MetadataFilter[]): void {
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
    this.userFiltersSubject.next(this.parseUserFilters(this.userFiltersBeingEdited));
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
    for (const filter of this.urlFilters) {
      this.userFiltersBeingEdited[filter.metadataName] = filter.value;
    }
    this.onUserFilterChanged();
  }

  private initialize(currentView: View): void {
    if (!this.subscribed) {
      this.subscribeToSubscriptions();
      this.subscribed = true;
    }
    this.setViewFilters(currentView);
    this.setNotUserFilterableMetadataNames();
    this.setNumericMetadataNames(currentView);
    this.setColumns(currentView);
    this.userFilterColumnsSubject.next(this.columns.filter((c) => c.userFilterable === true));
    this.onUserFilterChanged();
    this.resetFilters();
  }

  private subscribeToSubscriptions(): void {
    const userFilterSubscription = this.userFilters$.subscribe((userFilters) => {
      this.update(userFilters, this.currentView!);
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
        this.viewFilters.push(
          this.tabService.filterQuery2MetadataFilter(metadataName, currentView.metadataFilter![metadataName]),
        );
      }
    }
  }

  private setNotUserFilterableMetadataNames(): void {
    this.notUserFilterableMetadataNames.clear();
    for (const metadataName of this.viewFilters.map((f) => f.metadataName)) {
      this.notUserFilterableMetadataNames.add(metadataName);
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
        userFilterable: !this.notUserFilterableMetadataNames.has(metadataName),
      });
    }
  }

  private update(userFilters: MetadataFilter[], currentView: View): void {
    const allFilters: MetadataFilter[] = [...this.viewFilters, ...userFilters];
    firstValueFrom(
      this.httpService.getMetadata(currentView, {
        metadataNames: currentView.metadataNames,
        filterHeader: allFilters.map((f) => f.metadataName),
        filter: allFilters.map((f) => f.value),
        limit: this.clientSettingsService.getAmountOfRecordsInTable(),
      }),
    )
      .then((metadata) => {
        this.lastMetadata = metadata;
        this.tableDataSubject.next(this.metadata2TableData(metadata, allFilters, currentView));
        this.userFilterChoicesSubject.next(this.getUniqueOptions(metadata));
        this.toastService.showSuccess('Data loaded!');
      })
      .catch((error) => {
        this.errorHandling.handleError(error);
      });
  }

  private metadata2TableData(
    metadata: Record<string, string>[],
    allFilters: MetadataFilter[],
    currentView: View,
  ): TableData {
    const exactFilteredNames = new Set<string>(allFilters.filter((f) => f.exact).map((f) => f.metadataName));
    const retainedMetadataNames = new Set<string>(currentView.metadataNames.filter((n) => !exactFilteredNames.has(n)));
    const columns = this.columns.filter((c) => retainedMetadataNames.has(c.name));
    // No need to remove data from metadata and numericMetadataNames about columns that
    // are not in the retained columns. The observer taking this data should be able to
    // handle this unused data.
    return {
      rows: metadata,
      columns,
      numericMetadataNames: this.numericMetadataNames,
    };
  }

  private getUniqueOptions(rows: Record<string, string>[]): Map<string, string[]> {
    const result: Map<string, string[]> = new Map<string, string[]>();
    for (const column of this.columns.filter((c) => c.userFilterable)) {
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

  private parseUserFilters(rawValues: Record<string, string>): MetadataFilter[] {
    if (!this.currentView) {
      throw new Error('Cannot happen because we subscribe to subscriptions only after receiving the first view');
    }
    const values = new Map<string, string>(Object.entries(rawValues));
    let metadataNames: string[] = [...values.keys()];
    metadataNames.sort();
    const userFilters: MetadataFilter[] = [];
    for (const metadataName of metadataNames) {
      const value: string = values.get(metadataName)!;
      userFilters.push(this.tabService.filterQuery2MetadataFilter(metadataName, value));
    }
    return userFilters;
  }
}
