import { Component, Input } from '@angular/core';

export interface NodeValueLabels {
  isEdited: boolean;
  isMessageNull: boolean;
  isMessageEmpty: boolean;
  stubbed: boolean;
  encoding: string | undefined;
  messageClassName: string | undefined;
  // TODO: Remove here and remove from checkpoint.ts. Issue https://github.com/wearefrank/ladybug-frontend/issues/1132.
  showConverted?: boolean;
  charactersRemoved: number;
  stubNotFound?: string;
  isReadOnly?: boolean;
}

@Component({
  selector: 'app-report-alert-message2',
  standalone: true,
  templateUrl: './report-alert-message2.component.html',
  styleUrl: './report-alert-message2.component.css',
})
export class ReportAlertMessage2Component {
  @Input() labels: NodeValueLabels | undefined;
}
