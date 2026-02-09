import { CommonModule } from '@angular/common';
import { Component, inject, Input, NgZone, OnDestroy, OnInit, output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MonacoEditorComponent } from '../../monaco-editor/monaco-editor.component';
import { AngularSplitModule } from 'angular-split';
import { HttpService } from '../../shared/services/http.service';
import { BehaviorSubject, catchError, firstValueFrom, Observable, Subject, Subscription } from 'rxjs';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { Transformation } from '../../shared/interfaces/transformation';
import { DifferenceModalComponent } from '../difference-modal/difference-modal.component';
import { DifferencesBuilder } from '../../shared/util/differences-builder';
import { NodeValueState, PartialReport, UpdateNode } from '../report.component';
import {
  NodeValueLabels,
  ReportAlertMessage2Component,
} from '../report-alert-message2/report-alert-message2.component';
import { ButtonCommand, DownloadOptions, ReportButtons, ReportButtonsState } from '../report-buttons/report-buttons';
import { TestResult } from '../../shared/interfaces/test-result';
import { UpdateReport } from '../../shared/interfaces/update-report';
import { ReportMetadataTable } from '../report-metadata-table/report-metadata-table';
import { OverwriteTransformationComponent } from '../overwrite-transformation-modal/overwrite-transformation-modal.component';
import { MatDialog } from '@angular/material/dialog';

export interface Variable {
  name: string;
  value: string;
}

const HEIGHT_OF_FIXED_SIZE_ELEMENTS = 550;
const MIN_MONACO_EDITOR_HEIGHT = 100;

@Component({
  selector: 'app-report-value',
  imports: [
    MonacoEditorComponent,
    CommonModule,
    FormsModule,
    AngularSplitModule,
    DifferenceModalComponent,
    ReportAlertMessage2Component,
    ReportButtons,
    ReportMetadataTable,
  ],
  templateUrl: './report-value.component.html',
  styleUrl: './report-value.component.css',
})
export class ReportValueComponent implements OnInit, OnDestroy {
  nodeValueState = output<NodeValueState>();
  button = output<ButtonCommand>();
  save = output<UpdateNode>();
  downloadRequest = output<DownloadOptions>();
  @Input({ required: true }) report$!: Observable<PartialReport | undefined>;
  @Input({ required: true }) saveDone$!: Observable<void>;
  @Input({ required: true }) rerunResult$!: Observable<TestResult | undefined>;
  @ViewChild(DifferenceModalComponent) saveModal!: DifferenceModalComponent;
  @ViewChild(OverwriteTransformationComponent) overwriteTransformationModal!: OverwriteTransformationComponent;

  editedName = '';
  editedDescription = '';
  editedPath = '';
  editedTransformation = '';
  // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  // Use from input report when type system issue has been fixed.
  originalVariables: Variable[] = [];
  editedVariables: Variable[] = [];
  duplicateVariables: Set<number> = new Set<number>();
  editedReportStubStrategy?: string;

  // It would have been nice to make this protected, but we need to edit this during Karma tests.
  report?: PartialReport;

  labels: NodeValueLabels = {
    isEdited: false,
    // These are all dummies
    isMessageNull: false,
    isMessageEmpty: false,
    stubbed: false,
    charactersRemoved: 0,
    encoding: undefined,
    messageClassName: undefined,
  };

  buttonStateSubject = new BehaviorSubject<ReportButtonsState>(
    ReportValueComponent.getButtonState(ReportValueComponent.getDefaultNodeValueState()),
  );

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
  protected monacoEditorInitialHeight = 0;
  protected metadataTableVisible = false;
  protected transformationContentRequestSubject = new BehaviorSubject<string | undefined>(undefined);
  protected transformationReadOnlySubject = new BehaviorSubject<boolean>(true);
  protected reportContentRequestSubject = new BehaviorSubject<string | undefined>(undefined);
  protected reportReadOnlySubject = new BehaviorSubject<boolean>(true);
  protected originalReportStubStrategySubject = new BehaviorSubject<string | undefined>(undefined);
  protected buttonComponentResetSubject = new Subject<void>();
  private _height = 0;
  private http = inject(HttpService);
  private errorHandler = inject(ErrorHandling);
  private subscriptions = new Subscription();
  private dialog = inject(MatDialog);
  private ngZone = inject(NgZone);

  get height(): number {
    return this._height;
  }

  @Input() set height(theHeight: number) {
    this._height = theHeight;
    this.monacoEditorInitialHeight = this._height - HEIGHT_OF_FIXED_SIZE_ELEMENTS;
    if (this.monacoEditorInitialHeight < MIN_MONACO_EDITOR_HEIGHT) {
      this.monacoEditorInitialHeight = MIN_MONACO_EDITOR_HEIGHT;
    }
  }

  ngOnInit(): void {
    this.transformationReadOnlySubject.next(false);
    this.reportReadOnlySubject.next(true);
    this.subscriptions.add(
      this.report$!.subscribe((report) => {
        if (report !== undefined) {
          this.newReport(report);
        }
      }),
    );
    this.subscriptions.add(this.saveDone$.subscribe(() => this.saveModal.closeModal()));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onInputChange(): void {
    this.refreshDuplicateVariables();
    const isReadOnly = this.report ? !this.report.crudStorage : true;
    const isEdited = this.isEdited();
    this.labels.isEdited = isEdited;
    this.labels.isReadOnly = isReadOnly;
    const state: NodeValueState = { isReadOnly, isEdited, storageId: this.report?.storageId };
    this.buttonStateSubject.next(ReportValueComponent.getButtonState(state));
    this.nodeValueState.emit(state);
  }

  onTransformationEdited(value: string): void {
    this.editedTransformation = value;
    this.onInputChange();
  }

  onButton(command: ButtonCommand): void {
    switch (command) {
      case 'makeNull': {
        throw new Error('Button makeNull should not be accessible when no checkpoint is shown');
      }
      case 'prettify': {
        throw new Error('Button prettify should not be accessible when no checkpoint is shown');
      }
      case 'save': {
        this.saveModal.open(this.getDifferences().build());
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
        throw new Error('Message context does not exist for report nodes');
      }
      case 'showMessageContext': {
        throw new Error('Message context does not exist for report nodes');
      }
    }
  }

  onReportStubStrategyChange(strategy: string): void {
    this.editedReportStubStrategy = strategy;
    this.onInputChange();
  }

  removeVariable(index: number): void {
    this.editedVariables.splice(index, 1);
    this.onInputChange();
  }

  addEmptyVariableWhenNeeded(): void {
    if (this.editedVariables.length > 0) {
      const lastVariable: Variable = this.editedVariables.at(-1)!;
      if (lastVariable.name.length > 0) {
        this.editedVariables.push({ name: '', value: '' });
        this.refreshDuplicateVariables();
      }
    }
  }

  // This one is tested with Karma tests.
  getDifferences(): DifferencesBuilder {
    if (this.editedReportStubStrategy === undefined) {
      throw new Error(
        'ReportValueComponent.getDifferences(): Unexpectedly have editedReportStubStrategy === undefined because a report was read',
      );
    }
    const result = new DifferencesBuilder()
      .nonNullableVariable(this.report!.name, this.editedName, 'Name', true)
      .nullableVariable(
        this.report!.description,
        this.getRealEditedValueForNullable(this.editedDescription),
        'Description',
        true,
      )
      .nullableVariable(this.report!.path, this.getRealEditedValueForNullable(this.editedPath), 'Path', true)
      .nullableVariable(
        this.report!.transformation,
        this.getRealEditedValueForNullable(this.editedTransformation),
        'Transformation',
        true,
      )
      .nonNullableVariable(this.report!.stubStrategy, this.editedReportStubStrategy, 'Report level stub strategy');
    const editedVariables = this.getRealEditedVariables();
    const originalVariableNames: string[] = this.originalVariables.map((v) => v.name);
    const editedVariableNames: string[] = editedVariables.map((v) => v.name);
    result.nonNullableVariable(
      originalVariableNames.join('\n'),
      editedVariableNames.join('\n'),
      'Defined variables (ordered)',
      true,
    );
    for (const n of ReportValueComponent.getCommonVariableNames(originalVariableNames, editedVariableNames)) {
      const originalValue = this.originalVariables.find((v) => v.name === n)!.value;
      const editedValue = editedVariables.find((v) => v.name === n)!.value;
      result.nonNullableVariable(originalValue, editedValue, `Variable ${n}`, true);
    }
    return result;
  }

  requestSave(): void {
    if (!this.isEdited()) {
      throw new Error('ReportValueComponent.save() should not be called when the report node has not been edited');
    }
    const updateReport: UpdateReport = {};
    if (this.editedName !== this.report!.name) {
      updateReport.name = this.editedName;
    }
    const description: string | null = this.getRealEditedValueForNullable(this.editedDescription);
    if (description !== this.report!.description) {
      updateReport.description = description;
    }
    const path: string | null = this.getRealEditedValueForNullable(this.editedPath);
    if (path !== this.report!.path) {
      updateReport.path = path;
    }
    const transformation = this.getRealEditedValueForNullable(this.editedTransformation);
    if (transformation !== this.report!.transformation) {
      updateReport.transformation = transformation;
    }
    if (!this.hasNoUnsavedVariables()) {
      updateReport.variables = this.getVariablesUpdate();
    }
    if (this.editedReportStubStrategy !== this.report!.stubStrategy) {
      updateReport.stubStrategy = this.editedReportStubStrategy;
    }
    this.save.emit({ updateReport });
  }

  onOverwriteTransformation(): void {
    const config = {
      hasBackdrop: false,
    };
    const dialogReference = this.dialog.open(OverwriteTransformationComponent, config);
    dialogReference.afterClosed().subscribe((choice) => {
      if (choice === true) {
        // TODO: Rethrow error when obtaining transformation fails? Issue https://github.com/wearefrank/ladybug-frontend/issues/1133.
        const transformationResponsePromise: Promise<Transformation> = firstValueFrom(
          this.http.getTransformation().pipe(catchError(this.errorHandler.handleError())),
        );
        transformationResponsePromise.then((transformationResponse) =>
          this.transformationContentRequestSubject.next(transformationResponse.transformation),
        );
      }
    });
  }

  // For testing purposes
  setVariables(variables: Variable[]): void {
    this.originalVariables = variables;
    this.editedVariables = ReportValueComponent.calculateEditedVariables(this.originalVariables);
    this.refreshDuplicateVariables();
  }

  protected onDownload(downloadOptions: DownloadOptions): void {
    this.downloadRequest.emit(downloadOptions);
  }

  private newReport(report: PartialReport): void {
    // When a report is edited, the update report is received from a
    // subscription. Reacting on a subscription is not automatically
    // in the Angular zone, so we have to run in the Angular zone
    // explicitly.
    this.ngZone.run(() => {
      this.metadataTableVisible = false;
      this.report = report;
      this.editedName = this.report.name;
      this.editedDescription = this.getEditorTextOfNullable(this.report.description);
      this.editedPath = this.getEditorTextOfNullable(this.report.path);
      this.editedTransformation = this.getEditorTextOfNullable(this.report.transformation);
      this.originalVariables = ReportValueComponent.initVariables(report.variables);
      this.editedVariables = ReportValueComponent.calculateEditedVariables(this.originalVariables);
      this.refreshDuplicateVariables();
      this.editedReportStubStrategy = report.stubStrategy;
      this.transformationContentRequestSubject.next(this.getEditorTextOfNullable(this.report!.transformation));
      this.reportContentRequestSubject.next(this.report.xml);
      this.originalReportStubStrategySubject.next(report.stubStrategy);
      this.buttonComponentResetSubject.next();
      this.onInputChange();
    });
  }

  private refreshDuplicateVariables(): void {
    this.duplicateVariables = new Set<number>();
    const variableNames = new Set<string>();
    for (const [index, variable] of this.editedVariables.entries()) {
      if (variableNames.has(variable.name)) {
        this.duplicateVariables.add(index);
      }
      variableNames.add(variable.name);
    }
  }

  private hasNoUnsavedVariables(): boolean {
    const realVariables: Variable[] = this.getRealEditedVariables();
    if (realVariables.length !== this.originalVariables.length) {
      return false;
    }
    for (const index in this.originalVariables) {
      if (
        realVariables[index].name !== this.originalVariables[index].name ||
        realVariables[index].value !== this.originalVariables[index].value
      ) {
        return false;
      }
    }
    return true;
  }

  private getRealEditedVariables(): Variable[] {
    return this.editedVariables.filter((v) => v.name.trim().length > 0);
  }

  private getEditorTextOfNullable(transformation: string | null): string {
    if (transformation === '') {
      console.log(
        'ReportValueComponent.getEditorTextOfTransformation(): transformation should not be the empty string',
      );
    }
    return transformation === null ? '' : transformation;
  }

  private getRealEditedValueForNullable(text: string): string | null {
    return text.trim().length === 0 ? null : text.trim();
  }

  private isEdited(): boolean {
    if (this.report === undefined) {
      return false;
    }
    if (this.editedReportStubStrategy === undefined) {
      throw new Error(
        'ReportValueComponent.isEdited(): Unexpectedly have editedReportStubStrategy === undefined because a report was read',
      );
    }
    const nameUnchanged = this.editedName === this.report!.name;
    const descriptionUnchanged =
      this.getRealEditedValueForNullable(this.editedDescription) === this.report!.description;
    const pathUnchanged = this.getRealEditedValueForNullable(this.editedPath) === this.report!.path;
    const transformationUnchanged =
      this.getRealEditedValueForNullable(this.editedTransformation) === this.report!.transformation;
    const reportStubStrategyUnchanged = this.editedReportStubStrategy === this.report.stubStrategy;
    return !(
      nameUnchanged &&
      descriptionUnchanged &&
      pathUnchanged &&
      transformationUnchanged &&
      this.hasNoUnsavedVariables() &&
      reportStubStrategyUnchanged
    );
  }

  private static getDefaultNodeValueState(): NodeValueState {
    return { isReadOnly: true, isEdited: false };
  }

  private static getButtonState(nodeValueState: NodeValueState): ReportButtonsState {
    const saveAllowed = nodeValueState.isEdited && !nodeValueState.isReadOnly;
    return {
      isReport: true,
      isCheckpoint: false,
      saveAllowed: saveAllowed,
    };
  }

  // TODO: Fix issue with types. Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  // Report.variables is declared to be a string, but it is really an object.
  static initVariables(variables: string | null): Variable[] {
    if (!variables) return [];
    return Object.entries(variables).map(([name, value]) => ({ name, value }));
  }

  static calculateEditedVariables(originalVariables: Variable[]): Variable[] {
    const result: Variable[] = [];
    for (const original of originalVariables) {
      result.push({ name: original.name, value: original.value });
    }
    // Empty row to allow user to add new variable.
    result.push({ name: '', value: '' });
    return result;
  }

  static getCommonVariableNames(originalVariableNames: string[], editedVariableNames: string[]): string[] {
    const originalVariableNamesSet = new Set<string>(originalVariableNames);
    const resultSet = new Set<string>();
    for (const variableName of editedVariableNames) {
      if (originalVariableNamesSet.has(variableName)) {
        resultSet.add(variableName);
      }
    }
    return [...resultSet].toSorted();
  }

  private getVariablesUpdate(): Record<string, string> {
    const variables: Record<string, string> = {};
    for (const v of this.getRealEditedVariables()) {
      variables[v.name] = v.value;
    }
    return variables;
  }
}
