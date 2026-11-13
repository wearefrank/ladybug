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

// Instruction to the tree view.
export type TreeViewCommand = TreeViewShowCommand | TreeViewExpandCommand;

// Instruction to the tree view to show a (new version of a) report.
// Afterwards, the key of the selected node is emitted.
export interface TreeViewShowCommand {
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

// Instruction to the tree view to expand or collapse all nodes. The
// selection does not change, so nothing is emitted.
// 'expandAll': expand all nodes.
// 'collapseAll': collapse all nodes except the ancestors of the selected
//   node, so that the selected node stays visible.
// Ignored when no tree has been shown yet.
export interface TreeViewExpandCommand {
  command: 'expandAll' | 'collapseAll';
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

// What the tree view can do depends on whether it has received a command.
interface TreeViewState {
  // The node to render, or null when there is nothing to render.
  readonly root: TreeViewNode | null;
  // Returns the state that shows the tree of the command.
  show(command: TreeViewShowCommand): Initialized;
  expandAll(): void;
  collapseAll(): void;
  // Selects the node. Returns its key.
  selectByUser(node: TreeViewNode): string;
  // Returns the key of the node that is selected afterwards, or undefined
  // when the selection did not change.
  toggleExpanded(node: TreeViewNode): string | undefined;
}

// No command received yet, so there is no tree.
class Uninitialized implements TreeViewState {
  readonly root = null;

  show(command: TreeViewShowCommand): Initialized {
    // An update without a previous tree acts like 'new'.
    return Initialized.create(command.node, new Map<string, TreeViewNode>(), [command.select]);
  }

  expandAll(): void {
    // There are no nodes to expand.
  }

  collapseAll(): void {
    // There are no nodes to collapse.
  }

  // Cannot happen, because nothing is rendered that the user can click.
  selectByUser(): never {
    throw new Error('Cannot select a node when the tree view has no tree');
  }

  toggleExpanded(): never {
    throw new Error('Cannot expand or collapse a node when the tree view has no tree');
  }
}

// A tree is shown. These invariants hold:
// - Exactly one node is selected.
// - All ancestors of the selected node are expanded.
class Initialized implements TreeViewState {
  private constructor(
    readonly root: TreeViewNode,
    private selected: TreeViewNode,
    private readonly nodesByKey: Map<string, TreeViewNode>,
    private readonly parents: Map<string, TreeViewNode>,
  ) {}

  // Builds the tree from the input. Expanded state is taken from
  // previousNodesByKey for nodes with a known key; other nodes are
  // expanded. The first of keysToSelect that is found is selected,
  // or the root if none is found.
  static create(
    input: TreeViewInputNode,
    previousNodesByKey: Map<string, TreeViewNode>,
    keysToSelect: (string | undefined)[],
  ): Initialized {
    const nodesByKey = new Map<string, TreeViewNode>();
    const parents = new Map<string, TreeViewNode>();
    const root = Initialized.createNode(input, null, previousNodesByKey, nodesByKey, parents);
    let selected = root;
    for (const key of keysToSelect) {
      const found = key === undefined ? undefined : nodesByKey.get(key);
      if (found) {
        selected = found;
        break;
      }
    }
    const result = new Initialized(root, selected, nodesByKey, parents);
    result.select(selected);
    return result;
  }

  private static createNode(
    input: TreeViewInputNode,
    parent: TreeViewNode | null,
    previousNodesByKey: Map<string, TreeViewNode>,
    nodesByKey: Map<string, TreeViewNode>,
    parents: Map<string, TreeViewNode>,
  ): TreeViewNode {
    const node: TreeViewNode = {
      ...input,
      expanded: previousNodesByKey.get(input.key)?.expanded ?? true,
      selected: false,
      children: [],
    };
    nodesByKey.set(node.key, node);
    if (parent) {
      parents.set(node.key, parent);
    }
    node.children = input.children.map((child) =>
      Initialized.createNode(child, node, previousNodesByKey, nodesByKey, parents),
    );
    return node;
  }

  get selectedKey(): string {
    return this.selected.key;
  }

  show(command: TreeViewShowCommand): Initialized {
    if (command.command === 'new') {
      return Initialized.create(command.node, new Map<string, TreeViewNode>(), [command.select]);
    }
    return Initialized.create(command.node, this.nodesByKey, [this.selected.key, command.select]);
  }

  expandAll(): void {
    this.setAllExpanded(true);
  }

  collapseAll(): void {
    this.setAllExpanded(false);
    this.expandAncestors(this.selected);
  }

  selectByUser(node: TreeViewNode): string {
    this.select(node);
    return node.key;
  }

  toggleExpanded(node: TreeViewNode): string | undefined {
    node.expanded = !node.expanded;
    // Keep the selected node visible: when one of its ancestors is
    // collapsed, the collapsed node becomes the selected node.
    if (!node.expanded && this.isAncestor(node, this.selected)) {
      this.select(node);
      return node.key;
    }
    return undefined;
  }

  private select(node: TreeViewNode): void {
    this.selected.selected = false;
    node.selected = true;
    this.selected = node;
    this.expandAncestors(node);
  }

  private expandAncestors(node: TreeViewNode): void {
    for (let ancestor = this.parents.get(node.key); ancestor; ancestor = this.parents.get(ancestor.key)) {
      ancestor.expanded = true;
    }
  }

  private setAllExpanded(expanded: boolean): void {
    for (const node of this.nodesByKey.values()) {
      node.expanded = expanded;
    }
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

// Shows the tree of a report. See Initialized for the invariants that
// hold after the first command.
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
  // and after commands 'new' and 'update'.
  @Output() nodeSelected = new EventEmitter<string>();

  private state: Uninitialized | Initialized = new Uninitialized();
  private subscription?: Subscription;

  protected get root(): TreeViewNode | null {
    return this.state.root;
  }

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
    this.nodeSelected.emit(this.state.selectByUser(node));
  }

  protected toggleExpanded(node: TreeViewNode, event: Event): void {
    event.stopPropagation();
    this.emitIfSelected(this.state.toggleExpanded(node));
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
    switch (command.command) {
      case 'new':
      case 'update': {
        const next = this.state.show(command);
        this.state = next;
        this.nodeSelected.emit(next.selectedKey);
        break;
      }
      case 'expandAll': {
        this.state.expandAll();
        break;
      }
      case 'collapseAll': {
        this.state.collapseAll();
        break;
      }
    }
  }

  private emitIfSelected(key: string | undefined): void {
    if (key !== undefined) {
      this.nodeSelected.emit(key);
    }
  }
}
