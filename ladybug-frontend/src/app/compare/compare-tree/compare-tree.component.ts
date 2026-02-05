import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { Report } from '../../shared/interfaces/report';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ReportHierarchyTransformer } from '../../shared/classes/report-hierarchy-transformer';
import { ReportUtil as ReportUtility } from '../../shared/util/report-util';
import { CompareData } from '../compare-data';
import { Checkpoint } from '../../shared/interfaces/checkpoint';
import { NodeLinkStrategy } from '../../shared/enums/node-link-strategy';
import {
  CreateTreeItem,
  FileTreeItem,
  FileTreeOptions,
  NgSimpleFileTree,
  OptionalParameters,
} from 'ng-simple-file-tree';
import { SimpleFileTreeUtil as SimpleFileTreeUtility } from '../../shared/util/simple-file-tree-util';

export const treeSideConst = ['left', 'right'] as const;
export type TreeSide = (typeof treeSideConst)[number];

@Component({
  selector: 'app-compare-tree',
  templateUrl: './compare-tree.component.html',
  styleUrls: ['./compare-tree.component.css'],
  imports: [ReactiveFormsModule, FormsModule, NgSimpleFileTree],
})
export class CompareTreeComponent {
  @Output() compareEvent: EventEmitter<void> = new EventEmitter<void>();
  @ViewChild('leftTree') leftTree!: NgSimpleFileTree;
  @ViewChild('rightTree') rightTree!: NgSimpleFileTree;
  @Input({ required: true }) nodeLinkStrategy!: NodeLinkStrategy;
  @Input({ required: true }) compareData!: CompareData;
  leftReport?: Report;
  rightReport?: Report;

  leftTreeOptions: FileTreeOptions = {
    highlightOpenFolders: false,
    doubleClickToOpenFolders: false,
    folderBehaviourOnClick: 'select',
    expandAllFolders: true,
    determineIconClass: SimpleFileTreeUtility.conditionalCssClass,
    hierarchyLines: {
      vertical: true,
    },
  };

  rightTreeOptions: FileTreeOptions = {
    ...this.leftTreeOptions,
    determineFontColor: (item: CreateTreeItem) => this.setRedLabels(item),
  };

  createTrees(leftReport: Report, rightReport: Report): void {
    this.leftReport = new ReportHierarchyTransformer().transform(leftReport);
    this.rightReport = new ReportHierarchyTransformer().transform(rightReport);
    const optional: OptionalParameters = { childrenKey: 'checkpoints' };
    this.leftTree.addItem(this.leftReport, optional);
    this.rightTree.addItem(this.rightReport, optional);
    this.selectFirstItem();
  }

  setRedLabels(item: CreateTreeItem): string {
    if (ReportUtility.isReport(item)) {
      return this.namesMatch(item.name, this.leftReport?.name);
    }
    if (ReportUtility.isCheckPoint(item)) {
      const checkpoint: Checkpoint | null = this.findCorrespondingCheckpoint(item, this.leftReport);
      return this.namesMatch(item.name, checkpoint?.name);
    }
    return '';
  }

  selectFirstItem(): void {
    if (ReportUtility.isReport(this.leftReport!)) {
      this.leftTree.selectItem(this.leftReport.path);
    }
    if (ReportUtility.isReport(this.rightReport)) {
      this.rightTree.selectItem(this.rightReport.path);
    }
    this.compareEvent.emit();
  }

  syncTrees(treeSide: TreeSide): void {
    if (treeSide === 'left') {
      this.selectItem(this.rightTree, this.leftTree.getSelected());
    } else if (treeSide === 'right') {
      this.selectItem(this.leftTree, this.rightTree.getSelected());
    }
  }

  selectItem(otherSide: NgSimpleFileTree, treeItem: FileTreeItem): void {
    // eslint-disable-next-line @typescript-eslint/switch-exhaustiveness-check
    switch (this.nodeLinkStrategy) {
      case 'PATH': {
        otherSide.selectItem(treeItem.path);
        break;
      }
      case 'CHECKPOINT_NUMBER': {
        this.selectByCheckPointNumber(otherSide, treeItem.originalValue as Report | Checkpoint);
      }
    }
    this.compareEvent.emit();
  }

  selectByCheckPointNumber(tree: NgSimpleFileTree, treeItem: Report | Checkpoint): void {
    if (ReportUtility.isReport(treeItem)) {
      tree.selectItem(tree.items[0].path);
    } else if (ReportUtility.isCheckPoint(treeItem)) {
      this.selectCheckpoint(tree, tree.items[0], treeItem);
    }
  }

  selectCheckpoint(tree: NgSimpleFileTree, item: FileTreeItem, checkpointToMatch: Checkpoint): void {
    if (item.children) {
      for (const child of item.children) {
        const checkpoint = child.originalValue as Checkpoint;
        if (
          ReportUtility.getCheckpointIdFromUid(checkpoint.uid) ===
          ReportUtility.getCheckpointIdFromUid(checkpointToMatch.uid)
        ) {
          tree.selectItem(child.path);
          return;
        } else {
          this.selectCheckpoint(tree, child, checkpointToMatch);
        }
      }
    }
  }

  namesMatch(name1: string | undefined, name2?: string | undefined): string {
    if (name1 && name2 && name1 === name2) {
      return '';
    }
    return 'red';
  }

  findCorrespondingCheckpoint(
    itemToMatch: Checkpoint | Report,
    report: Report | Checkpoint | undefined,
  ): Checkpoint | null {
    if (report) {
      const checkpoints: Checkpoint[] = report.checkpoints ?? [];
      for (let checkpoint of checkpoints) {
        if (
          ReportUtility.isCheckPoint(itemToMatch) &&
          ReportUtility.getCheckpointIdFromUid(checkpoint.uid) === ReportUtility.getCheckpointIdFromUid(itemToMatch.uid)
        ) {
          return checkpoint;
        }
        if (checkpoint.checkpoints) {
          return this.findCorrespondingCheckpoint(itemToMatch, checkpoint);
        }
      }
    }
    return null;
  }
}
