import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CheckpointMetadataTable } from './checkpoint-metadata-table';
import { StubStrategy } from '../../shared/enums/stub-strategy';
import { HierarchicalReport, HierarchicalCheckpoint } from '../../shared/interfaces/hierarchical-report';

describe('CheckpointMetadataTable', () => {
  let component: CheckpointMetadataTable;
  let fixture: ComponentFixture<CheckpointMetadataTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckpointMetadataTable],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckpointMetadataTable);
    component = fixture.componentInstance;
    component.checkpoint = getHierarchicalCheckpoint();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

function getHierarchicalCheckpoint(): HierarchicalCheckpoint {
  const report: HierarchicalReport = {
    name: 'My name',
    children: [],
    description: null,
    path: null,
    stubStrategy: 'Some stub strategy',
    linkMethod: 'Some link method',
    transformation: null,
    storageId: 0,
    storageName: 'My storage',
    crudStorage: true,
    estimatedMemoryUsage: 5,
    correlationId: '1',
    variables: {},
    rerunnable: true,
    xml: 'dummy xml',
    checkpointsFromView: null,
    startTime: 0,
  };
  report.children?.push({
    name: 'Some name',
    children: null,
    message: 'Some message',
    encoding: '',
    messageContext: null,
    type: 1,
    level: 1,
    stub: StubStrategy.checkpointIndex2Stub(0),
    stubbed: false,
    stubNotFound: null,
    preTruncatedMessageLength: 5,
    typeAsString: 'string',
    threadName: 'Some thread name',
    sourceClassName: '',
    messageClassName: '',
    id: 0,
    uid: '0#0',
    // Use report level stub strategy
    report,
  });
  return report.children![0];
}
