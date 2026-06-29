import { Injectable } from '@angular/core';
import { ReplaySubject, Subject } from 'rxjs';
import { debugOrTest, KEY_COMPARE, KEY_DEBUG, KEY_REPORT, KEY_TEST, routeKind, Tab } from '../interfaces/tab';
import { ActivatedRouteSnapshot, DetachedRouteHandle, Params } from '@angular/router';
import { isNumber } from '../../shared/util/util';
import { CompareData } from '../../compare/compare-data';
import { HierarchicalReport } from '../interfaces/hierarchical-report';

export interface MetadataFilter {
  metadataName: string;
  value: string;
}

export interface HtmlNavigation {
  path: string[];
  queryParameters?: Record<string, string>;
}

const FILTER_PREFIX = 'filter-';

@Injectable({
  providedIn: 'root',
})
export class TabService {
  private tabs: Tab[] = [
    // We have a default debug tab that does not filter on host or on application.
    // When the user directly visits the URL of a report, then the debug tab should
    // not be lost. If the debug tab is later visited with filter parameters, then
    // these filter parameters are applied by changing the existing debug tab.
    {
      kind: KEY_DEBUG,
      key: KEY_DEBUG,
      title: 'Debug',
      returnToKey: KEY_DEBUG,
    },
    {
      kind: KEY_TEST,
      key: KEY_TEST,
      title: 'Test',
      returnToKey: KEY_TEST,
    },
  ];

  private refreshSubject: Subject<string | null> = new ReplaySubject();
  public refresh$ = this.refreshSubject.asObservable();
  private compareCache: Map<string, CompareData> = new Map<string, CompareData>();
  private reportCache: Map<string, HierarchicalReport> = new Map<string, HierarchicalReport>();

  getTabs(): Tab[] {
    return [...this.tabs];
  }

  removeTab(key: string): void {
    const optionalRemovedTab: Tab | undefined = this.findTab(key);
    const navigation: debugOrTest | null = optionalRemovedTab === undefined ? null : optionalRemovedTab.returnToKey;
    this.tabs = this.tabs.filter((t) => t.key !== key);
    if (this.compareCache.has(key)) {
      this.compareCache.delete(key);
    }
    if (this.reportCache.has(key)) {
      this.reportCache.delete(key);
    }
    this.refreshSubject.next(navigation);
  }

  visitDebugTab(route: ActivatedRouteSnapshot): void {
    const key = this.getKey(route);
    let debugTab: Tab | undefined = this.findDebugTab();
    if (debugTab === undefined) {
      debugTab = {
        kind: KEY_DEBUG,
        key,
        title: 'Debug',
        returnToKey: KEY_DEBUG,
      };
      this.tabs.unshift(debugTab);
    }
    if (debugTab.key !== key) {
      debugTab.handle = undefined;
    }
    debugTab.key = key;
    this.refreshSubject.next(debugTab.key);
  }

  openReportTab(
    storageName: string,
    storageId: number,
    name: string,
    report?: HierarchicalReport,
    returnToKey?: debugOrTest,
  ): void {
    const key: string = this.getReportTabKey(storageName, storageId);
    if (this.findTab(key) === undefined) {
      this.addTab(KEY_REPORT, key, name, returnToKey);
    }
    // Do not renew cache when same storage name and storage id combination
    // is opened with an updated report. The user should close the tab
    // to update the cache.
    if (report !== undefined && this.reportCache.get(key) === undefined) {
      this.reportCache.set(key, report);
    }
    this.refreshSubject.next(key);
  }

  openCompareTab(
    leftStorageName: string,
    leftStorageId: number,
    rightStorageName: string,
    rightStorageId: number,
    data: CompareData,
    returnToKey?: debugOrTest,
  ): void {
    const key: string = this.getCompareTabKey(leftStorageName, leftStorageId, rightStorageName, rightStorageId);
    if (this.findTab(key) === undefined) {
      this.addTab(KEY_COMPARE, key, 'Compare', returnToKey);
      this.compareCache.set(key, data);
    }
    this.refreshSubject.next(key);
  }

  getCompareData(key: string): CompareData | undefined {
    return this.compareCache.get(key);
  }

  getReportData(key: string): HierarchicalReport | undefined {
    return this.reportCache.get(key);
  }

  storeHandle(route: ActivatedRouteSnapshot, handle: DetachedRouteHandle): void {
    const key = this.getKey(route);
    const tab: Tab | undefined = this.findTab(key);
    if (tab === undefined) {
      throw new Error(`TabService.storeHandle() finds no tab with key ${key}`);
    }
    tab.handle = handle;
  }

  getHandle(route: ActivatedRouteSnapshot): DetachedRouteHandle | undefined {
    const key = this.getKey(route);
    const tab: Tab | undefined = this.findTab(key);
    return tab === undefined ? undefined : tab.handle;
  }

  getReportTabKey(storageName: string, storageId: number): string {
    return [KEY_REPORT, storageName, `${storageId}`].join('/');
  }

  getCompareTabKey(
    leftStorageName: string,
    leftStorageId: number,
    rightStorageName: string,
    rightStorageId: number,
  ): string {
    return [KEY_COMPARE, leftStorageName, `${leftStorageId}`, rightStorageName, `${rightStorageId}`].join('/');
  }

  getKey(route: ActivatedRouteSnapshot): string {
    const routePath: string = route.routeConfig?.path || '';
    if (routePath.length === 0) {
      return '';
    }
    const kind = routePath.split('/')[0];
    switch (kind) {
      case KEY_DEBUG: {
        const filters: MetadataFilter[] = this.routeGetFilters(route);
        return `${KEY_DEBUG}${this.encodeFiltersForKey(filters)}`;
      }
      case KEY_TEST: {
        return kind;
      }
      case KEY_REPORT: {
        const storageName: string = this.routeParam(route, 'storageName');
        const storageIdStr: string = this.routeParam(route, 'storageId');
        if (!isNumber(storageIdStr)) {
          throw new Error(`Tried to parse a numberic storage id from string ${storageIdStr}`);
        }
        const storageId: number = +storageIdStr;
        return this.getReportTabKey(storageName, storageId);
      }
      case KEY_COMPARE: {
        const leftStorageName = this.routeParam(route, 'leftStorageName');
        const leftStorageIdStr = this.routeParam(route, 'leftStorageId');
        const rightStorageName = this.routeParam(route, 'rightStorageName');
        const rightStorageIdStr = this.routeParam(route, 'rightStorageId');
        if (!isNumber(leftStorageIdStr)) {
          throw new Error(`Tried to parse a numeric left storage id from string ${leftStorageIdStr}`);
        }
        if (!isNumber(rightStorageIdStr)) {
          throw new Error(`Tried to parse a numeric right storage id from string ${rightStorageIdStr}`);
        }
        const leftStorageId: number = +leftStorageIdStr;
        const rightStorageId: number = +rightStorageIdStr;
        return this.getCompareTabKey(leftStorageName, leftStorageId, rightStorageName, rightStorageId);
      }
      default: {
        throw new Error(`Unknown route kind ${kind}`);
      }
    }
  }

  setTitle(key: string, title: string): void {
    const tab: Tab | undefined = this.findTab(key);
    if (tab === undefined) {
      throw new Error(`Cannot set title of tab because no tab for key ${key}`);
    }
    tab.title = title;
  }

  getPathParam(route: ActivatedRouteSnapshot, parameter: string): string {
    return route.paramMap.get(parameter) as string;
  }

  routeGetFilters(route: ActivatedRouteSnapshot): MetadataFilter[] {
    const filterParameters: string[] = [];
    const queryParameters: Params = route.queryParams;
    for (const [k, _] of Object.entries(queryParameters)) {
      if (k.slice(0, FILTER_PREFIX.length) === FILTER_PREFIX) {
        filterParameters.push(k);
      }
    }
    filterParameters.sort();
    const result: MetadataFilter[] = [];
    for (const s of filterParameters) {
      result.push({
        metadataName: s.slice(FILTER_PREFIX.length),
        // No need to decode. Angular should have done so.
        value: queryParameters[s],
      });
    }
    return result;
  }

  findTab(key: string): Tab | undefined {
    const result: Tab[] = this.tabs.filter((t) => t.key === key);
    if (result.length === 0) {
      return undefined;
    } else if (result.length === 1) {
      return result[0];
    } else {
      throw new Error(`Multiple tabs found for key ${key}`);
    }
  }

  keyToNavigation(key: string): HtmlNavigation {
    const querySplit: string[] = key.split('?');
    if (querySplit.length === 1) {
      return {
        path: this.getPathComponents(querySplit[0]),
      };
    } else if (querySplit.length === 2) {
      return {
        path: this.getPathComponents(querySplit[0]),
        queryParameters: this.getQueryObject(querySplit[1]),
      };
    } else {
      throw new Error(`Could not convert key to Navigation: ${key}`);
    }
  }

  private getPathComponents(path: string): string[] {
    return path.split('/');
  }

  private getQueryObject(queryString: string): Record<string, string> {
    const result: Record<string, string> = {};
    const queryComponents = queryString.split('&');
    for (const queryComponent of queryComponents) {
      const keyAndValue: string[] = queryComponent.split('=');
      if (keyAndValue.length !== 2) {
        throw new Error(`Invalid query string component: ${queryComponent}`);
      }
      const key = keyAndValue[0];
      const value = decodeURIComponent(keyAndValue[1]);
      result[key] = value;
    }
    return result;
  }

  private addTab(kind: routeKind, key: string, title?: string, returnToKey?: debugOrTest): Tab {
    const tab: Tab = {
      kind,
      key,
      title: title === undefined ? key : title,
      returnToKey: returnToKey === undefined ? 'debug' : returnToKey,
    };
    this.tabs.push(tab);
    return tab;
  }

  private routeParam(route: ActivatedRouteSnapshot, parameter: string): string {
    return route.params[parameter] || '';
  }

  private encodeFiltersForKey(filters: MetadataFilter[]): string {
    if (filters.length === 0) {
      return '';
    }
    return `?${filters.map((f) => this.encodeFilterItemForKey(f)).join('&')}`;
  }

  private encodeFilterItemForKey(f: MetadataFilter): string {
    return `${FILTER_PREFIX}${f.metadataName}=${encodeURIComponent(f.value)}`;
  }

  private findDebugTab(): Tab | undefined {
    const result: Tab[] = this.tabs.filter((t) => t.kind === KEY_DEBUG);
    if (result.length === 0) {
      return undefined;
    } else if (result.length === 1) {
      return result[0];
    } else {
      throw new Error('Expected to find exactly one debug tab');
    }
  }
}
