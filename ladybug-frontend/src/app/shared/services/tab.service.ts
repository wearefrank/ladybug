import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { ReportData } from '../interfaces/report-data';
import { CompareData } from '../../compare/compare-data';
import { CloseTab } from '../interfaces/close-tab';
import { Report } from '../interfaces/report';

@Injectable({
  providedIn: 'root',
})
export class TabService {
  activeReportTabs = new Map<string, ReportData>();
  activeCompareTabs = new Map<string, CompareData>();

  private openReportInTabSubject: Subject<ReportData> = new ReplaySubject();
  private openInCompareSubject: Subject<CompareData> = new ReplaySubject();
  private closeTabSubject: Subject<CloseTab> = new ReplaySubject();

  openReportInTab$: Observable<ReportData> = this.openReportInTabSubject.asObservable();

  openInCompare$: Observable<CompareData> = this.openInCompareSubject.asObservable();

  closeTab$: Observable<CloseTab> = this.closeTabSubject.asObservable();

  openNewTab(value: ReportData): void {
    this.activeReportTabs.set(value.report.storageId.toString(), value);
    this.openReportInTabSubject.next(value);
  }

  openNewCompareTab(value: CompareData): void {
    this.openInCompareSubject.next(value);
    this.activeCompareTabs.set(value.id, value);
  }

  createCompareTabId(originalReport: Report, runResultReport: Report): string {
    return `${originalReport.storageId}-${runResultReport.storageId}`;
  }

  closeTab(value: CompareData | ReportData): void {
    let closeTab: CloseTab;
    if (this.isCompareData(value)) {
      this.activeCompareTabs.delete(value.id);
      closeTab = { id: value.id, type: 'compare' };
    } else {
      this.activeReportTabs.delete(value.report.storageId.toString());
      closeTab = { id: value.report.storageId.toString(), type: 'report' };
    }
    this.closeTabSubject.next(closeTab);
  }

  isCompareData(value: CompareData | ReportData): value is CompareData {
    return !!value && !!(value as CompareData).originalReport;
  }
}
