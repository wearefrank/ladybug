import { Component, EventEmitter, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { HierarchicalReport, HierarchicalCheckpoint } from '../../shared/interfaces/hierarchical-report';
import { TreeViewCommand, TreeViewComponent } from '../../shared/components/tree-view/tree-view.component';
import { keyOf, REPORT_KEY, ReportTransformer } from './report-transformer';

@Component({
  selector: 'app-debug-tree',
  templateUrl: './debug-tree.component.html',
  styleUrls: ['./debug-tree.component.css'],
  standalone: true,
  imports: [TreeViewComponent],
})
export class DebugTreeComponent {
  @Output() selectReportEvent = new EventEmitter<HierarchicalReport | HierarchicalCheckpoint>();

  protected commands = new Subject<TreeViewCommand>();
  protected lastReport: HierarchicalReport | null = null;
  protected checkpointAndStorageIdShown = false;

  private searchTerm = '';
  // The report and its checkpoints, by the key of their tree node.
  private valuesByKey: ReadonlyMap<string, HierarchicalReport | HierarchicalCheckpoint> = new Map();

  addReportToTree(report: HierarchicalReport): void {
    this.lastReport = report;
    this.showReport('new');
  }

  updateExistingReport(report: HierarchicalReport): void {
    this.lastReport = report;
    this.showReport('update');
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
    const transformer = new ReportTransformer(this.searchTerm, this.checkpointAndStorageIdShown);
    const node = transformer.transform(report);
    this.valuesByKey = transformer.valuesByKey;
    const firstCheckpoint = report.children?.[0];
    const select = firstCheckpoint === undefined ? REPORT_KEY : keyOf(firstCheckpoint);
    this.commands.next({ command, node, select });
  }
}
