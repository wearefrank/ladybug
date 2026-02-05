import { Injectable } from '@angular/core';
import { debounceTime, Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class FilterService {
  private showFilterSubject = new Subject<boolean>();
  private metadataLabelsSubject = new Subject<string[]>();
  private filterContextSubject = new Subject<Map<string, string>>();
  private currentRecordsSubject = new Subject<Map<string, string[]>>();
  private metadataTypesSubject = new Subject<Map<string, string>>();
  private filterErrorSubject = new Subject<[boolean, Map<string, string>]>();
  private filterSidePanelVisibleSubject = new Subject<boolean>();

  filterSidePanel$: Observable<boolean> = this.filterSidePanelVisibleSubject.asObservable();
  showFilter$: Observable<boolean> = this.showFilterSubject.asObservable();
  metadataLabels$: Observable<string[]> = this.metadataLabelsSubject.asObservable();
  currentRecords$: Observable<Map<string, string[]>> = this.currentRecordsSubject.asObservable();
  metadataTypes$: Observable<Map<string, string>> = this.metadataTypesSubject.asObservable();
  filterContext$: Observable<Map<string, string>> = this.filterContextSubject.pipe(debounceTime(300));

  private metadataLabels: string[] = [];
  private filters: Map<string, string> = new Map<string, string>();
  private metadataTypes: Map<string, string> = new Map<string, string>();

  setShowFilter(show: boolean): void {
    this.showFilterSubject.next(show);
  }

  setMetadataLabels(metadataLabels: string[]): void {
    //Safely transform old filter to filter with new metadata columns
    let wasChanged = false;
    for (const metadataLabel of this.metadataLabels) {
      if (!metadataLabels.includes(metadataLabel)) {
        this.filters.delete(metadataLabel);
        wasChanged = true;
      }
    }
    if (wasChanged) {
      this.filterContextSubject.next(this.filters);
    }
    this.metadataLabels = metadataLabels;
    this.metadataLabelsSubject.next(metadataLabels);
  }

  updateFilterContext(filterName: string, filterContext: string): void {
    if (filterContext.length > 0) this.filters.set(filterName, filterContext);
    else this.filters.delete(filterName);
    this.filterContextSubject.next(this.filters);
  }

  getCurrentFilterContext(): Map<string, string> {
    return this.filters;
  }

  resetFilter(): void {
    this.filters = new Map<string, string>();
    this.filterContextSubject.next(this.filters);
  }

  setCurrentRecords(records: Map<string, string[]>): void {
    this.currentRecordsSubject.next(records);
  }

  setMetadataTypes(metadataTypes: Map<string, string>): void {
    this.metadataTypes = new Map<string, string>(Object.entries(metadataTypes));
    this.metadataTypesSubject.next(this.metadataTypes);
  }

  toggleShowFilterSidePanel(value: boolean): void {
    this.filterSidePanelVisibleSubject.next(value);
  }
}
