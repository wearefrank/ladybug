import { ChangeDetectorRef, Component, inject, ViewChild, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AngularSplitModule, SplitComponent } from 'angular-split';
import { TabService } from '../shared/services/tab.service';
import { ReportValueComponent } from './report-value/report-value.component';
import { CheckpointValueComponent } from './checkpoint-value/checkpoint-value.component';
import { TestRefreshService } from '../test/test-refresh.service';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { DebugTreeComponent } from '../debug/debug-tree/debug-tree.component';
import { ReportComponentCallback, ReportSharedStrategy } from '../shared/classes/report-shared-strategy';
import { isNumber } from '../shared/util/util';
import { HttpService } from '../shared/services/http.service';
import { firstValueFrom } from 'rxjs';

const MIN_HEIGHT = 20;

@Component({
  selector: 'app-report',
  imports: [AngularSplitModule, DebugTreeComponent, ReportValueComponent, CheckpointValueComponent],
  templateUrl: './report.component.html',
  styleUrl: './report.component.css',
  providers: [ReportSharedStrategy],
})
export class ReportComponent implements ReportComponentCallback, OnInit, AfterViewInit, OnDestroy {
  @ViewChild(SplitComponent) splitter!: SplitComponent;
  @ViewChild(DebugTreeComponent) debugTreeComponent!: DebugTreeComponent;

  protected monacoEditorHeight!: number;
  protected sharedStrategy = inject(ReportSharedStrategy);
  private tabService = inject(TabService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private httpService = inject(HttpService);
  private cdr = inject(ChangeDetectorRef);
  private testRefreshService = inject(TestRefreshService);
  private storageName!: string;
  private storageId!: number;
  private tabKey!: string;

  ngOnInit(): void {
    this.setTabKey();
    this.sharedStrategy.setCallback(this);
    // this.route.url.subscribe(() => this.handleUrlChange());
    this.sharedStrategy.listenToHeight();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.handleUrlChange());
  }

  ngOnDestroy(): void {
    this.sharedStrategy.unsubscribe();
  }

  // Entry point when report is opened from debug table.
  // Also called when this component lives in a dedicated
  // tab after the HierarchicalReport has been fetched
  // via the URL.
  addReport(report: HierarchicalReport): void {
    this.debugTreeComponent.addReportToTree(report);
  }

  closeEntireTree(): void {
    this.debugTreeComponent.closeEntireTree();
    if (this.tabKey) {
      this.tabService.removeTab(this.tabKey);
    }
    this.sharedStrategy.changeReportValueState('none');
  }

  navigateToTestTab(): void {
    this.router.navigate(['/test']);
  }

  onRefreshReport(report: HierarchicalReport): void {
    this.addReport(report);
    // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1129.
    // Add logic to restore the checkpoint that was selected before. Take
    // care not to create an infinite loop - logic reacting to selected node
    // event coming from tree trying to manipulate the debug tree again.
    this.testRefreshService.refreshAll();
  }

  handleHeightChanges(clientHeight: number): void {
    this.monacoEditorHeight = clientHeight;
    if (this.monacoEditorHeight < MIN_HEIGHT) {
      this.monacoEditorHeight = MIN_HEIGHT;
    }
    this.cdr.detectChanges();
  }

  private setTabKey(): void {
    this.storageName = this.tabService.getPathParam(this.route.snapshot, 'storageName');
    const storageIdStr: string = this.tabService.getPathParam(this.route.snapshot, 'storageId');
    if (!isNumber(storageIdStr)) {
      throw new Error(`Cannot open ReportComponent because storage id not a number: ${storageIdStr}`);
    }
    this.storageId = +storageIdStr;
    this.tabKey = this.tabService.getReportTabKey(this.storageName, this.storageId);
  }

  private handleUrlChange(): void {
    // TODO: Take care here when working on issue https://github.com/wearefrank/ladybug-frontend/issues/1125
    const optionalCachedReport = this.tabService.getReportData(this.tabKey);
    if (optionalCachedReport === undefined) {
      firstValueFrom(this.httpService.getHierarchicalReports([this.storageId], this.storageName, null)).then(
        (report: HierarchicalReport[]) => {
          this.handleEntry(report[0]);
        },
      );
    } else {
      this.handleEntry(optionalCachedReport);
    }
  }

  private handleEntry(report: HierarchicalReport): void {
    // This report can have been opened from outside Ladybug. We need to have a Tab.
    if (this.tabService.findTab(this.tabKey) === undefined) {
      this.tabService.openReportTab(this.storageName, this.storageId, report.name, report);
    }
    this.tabService.setTitle(this.tabKey, report.name);
    this.addReport(report);
  }
}
