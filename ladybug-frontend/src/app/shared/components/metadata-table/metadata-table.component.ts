import { Component, Input } from '@angular/core';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CopyTooltipDirective } from '../../directives/copy-tooltip.directive';
import { Report } from '../../interfaces/report';
import { Checkpoint } from '../../interfaces/checkpoint';
import { ReportUtil as ReportUtility } from '../../util/report-util';

// TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1124.
// In CompareComponent use ReportMetadataTable and CheckpointMetadataTable
// and then remove MetadataTableComponent.
@Component({
  selector: 'app-metadata-table',
  templateUrl: './metadata-table.component.html',
  styleUrls: ['./metadata-table.component.css'],
  standalone: true,
  imports: [ClipboardModule, MatTooltipModule, CopyTooltipDirective],
})
export class MetadataTableComponent {
  @Input({ required: true }) report!: Report | Checkpoint;

  protected readonly ReportUtil = ReportUtility;
}
