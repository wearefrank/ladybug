import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { View } from '../interfaces/view';
import { OptionsSettings } from '../interfaces/options-settings';
import { Report } from '../interfaces/report';
import { CompareHierarchicalReport, CompareReport } from '../interfaces/compare-reports';
import { TestListItem } from '../interfaces/test-list-item';
import { CloneReport } from '../interfaces/clone-report';
import { UploadParameters } from '../interfaces/upload-params';
import { UpdatePathSettings } from '../interfaces/update-path-settings';
import { TestResult } from '../interfaces/test-result';
import { UpdateReport } from '../interfaces/update-report';
import { UpdateReportResponse } from '../interfaces/update-report-response';
import { ClientSettingsService } from './client.settings.service';
import { HierarchicalCheckpoint, HierarchicalReport } from '../interfaces/hierarchical-report';
import { isNumber } from '../util/util';

export interface MetadataParameters {
  limit: number;
  filterHeader: string[];
  filter: string[];
  metadataNames: string[];
}

@Injectable({
  providedIn: 'root',
})
export class HttpService {
  private readonly headers: HttpHeaders = new HttpHeaders().set('Content-Type', 'application/json');
  private http = inject(HttpClient);
  private clientSettingsService = inject(ClientSettingsService);

  getViews(): Observable<View[]> {
    return this.http.get<Record<string, View>>('api/testtool/views').pipe(map((response) => Object.values(response)));
  }

  getMetadata(view: View, params: MetadataParameters): Observable<Record<string, string>[]> {
    return this.http.get<Record<string, string>[]>(`api/metadata/${view.storageName}`, {
      params: {
        limit: params.limit,
        filterHeader: params.filterHeader,
        filter: params.filter,
        metadataNames: params.metadataNames,
      },
    });
  }

  getUserHelp(storage: string, metadataNames: string[]): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`api/metadata/${storage}/userHelp`, {
      params: {
        metadataNames: metadataNames,
      },
    });
  }

  getMetadataCount(storage: string): Observable<number> {
    return this.http.get<number>(`api/metadata/${storage}/count`);
  }

  // TODO issue https://github.com/wearefrank/ladybug/issues/743.
  // Should return HierarchicalReport instead of Report.
  getLatestReports(amount: number, storage: string): Observable<Report[]> {
    return this.http.get<Report[]>(`api/report/latest/${storage}/${amount}`);
  }

  getReportInProgress(index: number): Observable<HierarchicalReport> {
    return this.http.get<HierarchicalReport>(`api/testtool/in-progress/${index}`);
  }

  deleteReportInProgress(index: number): Observable<Report> {
    return this.http.delete<Report>(`api/testtool/in-progress/${index}`);
  }

  getReportsInProgressThresholdTime(): Observable<number> {
    return this.http.get<number>('api/testtool/in-progress/threshold-time');
  }

  getTestReports(metadataNames: string[], storage: string): Observable<TestListItem[]> {
    return this.http.get<TestListItem[]>(`api/metadata/${storage}`, {
      params: { metadataNames: metadataNames },
    });
  }

  // TODO issue https://github.com/wearefrank/ladybug/issues/743.
  // Remove this and use getHierarchicalReports() instead.
  getReport(reportId: number, storage: string): Observable<Report> {
    const transformationEnabled: string = this.clientSettingsService.isTransformationEnabled() ? 'true' : 'false';
    return this.http
      .get<
        Record<string, Report | string>
      >(`api/report/${storage}/${reportId}?globalTransformer=${transformationEnabled}`)
      .pipe(
        map((e) => {
          const report = e['report'] as Report;
          report.storageName = storage;
          report.xml = e['xml'] as string;
          return report;
        }),
      );
  }

  // TODO issue https://github.com/wearefrank/ladybug/issues/743.
  // Remove this and use getHierarchicalReports() instead.
  getReports(reportIds: number[], storage: string): Observable<Record<string, CompareReport>> {
    const transformationEnabled = this.clientSettingsService.isTransformationEnabled() ? 'true' : 'false';
    return this.http
      .get<
        Record<string, CompareReport>
      >(`api/report/${storage}?globalTransformer=${transformationEnabled}`, { params: { storageIds: reportIds } })
      .pipe(
        map((data) => {
          for (const report of reportIds) {
            data[report].report.xml = data[report].xml;
            data[report].report.storageName = storage;
          }
          return data;
        }),
      );
  }

  // viewName can be null. I opened a report from the test tab and saw URL the following
  // URL working:
  // http://localhost/ladybug/api/report/shownReports/Test?globalTransformer=false&view=&storageIds=0
  getHierarchicalReports(
    reportIds: number[],
    storage: string,
    viewName: string | null,
  ): Observable<HierarchicalReport[]> {
    const transformationEnabled: string = this.clientSettingsService.isTransformationEnabled() ? 'true' : 'false';
    const viewNameString: string = viewName === null ? '' : viewName;
    const viewQuery = `&view=${viewNameString}`;
    return this.http
      .get<
        Record<string, CompareHierarchicalReport>
      >(`api/report/shownReports/${storage}?globalTransformer=${transformationEnabled}${viewQuery}`, { params: { storageIds: reportIds } })
      .pipe(
        map((data) => {
          const result: HierarchicalReport[] = [];
          for (const reportId of reportIds) {
            const report: HierarchicalReport = data[reportId].report;
            report.xml = data[reportId].xml;
            report.checkpointsFromView = viewName;
            result.push(report);
            if (report.children !== null) {
              for (const child of report.children) {
                this.forChildSetReport(child, report);
              }
            }
          }
          return result;
        }),
      );
  }

  private forChildSetReport(child: HierarchicalCheckpoint, report: HierarchicalReport): void {
    child.report = report;
    child.id = this.extractIdFromUid(child.uid);
    if (child.children !== null) {
      for (const grandChild of child.children!) {
        this.forChildSetReport(grandChild, report);
      }
    }
  }

  private extractIdFromUid(uid: string): number {
    const idString: string = uid.split('#')[1];
    if (isNumber(idString)) {
      return +idString;
    } else {
      console.log(`Could not extract id from received report, uid=${uid}`);
      return -1;
    }
  }

  updateReport(reportId: string, body: UpdateReport, storage: string): Observable<UpdateReportResponse> {
    return this.http.post<UpdateReportResponse>(`api/report/${storage}/${reportId}`, body);
  }

  copyReport(data: Record<string, number[]>, storage: string): Observable<void> {
    return this.http.put<void>(`api/report/store/${storage}`, data);
  }

  updatePath(reportIds: number[], storage: string, map: UpdatePathSettings): Observable<void> {
    return this.http.put<void>(`api/report/move/${storage}`, map, {
      params: { storageIds: reportIds },
    });
  }

  uploadReport(formData: FormData): Observable<HierarchicalReport[]> {
    return this.http.post<CompareHierarchicalReport[]>('api/report/upload', formData).pipe(
      map((data) => {
        const result: HierarchicalReport[] = [];
        for (const item of data) {
          const report: HierarchicalReport = item.report;
          report.xml = item.xml;
          report.checkpointsFromView = null;
          result.push(report);
          if (report.children !== null) {
            for (const child of report.children) {
              this.forChildSetReport(child, report);
            }
          }
        }
        return result;
      }),
    );
  }

  uploadReportToStorage(formData: FormData, storage: string): Observable<void> {
    return this.http.post<void>(`api/report/upload/${storage}`, formData);
  }

  postSettingsAsDataAdmin(settings: UploadParameters): Observable<void> {
    return this.http.post<void>('api/testtool', settings);
  }

  postTransformationAsObserver(transformation: string): Observable<void> {
    return this.http.post<void>('api/testtool/transformation', {
      transformation: transformation,
    });
  }

  getSettings(): Observable<OptionsSettings> {
    return this.http.get<OptionsSettings>('api/testtool');
  }

  resetSettings(): Observable<OptionsSettings> {
    return this.http.get<OptionsSettings>('api/testtool/reset');
  }

  runReport(storage: string, reportId: number): Observable<TestResult> {
    return this.http.post<TestResult>(`api/runner/run/${storage}/${reportId}`, {
      headers: this.headers,
      observe: 'response',
    });
  }

  runDisplayReport(reportId: string, storage: string): Observable<Report> {
    return this.http.put<Report>(`api/runner/replace/${storage}/${reportId}`, {
      headers: this.headers,
      observe: 'response',
    });
  }

  cloneReport(storage: string, storageId: number, map: CloneReport): Observable<void> {
    return this.http.post<void>(`api/report/move/${storage}/${storageId}`, map);
  }

  deleteReport(reportIds: number[], storage: string): Observable<void> {
    return this.http.delete<void>(`api/report/${storage}`, {
      params: { storageIds: reportIds },
    });
  }

  deleteAllReports(storage: string): Observable<void> {
    return this.http.delete<void>(`api/report/all/${storage}`);
  }

  // TODO issue https://github.com/wearefrank/ladybug/issues/743.
  // This method should not be needed anymore.
  getUnmatchedCheckpoints(storageName: string, storageId: number, viewName: string): Observable<string[]> {
    return this.http.get<string[]>(`api/report/${storageName}/${storageId}/checkpoints/uids`, {
      params: { view: viewName, invert: true },
    });
  }

  getWarningsAndErrors(storageName: string): Observable<string | undefined> {
    const cleanStorageName: string = storageName.replaceAll(' ', '');
    return this.http.get(`api/report/warningsAndErrors/${cleanStorageName}`, {
      responseType: 'text',
    });
  }

  getStubStrategies(): Observable<string[]> {
    return this.http.get<string[]>(`api/testtool/stub-strategies`, {
      headers: this.headers,
    });
  }

  getVersion(): Observable<string> {
    return this.http.get('api/testtool/version', {
      responseType: 'text',
    });
  }

  processCustomReportAction(storage: string, reportIds: number[]): Observable<void> {
    return this.http.post<void>(`api/report/customreportaction?storage=${storage}`, reportIds);
  }
}
