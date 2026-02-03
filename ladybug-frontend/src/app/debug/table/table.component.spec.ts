import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TableComponent } from './table.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { View } from '../../shared/interfaces/view';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('TableComponent', () => {
  let component: TableComponent;
  let fixture: ComponentFixture<TableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TableComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TableComponent);
    component = fixture.componentInstance;
    component.currentView = {
      storageName: 'mockStorage',
      metadataNames: ['mockMetadata'],
      metadataTypes: new Map(),
    } as View;
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
