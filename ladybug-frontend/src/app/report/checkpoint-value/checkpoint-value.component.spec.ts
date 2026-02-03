import { ComponentFixture, fakeAsync, flush, TestBed } from '@angular/core/testing';

import { CheckpointValueComponent, PartialCheckpoint } from './checkpoint-value.component';
import { Observable, Subject, Subscription } from 'rxjs';
import { PartialReport } from '../report.component';
import { StubStrategy } from '../../shared/enums/stub-strategy';
import { ReportButtonsState } from '../report-buttons/report-buttons';
import { TestResult } from '../../shared/interfaces/test-result';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('CheckpointValue', () => {
  let component: CheckpointValueComponent;
  let fixture: ComponentFixture<CheckpointValueComponent>;
  let originalValueSubject: Subject<PartialCheckpoint> | undefined;
  let saveDoneSubject: Subject<void> | undefined;
  let nodeValueStateSpy: jasmine.Spy | undefined;
  let buttonState: ReportButtonsState | undefined;
  let buttonStateSubscription: Subscription | undefined;
  let saveSpy: jasmine.Spy | undefined;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
      imports: [CheckpointValueComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckpointValueComponent);
    component = fixture.componentInstance;
    originalValueSubject = new Subject<PartialCheckpoint>();
    saveDoneSubject = new Subject<void>();
    nodeValueStateSpy = spyOn(component.nodeValueState, 'emit');
    component.originalCheckpoint$ = originalValueSubject;
    component.saveDone$ = saveDoneSubject;
    component.rerunResult$ = new Subject<TestResult | undefined>() as Observable<TestResult | undefined>;
    buttonStateSubscription = component.buttonStateSubject.subscribe((newButtonState) => {
      buttonState = newButtonState;
    });
    saveSpy = spyOn(component.save, 'emit');
    fixture.detectChanges();
  });

  afterEach(() => {
    buttonStateSubscription?.unsubscribe();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('When a new checkpoint is selected then consistently show not edited', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint('My value'));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
  }));

  it('When checkpoint value is edited then consistently show this change', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint('My value'));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onActualEditorContentsChanged('My other value');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointMessage).toEqual(
      'My other value',
    );
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
    component.onActualEditorContentsChanged('My value');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(3);
    expectNotEdited();
  }));

  it('When null checkpoint value is edited and cleared then it becomes the empty string', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint(null));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onActualEditorContentsChanged('My other value');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointMessage).toEqual(
      'My other value',
    );
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
    component.onActualEditorContentsChanged('');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(3);
    expect(component.getEditedRealCheckpointValue()).toEqual('');
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointMessage).toEqual('');
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
  }));

  it('When make null button is clicked while editor is empty then checkpoint value becomes null', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint(''));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onButton('makeNull');
    flush();
    expect(component.getEditedRealCheckpointValue()).toEqual(null);
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointMessage).toEqual(null);
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
  }));

  it('When make null button is clicked while editor is not empty then checkpoint value becomes null', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint('Some value'));
    flush();
    expect(component.getEditedRealCheckpointValue()).toEqual('Some value');
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onButton('makeNull');
    flush();
    expect(component.getEditedRealCheckpointValue()).toEqual(null);
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointMessage).toEqual(null);
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
  }));

  it('When checkpoint level stub strategy is edited then consistently show this change', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint('My value'));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onCheckpointStubStrategyChange(StubStrategy.checkpointIndex2Stub(1));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    // Checkpoint stub strategy has to be updated by dedicated HTTP request.
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.stub).toEqual(
      StubStrategy.checkpointIndex2Stub(1),
    );
    expect(saveSpy?.calls.mostRecent().args[0].updateReport.checkpoints[0]?.checkpointId).toEqual('0');
    expect(saveSpy?.calls.mostRecent().args[0].checkpointUidToRestore).toEqual('0#0');
    component.onCheckpointStubStrategyChange(StubStrategy.checkpointIndex2Stub(0));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(3);
    expectNotEdited();
  }));

  it('When report level stub strategy is edited then consistently show this change', fakeAsync(() => {
    originalValueSubject!.next(getPartialCheckpoint('My value'));
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(1);
    expectNotEdited();
    component.onReportStubStrategyChange('Other stub strategy');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(2);
    expectIsEdited();
    component.requestSave();
    expect(saveSpy?.calls.mostRecent().args[0].updateReport?.stubStrategy).toEqual('Other stub strategy');
    component.onReportStubStrategyChange('Some stub strategy');
    flush();
    expect(component.nodeValueState.emit).toHaveBeenCalledTimes(3);
    expectNotEdited();
  }));

  it('When the checkpoint-s report is in a CRUD storage then the emitted events indicate not read-only', fakeAsync(() => {
    let checkpoint = getPartialCheckpoint('Some value');
    checkpoint.parentReport.crudStorage = true;
    originalValueSubject!.next(checkpoint);
    flush();
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isReadOnly).toEqual(false);
    expect(component.labels?.isReadOnly).toEqual(false);
    component.onActualEditorContentsChanged('My other value');
    flush();
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isReadOnly).toEqual(false);
    expect(component.labels?.isReadOnly).toEqual(false);
  }));

  it('When the checkpoint-s report is not in a CRUD storage then the emitted events indicate read-only', fakeAsync(() => {
    let checkpoint = getPartialCheckpoint('Some value');
    checkpoint.parentReport.crudStorage = false;
    originalValueSubject!.next(checkpoint);
    flush();
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isReadOnly).toEqual(true);
    expect(component.labels?.isReadOnly).toEqual(true);
    component.onActualEditorContentsChanged('My other value');
    flush();
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isReadOnly).toEqual(true);
    expect(component.labels?.isReadOnly).toEqual(true);
  }));

  function expectNotEdited(): void {
    expect(component.labels?.isEdited).toEqual(false);
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isEdited).toEqual(false);
    expect(component.getDifferences().data.length).toEqual(0);
    expect(buttonState?.saveAllowed).toEqual(false);
  }

  function expectIsEdited(): void {
    expect(component.labels?.isEdited).toEqual(true);
    expect(nodeValueStateSpy?.calls.mostRecent().args[0].isEdited).toEqual(true);
    expect(component.getDifferences().data.length).not.toEqual(0);
    expect(buttonState?.saveAllowed).toEqual(true);
  }
});

function getPartialCheckpoint(message: string | null): PartialCheckpoint {
  const parentSeed = {
    name: 'My name',
    description: null,
    path: null,
    transformation: null,
    variables: 'not applicable, have to fix type mismatch',
    xml: 'dummy xml',
    crudStorage: true,
    // Does not have to be a stub strategy known by the FF!.
    stubStrategy: 'Some stub strategy',
    correlationId: '1',
    estimatedMemoryUsage: 5,
    storageName: 'My storage',
  };
  const parent: PartialReport = { ...parentSeed };
  const result = {
    index: 0,
    message,
    stubbed: false,
    preTruncatedMessageLength: message === null ? 0 : message.length,
    // Use report level stub strategy
    stub: StubStrategy.checkpointIndex2Stub(0),
    parentReport: parent,
    name: 'Some name',
    threadName: 'Some thread name',
    typeAsString: 'string',
    level: 1,
    uid: '0#0',
  };
  return { ...result };
}
