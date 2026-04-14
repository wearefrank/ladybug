import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HttpService } from '../../shared/services/http.service';
import { CreateTreeItem, FileTreeOptions, NgSimpleFileTree } from 'ng-simple-file-tree';
import { SimpleFileTreeUtil as SimpleFileTreeUtility } from '../../shared/util/simple-file-tree-util';
import { DebugTabService } from '../debug-tab.service';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { HierarchicalReport, HierarchicalCheckpoint } from '../../shared/interfaces/hierarchical-report';
import { CHECKPOINT_TYPE_STRINGS, CheckpointType } from '../../shared/enums/checkpoint-type';
import { Observable, Subscription } from 'rxjs';

interface FrankTreeNode {
  name: string;
  iconClass?: string;
  children?: FrankTreeNode[];
  originalValue: HierarchicalReport | HierarchicalCheckpoint;
}

@Component({
  selector: 'app-debug-tree-new',
  templateUrl: './debug-tree-new.component.html',
  styleUrls: ['./debug-tree-new.component.css'],
  standalone: true,
  imports: [NgSimpleFileTree],
})
export class DebugTreeNewComponent implements OnInit, OnDestroy {
  @ViewChild('tree') tree!: NgSimpleFileTree;
  @Input() report$!: Observable<HierarchicalReport | null>;
  @Output() selectReportEvent = new EventEmitter<HierarchicalReport | HierarchicalCheckpoint>();

  subscriptions: Subscription = new Subscription();
  treeOptions: FileTreeOptions = {
    hierarchyLines: {
      vertical: true,
    },
    highlightOpenFolders: false,
    folderBehaviourOnClick: 'select',
    doubleClickToOpenFolders: false,
    autoOpenCondition: this.conditionalOpenFunction,
    determineIconClass: SimpleFileTreeUtility.conditionalCssClass,
  };

  protected checkpointAndStorageIdShown = false;

  private lastReport: HierarchicalReport | null = null;

  private httpService = inject(HttpService);
  private debugTab = inject(DebugTabService);
  private errorHandler = inject(ErrorHandling);

  private readonly THROWABLE_ENCODER: string = 'printStackTrace()';

  ngOnInit(): void {
    this.subscribeToSubscriptions();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  subscribeToSubscriptions(): void {
    const newReport: Subscription = this.report$.subscribe((report: HierarchicalReport | null) => {
      this.lastReport = report;
      this.refreshReports();
    });
    this.subscriptions.add(newReport);
    const refreshAll: Subscription = this.debugTab.refreshAll$.subscribe(() => this.refreshReports());
    this.subscriptions.add(refreshAll);
    const refreshTree: Subscription = this.debugTab.refreshTree$.subscribe(() => this.refreshReports());
    this.subscriptions.add(refreshTree);
  }

  async refreshReports(): Promise<void> {
    if (this.lastReport === null) {
      this.closeEntireTree();
    } else {
      this.closeEntireTree();
      this.addReportToTree(this.lastReport);
    }
  }

  addReportToTree(report: HierarchicalReport): void {
    const preparedReport: FrankTreeNode = this.prepareReportForTree(report);
    const rootNodePath: string = this.tree.addItem(preparedReport);
    this.selectFirstCheckpoint(rootNodePath);
  }

  closeEntireTree(): void {
    this.debugTab.setAnyReportsOpen(false);
    this.tree.clearItems();
  }

  private prepareReportForTree(report: HierarchicalReport): FrankTreeNode {
    return {
      name: this.getReportName(report),
      originalValue: report,
      children: report.children === null ? undefined : report.children.map((c) => this.prepareCheckpointForTree(c)),
    };
  }

  private prepareCheckpointForTree(checkpoint: HierarchicalCheckpoint): FrankTreeNode {
    return {
      name: this.getCheckpointName(checkpoint),
      iconClass: this.getImage(checkpoint.type, checkpoint.encoding ?? '', checkpoint.level),
      originalValue: checkpoint,
      children:
        checkpoint.children === null ? undefined : checkpoint.children.map((c) => this.prepareCheckpointForTree(c)),
    };
  }

  private getReportName(report: HierarchicalReport): string {
    return this.checkpointAndStorageIdShown ? `${report.name} (${report.storageId})` : report.name;
  }

  private getCheckpointName(checkpoint: HierarchicalCheckpoint): string {
    if (this.checkpointAndStorageIdShown) {
      const idComponents: string[] = checkpoint.uid.split('#');
      if (idComponents.length !== 2) {
        throw new Error(`Invalid checkpoint uid: [${checkpoint.uid}]`);
      }
      const id = idComponents[1];
      return `${checkpoint.name} (${id})`;
    } else {
      return checkpoint.name;
    }
  }

  private getImage(type: CheckpointType, encoding: string, level: number): string {
    let iconClass = CHECKPOINT_TYPE_STRINGS[type];
    if (encoding === this.THROWABLE_ENCODER) {
      iconClass += ' red';
    }
    iconClass += level % 2 == 0 ? ' even' : ' odd';
    return iconClass;
  }

  changeSearchTerm(event: KeyboardEvent): void {
    const term: string = (event.target as HTMLInputElement).value;
    this.tree.searchTree(term);
  }

  conditionalOpenFunction(item: CreateTreeItem): boolean {
    const type = item['type'];
    return type === undefined || type === CheckpointType.Startpoint || type === CheckpointType.Endpoint;
  }

  toggleCheckpointAndStorageIdShown(): void {
    this.checkpointAndStorageIdShown = !this.checkpointAndStorageIdShown;
    this.refreshReports();
  }

  private selectFirstCheckpoint(rootNodePath: string): void {
    const last = this.tree.items.length - 1;
    const lastAdded = this.tree.items[last];
    if (lastAdded.children) {
      const firstCheckpoint = lastAdded.children[0];
      this.tree.selectItem(firstCheckpoint.path);
    } else {
      this.tree.selectItem(rootNodePath);
    }
  }
}
