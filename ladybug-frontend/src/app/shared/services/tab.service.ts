import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { KEY_COMPARE, KEY_DEBUG, KEY_REPORT, KEY_TEST, routeKind, Tab } from '../interfaces/tab';
import { ActivatedRouteSnapshot, DetachedRouteHandle } from '@angular/router';
import { isNumber } from '../../shared/util/util';
import { CompareData } from '../../compare/compare-data';
import { HierarchicalReport } from '../interfaces/hierarchical-report';

@Injectable({
  providedIn: 'root',
})
export class TabService {
  private activeTabsList: Tab[] = [
    {
      kind: KEY_DEBUG,
      key: KEY_DEBUG,
      title: 'Debug',
    },
    {
      kind: KEY_TEST,
      key: KEY_TEST,
      title: 'Test',
    },
  ];

  private refreshSubject: Subject<void> = new ReplaySubject();
  refresh$ = this.refreshSubject as Observable<void>;
  private compareCache: Map<string, CompareData> = new Map<string, CompareData>();
  private reportCache: Map<string, HierarchicalReport> = new Map<string, HierarchicalReport>();

  getTabs(): Tab[] {
    return [...this.activeTabsList];
  }

  removeTab(key: string): void {
    this.activeTabsList = this.activeTabsList.filter((t) => t.key !== key);
    this.refreshSubject.next();
    if (this.compareCache.has(key)) {
      this.compareCache.delete(key);
    }
    if (this.reportCache.has(key)) {
      this.reportCache.delete(key);
    }
  }

  openReportTab(storageName: string, storageId: number, name: string, report?: HierarchicalReport): string {
    const key: string = this.getReportTabKey(storageName, storageId);
    if (this.findTab(key) === undefined) {
      this.addTab(KEY_REPORT, key, name);
    }
    // Do not renew cache when same storage name and storage id combination
    // is opened with an updated report. The user should close the tab
    // to update the cache.
    if (report !== undefined && this.reportCache.get(key) === undefined) {
      this.reportCache.set(key, report);
    }
    this.refreshSubject.next();
    return key;
  }

  openCompareTab(
    leftStorageName: string,
    leftStorageId: number,
    rightStorageName: string,
    rightStorageId: number,
    data: CompareData,
  ): string {
    const key: string = this.getCompareTabKey(leftStorageName, leftStorageId, rightStorageName, rightStorageId);
    if (this.findTab(key) === undefined) {
      this.addTab(KEY_COMPARE, key, 'Compare');
      this.compareCache.set(key, data);
    }
    this.refreshSubject.next();
    return key;
  }

  getCompareData(key: string): CompareData | undefined {
    return this.compareCache.get(key);
  }

  getReportData(key: string): HierarchicalReport | undefined {
    return this.reportCache.get(key);
  }

  storeHandle(route: ActivatedRouteSnapshot, handle: DetachedRouteHandle): void {
    const key = this.getKey(route);
    const tab: Tab = this.getOrMakeTabFromKey(key);
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
      case KEY_DEBUG:
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

  private addTab(kind: routeKind, key: string, title?: string): Tab {
    const tab: Tab = {
      kind,
      key,
      title: title === undefined ? key : title,
    };
    this.activeTabsList.push(tab);
    return tab;
  }

  private findTab(key: string): Tab | undefined {
    const result: Tab[] = this.activeTabsList.filter((t) => t.key === key);
    if (result.length === 0) {
      return undefined;
    } else if (result.length === 1) {
      return result[0];
    } else {
      throw new Error(`Multiple tabs found for key ${key}`);
    }
  }

  private routeParam(route: ActivatedRouteSnapshot, parameter: string): string {
    return route.params[parameter] || '';
  }

  private getOrMakeTabFromKey(key: string): Tab {
    let tab: Tab | undefined = this.findTab(key);
    if (tab === undefined) {
      tab = this.addTab(KEY_REPORT, key);
    }
    return tab;
  }
}
