import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TableComponent2 } from './table2.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { View } from '../../shared/interfaces/view';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('TableComponent', () => {
  let component: TableComponent2;
  let fixture: ComponentFixture<TableComponent2>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TableComponent2],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TableComponent2);
    component = fixture.componentInstance;
    component.currentView = {
      storageName: 'mockStorage',
      metadataNames: ['mockMetadata'],
      // TODO: Am I confusing metadataNames and metadataLabels?
      metadataLabels: ['mockMetadata'],
      metadataTypes: new Map(),
    } as View;
    // TODO: Pass this to the table component.
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should convert JSON to CSV correctly', () => {
    const testData = [
      { id: 1, name: 'Alice' },
      { id: 2, name: 'Bob' },
    ];

    const expectedCsv = 'id,name\n' + '"1","Alice"\n' + '"2","Bob"';

    const csv = component.jsonToCsv(testData);
    expect(csv).toBe(expectedCsv);
  });

  it('should trigger a CSV file download', () => {
    const spyCreate = spyOn(document, 'createElement').and.callThrough();
    const spyAppend = spyOn(document.body, 'append').and.callThrough();

    component['triggerCsvDownload']('test,data', 'test.csv');

    expect(spyCreate).toHaveBeenCalledWith('a');
    expect(spyAppend).toHaveBeenCalled();
  });
});
