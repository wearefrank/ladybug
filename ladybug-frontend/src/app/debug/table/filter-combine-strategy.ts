import { Injectable } from '@angular/core';
import { FilterFromUrl } from 'src/app/shared/services/tab.service';

export interface Request {
  filterNames: string[];
  filterValues: string[];
  metadataNames: string[];
}

@Injectable({
  providedIn: 'root',
})
export class FilterCombineStrategy {
  private metadataNamesOfView?: string[];
  private filtersFromUrl?: FilterFromUrl[];
  private filtersFromUrlMap: Map<string, string> = new Map<string, string>();

  private checkInitialized(): void {
    if (this.metadataNamesOfView === undefined || this.filtersFromUrl === undefined) {
      throw new Error('Expected that metadataNamesOfView and filtersFromUrl have been set');
    }
  }

  setMetadataNamesOfView(value: string[]): void {
    this.metadataNamesOfView = value;
  }

  setFiltersFromUrl(value: FilterFromUrl[]): void {
    this.filtersFromUrl = value;
    const m = new Map<string, string>();
    for (const entry of value) {
      m.set(entry.metadataName, entry.value);
    }
    this.filtersFromUrlMap = m;
  }

  // Only these are expected in the filter drawer
  getShownMetadataLabels(): string[] {
    this.checkInitialized();
    return this.metadataNamesOfView!.filter((name) => !this.filtersFromUrlMap.has(name));
  }

  getTypesOfShownMetadataLabels(typesFromView: Map<string, string>): Map<string, string> {
    this.checkInitialized();
    const result = new Map<string, string>();
    const shownMetadataNames: Set<string> = new Set<string>(this.getShownMetadataLabels());
    for (const [key, value] of typesFromView.entries()) {
      if (shownMetadataNames.has(key)) {
        result.set(key, value);
      }
    }
    return result;
  }

  getHttpRequestParameters(userFilterKeys: string[], userFilterValues: string[]): Request {
    this.checkInitialized();
    const shownMetadataLabels: string[] = [...this.getShownMetadataLabels()];
    const allowedUserFilterKeys = new Set<string>(shownMetadataLabels);
    const offensiveUserFilterKeys = userFilterKeys.filter((filter) => !allowedUserFilterKeys.has(filter));
    if (offensiveUserFilterKeys.length > 0) {
      throw new Error(
        `The user was erroneously allowed to filter on values managed by the url: ${offensiveUserFilterKeys}`,
      );
    }
    const urlFilterKeys: string[] = [...this.filtersFromUrl!.map((f) => f.metadataName)];
    return {
      filterNames: [...userFilterKeys, ...urlFilterKeys],
      filterValues: [...userFilterValues, ...this.filtersFromUrl!.map((f) => f.value)],
      metadataNames: [...shownMetadataLabels, ...urlFilterKeys],
    };
  }
}
