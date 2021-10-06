import {Component, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/components/tree/tree.component";
import {DisplayComponent} from "../shared/components/display/display.component";

@Component({
  selector: 'app-compare',
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.css']
})
export class CompareComponent {
  leftReports: any[] = [];
  rightReports: any[] = [];
  @ViewChild('leftTree') leftTreeComponent!: TreeComponent;
  @ViewChild('rightTree') rightTreeComponent!: TreeComponent;
  @ViewChild('leftDisplay') leftDisplayComponent!: DisplayComponent;
  @ViewChild('rightDisplay') rightDisplayComponent!: DisplayComponent;
  leftId: string = "leftId"
  rightId: string = "rightId"
  leftCurrentReport: any = {};
  rightCurrentReport: any = {}
  leftReportSelected: boolean = false;
  rightReportSelected: boolean = false;
  constructor() { }

  /**
   * Adds a report to the left tree
   * @param newReport - report to be added
   */
  addReportNodeLeft(newReport: any) {
    if (this.leftId === newReport.id) {
      this.leftReports.push(newReport);
      this.leftTreeComponent?.handleChange(this.leftReports);
    }
  }

  /**
   * Adds a report to the right tree
   * @param newReport - report to be added
   */
  addReportNodeRight(newReport: any) {
    if (this.rightId === newReport.id) {
      this.rightReports.push(newReport);
      this.rightTreeComponent?.handleChange(this.rightReports);
    }
  }

  /**
   * Show the report of the left tree on the left display
   * @param currentReport - the report to be displayed
   */
  selectReportLeft(currentReport: any) {
    this.leftReportSelected = true;
    this.leftCurrentReport = currentReport;
    this.leftDisplayComponent?.showReport(this.leftCurrentReport);
  }

  /**
   * Show the report of the right tree on the right display
   * @param currentReport - the report to be displayed
   */
  selectReportRight(currentReport: any) {
    this.rightReportSelected = true;
    this.rightCurrentReport = currentReport;
    this.rightDisplayComponent?.showReport(this.rightCurrentReport);
  }

  /**
   * Close the left report
   * @param currentNode - the left node to be removed
   */
  closeReportLeft(currentNode: any) {
    this.leftReportSelected = false
    this.leftCurrentReport = {};
    this.leftTreeComponent?.removeNode(currentNode);
  }

  /**
   * Close the right report
   * @param currentNode - the right node to be removed
   */
  closeReportRight(currentNode: any) {
    this.rightReportSelected = false;
    this.rightCurrentReport = {};
    this.rightTreeComponent?.removeNode(currentNode);
  }
}
