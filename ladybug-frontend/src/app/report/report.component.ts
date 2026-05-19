import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  inject,
  Input,
  ViewChild,
  OnInit,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AngularSplitModule, SplitComponent } from 'angular-split';
import { debounceTime, fromEventPattern, Subscription } from 'rxjs';
import { DebugComponent } from '../debug/debug.component';
import { HierarchicalReportData } from '../shared/interfaces/report-data';
import { TabService } from '../shared/services/tab.service';
import { NodeEventHandler } from 'rxjs/internal/observable/fromEvent';
import { ReportValueComponent } from './report-value/report-value.component';
import { CheckpointValueComponent } from './checkpoint-value/checkpoint-value.component';
import { DebugTabService } from '../debug/debug-tab.service';
import { TestRefreshService } from '../test/test-refresh.service';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { DebugTreeNewComponent } from '../debug/debug-tree-new/debug-tree-new.component';
import { ReportComponentCallback, ReportSharedStrategy } from '../shared/classes/report-shared-strategy';

const MIN_HEIGHT = 20;
const MARGIN_IF_NOT_NEW_TAB = 50;

@Component({
  selector: 'app-report',
  imports: [AngularSplitModule, DebugTreeNewComponent, ReportValueComponent, CheckpointValueComponent],
  templateUrl: './report.component.html',
  styleUrl: './report.component.css',
  providers: [ReportSharedStrategy],
})
export class ReportComponent implements ReportComponentCallback, OnInit, AfterViewInit, OnDestroy {
  static readonly ROUTER_PATH: string = 'report';
  @Input() newTab = true;
  @ViewChild(SplitComponent) splitter!: SplitComponent;
  @ViewChild(DebugTreeNewComponent) debugTreeComponent!: DebugTreeNewComponent;

  protected monacoEditorHeight!: number;
  protected sharedStrategy = inject(ReportSharedStrategy);
  private host = inject(ElementRef);
  private tabService = inject(TabService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private debugTab = inject(DebugTabService);
  private testRefreshService = inject(TestRefreshService);
  private subscriptions: Subscription = new Subscription();
  private newTabReportData?: HierarchicalReportData;

  ngOnInit(): void {
    this.sharedStrategy.setCallback(this);
    if (this.newTab) {
      this.route.url.subscribe(() => this.handleUrlChange());
    }
    this.listenToHeight();
  }

  ngAfterViewInit(): void {
    if (this.newTab) {
      setTimeout(() => this.handleUrlChange());
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
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
    if (this.newTab && this.newTabReportData) {
      this.tabService.closeTab(this.newTabReportData);
    }
    this.sharedStrategy.changeReportValueState('none');
  }

  navigateToTestTab(): void {
    this.router.navigate(['/test']);
  }

  onRefreshReport(report: HierarchicalReport): void {
    if (this.newTab) {
      this.addReport(report);
      // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1129.
      // Add logic to restore the checkpoint that was selected before. Take
      // care not to create an infinite loop - logic reacting to selected node
      // event coming from tree trying to manipulate the debug tree again.
      this.testRefreshService.refreshAll();
    } else {
      this.debugTab.refreshAll({
        reportIds: [this.sharedStrategy.nodeValueState!.storageId!],
        displayToast: false,
      });
    }
  }

  private handleUrlChange(): void {
    // TODO: Take care here when working on issue https://github.com/wearefrank/ladybug-frontend/issues/1125
    // TODO issue https://github.com/wearefrank/ladybug/issues/816. The storage id is not enough to find
    // the right report. We need to have the storage name in the URL as well.
    this.newTabReportData = this.tabService.activeReportTabs.get(this.getIdFromPath());
    if (this.newTabReportData) {
      this.addReport(this.newTabReportData!.report);
    } else {
      this.router.navigate([DebugComponent.ROUTER_PATH]);
    }
  }

  private getIdFromPath(): string {
    return this.route.snapshot.paramMap.get('id') as string;
  }

  private listenToHeight(): void {
    const resizeObserver$ = fromEventPattern<ResizeObserverEntry[]>((handler: NodeEventHandler) => {
      const resizeObserver = new ResizeObserver(handler);
      resizeObserver.observe(this.host.nativeElement);
      return (): void => resizeObserver.disconnect();
    });

    const resizeSubscription = resizeObserver$.pipe(debounceTime(50)).subscribe((entries: ResizeObserverEntry[]) => {
      const entry = (entries[0] as unknown as ResizeObserverEntry[])[0];
      this.handleHeightChanges(entry.target.clientHeight);
    });
    this.subscriptions.add(resizeSubscription);
  }

  private handleHeightChanges(clientHeight: number): void {
    this.monacoEditorHeight = clientHeight;
    if (!this.newTab) {
      this.monacoEditorHeight = this.monacoEditorHeight - MARGIN_IF_NOT_NEW_TAB;
    }
    if (this.monacoEditorHeight < MIN_HEIGHT) {
      this.monacoEditorHeight = MIN_HEIGHT;
    }
    this.cdr.detectChanges();
  }
}
