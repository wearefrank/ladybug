import { Component, EventEmitter, inject, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { DebugTabService } from '../debug-tab.service';
import { HierarchicalReport, HierarchicalCheckpoint } from '../../shared/interfaces/hierarchical-report';
import { CheckpointType } from '../../shared/enums/checkpoint-type';
import {
  TreeViewCommand,
  TreeViewComponent,
  TreeViewInputNode,
  TreeViewNodeType,
} from '../../shared/components/tree-view/tree-view.component';

// The tree always shows one report, so the root needs no unique key.
const REPORT_KEY = 'report';
const THROWABLE_ENCODER = 'printStackTrace()';

const NODE_TYPES: Record<CheckpointType, TreeViewNodeType> = {
  [CheckpointType.Startpoint]: 'startpoint',
  [CheckpointType.Endpoint]: 'endpoint',
  [CheckpointType.Abortpoint]: 'abortpoint',
  [CheckpointType.Inputpoint]: 'inputpoint',
  [CheckpointType.Outputpoint]: 'outputpoint',
  [CheckpointType.Infopoint]: 'infopoint',
  // Value 7 is THREAD_CREATEPOINT in the backend.
  [CheckpointType.ThreadStartpointError]: 'threadCreatepoint',
  [CheckpointType.ThreadStartpoint]: 'threadStartpoint',
  [CheckpointType.ThreadEndpoint]: 'threadEndpoint',
};

@Component({
  selector: 'app-debug-tree',
  templateUrl: './debug-tree.component.html',
  styleUrls: ['./debug-tree.component.css'],
  standalone: true,
  imports: [TreeViewComponent],
})
export class DebugTreeComponent implements OnInit, OnDestroy {
  @Output() selectReportEvent = new EventEmitter<HierarchicalReport | HierarchicalCheckpoint>();

  subscriptions: Subscription = new Subscription();

  protected commands = new Subject<TreeViewCommand>();
  protected lastReport: HierarchicalReport | null = null;
  protected checkpointAndStorageIdShown = false;

  private searchTerm = '';
  // The report and its checkpoints, by the key of their tree node.
  private valuesByKey = new Map<string, HierarchicalReport | HierarchicalCheckpoint>();
  private debugTab = inject(DebugTabService);

  ngOnInit(): void {
    this.subscribeToSubscriptions();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToSubscriptions(): void {
    const refreshAll: Subscription = this.debugTab.refreshAll$.subscribe(() => this.refreshReports());
    this.subscriptions.add(refreshAll);
  }

  addReportToTree(report: HierarchicalReport): void {
    this.lastReport = report;
    this.showReport('new');
  }

  closeEntireTree(): void {
    this.lastReport = null;
    this.commands.next({ command: 'clear' });
    // The search field is removed with the buttons, so forget its value.
    this.searchTerm = '';
  }

  protected expandAll(): void {
    this.commands.next({ command: 'expandAll' });
  }

  protected collapseAll(): void {
    this.commands.next({ command: 'collapseAll' });
  }

  protected changeSearchTerm(event: KeyboardEvent): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.refreshReports();
  }

  protected toggleCheckpointAndStorageIdShown(): void {
    this.checkpointAndStorageIdShown = !this.checkpointAndStorageIdShown;
    this.refreshReports();
  }

  protected emitNodeSelected(key: string): void {
    const value = this.valuesByKey.get(key);
    if (value !== undefined) {
      this.selectReportEvent.emit(value);
    }
  }

  // Shows the current report again, keeping the selection and the
  // expanded state of the nodes.
  private refreshReports(): void {
    if (this.lastReport !== null) {
      this.showReport('update');
    }
  }

  private showReport(command: 'new' | 'update'): void {
    const report = this.lastReport!;
    this.valuesByKey = new Map<string, HierarchicalReport | HierarchicalCheckpoint>();
    const node = this.toReportNode(report);
    const firstCheckpoint = report.children?.[0];
    const select = firstCheckpoint === undefined ? REPORT_KEY : this.keyOf(firstCheckpoint);
    this.commands.next({ command, node, select });
  }

  private toReportNode(report: HierarchicalReport): TreeViewInputNode {
    this.valuesByKey.set(REPORT_KEY, report);
    return this.toNode(REPORT_KEY, 'report', this.getReportName(report), false, null, report.children);
  }

  private toCheckpointNode(checkpoint: HierarchicalCheckpoint): TreeViewInputNode {
    const key = this.keyOf(checkpoint);
    this.valuesByKey.set(key, checkpoint);
    return this.toNode(
      key,
      NODE_TYPES[checkpoint.type as CheckpointType],
      this.getCheckpointName(checkpoint),
      checkpoint.encoding === THROWABLE_ENCODER,
      checkpoint.level,
      checkpoint.children,
    );
  }

  private toNode(
    key: string,
    type: TreeViewNodeType,
    text: string,
    valueIsError: boolean,
    level: number | null,
    children: HierarchicalCheckpoint[] | null,
  ): TreeViewInputNode {
    return {
      key,
      type,
      text,
      valueIsError,
      level,
      highlighted: this.searchTerm !== '' && text.includes(this.searchTerm),
      children: (children ?? []).map((child) => this.toCheckpointNode(child)),
    };
  }

  // The index of the checkpoint in the report. It does not depend on the
  // view and it does not include the storage, which can differ when the
  // same report is retrieved again.
  private keyOf(checkpoint: HierarchicalCheckpoint): string {
    return `${checkpoint.id}`;
  }

  private getReportName(report: HierarchicalReport): string {
    return this.checkpointAndStorageIdShown ? `${report.name} (${report.storageId})` : report.name;
  }

  private getCheckpointName(checkpoint: HierarchicalCheckpoint): string {
    return this.checkpointAndStorageIdShown ? `${checkpoint.name} (${checkpoint.id})` : checkpoint.name;
  }
}
