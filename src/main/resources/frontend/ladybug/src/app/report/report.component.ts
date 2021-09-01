import {AfterViewInit, Component, Injectable, OnInit, ViewChild} from '@angular/core';
import {TreeComponent} from "../shared/components/tree/tree.component";
import {DisplayComponent} from "../shared/components/display/display.component";

@Injectable()
export class ReportData {
  data = {};
}

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements AfterViewInit {
  @ViewChild(TreeComponent) treeComponent!: TreeComponent;
  @ViewChild(DisplayComponent) displayComponent!: DisplayComponent;

  constructor(public reportData: ReportData) {
  }

  /**
   Add a new report and notify the tree of the change
   */
  ngAfterViewInit() {
    this.treeComponent?.handleChange([this.reportData]);
  }

  /**
   * Select a report to be viewed in the display
   * @param currentReport - the report to be viewed
   */
  selectReport(currentReport: any) {
    this.displayComponent.showReport(currentReport);
  }

}
