import { Component, Input, OnDestroy, OnInit, output, ViewChild } from '@angular/core';
import { BehaviorSubject, Observable, Subject, Subscription } from 'rxjs';
import { MonacoEditorComponent } from '../../monaco-editor/monaco-editor.component';
import { DifferenceModalComponent } from '../difference-modal/difference-modal.component';
import { DifferencesBuilder } from '../../shared/util/differences-builder';
import {
  NodeValueLabels,
  ReportAlertMessage2Component,
} from '../report-alert-message2/report-alert-message2.component';
import { NodeValueState, PartialReport, UpdateNode } from '../report.component';
import { ButtonCommand, DownloadOptions, ReportButtons, ReportButtonsState } from '../report-buttons/report-buttons';
import { StubStrategy } from '../../shared/enums/stub-strategy';
import { TestResult } from '../../shared/interfaces/test-result';
import { CheckpointMetadataTable } from '../checkpoint-metadata-table/checkpoint-metadata-table';
import { MessagecontextTableComponent } from '../../shared/components/messagecontext-table/messagecontext-table.component';
import { Checkpoint } from '../../shared/interfaces/checkpoint';
import { prettify } from '../report.component';
import { UpdateCheckpoint, UpdateReport } from 'src/app/shared/interfaces/update-report';

export interface PartialCheckpoint {
  index: number;
  uid: string;
  message: string | null;
  stubbed: boolean;
  // TODO: Server will not send undefined. Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  encoding?: string | null;
  // TODO: Server will not send undefined. Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  messageClassName?: string | null;
  showConverted?: boolean;
  preTruncatedMessageLength: number;
  stubNotFound?: string;
  stub: number;
  parentReport: PartialReport;
  name: string;
  threadName: string;
  typeAsString: string;
  level: number;
  sourceClassName?: number;
}

@Component({
  selector: 'app-checkpoint-value',
  imports: [
    MonacoEditorComponent,
    DifferenceModalComponent,
    ReportAlertMessage2Component,
    ReportButtons,
    CheckpointMetadataTable,
    MessagecontextTableComponent,
  ],
  templateUrl: './checkpoint-value.component.html',
  styleUrl: './checkpoint-value.component.css',
})
export class CheckpointValueComponent implements OnInit, OnDestroy {
  nodeValueState = output<NodeValueState>();
  button = output<ButtonCommand>();
  save = output<UpdateNode>();
  downloadRequest = output<DownloadOptions>();
  @Input() height = 0;
  @Input({ required: true }) originalCheckpoint$!: Observable<PartialCheckpoint | undefined>;
  @Input({ required: true }) saveDone$!: Observable<void>;
  @Input({ required: true }) rerunResult$!: Observable<TestResult | undefined>;
  @ViewChild(DifferenceModalComponent) saveModal!: DifferenceModalComponent;
  labels: NodeValueLabels | undefined;
  buttonStateSubject = new BehaviorSubject<ReportButtonsState>(CheckpointValueComponent.getDefaultButtonState());

  protected originalCheckpoint: PartialCheckpoint | undefined;
  protected metadataTableVisible = false;
  protected messageContextTableVisible = false;
  protected editorContentsSubject = new BehaviorSubject<string | undefined>(undefined);
  protected editorReadOnlySubject = new BehaviorSubject<boolean>(true);
  protected originalCheckpointStubStrategySubject = new BehaviorSubject<number | undefined>(undefined);
  protected originalReportStubStrategySubject = new BehaviorSubject<string | undefined>(undefined);
  protected buttonComponentResetSubject = new Subject<void>();
  private actualEditorContents = '';
  private actualCheckpointStubStrategy?: number;
  private actualReportStubStrategy?: string;
  private emptyIsNull = true;
  private subscriptions = new Subscription();

  ngOnInit(): void {
    this.subscriptions.add(
      this.originalCheckpoint$.subscribe((value: PartialCheckpoint | undefined) => {
        if (value !== undefined) {
          this.newOriginalCheckpoint(value);
        }
      }),
    );
    this.subscriptions.add(this.saveDone$.subscribe(() => this.saveModal.closeModal()));
    this.editorReadOnlySubject.next(false);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onActualEditorContentsChanged(value: string): void {
    // We set the actualEditorContents already when we request new editor contents.
    // When the new value is converted, we do not have to react.
    if (this.actualEditorContents === value) {
      return;
    }
    this.actualEditorContents = value;
    if (this.actualEditorContents.length > 0) {
      this.emptyIsNull = false;
    }
    this.handleLabelsAndNodeValueState();
  }

  onCheckpointStubStrategyChange(strategy: number): void {
    this.actualCheckpointStubStrategy = strategy;
    this.handleLabelsAndNodeValueState();
  }

  onReportStubStrategyChange(strategy: string): void {
    this.actualReportStubStrategy = strategy;
    this.handleLabelsAndNodeValueState();
  }

  onButton(command: ButtonCommand): void {
    switch (command) {
      case 'makeNull': {
        this.editToNull();
        break;
      }
      case 'prettify': {
        const result = prettify(this.actualEditorContents);
        const editorContents: string = result === null ? '' : result;
        this.editorContentsSubject.next(editorContents);
        break;
      }
      case 'save': {
        this.saveCheckpoint();
        break;
      }
      case 'copyReport': {
        this.button.emit('copyReport');
        break;
      }
      case 'rerun': {
        this.button.emit('rerun');
        break;
      }
      case 'customReportAction': {
        this.button.emit('customReportAction');
        break;
      }
      case 'showMetadata': {
        this.metadataTableVisible = true;
        break;
      }
      case 'hideMetadata': {
        this.metadataTableVisible = false;
        break;
      }
      case 'hideMessageContext': {
        this.messageContextTableVisible = false;
        break;
      }
      case 'showMessageContext': {
        this.messageContextTableVisible = true;
        break;
      }
    }
  }

  getDifferences(): DifferencesBuilder {
    if (this.originalCheckpoint === undefined) {
      throw new Error('CheckpointValueComponent.getDifferences(): Did not expect originalCheckpoint to be undefined');
    }
    if (this.actualCheckpointStubStrategy === undefined) {
      throw new Error(
        'CheckpointValueComponent.getDifferences(): Expected that actualCheckpointStubStrategy !== undefined because a checkpoint was read',
      );
    }
    if (this.actualReportStubStrategy === undefined) {
      throw new Error(
        'CheckpointValueComponent.getDifferences(): Did not expect actualReportStubStrategy to be undefined',
      );
    }
    return new DifferencesBuilder()
      .nullableVariable(this.originalCheckpoint.message, this.getEditedRealCheckpointValue(), 'Value', true)
      .nonNullableVariable(
        this.originalCheckpoint.parentReport.stubStrategy,
        this.actualReportStubStrategy,
        'Report level stub strategy',
      )
      .nonNullableVariable(
        StubStrategy.checkpoints[StubStrategy.checkpointStubToIndex(this.originalCheckpoint.stub)],
        StubStrategy.checkpoints[StubStrategy.checkpointStubToIndex(this.actualCheckpointStubStrategy)],
        "This checkpoint's stub strategy",
      );
  }

  getEditedRealCheckpointValue(): string | null {
    if (this.emptyIsNull) {
      return this.actualEditorContents.length === 0 ? null : this.actualEditorContents;
    } else {
      return this.actualEditorContents;
    }
  }

  requestSave(): void {
    if (!this.isEdited()) {
      throw new Error(
        'CheckpointValueComponent.save() should not be reachable when the checkpoint has not been edited',
      );
    }
    const message: string | null = this.getEditedRealCheckpointValue();
    const checkpointId = `${this.originalCheckpoint!.index}`;
    let update: UpdateReport = {};
    const newMessage: boolean = message !== this.originalCheckpoint!.message;
    const newStub = this.actualCheckpointStubStrategy !== this.originalCheckpoint!.stub;
    if (newMessage || newStub) {
      const checkpointUpdate: UpdateCheckpoint = {
        checkpointId: checkpointId,
      };
      if (newMessage) {
        checkpointUpdate.checkpointMessage = message;
      }
      if (newStub) {
        checkpointUpdate.stub = this.actualCheckpointStubStrategy;
      }
      update.checkpoints = [checkpointUpdate];
    }
    if (this.actualReportStubStrategy !== this.originalCheckpoint!.parentReport.stubStrategy) {
      update.stubStrategy = this.actualReportStubStrategy;
    }
    this.save.emit({
      checkpointUidToRestore: this.originalCheckpoint!.uid,
      updateReport: update,
    });
  }

  protected onDownload(downloadOptions: DownloadOptions): void {
    this.downloadRequest.emit(downloadOptions);
  }

  // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  protected asCheckpoint(p: PartialCheckpoint): Checkpoint {
    return p as Checkpoint;
  }

  protected monacoOptions: Partial<monaco.editor.IStandaloneEditorConstructionOptions> = {
    theme: 'vs-light',
    language: 'xml',
    inlineCompletionsAccessibilityVerbose: true,
    automaticLayout: true,
    padding: { bottom: 200 },
    selectOnLineNumbers: true,
    scrollBeyondLastLine: false,
    wordWrap: 'on',
  };

  private newOriginalCheckpoint(originalCheckpoint: PartialCheckpoint | undefined): void {
    if (originalCheckpoint === undefined) {
      throw new Error('CheckpointValueComponent.neworiginalCheckpoint(): Did not expect to receive value undefined');
    }
    this.metadataTableVisible = false;
    this.messageContextTableVisible = false;
    this.originalCheckpoint = originalCheckpoint;
    this.emptyIsNull = this.originalCheckpoint.message === null;
    const requestedEditorContents = originalCheckpoint.message === null ? '' : originalCheckpoint.message;
    // Do not trust the Monaco editor sends an event for the first text.
    this.actualEditorContents = requestedEditorContents;
    this.actualCheckpointStubStrategy = originalCheckpoint.stub;
    this.actualReportStubStrategy = originalCheckpoint.parentReport.stubStrategy;
    this.handleLabelsAndNodeValueState();
    this.editorContentsSubject.next(requestedEditorContents);
    this.originalCheckpointStubStrategySubject.next(originalCheckpoint.stub);
    this.originalReportStubStrategySubject.next(originalCheckpoint.parentReport.stubStrategy);
    this.buttonComponentResetSubject.next();
  }

  private editToNull(): void {
    this.emptyIsNull = true;
    // Do not trust the Monaco editor sends an event for the updated text.
    this.actualEditorContents = '';
    // Editor contents may be the empty string, then still saved changes.
    this.handleLabelsAndNodeValueState();
    this.editorContentsSubject.next('');
  }

  private handleLabelsAndNodeValueState(): void {
    if (this.originalCheckpoint === undefined) {
      throw new Error(
        'CheckpointValueComponent.handleLabelsAndNodeValueState(): Did not expect that there was no checkpoint',
      );
    }
    if (this.actualCheckpointStubStrategy === undefined) {
      throw new Error(
        'CheckpointValueComponent.handleLabelsAndNodeValueState(): Expected that actualCheckpointStubStrategy !== undefined because a checkpoint was read',
      );
    }
    if (this.actualReportStubStrategy === undefined) {
      throw new Error(
        'CheckpointValueComponent.handleLabelsAndNodeValueState(): Expected that actualReportStubStrategy !== undefined because a checkpoint was read',
      );
    }
    const isEdited = this.isEdited();
    const editedCheckpointValue = this.getEditedRealCheckpointValue();
    const isReadOnly = this.originalCheckpoint === undefined ? true : !this.originalCheckpoint.parentReport.crudStorage;
    this.labels = {
      isReadOnly,
      isEdited,
      isMessageNull: editedCheckpointValue === null,
      isMessageEmpty: editedCheckpointValue === '',
      stubbed: this.originalCheckpoint.stubbed,
      encoding: CheckpointValueComponent.getEncoding(this.originalCheckpoint.encoding),
      messageClassName:
        this.originalCheckpoint.messageClassName === undefined || this.originalCheckpoint.messageClassName === null
          ? undefined
          : this.originalCheckpoint.messageClassName,
      charactersRemoved: CheckpointValueComponent.getCharactersRemoved(
        this.originalCheckpoint.message,
        this.originalCheckpoint.preTruncatedMessageLength,
      ),
      stubNotFound: this.originalCheckpoint.stubNotFound,
    };
    this.nodeValueState.emit({
      isReadOnly,
      isEdited,
      storageId: this.originalCheckpoint?.parentReport.storageId,
    });
    const saveAllowed = !isReadOnly && isEdited;
    this.buttonStateSubject.next({
      isReport: false,
      isCheckpoint: true,
      saveAllowed,
    });
  }

  private isEdited(): boolean {
    const editedCheckpointValue = this.getEditedRealCheckpointValue();
    const isCheckpointEdited = editedCheckpointValue !== this.originalCheckpoint!.message;
    const isCheckpointStubStrategyEdited = this.actualCheckpointStubStrategy !== this.originalCheckpoint!.stub;
    const isReportStubStrategyEdited =
      this.actualReportStubStrategy !== this.originalCheckpoint!.parentReport.stubStrategy;
    return isCheckpointEdited || isCheckpointStubStrategyEdited || isReportStubStrategyEdited;
  }

  private static getEncoding(e: string | null | undefined): string | undefined {
    return e === undefined || e === null || e.length === 0 ? undefined : e;
  }

  private static getCharactersRemoved(originalMessage: string | null, preTruncatedMessageLength: number): number {
    // In the backend code you see that a negative number is returned for unspecified.
    if (preTruncatedMessageLength < 0) {
      return 0;
    }
    const originalLength = originalMessage === null ? 0 : originalMessage.length;
    return preTruncatedMessageLength - originalLength;
  }

  private saveCheckpoint(): void {
    this.saveModal.open(this.getDifferences().build());
  }

  private static getDefaultButtonState(): ReportButtonsState {
    return {
      isReport: false,
      isCheckpoint: true,
      saveAllowed: false,
    };
  }
}
