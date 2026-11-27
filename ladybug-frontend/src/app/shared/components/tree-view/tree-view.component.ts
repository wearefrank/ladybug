import {
  afterNextRender,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Injector,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
} from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { TreeViewNodeComponent } from './tree-view-node.component';

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
  // The level of the checkpoint as provided by the backend. A startpoint
  // has level n, its children and its endpoint (or abortpoint) have level
  // n + 1. Icons on even levels are black, icons on odd levels are gray.
  // An endpoint, abortpoint or thread endpoint gets the color of its
  // startpoint, so it is colored as if its level were one lower.
  // Null for a report.
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
export type TreeViewCommand =
  | TreeViewShowCommand
  | TreeViewExpandCommand
  | TreeViewSelectCommand
  | TreeViewClearCommand;

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

// Instruction to the tree view to select the node with the given key and
// expand its ancestors. Ignored when no node has this key or when no tree
// has been shown yet.
//
// Nothing is emitted, for two reasons. First, the caller knows the key
// already. Second, emitting could cause an endless loop when two tree
// views are kept in sync, as in the compare tab: a selection in the left
// tree selects the corresponding node in the right tree. If that emitted,
// it would select the corresponding node in the left tree again, which
// would emit again, and so on.
export interface TreeViewSelectCommand {
  command: 'select';
  key: string;
}

// Instruction to the tree view to remove the tree, for example when the
// report is closed. Afterwards nothing is shown and there is no selected
// node, as before the first command. Nothing is emitted.
export interface TreeViewClearCommand {
  command: 'clear';
}

// Where the selected node was on screen before a command changed the tree.
interface SelectedNodePosition {
  key: string;
  // Distance in pixels from the top of the visible part of the tree view
  // to the top of the selected node.
  offset: number;
}

// The host element of the tree view, which is its scroll box. Measures
// and scrolls to the selected node as it is currently rendered.
class ScrollBox {
  // Nodes have fractional positions but scrollTop is a whole number. A node
  // that is scrolled to an edge can therefore stick out of the visible part
  // by a fraction of a pixel. It still counts as visible.
  private static readonly ROUNDING_TOLERANCE = 1;

  constructor(private readonly host: HTMLElement) {}

  // The distance from the top of the visible part to the top of the
  // selected node, or null when the selected node is not completely
  // visible.
  visibleSelectedNodeOffset(): number | null {
    const element = this.selectedElement();
    if (element === null) {
      return null;
    }
    const offset = this.offsetOf(element);
    const tolerance = ScrollBox.ROUNDING_TOLERANCE;
    if (offset < -tolerance || offset + element.offsetHeight > this.host.clientHeight + tolerance) {
      return null;
    }
    return offset;
  }

  // Scrolls so that the selected node is at the given distance from the
  // top of the visible part.
  moveSelectedNodeTo(offset: number): void {
    const element = this.selectedElement();
    if (element === null) {
      return;
    }
    // distanceFromContentTop is measured from the top of all content,
    // also the part scrolled out of view, down to the node. It consists of
    // two parts: the part scrolled out of view above the visible part,
    // which is scrollTop, and the part from the top of the visible part to
    // the node, which is the offset.
    this.host.scrollTop = this.distanceFromContentTop(element) - offset;
  }

  // Scrolls just enough to show the selected node; when it is visible
  // already, nothing changes.
  revealSelectedNode(): void {
    this.selectedElement()?.scrollIntoView({ block: 'nearest' });
  }

  scrollToTop(): void {
    this.host.scrollTop = 0;
  }

  // The rendered node that is selected. The class is set by
  // TreeViewNodeComponent; it is the row of the node itself, without
  // its children.
  private selectedElement(): HTMLElement | null {
    return this.host.querySelector<HTMLElement>('.sft-item-selected');
  }

  // The distance from the top of the visible part to the element.
  private offsetOf(element: HTMLElement): number {
    return element.getBoundingClientRect().top - this.host.getBoundingClientRect().top;
  }

  // The distance from the top of the content, including the part that is
  // scrolled out of view, to the element. Does not depend on scrollTop.
  private distanceFromContentTop(element: HTMLElement): number {
    return element.getBoundingClientRect().top - this.host.getBoundingClientRect().top + this.host.scrollTop;
  }
}

// What the tree view can do depends on whether it currently shows a tree.
interface TreeViewState {
  // The node to render, or null when there is nothing to render.
  readonly root: TreeViewNode | null;
  // Returns the state that shows the tree of the command.
  show(command: TreeViewShowCommand): Initialized;
  expandAll(): void;
  collapseAll(): void;
  // Selects the node with the given key, if there is one.
  selectByKey(key: string): void;
  // Selects the node. Returns its key.
  selectByUser(node: TreeViewNode): string;
  // Returns the key of the node that is selected afterwards, or null
  // when the selection did not change.
  toggleExpanded(node: TreeViewNode): string | null;
  // Returns where the selected node is on screen, or null when there is
  // no selected node or when it is not completely visible.
  selectedNodePosition(scrollBox: ScrollBox): SelectedNodePosition | null;
  // Scrolls so that the selected node is where it was before, when it is
  // still the selected node. Otherwise scrolls just enough to show the
  // selected node.
  keepSelectedNodeInPlace(before: SelectedNodePosition | null, scrollBox: ScrollBox): void;
}

// No tree, because no tree has been shown yet or because it was cleared.
class Uninitialized implements TreeViewState {
  readonly root = null;

  show(command: TreeViewShowCommand): Initialized {
    // An update without a previous tree acts like 'new'.
    return new StateInitializedBuilder().build(command.node, new Map<string, TreeViewNode>(), [command.select]);
  }

  expandAll(): void {
    // There are no nodes to expand.
  }

  collapseAll(): void {
    // There are no nodes to collapse.
  }

  selectByKey(): void {
    // There are no nodes to select.
  }

  // Cannot happen, because nothing is rendered that the user can click.
  selectByUser(): never {
    throw new Error('Cannot select a node when the tree view has no tree');
  }

  toggleExpanded(): never {
    throw new Error('Cannot expand or collapse a node when the tree view has no tree');
  }

  selectedNodePosition(): null {
    // There is no selected node.
    return null;
  }

  keepSelectedNodeInPlace(): void {
    // There is no selected node to keep in place.
  }
}

// A tree is shown. These invariants hold:
// - Exactly one node is selected.
// - All ancestors of the selected node are expanded.
class Initialized implements TreeViewState {
  // Use StateInitializedBuilder to create an instance.
  constructor(
    readonly root: TreeViewNode,
    private selected: TreeViewNode,
    private readonly nodesByKey: Map<string, TreeViewNode>,
    private readonly parents: Map<string, TreeViewNode>,
  ) {
    this.select(selected);
  }

  get selectedKey(): string {
    return this.selected.key;
  }

  show(command: TreeViewShowCommand): Initialized {
    if (command.command === 'new') {
      return new StateInitializedBuilder().build(command.node, new Map<string, TreeViewNode>(), [command.select]);
    }
    return new StateInitializedBuilder().build(command.node, this.nodesByKey, [this.selected.key, command.select]);
  }

  expandAll(): void {
    this.setAllExpanded(true);
  }

  collapseAll(): void {
    this.setAllExpanded(false);
    this.expandAncestors(this.selected);
  }

  selectByKey(key: string): void {
    const node = this.nodesByKey.get(key);
    if (node) {
      this.select(node);
    }
  }

  selectByUser(node: TreeViewNode): string {
    this.select(node);
    return node.key;
  }

  selectedNodePosition(scrollBox: ScrollBox): SelectedNodePosition | null {
    const offset = scrollBox.visibleSelectedNodeOffset();
    return offset === null ? null : { key: this.selected.key, offset };
  }

  keepSelectedNodeInPlace(before: SelectedNodePosition | null, scrollBox: ScrollBox): void {
    if (before !== null && before.key === this.selected.key) {
      scrollBox.moveSelectedNodeTo(before.offset);
    } else {
      scrollBox.revealSelectedNode();
    }
  }

  toggleExpanded(node: TreeViewNode): string | null {
    node.expanded = !node.expanded;
    // Keep the selected node visible: when one of its ancestors is
    // collapsed, the collapsed node becomes the selected node.
    if (!node.expanded && this.isAncestor(node, this.selected)) {
      this.select(node);
      return node.key;
    }
    return null;
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

// Builds an Initialized state from the input of a show command.
class StateInitializedBuilder {
  private previousNodesByKey = new Map<string, TreeViewNode>();
  private nodesByKey = new Map<string, TreeViewNode>();
  private parents = new Map<string, TreeViewNode>();

  // Builds the tree from the input. Expanded state is taken from
  // previousNodesByKey for nodes with a known key; other nodes are
  // expanded. The first of keysToSelect that is found is selected,
  // or the root if none is found.
  build(
    input: TreeViewInputNode,
    previousNodesByKey: Map<string, TreeViewNode>,
    keysToSelect: (string | undefined)[],
  ): Initialized {
    this.previousNodesByKey = previousNodesByKey;
    this.nodesByKey = new Map<string, TreeViewNode>();
    this.parents = new Map<string, TreeViewNode>();
    const root = this.createNode(input, null);
    return new Initialized(root, this.findSelected(root, keysToSelect), this.nodesByKey, this.parents);
  }

  private createNode(input: TreeViewInputNode, parent: TreeViewNode | null): TreeViewNode {
    const node: TreeViewNode = {
      ...input,
      expanded: this.previousNodesByKey.get(input.key)?.expanded ?? true,
      selected: false,
      children: [],
    };
    this.nodesByKey.set(node.key, node);
    if (parent) {
      this.parents.set(node.key, parent);
    }
    node.children = input.children.map((child) => this.createNode(child, node));
    return node;
  }

  private findSelected(root: TreeViewNode, keysToSelect: (string | undefined)[]): TreeViewNode {
    for (const key of keysToSelect) {
      const found = key === undefined ? undefined : this.nodesByKey.get(key);
      if (found) {
        return found;
      }
    }
    return root;
  }
}

// Shows the tree of a report. See Initialized for the invariants that
// hold after the first command.
//
// Scrolling: when a command changes which nodes are shown above the
// selected node, for example 'update' after the user chose another view,
// the selected node stays at the same height on screen. The content
// around it moves instead. When that is not possible because a different
// node is selected afterwards or because the selected node was not
// visible, the tree view scrolls just enough to show the selected node.
@Component({
  selector: 'app-tree-view',
  standalone: true,
  imports: [TreeViewNodeComponent],
  templateUrl: './tree-view.component.html',
  // The tree view owns its scrollbar. The user of the component gives it
  // a height, for example as a flex item, and the nodes scroll within it.
  // The bottom padding (Bootstrap pb-5) is inside the scroll box, leaving
  // empty space below the last node.
  // Space around the tree view, such as a margin, is up to the user.
  // overflow-anchor: none switches off the scroll anchoring of the browser,
  // which some browsers do and others do not. Keeping the selected node in
  // place is done by this component, see ScrollBox, the same way in every
  // browser.
  host: { class: 'pb-5', style: 'display: block; overflow: auto; overflow-anchor: none' },
})
export class TreeViewComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) commands!: Observable<TreeViewCommand>;
  // Emits the key of the selected node, both when the user selects a node
  // and after commands 'new' and 'update'.
  @Output() nodeSelected = new EventEmitter<string>();

  private state: Uninitialized | Initialized = new Uninitialized();
  private subscription?: Subscription;
  // The host element is the scroll box, see the host style above.
  private scrollBox = new ScrollBox(inject<ElementRef<HTMLElement>>(ElementRef).nativeElement);
  private injector = inject(Injector);

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

  protected toggleExpanded(node: TreeViewNode): void {
    this.emitIfSelected(this.state.toggleExpanded(node));
  }

  private execute(command: TreeViewCommand): void {
    switch (command.command) {
      case 'new': {
        const next = this.state.show(command);
        this.state = next;
        this.nodeSelected.emit(next.selectedKey);
        // A different report: start at the top, not at the scroll
        // position of the previous report.
        this.afterRender(() => this.scrollBox.scrollToTop());
        break;
      }
      case 'update': {
        const before = this.selectedNodePosition();
        const next = this.state.show(command);
        this.state = next;
        this.nodeSelected.emit(next.selectedKey);
        this.afterRender(() => this.keepSelectedNodeInPlace(before));
        break;
      }
      case 'expandAll': {
        const before = this.selectedNodePosition();
        this.state.expandAll();
        this.afterRender(() => this.keepSelectedNodeInPlace(before));
        break;
      }
      case 'collapseAll': {
        const before = this.selectedNodePosition();
        this.state.collapseAll();
        this.afterRender(() => this.keepSelectedNodeInPlace(before));
        break;
      }
      case 'select': {
        // Does not emit on purpose, see TreeViewSelectCommand.
        this.state.selectByKey(command.key);
        this.afterRender(() => this.keepSelectedNodeInPlace(null));
        break;
      }
      case 'clear': {
        this.state = new Uninitialized();
        break;
      }
    }
  }

  private emitIfSelected(key: string | null): void {
    if (key !== null) {
      this.nodeSelected.emit(key);
    }
  }

  // Runs the action when Angular has updated the DOM for the changed state.
  private afterRender(action: () => void): void {
    afterNextRender({ write: action }, { injector: this.injector });
  }

  private selectedNodePosition(): SelectedNodePosition | null {
    return this.state.selectedNodePosition(this.scrollBox);
  }

  // Asks the state at the moment of rendering, which is the state after
  // the command.
  private keepSelectedNodeInPlace(before: SelectedNodePosition | null): void {
    this.state.keepSelectedNodeInPlace(before, this.scrollBox);
  }
}
