import { Component, inject, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { HttpService } from '../shared/services/http.service';
import { CloneModalComponent } from './clone-modal/clone-modal.component';
import { TestSettingsModalComponent } from './test-settings-modal/test-settings-modal.component';
import { TestResult } from '../shared/interfaces/test-result';
import { ReranReport } from '../shared/interfaces/reran-report';
import { catchError, Observable, of, Subscription } from 'rxjs';
import { Report } from '../shared/interfaces/report';
import { HelperService } from '../shared/services/helper.service';
import { ToastService } from '../shared/services/toast.service';
import { AppVariablesService } from '../shared/services/app.variables.service';
import { UpdatePathSettings } from '../shared/interfaces/update-path-settings';
import { TestFolderTreeComponent } from './test-folder-tree/test-folder-tree.component';
import { FormsModule, NgModel, ReactiveFormsModule } from '@angular/forms';
import { TestListItem } from '../shared/interfaces/test-list-item';
import { OptionsSettings } from '../shared/interfaces/options-settings';
import { ErrorHandling } from '../shared/classes/error-handling.service';
import { BooleanToStringPipe } from '../shared/pipes/boolean-to-string.pipe';
import { HttpErrorResponse } from '@angular/common/http';
import { TestReportsService } from './test-reports.service';
import { TestTableComponent } from './test-table/test-table.component';
import { DeleteModalComponent } from '../shared/components/delete-modal/delete-modal.component';
import { LoadingSpinnerComponent } from '../shared/components/loading-spinner/loading-spinner.component';
import { TabService } from '../shared/services/tab.service';
import { CompareData } from '../compare/compare-data';
import { CompareReport } from '../shared/interfaces/compare-reports';
import { TestRefreshService } from './test-refresh.service';

export const updatePathActionConst = ['move', 'copy'] as const;
export type UpdatePathAction = (typeof updatePathActionConst)[number];

@Component({
  selector: 'app-test',
  templateUrl: './test.component.html',
  styleUrls: ['./test.component.css'],
  standalone: true,
  imports: [
    TestFolderTreeComponent,
    ReactiveFormsModule,
    FormsModule,
    TestSettingsModalComponent,
    CloneModalComponent,
    BooleanToStringPipe,
    DeleteModalComponent,
    TestTableComponent,
    LoadingSpinnerComponent,
  ],
})
export class TestComponent implements OnInit, OnDestroy {
  static readonly ROUTER_PATH: string = 'test';

  @ViewChild(CloneModalComponent) protected cloneModal!: CloneModalComponent;
  @ViewChild(TestSettingsModalComponent)
  protected testSettingsModal!: TestSettingsModalComponent;
  @ViewChild(DeleteModalComponent) protected deleteModal!: DeleteModalComponent;
  @ViewChild(TestFolderTreeComponent)
  protected testFileTreeComponent?: TestFolderTreeComponent;
  @ViewChild('moveToInput', { read: NgModel })
  protected moveToInputModel?: NgModel;
  @ViewChild(TestTableComponent)
  protected testTableComponent?: TestTableComponent;

  protected reports: TestListItem[] = [];
  protected filteredReports: TestListItem[] = [];
  protected generatorEnabled = false;
  protected currentFilter = '';
  protected loading = true;
  protected showStorageIds?: boolean;
  protected childrenLoaded = false;

  protected testReportsService = inject(TestReportsService);
  protected appVariablesService = inject(AppVariablesService);
  protected currentUploadFile = '';

  private updatePathAction: UpdatePathAction = 'move';
  private testReportServiceSubscription?: Subscription;
  private testRefreshServiceSubscription?: Subscription;
  private httpService = inject(HttpService);
  private helperService = inject(HelperService);
  private toastService = inject(ToastService);
  private errorHandler = inject(ErrorHandling);
  private tabService = inject(TabService);
  private testRefreshService = inject(TestRefreshService);
  private ngZone = inject(NgZone);

  constructor() {
    this.getStorageIdsFromLocalStorage();
    this.setGeneratorStatusFromLocalStorage();
  }

  ngOnInit(): void {
    this.loadData();
    localStorage.removeItem('generatorEnabled');
    this.testRefreshServiceSubscription = this.testRefreshService.refreshAll$.subscribe(() => {
      this.ngZone.run(() => this.loadData());
    });
  }

  ngOnDestroy(): void {
    this.testReportServiceSubscription?.unsubscribe();
    this.testRefreshServiceSubscription?.unsubscribe();
  }

  getStorageIdsFromLocalStorage(): void {
    this.showStorageIds = localStorage.getItem('showReportStorageIds') === 'true';
  }

  updateShowStorageIds(show: boolean): void {
    this.showStorageIds = show;
  }

  setGeneratorStatusFromLocalStorage(): void {
    const generatorStatus: string | null = localStorage.getItem('generatorEnabled');
    if (generatorStatus && generatorStatus.length <= 5) {
      this.generatorEnabled = generatorStatus === 'true';
    } else {
      this.httpService
        .getSettings()
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: (response: OptionsSettings) => {
            this.generatorEnabled = response.generatorEnabled;
            localStorage.setItem('generatorEnabled', String(this.generatorEnabled));
          },
        });
    }
  }

  loadData(path?: string): void {
    this.loading = true;
    if (path && path.endsWith('/')) {
      path = path.slice(0, -1);
    }
    this.testReportServiceSubscription = this.testReportsService.testReports$.subscribe({
      next: (value: TestListItem[]) => {
        this.reports = value;
        if (this.testFileTreeComponent) {
          this.testFileTreeComponent.setData(this.reports);
          this.testFileTreeComponent.tree.selectItem(path ?? this.testFileTreeComponent.rootFolder.name);
        }
        this.matches();
        this.loading = false;
      },
    });
    this.testReportsService.getReports();
  }

  openCloneModal(): void {
    if (this.getSelectedReports().length === 1) {
      this.cloneModal.open(this.getSelectedReports()[0]);
    } else {
      this.toastService.showWarning('Make sure only one report is selected at a time');
    }
  }

  resetRunner(): void {
    for (const report of this.reports) {
      report.reranReport = null;
    }
    this.matches();
  }

  run(report: TestListItem): void {
    if (this.generatorEnabled) {
      this.httpService
        .runReport(this.testReportsService.storageName, report.storageId)
        .pipe(
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          catchError((error: HttpErrorResponse): Observable<any> => {
            report.error = error.message;
            return of(error);
          }),
        )
        .subscribe({
          next: (response: TestResult): void => {
            report.reranReport = this.createReranReport(response);
            this.matches();
          },
        });
    } else {
      this.toastService.showWarning('Generator is disabled!');
    }
  }

  runSelected(): void {
    for (const report of this.getSelectedReports()) {
      this.run(report);
    }
  }

  createReranReport(result: TestResult): ReranReport {
    const originalReport: Report = result.originalReport;
    const runResultReport: Report = result.runResultReport;

    originalReport.xml = result.originalXml;
    runResultReport.xml = result.runResultXml;

    return {
      id: result.originalReport.storageId,
      originalReport: result.originalReport,
      runResultReport: result.runResultReport,
      color: result.equal ? 'green' : 'red',
      resultString: result.info,
    };
  }

  openDeleteModal(deleteAllReports: boolean): void {
    const reportsToBeDeleted: TestListItem[] = this.getSelectedReports();
    if (
      this.filteredReports &&
      (reportsToBeDeleted.length > 0 || (deleteAllReports && this.filteredReports.length > 0))
    ) {
      this.deleteModal.open(deleteAllReports, reportsToBeDeleted);
    } else {
      this.toastService.showWarning('No reports to be deleted!');
    }
  }

  deleteReports(deleteAllReports: boolean): void {
    if (deleteAllReports) {
      this.deleteAllReports();
    } else if (this.reports) {
      this.httpService
        .deleteReport(this.helperService.getSelectedIds(this.filteredReports), this.testReportsService.storageName)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: () => this.loadData(this.currentFilter),
        });
    }
  }

  deleteAllReports(): void {
    this.httpService
      .deleteAllReports(this.testReportsService.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: () => this.loadData(this.currentFilter),
      });
  }

  downloadSelected(): void {
    const selectedReports: TestListItem[] = this.getSelectedReports();
    if (selectedReports.length > 0) {
      let queryString = '';
      for (let report of selectedReports) {
        queryString += `id=${report.storageId}&`;
      }
      this.helperService.download(queryString, this.testReportsService.storageName, true, false);
    } else {
      this.toastService.showWarning('No Report Selected!');
    }
  }

  uploadReport(event: Event): void {
    // Allow same file to be uploaded multiple times
    this.currentUploadFile = '';
    const eventTarget: HTMLInputElement = event.target as HTMLInputElement;
    const file: File | undefined = eventTarget.files?.[0];
    if (file) {
      const formData: FormData = new FormData();
      formData.append('file', file);
      this.httpService
        .uploadReportToStorage(formData, this.testReportsService.storageName)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: () => this.loadData(this.currentFilter),
        });
    }
  }

  copySelected(): void {
    const copiedIds: number[] = this.helperService.getSelectedIds(this.filteredReports);
    const data: Record<string, number[]> = {
      [this.testReportsService.storageName]: copiedIds,
    };
    this.httpService
      .copyReport(data, this.testReportsService.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: () => this.testReportsService.getReports(),
      });
  }

  updatePath(): void {
    const reportIds: number[] = this.helperService.getSelectedIds(this.filteredReports);
    if (reportIds.length > 0) {
      const path: string = this.transformPath(this.moveToInputModel?.value);
      const map: UpdatePathSettings = {
        path: path,
        action: this.updatePathAction,
      };
      this.httpService
        .updatePath(reportIds, this.testReportsService.storageName, map)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: () => this.loadData(path),
        });
    } else {
      this.toastService.showWarning('No Report Selected!');
    }
  }

  setUpdatePathAction(value: UpdatePathAction): void {
    this.updatePathAction = value;
  }

  changeFilter(filter: string): void {
    this.currentFilter =
      !filter || filter === this.testFileTreeComponent?.rootFolder.path ? '' : this.transformPath(filter);
    this.matches();
  }

  transformPath(path: string): string {
    if (path.length > 0 && !path.startsWith('/')) {
      path = `/${path}`;
    }
    if (!path.endsWith('/')) {
      path = `${path}/`;
    }
    return path;
  }

  matches(): void {
    const filteredReports = [];
    for (const report of this.reports) {
      if (report.path === null || report.path === 'null') report.path = '';
      const name: string = report.path + report.name;
      if (new RegExp(`(/)?${this.currentFilter}.*`).test(name)) {
        filteredReports.push(report);
      }
    }
    this.filteredReports = filteredReports;
  }

  getSelectedReports(): TestListItem[] {
    return this.filteredReports.filter((item: TestListItem) => item.checked) ?? [];
  }

  openCompareTab(): void {
    const checkedReportIds = this.testTableComponent?.reports.filter((r) => r.checked).map((r) => r.storageId);
    if (checkedReportIds && checkedReportIds.length == 2) {
      this.httpService
        .getReports(checkedReportIds, this.testReportsService.storageName)
        .subscribe((resp: Record<string, CompareReport>) => {
          const reports: Report[] = Object.values(resp).map((r) => r.report);
          const compareData: CompareData = {
            id: this.tabService.createCompareTabId(reports[0], reports[1]),
            originalReport: reports[0],
            runResultReport: reports[1],
          };
          this.tabService.openNewCompareTab(compareData);
        });
    }
  }

  processCustomReportAction(): void {
    this.httpService
      .processCustomReportAction(
        this.testReportsService.storageName,
        this.helperService.getSelectedIds(this.filteredReports),
      )
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (data: Record<string, string>) => {
          if (data.success) {
            this.toastService.showSuccess(data.success);
          }
          if (data.error) {
            this.toastService.showDanger(data.error);
          }
        },
        error: () => {
          this.toastService.showDanger('Failed to process custom report action');
        },
      });
  }

  protected childComponentsLoaded(): void {
    setTimeout(() => {
      this.childrenLoaded = true;
    });
  }

  protected selectItemInFolderTree(report: TestListItem): void {
    let path = report.path;
    if (path.endsWith('/')) {
      path = path.slice(0, -1);
    }
    this.testFileTreeComponent?.tree?.selectItem(path);
  }
}
