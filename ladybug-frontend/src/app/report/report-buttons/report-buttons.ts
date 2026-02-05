import { Component, inject, Input, NgZone, OnDestroy, OnInit, output } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { StubStrategy } from '../../shared/enums/stub-strategy';
import { FormsModule } from '@angular/forms';
import { TestResult } from '../../shared/interfaces/test-result';
import { AppVariablesService } from '../../shared/services/app.variables.service';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { BooleanToStringPipe } from '../../shared/pipes/boolean-to-string.pipe';

export interface ReportButtonsState {
  isReport: boolean;
  isCheckpoint: boolean;
  saveAllowed: boolean;
}

export type ButtonCommand =
  | 'makeNull'
  | 'prettify'
  | 'save'
  | 'copyReport'
  | 'rerun'
  | 'customReportAction'
  | 'hideMetadata'
  | 'showMetadata'
  | 'hideMessageContext'
  | 'showMessageContext';

export interface DownloadOptions {
  downloadReport: boolean;
  downloadXmlSummary: boolean;
}

@Component({
  selector: 'app-report-buttons',
  imports: [
    FormsModule,
    NgbDropdown,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    NgbDropdownMenu,
    NgbDropdownToggle,
    BooleanToStringPipe,
  ],
  templateUrl: './report-buttons.html',
  styleUrl: './report-buttons.css',
})
export class ReportButtons implements OnInit, OnDestroy {
  reportCommand = output<ButtonCommand>();
  checkpointStubStrategyChange = output<number>();
  reportStubStrategyChange = output<string>();
  downloadRequest = output<DownloadOptions>();
  @Input({ required: true }) state$!: Observable<ReportButtonsState>;
  @Input() originalCheckpointStubStrategy$?: Observable<number | undefined>;
  @Input({ required: true }) originalReportStubStrategy$!: Observable<string | undefined>;
  @Input({ required: true }) reset$!: Observable<void>;
  @Input({ required: true }) rerunResult$!: Observable<TestResult | undefined>;

  protected state: ReportButtonsState = {
    isReport: false,
    isCheckpoint: false,
    saveAllowed: false,
  };

  protected readonly StubStrategy = StubStrategy;
  protected currentCheckpointStubStrategyStr?: string;
  protected currentReportStubStrategy?: string;
  protected rerunResult?: TestResult;
  protected metadataTableVisible = false;
  protected messageContextTableVisible = false;
  protected appVariablesService = inject(AppVariablesService);
  private ngZone = inject(NgZone);
  private subscriptions = new Subscription();

  ngOnInit(): void {
    this.subscriptions.add(
      this.state$.subscribe((state) => {
        this.ngZone.run(() => {
          this.state = state;
        });
      }),
    );
    this.subscriptions.add(
      this.originalCheckpointStubStrategy$?.subscribe((checkpointStubStrategy) => {
        this.ngZone.run(() => {
          if (checkpointStubStrategy !== undefined) {
            // Martijn did not get the dropdown to work when it was filled with number values that
            // had to be shown as strings. The dropdown only deals with string representations.
            this.currentCheckpointStubStrategyStr =
              StubStrategy.checkpoints[StubStrategy.checkpointStubToIndex(checkpointStubStrategy)];
          }
        });
      }),
    );
    this.subscriptions.add(
      this.originalReportStubStrategy$.subscribe((reportStubStrategy) => {
        this.ngZone.run(() => {
          if (reportStubStrategy !== undefined) {
            this.currentReportStubStrategy = reportStubStrategy;
          }
        });
      }),
    );
    this.subscriptions.add(
      this.reset$.subscribe(() => {
        this.ngZone.run(() => this.reset());
      }),
    );
    this.subscriptions.add(
      this.rerunResult$.subscribe((rerunResult) => {
        this.ngZone.run(() => (this.rerunResult = rerunResult));
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  protected makeNull(): void {
    this.reportCommand.emit('makeNull');
  }

  protected prettify(): void {
    this.reportCommand.emit('prettify');
  }

  protected save(): void {
    this.reportCommand.emit('save');
  }

  protected copyReport(): void {
    this.reportCommand.emit('copyReport');
  }

  protected rerun(): void {
    this.reportCommand.emit('rerun');
  }

  protected processCustomReportAction(): void {
    this.reportCommand.emit('customReportAction');
  }

  protected onCheckpointStubStrategyChange(stubStrategyString: string): void {
    const options: string[] = [...StubStrategy.checkpoints];
    const index: number = options.indexOf(stubStrategyString);
    if (index === -1) {
      throw new Error(`ReportButtons.onCheckpointStubStrategyChange(): Unknown valu ${stubStrategyString}`);
    }
    this.checkpointStubStrategyChange.emit(StubStrategy.checkpointIndex2Stub(index));
  }

  protected onReportStubStrategyChange(reportStubStrategy: string): void {
    this.reportStubStrategyChange.emit(reportStubStrategy);
  }

  protected onDownload(downloadOptions: DownloadOptions): void {
    this.downloadRequest.emit(downloadOptions);
  }

  protected toggleMetadataTable(): void {
    this.metadataTableVisible = !this.metadataTableVisible;
    if (this.metadataTableVisible) {
      this.reportCommand.emit('showMetadata');
    } else {
      this.reportCommand.emit('hideMetadata');
    }
  }

  protected toggleMessageContextTable(): void {
    this.messageContextTableVisible = !this.messageContextTableVisible;
    if (this.messageContextTableVisible) {
      this.reportCommand.emit('showMessageContext');
    } else {
      this.reportCommand.emit('hideMessageContext');
    }
  }

  private reset(): void {
    this.metadataTableVisible = false;
    this.messageContextTableVisible = false;
  }
}
