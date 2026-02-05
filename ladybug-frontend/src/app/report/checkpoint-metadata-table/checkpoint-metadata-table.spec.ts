import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CheckpointMetadataTable } from './checkpoint-metadata-table';
import { PartialReport } from '../report.component';
import { PartialCheckpoint } from '../checkpoint-value/checkpoint-value.component';
import { StubStrategy } from '../../shared/enums/stub-strategy';

describe('CheckpointMetadataTable', () => {
  let component: CheckpointMetadataTable;
  let fixture: ComponentFixture<CheckpointMetadataTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckpointMetadataTable],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckpointMetadataTable);
    component = fixture.componentInstance;
    component.checkpoint = getPartialCheckpoint();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

function getPartialCheckpoint(): PartialCheckpoint {
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
    uid: '0#0',
    message: 'Some message',
    stubbed: false,
    preTruncatedMessageLength: 5,
    // Use report level stub strategy
    stub: StubStrategy.checkpointIndex2Stub(0),
    parentReport: parent,
    name: 'Some name',
    threadName: 'Some thread name',
    typeAsString: 'string',
    level: 1,
  };
  return { ...result };
}
