import { Component, Input } from '@angular/core';
import { CopyTooltipDirective } from 'src/app/shared/directives/copy-tooltip.directive';
import { PartialReport } from '../report.component';

@Component({
  selector: 'app-report-metadata-table2',
  imports: [CopyTooltipDirective],
  templateUrl: './report-metadata-table.html',
  styleUrl: './report-metadata-table.css',
})
export class ReportMetadataTable {
  @Input({ required: true }) report!: PartialReport;
}
