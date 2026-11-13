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
