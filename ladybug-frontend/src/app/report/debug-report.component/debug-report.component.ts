import { ChangeDetectorRef, Component, inject, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AngularSplitModule, SplitComponent } from 'angular-split';
import { ReportValueComponent } from '../report-value/report-value.component';
import { CheckpointValueComponent } from '../checkpoint-value/checkpoint-value.component';
import { DebugTabService } from '../../debug/debug-tab.service';
import { HierarchicalReport } from '../../shared/interfaces/hierarchical-report';
import { DebugTreeComponent } from '../../debug/debug-tree/debug-tree.component';
import { ReportComponentCallback, ReportSharedStrategy } from '../../shared/classes/report-shared-strategy';

const MIN_HEIGHT = 20;
const MARGIN = 50;

@Component({
  selector: 'app-debug-report',
  imports: [AngularSplitModule, DebugTreeComponent, ReportValueComponent, CheckpointValueComponent],
  templateUrl: './debug-report.component.html',
  styleUrl: './debug-report.component.css',
  providers: [ReportSharedStrategy],
})
export class DebugReportComponent implements ReportComponentCallback, OnInit, OnDestroy {
  @ViewChild(SplitComponent) splitter!: SplitComponent;
  @ViewChild(DebugTreeComponent) debugTreeComponent!: DebugTreeComponent;

  protected monacoEditorHeight!: number;
  protected sharedStrategy = inject(ReportSharedStrategy);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private debugTab = inject(DebugTabService);

  ngOnInit(): void {
    this.sharedStrategy.setCallback(this);
    this.sharedStrategy.listenToHeight();
  }

  ngOnDestroy(): void {
    this.sharedStrategy.unsubscribe();
  }

  // Entry point when report is opened from debug table.
  addReport(report: HierarchicalReport): void {
    this.debugTreeComponent.addReportToTree(report);
  }

  closeEntireTree(): void {
    this.debugTreeComponent.closeEntireTree();
    this.sharedStrategy.changeReportValueState('none');
  }

  navigateToTestTab(): void {
    this.router.navigate(['/test']);
  }

  onRefreshReport(_: HierarchicalReport): void {
    this.debugTab.refreshAll({
      reportIds: [this.sharedStrategy.nodeValueState!.storageId!],
      displayToast: false,
    });
  }

  handleHeightChanges(clientHeight: number): void {
    this.monacoEditorHeight = clientHeight;
    this.monacoEditorHeight = this.monacoEditorHeight - MARGIN;
    if (this.monacoEditorHeight < MIN_HEIGHT) {
      this.monacoEditorHeight = MIN_HEIGHT;
    }
    this.cdr.detectChanges();
  }
}
