import { Component, Output, ViewChild } from '@angular/core';
import { Subject } from 'rxjs';
import { SimpleFileTreeUtil as SimpleFileTreeUtility } from '../../shared/util/simple-file-tree-util';
import { CreateTreeItem, FileTreeOptions, NgSimpleFileTree } from 'ng-simple-file-tree';
import { TestListItem } from '../../shared/interfaces/test-list-item';

@Component({
  standalone: true,
  imports: [NgSimpleFileTree],
  selector: 'app-test-folder-tree',
  templateUrl: './test-folder-tree.component.html',
  styleUrl: './test-folder-tree.component.css',
})
export class TestFolderTreeComponent {
  @ViewChild('tree') tree!: NgSimpleFileTree;
  @Output() changeFolderEvent: Subject<string> = new Subject<string>();
  readonly rootFolder: CreateTreeItem = {
    name: 'Reports',
    treePath: '',
  };
  treeOptions: FileTreeOptions = {
    hierarchyLines: {
      vertical: true,
    },
    folderBehaviourOnClick: 'select',
    doubleClickToOpenFolders: false,
    expandAllFolders: true,
    highlightOpenFolders: false,
    autoSelectCondition: (item: CreateTreeItem) => item.treePath === this.rootFolder.name,
    determineIconClass: SimpleFileTreeUtility.conditionalCssClass,
  };

  setData(data: TestListItem[]): void {
    this.resetTree();
    this.convertToTree(data);
    this.tree.selectItem(this.rootFolder.path!);
  }

  resetTree(): void {
    this.tree.items = [];
    this.rootFolder.children = [];
  }

  convertToTree(data: TestListItem[]): void {
    for (let item of data) {
      if (item.path && item.path !== 'null') {
        this.createFolderForRemainingPath(
          item,
          item.path.split('/').filter((s) => s !== ''),
          this.rootFolder,
        );
      }
    }
    this.tree.addItem(this.rootFolder);
  }

  createFolderForRemainingPath(itemToAdd: CreateTreeItem, routeParts: string[], currentLocation: CreateTreeItem): void {
    if (routeParts.length === 0) {
      return;
    }

    const nextRoutePart = routeParts[0];
    let newFolder: CreateTreeItem | undefined = currentLocation.children?.find((child) => child.name === nextRoutePart);
    currentLocation.children ??= [];

    if (!newFolder) {
      newFolder = { name: nextRoutePart };
      currentLocation.children.push(newFolder);
    }

    if (currentLocation.children) {
      currentLocation.children.sort((a, b) => a.name.localeCompare(b.name));
    }

    this.createFolderForRemainingPath(itemToAdd, routeParts.slice(1), newFolder);
  }
}
