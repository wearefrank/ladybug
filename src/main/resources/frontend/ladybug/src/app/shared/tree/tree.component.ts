import {Component, Input, OnInit} from '@angular/core';

declare var $: any;

@Component({
  selector: 'app-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css']
})

export class TreeComponent {
  @Input()
  reports: any[] = [];
  reportTree: any[] = [];

  constructor() {
  }

  handleChange() {
    this.reportTree = [];
    for (let report of this.reports) {
      let item = {
        text: report.name,
        ladybug: report,
        icon: "fa fa-plus",
        nodes: []
      }

      for (let checkpoint of report.checkpoints) {
        let node = {
          text: checkpoint.name,
          ladybug: checkpoint,
          level: checkpoint.level,
          icon: "fa fa-arrow-right"
        }
        // @ts-ignore
        item.nodes.push(node)
      }
      this.reportTree.push(item)
    }

    $('#treeView').treeview({
      data: this.reportTree
    });
  }
}
