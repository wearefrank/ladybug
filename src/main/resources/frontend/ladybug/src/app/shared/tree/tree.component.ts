import {Component, Input} from '@angular/core';
declare var $: any;

@Component({
  selector: 'app-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css']
})

export class TreeComponent {
  @Input()
  reports: any[] = [];
  treeId: string = Math.random().toString(36).substring(7);

  constructor() {
  }

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
    $('#' + this.treeId).treeview( { data: [] });
  }

  /**
    Add a tree node and re-render the tree
   */
  handleChange() {
    // Reset the items in the tree
    let tree = [];

    // For each item that has been selected show the node and its children
    for (let report of this.reports) {
      let item = {
        text: report.name,
        ladybug: report,
        icon: "fa fa-plus",
        nodes: []
      }

      // For each of the child nodes add it to the parent
      for (let checkpoint of report.checkpoints) {
        console.log(checkpoint.level)
        let node = {
          text: checkpoint.name,
          ladybug: checkpoint,
          level: checkpoint.level,
          icon: "fa fa-arrow-right"
        }
        // @ts-ignore
        item.nodes.push(node)
      }
      tree.push(item)
    }

    // Update the treeview
    $('#' + this.treeId).treeview({
      data: tree,
      selectedBackColor: "#1ab394"
    });
  }
}
