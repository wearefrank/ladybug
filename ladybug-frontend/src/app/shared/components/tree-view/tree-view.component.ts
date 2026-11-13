import { NgClass, NgTemplateOutlet } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

// The kinds of nodes the tree view can show. Each kind has its own icon.
// A report is the root of the tree. The other kinds are checkpoints; their
// names follow the backend enum org.wearefrank.ladybug.CheckpointType.
export type TreeViewNodeType =
  | 'report'
  | 'startpoint'
  | 'endpoint'
  | 'abortpoint'
  | 'inputpoint'
  | 'outputpoint'
  | 'infopoint'
  | 'threadCreatepoint'
  | 'threadStartpoint'
  | 'threadEndpoint';

// Input for the tree view, provided by the user of the component.
// It only holds the key that identifies a node and the business
// information that is shown. How the tree looks is configuration
// that does not belong here. The input only contains the nodes that
// should be visible; nodes to hide are left out by the user.
// The input of the tree view is a single root node; there is
// always exactly one root.
export interface TreeViewInputNode {
  // Identifies the node, for example to select it. Should be unique
  // within the tree and should not change when the shown text changes.
  key: string;
  // Determines the icon. The tree view maps types to icons.
  type: TreeViewNodeType;
  // The text that is shown for the node.
  text: string;
  // True when the value of the node is an error, for example a checkpoint
  // whose message holds a thrown exception (encoding printStackTrace()).
  // The icon of such a node is shown in red.
  valueIsError: boolean;
  // The level of the checkpoint as provided by the backend. It is not
  // the same as the depth in the tree: an endpoint is shown as a child
  // of its startpoint but has the same level. Icons on even levels are
  // black, icons on odd levels are gray. Null for a report.
  level: number | null;
  // Draws attention to the node. Used in the compare tab when the node
  // differs from the corresponding node in the other tree, and in the
  // debug tab when the node matches the search string.
  highlighted: boolean;
  children: TreeViewInputNode[];
}

// A node as it is rendered: the input plus the state that
// changes when the user works with the tree.
export interface TreeViewNode extends Omit<TreeViewInputNode, 'children'> {
  expanded: boolean;
  selected: boolean;
  children: TreeViewNode[];
}

// Instruction to the tree view to show a (new version of a) report.
export interface TreeViewCommand {
  // 'new': discard the current tree and show the given tree. All nodes are
  //   expanded and the node with key `select` is selected.
  // 'update': replace the tree, but keep the selection and the expanded
  //   state of nodes whose key is still present. Nodes that are new are
  //   expanded. If the selected node is gone, the node with key `select`
  //   is selected.
  command: 'new' | 'update';
  // The root of the tree. Keys must be unique within the tree.
  node: TreeViewInputNode;
  // The key of the node to select when no previously selected node can be
  // kept. When absent or when no node has this key, the root is selected.
  select?: string;
}

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

// Shows the tree of a report. After the first command, these invariants hold:
// - Exactly one node is selected.
// - All ancestors of the selected node are expanded.
@Component({
  selector: 'app-tree-view',
  standalone: true,
  imports: [NgClass, NgTemplateOutlet],
  templateUrl: './tree-view.component.html',
  styleUrl: './tree-view.component.css',
})
export class TreeViewComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) commands!: Observable<TreeViewCommand>;
  // Emits the key of the selected node, both when the user selects a node
  // and after each command.
  @Output() nodeSelected = new EventEmitter<string>();

  protected root: TreeViewNode | null = null;
  private selected: TreeViewNode | null = null;
  private nodesByKey = new Map<string, TreeViewNode>();
  private parents = new Map<string, TreeViewNode>();
  private subscription?: Subscription;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['commands']) {
      this.subscription?.unsubscribe();
      this.subscription = this.commands.subscribe((command) => this.execute(command));
    }
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  protected selectByUser(node: TreeViewNode): void {
    this.select(node);
  }

  protected toggleExpanded(node: TreeViewNode, event: Event): void {
    event.stopPropagation();
    node.expanded = !node.expanded;
    // Keep the selected node visible: when one of its ancestors is
    // collapsed, the collapsed node becomes the selected node.
    if (!node.expanded && this.selected && this.isAncestor(node, this.selected)) {
      this.select(node);
    }
  }

  protected getIconClass(node: TreeViewNode): string {
    let result = ICON_CLASSES[node.type];
    if (node.type === 'abortpoint' || node.valueIsError) {
      result += ' red';
    }
    if (node.level !== null) {
      result += node.level % 2 === 0 ? ' even' : ' odd';
    }
    return result;
  }

  private execute(command: TreeViewCommand): void {
    const isUpdate = command.command === 'update';
    const previousNodesByKey = isUpdate ? this.nodesByKey : new Map<string, TreeViewNode>();
    const previousSelectedKey = isUpdate ? this.selected?.key : undefined;
    this.nodesByKey = new Map<string, TreeViewNode>();
    this.parents = new Map<string, TreeViewNode>();
    this.selected = null;
    this.root = this.createNode(command.node, previousNodesByKey, null);
    const toSelect = this.findNode(previousSelectedKey) ?? this.findNode(command.select) ?? this.root;
    this.select(toSelect);
  }

  private createNode(
    input: TreeViewInputNode,
    previousNodesByKey: Map<string, TreeViewNode>,
    parent: TreeViewNode | null,
  ): TreeViewNode {
    const node: TreeViewNode = {
      ...input,
      expanded: previousNodesByKey.get(input.key)?.expanded ?? true,
      selected: false,
      children: [],
    };
    this.nodesByKey.set(node.key, node);
    if (parent) {
      this.parents.set(node.key, parent);
    }
    node.children = input.children.map((child) => this.createNode(child, previousNodesByKey, node));
    return node;
  }

  private findNode(key: string | undefined): TreeViewNode | undefined {
    return key === undefined ? undefined : this.nodesByKey.get(key);
  }

  private select(node: TreeViewNode): void {
    if (this.selected) {
      this.selected.selected = false;
    }
    node.selected = true;
    this.selected = node;
    for (let ancestor = this.parents.get(node.key); ancestor; ancestor = this.parents.get(ancestor.key)) {
      ancestor.expanded = true;
    }
    this.nodeSelected.emit(node.key);
  }

  private isAncestor(candidate: TreeViewNode, node: TreeViewNode): boolean {
    for (let ancestor = this.parents.get(node.key); ancestor; ancestor = this.parents.get(ancestor.key)) {
      if (ancestor === candidate) {
        return true;
      }
    }
    return false;
  }
}
