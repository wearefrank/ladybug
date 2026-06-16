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
  private metadataLabelsOfView?: string[];
  private filtersFromUrl?: FilterFromUrl[];
  private filtersFromUrlMap: Map<string, string> = new Map<string, string>();
  private shownMetadataNames?: string[];
  private shownMetadataLabels?: string[];

  private isInitialized(): boolean {
    return this.metadataNamesOfView !== undefined && this.filtersFromUrl !== undefined;
  }

  private checkInitialized(): void {
    if (!this.isInitialized()) {
      throw new Error('Expected that metadataNamesOfView and filtersFromUrl have been set');
    }
  }

  setViewInformation(metadataNames: string[], metadataLabels: string[]): void {
    this.metadataNamesOfView = metadataNames;
    this.metadataLabelsOfView = metadataLabels;
    this.update();
  }

  setFiltersFromUrl(value: FilterFromUrl[]): void {
    this.filtersFromUrl = value;
    const m = new Map<string, string>();
    for (const entry of value) {
      m.set(entry.metadataName, entry.value);
    }
    this.filtersFromUrlMap = m;
    this.update();
  }

  getShownMetadataNames(): string[] {
    this.checkInitialized();
    return [...this.shownMetadataNames!];
  }

  // Only these are expected in the filter drawer
  getShownMetadataLabels(): string[] {
    this.checkInitialized();
    return [...this.shownMetadataLabels!];
  }

  getTypesOfShownMetadata(rawTypesFromView: Map<string, string>): Map<string, string> {
    this.checkInitialized();
    // Workaround because rawTypesFromView is not really a Map.
    // View.metadataTypes is captured from JSON. That cannot
    // produce a Map.
    const typesFromView: Map<string, string> = new Map<string, string>(Object.entries(rawTypesFromView));
    const result = new Map<string, string>();
    const shownMetadataNames: Set<string> = new Set<string>(this.getShownMetadataNames());
    for (const [key, value] of typesFromView.entries()) {
      if (shownMetadataNames.has(key)) {
        result.set(key, value);
      }
    }
    return result;
  }

  getHttpRequestParameters(userFilterKeys: string[], userFilterValues: string[]): Request {
    this.checkInitialized();
    const shownMetadataNames: string[] = [...this.getShownMetadataNames()];
    const allowedUserFilterKeys = new Set<string>(shownMetadataNames);
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
      metadataNames: [...shownMetadataNames, ...urlFilterKeys],
    };
  }

  private update(): void {
    if (this.isInitialized()) {
      this.shownMetadataNames = this.metadataNamesOfView!.filter((name) => !this.filtersFromUrlMap.has(name));
      const shownMetadataNamesSet = new Set<string>(this.shownMetadataNames);
      this.shownMetadataLabels = [];
      for (let i = 0; i < this.metadataNamesOfView!.length; ++i) {
        const metadataName = this.metadataNamesOfView![i];
        if (shownMetadataNamesSet.has(metadataName)) {
          this.shownMetadataLabels.push(this.metadataLabelsOfView![i]);
        }
      }
    }
  }
}
