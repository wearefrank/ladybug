import {
  AfterContentChecked,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { TestListItem } from '../../shared/interfaces/test-list-item';
import { catchError, Subscription } from 'rxjs';
import { HttpService } from '../../shared/services/http.service';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { TabService } from '../../shared/services/tab.service';
import { TestReportsService } from '../test-reports.service';
import { ToastService } from '../../shared/services/toast.service';
import { FormsModule } from '@angular/forms';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { NgClass, NgIf } from '@angular/common';
import { ClientSettingsService } from 'src/app/shared/services/client.settings.service';
import { HierarchicalReport } from 'src/app/shared/interfaces/hierarchical-report';
import { Router } from '@angular/router';
import { CompareData } from '../../compare/compare-data';

@Component({
  selector: 'app-test-table',
  standalone: true,
  imports: [
    FormsModule,
    MatTable,
    MatColumnDef,
    MatCell,
    MatCellDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatRow,
    MatRowDef,
    NgIf,
    NgClass,
  ],
  templateUrl: './test-table.component.html',
  styleUrl: './test-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TestTableComponent implements OnInit, OnDestroy, OnChanges, AfterContentChecked {
  @Input({ required: true }) reports!: TestListItem[];
  @Input() currentFilter = '';
  @Output() runEvent: EventEmitter<TestListItem> = new EventEmitter<TestListItem>();
  @Output() fullyLoaded: EventEmitter<void> = new EventEmitter<void>();
  @Output() changePath: EventEmitter<TestListItem> = new EventEmitter<TestListItem>();
  showStorageIds = true;
  amountOfSelectedReports = 0;
  protected displayedColumns: string[] = [];

  private router = inject(Router);
  private httpService = inject(HttpService);
  private clientSettingsService = inject(ClientSettingsService);
  private errorHandler = inject(ErrorHandling);
  private tabService = inject(TabService);
  private testReportsService = inject(TestReportsService);
  private toastService = inject(ToastService);
  private subscriptions = new Subscription();

  ngOnInit(): void {
    this.subscriptions.add(
      this.clientSettingsService.showStorageIdsInTestTabObservable.subscribe((value) => {
        this.showStorageIds = value;
        this.updateDisplayColumn();
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentFilter'] || changes['reports']) {
      this.amountOfSelectedReports = 0;
      for (const report of this.reports) {
        if (report.checked) {
          this.amountOfSelectedReports++;
        }
      }
      this.getFullPaths();
    }
  }

  ngAfterContentChecked(): void {
    this.fullyLoaded.next();
  }

  updateDisplayColumn(): void {
    this.displayedColumns = ['select'];
    if (this.showStorageIds) {
      this.displayedColumns.push('storageId');
    }
    this.displayedColumns.push('run', 'name', 'description', 'variables', 'runResults', 'options');
  }

  openReport(storageId: number): void {
    this.httpService
      .getHierarchicalReports([storageId], this.testReportsService.storageName, null)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (reports: HierarchicalReport[]): void => {
          // No need to download the same report twice. We cache the report
          // so the report component can fetch it when it opens.
          this.tabService.openReportTab(
            reports[0].storageName,
            reports[0].storageId,
            reports[0].name,
            reports[0],
            // When report closes return to test tab.
            'test',
          );
        },
      });
  }

  getFullPaths(): void {
    for (const report of this.reports) {
      if (report.path) {
        let transformedPath = report.path.replace(this.currentFilter, '');
        if (transformedPath.startsWith('/')) {
          transformedPath = transformedPath.slice(1);
        }
        if (transformedPath.endsWith('/')) {
          transformedPath = transformedPath.slice(0, -1);
        }
        report.fullPath = transformedPath;
      }
    }
  }

  replaceReport(_report: TestListItem): void {
    this.toastService.showWarning('Sorry this is not implemented as of now');
  }

  compareReports(report: TestListItem): void {
    if (report.reranReport) {
      const compareData: CompareData = {
        originalReport: {
          ...report.reranReport.originalReport,
          storageName: this.testReportsService.storageName,
        },
        // Temporary fix until https://github.com/wearefrank/ladybug/issues/283 is fixed
        runResultReport: {
          ...report.reranReport.runResultReport,
          storageName: 'Debug',
        },
      };
      this.tabService.openCompareTab(
        this.testReportsService.storageName,
        report.reranReport.originalReport.storageId,
        this.testReportsService.storageName,
        report.reranReport.runResultReport.storageId,
        compareData,
        // When comparison closes return to test tab
        'test',
      );
    }
  }

  toggleCheck(report: TestListItem): void {
    report.checked = !report.checked;
    if (report.checked) {
      this.amountOfSelectedReports++;
    } else {
      this.amountOfSelectedReports--;
    }
  }

  toggleSelectAll(): void {
    this.amountOfSelectedReports = this.amountOfSelectedReports === this.reports.length ? 0 : this.reports.length;
    if (this.amountOfSelectedReports > 0) {
      this.setCheckedForAllReports(true);
    } else {
      this.setCheckedForAllReports(false);
    }
  }

  setCheckedForAllReports(value: boolean): void {
    for (const report of this.reports) {
      report.checked = value;
    }
  }

  convertToKeyValueFormat(input: string): string {
    if (!input) return '';
    let object: Record<string, string>;
    try {
      object = JSON.parse(input);
    } catch {
      return input;
    }
    return Object.entries(object)
      .map(([key, value]) => `${key}=${value}`)
      .join('\n');
  }
}
