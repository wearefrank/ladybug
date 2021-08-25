import {Component, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/components/tree/tree.component";

@Component({
  selector: 'app-compare',
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.css']
})
export class CompareComponent {
  leftReports: any[] = [];
  rightReports: any[] = [];
  @ViewChild('leftTree') leftTreeComponent: TreeComponent | undefined;
  @ViewChild('rightTree') rightTreeComponent: TreeComponent | undefined;
  leftId: string = "leftId"
  rightId: string = "rightId"
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
}
