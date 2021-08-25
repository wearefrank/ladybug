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
   * Add a new report to the specific tree (left or right)
   * @param newReport - the report to be added to the specific tree
   */
  addReport(newReport: any) {
    if (newReport.id === this.leftId) {
      this.leftReports.push(newReport);
      this.leftTreeComponent?.handleChange(this.leftReports)
    } else {
      this.rightReports.push(newReport)
      this.rightTreeComponent?.handleChange(this.rightReports);
    }
  }

  /**
   * Select a report to be viewed in the display
   * @param currentReport - the report to be viewed
   */
  selectReport(currentReport: any) {
    if (currentReport.ladybug.id === this.leftId) {
      this.leftReportSelected = true;
      this.leftCurrentReport = currentReport;
      this.leftDisplayComponent?.showReport(currentReport);
    } else {
      this.rightReportSelected = true;
      this.rightCurrentReport = currentReport;
      this.rightDisplayComponent?.showReport(currentReport);
    }
  }

}
