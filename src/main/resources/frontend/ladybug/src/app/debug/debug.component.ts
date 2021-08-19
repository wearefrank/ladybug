import {Component, OnInit, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/tree/tree.component";
import {DisplayComponent} from "../shared/display/display.component";

@Component({
  selector: 'app-debug',
  templateUrl: './debug.component.html',
  styleUrls: ['./debug.component.css']
})
export class DebugComponent implements OnInit {
  reports: any[] = [];
  currentReport: any = {};
  @ViewChild(TreeComponent) treeComponent: TreeComponent | undefined;
  @ViewChild(DisplayComponent) displayComponent: DisplayComponent | undefined;

  constructor() {
  }

  ngOnInit(): void {
  }

  /*
    Add a new report and notify the tree of the change
   */
  addReport(newReport: string) {
    this.reports.push(newReport);
    this.treeComponent?.handleChange();
  }

  selectReport(currentReport: any) {
    this.currentReport = currentReport;
    this.displayComponent?.showReport();
  }
}
