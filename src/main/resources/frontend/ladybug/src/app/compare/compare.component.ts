import {Component, OnInit, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/components/tree/tree.component";

@Component({
  selector: 'app-compare',
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.css']
})
export class CompareComponent implements OnInit {
  reports: any[] = [];
  @ViewChild(TreeComponent) treeComponent: TreeComponent | undefined;
  constructor() { }

  ngOnInit(): void {
  }

  /*
  Add a new report and notify the tree of the change
 */
  addReport(newReport: string) {
    this.reports.push(newReport);
    this.treeComponent?.handleChange();
  }
}
