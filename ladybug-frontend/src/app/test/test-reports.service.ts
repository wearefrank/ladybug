import { inject, Injectable } from '@angular/core';
import { TestListItem } from '../shared/interfaces/test-list-item';
import { HttpService } from '../shared/services/http.service';
import { catchError, firstValueFrom, Observable, ReplaySubject } from 'rxjs';
import { ErrorHandling } from '../shared/classes/error-handling.service';

@Injectable({
  providedIn: 'root',
})
export class TestReportsService {
  metadataNames: string[] = ['storageId', 'name', 'path', 'description', 'variables'];
  storageName = 'Test';
  private testReportsSubject: ReplaySubject<TestListItem[]> = new ReplaySubject<TestListItem[]>(1);

  testReports$: Observable<TestListItem[]> = this.testReportsSubject.asObservable();
  private firstApiCall = true;

  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);

  constructor() {
    this.getReports();
  }

  getReports(): void {
    this.httpService
      .getTestReports(this.metadataNames, this.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: async (response: TestListItem[]) => {
          const sortedReports = this.sortByName(response);
          if (this.firstApiCall) {
            this.testReportsSubject.next(sortedReports);
            this.firstApiCall = false;
          } else {
            this.testReportsSubject.next(await this.matchRerunResults(sortedReports));
          }
        },
      });
  }

  async matchRerunResults(reports: TestListItem[]): Promise<TestListItem[]> {
    const oldReports: TestListItem[] = await firstValueFrom(this.testReportsSubject);
    const filteredReports: TestListItem[] = oldReports.filter((report: TestListItem) => !!report.reranReport);
    if (filteredReports.length > 0) {
      for (const report of reports) {
        for (const oldReport of filteredReports) {
          if (report.storageId === oldReport.storageId) {
            report.reranReport = oldReport.reranReport;
          }
        }
      }
    }
    return reports;
  }

  sortByName(reports: TestListItem[]): TestListItem[] {
    reports.sort((a: TestListItem, b: TestListItem): number => (a.name > b.name ? 1 : a.name === b.name ? 0 : -1));
    return reports;
  }
}
