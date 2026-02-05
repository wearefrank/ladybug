import { Component, Input } from '@angular/core';
import { CopyTooltipDirective } from '../../shared/directives/copy-tooltip.directive';
import { PartialCheckpoint } from '../checkpoint-value/checkpoint-value.component';

@Component({
  selector: 'app-checkpoint-metadata-table',
  imports: [CopyTooltipDirective],
  templateUrl: './checkpoint-metadata-table.html',
  styleUrl: './checkpoint-metadata-table.css',
})
export class CheckpointMetadataTable {
  @Input({ required: true }) checkpoint!: PartialCheckpoint;
}
