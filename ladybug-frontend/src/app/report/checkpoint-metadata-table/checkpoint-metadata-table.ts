import { Component, Input } from '@angular/core';
import { CopyTooltipDirective } from '../../shared/directives/copy-tooltip.directive';
import { HierarchicalCheckpoint } from '../../shared/interfaces/hierarchical-report';

@Component({
  selector: 'app-checkpoint-metadata-table',
  imports: [CopyTooltipDirective],
  templateUrl: './checkpoint-metadata-table.html',
  styleUrl: './checkpoint-metadata-table.css',
})
export class CheckpointMetadataTable {
  @Input({ required: true }) checkpoint!: HierarchicalCheckpoint;
}
