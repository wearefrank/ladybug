import { Component, ViewChild } from '@angular/core';
import { Report } from '../shared/interfaces/report';
import { ReportComponent } from '../report/report.component';
import { TracingTableComponent } from './tracing-table/tracing-table.component';

@Component({
  selector: 'app-tracing',
  templateUrl: './tracing.component.html',
  styleUrls: ['./tracing.component.css'],
  standalone: true,
  imports: [TracingTableComponent, ReportComponent],
})
export class TracingComponent {
  static readonly ROUTER_PATH: string = 'tracing';

  @ViewChild('reportComponent') customReportComponent!: ReportComponent;

  protected addReportToTree(report: Report): void {
    this.customReportComponent.addReportToTree(report);
  }
}
