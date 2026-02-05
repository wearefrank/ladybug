import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportMetadataTable } from './report-metadata-table';
import { PartialReport } from '../report.component';

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

function getAPartialReport(): PartialReport {
  const result = {
    name: 'My name',
    description: 'My description',
    path: 'my/path',
    transformation: 'dummy transformation',
    variables: 'not applicable, have to fix type mismatch',
    xml: 'dummy xml',
    crudStorage: true,
    // Does not have to be a stub strategy known by the FF!.
    stubStrategy: 'Some stub strategy',
    correlationId: '1',
    estimatedMemoryUsage: 5,
    storageName: 'My storage',
  };
  return { ...result };
}
