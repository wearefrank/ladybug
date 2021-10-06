import {Component, EventEmitter, Input, Output} from '@angular/core';
declare var $: any;

@Component({
  selector: 'app-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css']
})

export class TreeComponent {
  @Output() selectReportEvent = new EventEmitter<any>();
  @Input() reports: any[] = [];
  tree: any[] = []
  treeId: string = Math.random().toString(36).substring(7);

  constructor() {}

  /**
   * Collapse the entire tree
   */
  collapseAll() {
    $('#' + this.treeId).treeview('collapseAll', { silent: true})
  }

  /**
   * Expand the entire tree (up to 2 levels)
   */
  expandAll() {
    $('#' + this.treeId).treeview('expandAll', { levels: 2, silent: true})
  }

  /**
   * Close all nodes in the tree
   */
  closeAll() {
    this.reports.length = 0;
    $('#' + this.treeId).treeview( 'remove');
  }

  /**
   * Removes the entire node from the tree. If it is not the parent it recursively tries to find the parent
   * and eventually removes the parent when found
   * @param node - the node to be removed
   */
  removeNode(node: any) {
    console.log("tree")
    console.log(this.tree)
    if (node.root) {
      let result = this.tree.filter(report => {
        return report.id === node.nodeId;
      })
      let index = this.tree.indexOf(result[0]);
      this.tree.splice(index, 1);
      this.updateTreeView();
    } else {
      this.removeNode($('#' + this.treeId).treeview('getParent', node))
    }
  }

  /**
   * Handle change in the tree for the tree view
   * @param reports - the reports to be displayed
   */
  handleChange(reports: any[]) {
    this.reports = reports;

    // Reset the items in the tree
    this.tree = [];
    let id = 0;

    // For each item that has been selected show the node and its children
    for (let report of this.reports) {
      let rootNode = {
        text: report.name,
        ladybug: report,
        root: true,
        id: id++,
        nodes: []
      }

      // Keep track of the previous node (which could be the parent)
      let previousNode: any = {};

      // For each of the child nodes add it to the parent
      for (let checkpoint of report.checkpoints) {
        let node = {
          text: checkpoint.name,
          ladybug: checkpoint,
          root: false,
          id: id++,
          level: checkpoint.level
        }

        // If the previous node is its parent, push to the parent
        if (checkpoint.index > 0 && report.checkpoints[checkpoint.index - 1].level < checkpoint.level) {
          // If it doesnt have children yet, make sure it can have
          if (previousNode.nodes === undefined) {
            previousNode.nodes = [];
          }
          previousNode.nodes.push(node);
        } else {
          // Push it to the root
          // @ts-ignore
          rootNode.nodes.push(node)
          previousNode = node;
        }

      }
      this.tree.push(rootNode)
    }

    this.updateTreeView();
    $('#' + this.treeId).treeview('toggleNodeSelected', [ this.tree[this.tree.length - 1].nodes[0].id, { silent: false } ]);
  }

  /**
   * Update the tree view with the new data
   */
  updateTreeView() {
    // Update the tree view
    $('#' + this.treeId).treeview({
      data: this.tree,
      levels: 5,
      expandIcon: "fa fa-plus",
      collapseIcon: "fa fa-minus",
      emptyIcon: "fa fa-arrow-left",
      selectedBackColor: "#1ab394",
    });

    // When a node is selected, we send forward the data to the display
    $('#' + this.treeId).on('nodeSelected', (event: any, data: any) => {
      this.selectReportEvent.next(data)
    });
  }

}
