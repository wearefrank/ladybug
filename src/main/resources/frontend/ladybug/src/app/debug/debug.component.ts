import {Component, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/components/tree/tree.component";
import {DisplayComponent} from "../shared/components/display/display.component";

@Component({
  selector: 'app-debug',
  templateUrl: './debug.component.html',
  styleUrls: ['./debug.component.css']
})
export class DebugComponent {
  reports: any[] = [];
  currentReport: any = {};
  @ViewChild(TreeComponent) treeComponent!: TreeComponent;
  @ViewChild(DisplayComponent) displayComponent!: DisplayComponent;

  constructor() {}

  /**
    Add a new report and notify the tree of the change
   */
  addReport(newReport: any) {
    this.reports.push(newReport);
    this.treeComponent.handleChange(this.reports);
  }

  /**
   * Select a report to be viewed in the display
   * @param currentReport - the report to be viewed
   */
  selectReport(currentReport: any) {
    this.currentReport = currentReport;
    this.displayComponent.showReport(this.currentReport);
  }

  /**
   * Close a report
   * @param currentReport - the report to be closed
   */
  closeReport(currentReport: any) {
    this.currentReport = {}
    this.treeComponent.removeNode(currentReport);
  }
}
