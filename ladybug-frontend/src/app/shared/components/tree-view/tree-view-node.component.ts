import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import type { TreeViewNode, TreeViewNodeType } from './tree-view.component';

const ICON_CLASSES: Record<TreeViewNodeType, string> = {
  report: 'bi bi-folder icon-size',
  startpoint: 'bi bi-arrow-bar-right icon-size bolder-icon',
  endpoint: 'bi bi-arrow-bar-left icon-size bolder-icon',
  abortpoint: 'bi bi-x-lg icon-size scale-down bolder-icon',
  inputpoint: 'bi bi-arrow-right icon-size scale-down bolder-icon',
  outputpoint: 'bi bi-arrow-left icon-size scale-down bolder-icon',
  infopoint: 'bi bi-info-square icon-size scale-down bold-icon',
  threadCreatepoint: 'bi bi-fast-forward icon-size bold-icon',
  threadStartpoint: 'bi bi-chevron-double-right icon-size scale-down bold-icon',
  threadEndpoint: 'bi bi-chevron-double-left icon-size scale-down bold-icon',
};

// Renders one node of a TreeViewComponent and, recursively, its children.
// Only for use by TreeViewComponent, which owns the state of the tree.
// This component does not change the state; it only reports what the user
// clicked. Events of descendants are passed on, so TreeViewComponent
// receives all of them from the root node.
@Component({
  selector: 'app-tree-view-node',
  standalone: true,
  imports: [NgClass],
  templateUrl: './tree-view-node.component.html',
  styleUrl: './tree-view-node.component.css',
})
export class TreeViewNodeComponent {
  @Input({ required: true }) node!: TreeViewNode;
  // The key of the parent node, or null for the root.
  @Input({ required: true }) parentKey!: string | null;
  // Emits the node (this one or a descendant) the user selected.
  @Output() selectRequested = new EventEmitter<TreeViewNode>();
  // Emits the node (this one or a descendant) whose expansion toggle the user clicked.
  @Output() toggleRequested = new EventEmitter<TreeViewNode>();

  protected toggle(event: Event): void {
    // Toggling should not also select the node.
    event.stopPropagation();
    this.toggleRequested.emit(this.node);
  }

  protected getIconClass(): string {
    const node = this.node;
    let result = ICON_CLASSES[node.type];
    if (node.type === 'abortpoint' || node.valueIsError) {
      result += ' red';
    }
    if (node.level !== null) {
      const isClosing = (['endpoint', 'abortpoint', 'threadEndpoint'] as TreeViewNodeType[]).includes(node.type);
      const colorLevel = isClosing ? node.level - 1 : node.level;
      result += colorLevel % 2 === 0 ? ' even' : ' odd';
    }
    return result;
  }
}
