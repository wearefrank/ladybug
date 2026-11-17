import { ChangeDetectorRef, Component, inject, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AngularSplitModule, SplitComponent } from 'angular-split';
import { ReportValueComponent } from '../report-value/report-value.component';
import { CheckpointValueComponent } from '../checkpoint-value/checkpoint-value.component';
import { DebugTabService } from '../../debug/debug-tab.service';
import { HierarchicalReport } from '../../shared/interfaces/hierarchical-report';
import { DebugTreeComponent } from '../../debug/debug-tree/debug-tree.component';
import { ReportComponentCallback, ReportSharedStrategy } from '../../shared/classes/report-shared-strategy';
import { DebugTreeStateService, TreeCommand } from '../../debug/debug-tree-state.service';
import { ToastService } from '../../shared/services/toast.service';
import { Subscription } from 'rxjs';

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
  private treeState = inject(DebugTreeStateService);
  private toastService = inject(ToastService);
  private treeCommandSubscription?: Subscription;

  ngOnInit(): void {
    this.sharedStrategy.setCallback(this);
    this.sharedStrategy.listenToHeight();
    this.treeCommandSubscription = this.treeState.treeCommands$.subscribe((command) => this.onTreeCommand(command));
  }

  ngOnDestroy(): void {
    this.sharedStrategy.unsubscribe();
    this.treeCommandSubscription?.unsubscribe();
  }

  // The user closed the report.
  closeEntireTree(): void {
    this.clearTree();
    this.treeState.closed();
  }

  navigateToTestTab(): void {
    this.router.navigate(['/test']);
  }

  onRefreshReport(report: HierarchicalReport): void {
    this.treeState.saved(report);
    this.debugTab.refreshTable({
      reportIds: [report.storageId],
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

  private onTreeCommand(command: TreeCommand): void {
    if (command.command === 'close') {
      this.clearTree();
      return;
    }
    if (command.warnIfEdited && this.sharedStrategy.nodeValueState?.isEdited) {
      this.toastService.showWarning('Unsaved changes were discarded');
    }
    if (command.command === 'new') {
      this.debugTreeComponent.addReportToTree(command.report);
    } else {
      this.debugTreeComponent.updateExistingReport(command.report);
    }
  }

  private clearTree(): void {
    this.debugTreeComponent.closeEntireTree();
    this.sharedStrategy.changeReportValueState('none');
    // Forget whether the closed report was edited.
    this.sharedStrategy.nodeValueState = undefined;
  }
}
