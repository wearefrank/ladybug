import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportMetadataTable } from './report-metadata-table';
import { HierarchicalReport } from '../../shared/interfaces/hierarchical-report';

describe('ReportMetadataTable', () => {
  let component: ReportMetadataTable;
  let fixture: ComponentFixture<ReportMetadataTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportMetadataTable],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportMetadataTable);
    component = fixture.componentInstance;
    component.report = getAPartialReport();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

function getAPartialReport(): HierarchicalReport {
  return {
    name: 'My name',
    children: [],
    description: 'My description',
    path: 'my/path',
    stubStrategy: 'Some stub strategy',
    linkMethod: 'Some link method',
    transformation: 'dummy transformation',
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
}
