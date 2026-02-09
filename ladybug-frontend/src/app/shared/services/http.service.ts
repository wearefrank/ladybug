import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { View } from '../interfaces/view';
import { OptionsSettings } from '../interfaces/options-settings';
import { Report } from '../interfaces/report';
import { CompareReport } from '../interfaces/compare-reports';
import { TestListItem } from '../interfaces/test-list-item';
import { CloneReport } from '../interfaces/clone-report';
import { UploadParameters } from '../interfaces/upload-params';
import { UpdatePathSettings } from '../interfaces/update-path-settings';
import { TestResult } from '../interfaces/test-result';
import { UpdateReport } from '../interfaces/update-report';
import { UpdateReportResponse } from '../interfaces/update-report-response';
import { Transformation } from '../interfaces/transformation';
import { TableSettings } from '../interfaces/table-settings';

@Injectable({
  providedIn: 'root',
})
export class HttpService {
  private readonly headers: HttpHeaders = new HttpHeaders().set('Content-Type', 'application/json');
  private http = inject(HttpClient);

  getViews(): Observable<View[]> {
    return this.http.get<Record<string, View>>('api/testtool/views').pipe(map((response) => Object.values(response)));
  }

  getMetadataReports(settings: TableSettings, view: View): Observable<Report[]> {
    return this.http.get<Report[]>(`api/metadata/${view.storageName}`, {
      params: {
        limit: settings.displayAmount,
        filterHeader: [...settings.currentFilters.keys()],
        filter: [...settings.currentFilters.values()],
        metadataNames: view.metadataNames,
      },
    });
  }

  getUserHelp(storage: string, metadataNames: string[]): Observable<Report> {
    return this.http.get<Report>(`api/metadata/${storage}/userHelp`, {
      params: {
        metadataNames: metadataNames,
      },
    });
  }

  getMetadataCount(storage: string): Observable<number> {
    return this.http.get<number>(`api/metadata/${storage}/count`);
  }

  getLatestReports(amount: number, storage: string): Observable<Report[]> {
    return this.http.get<Report[]>(`api/report/latest/${storage}/${amount}`);
  }

  getReportInProgress(index: number): Observable<Report> {
    return this.http.get<Report>(`api/testtool/in-progress/${index}`);
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

  getReport(reportId: number, storage: string): Observable<Report> {
    return this.http
      .get<Record<string, Report | string>>(`api/report/${storage}/${reportId}?xml=true&globalTransformer=true`)
      .pipe(
        map((e) => {
          const report = e['report'] as Report;
          report.storageName = storage;
          report.xml = e['xml'] as string;
          return report;
        }),
      );
  }

  getReports(reportIds: number[], storage: string): Observable<Record<string, CompareReport>> {
    const transformationEnabled = localStorage.getItem('transformationEnabled') === 'true';
    return this.http
      .get<
        Record<string, CompareReport>
      >(`api/report/${storage}?xml=true&globalTransformer=${transformationEnabled}`, { params: { storageIds: reportIds } })
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

  uploadReport(formData: FormData): Observable<Report[]> {
    return this.http.post<Report[]>('api/report/upload', formData);
  }

  uploadReportToStorage(formData: FormData, storage: string): Observable<void> {
    return this.http.post<void>(`api/report/upload/${storage}`, formData);
  }

  postSettings(settings: UploadParameters): Observable<void> {
    return this.http.post<void>('api/testtool', settings);
  }

  postTransformation(transformation: string): Observable<void> {
    return this.http.post<void>('api/testtool/transformation', {
      transformation: transformation,
    });
  }

  getTransformation(defaultTransformation: boolean): Observable<Transformation> {
    return this.http.get<Transformation>(`api/testtool/transformation/${defaultTransformation}`);
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

  //This endpoint never existed in the backend, so this needs to be refactored
  // replaceReport(reportId: number, storage: string): Observable<void> {
  //   return this.http.put<void>(`api/runner/replace/${storage}/${reportId}`, {
  //     headers: this.headers,
  //   });
  // }

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

  getBackendVersion(): Observable<string> {
    return this.http.get('api/testtool/version', {
      responseType: 'text',
    });
  }

  processCustomReportAction(storage: string, reportIds: number[]): Observable<void> {
    return this.http.post<void>(`api/report/customreportaction?storage=${storage}`, reportIds);
  }
}
