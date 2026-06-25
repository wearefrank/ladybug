import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { DebugTableGridComponent } from './debug-table-grid.component';
import { TableData } from '../../shared/services/filter2.service';
import { By } from '@angular/platform-browser';

describe('DebugTableGridComponent', () => {
  let component: DebugTableGridComponent;
  let fixture: ComponentFixture<DebugTableGridComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugTableGridComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DebugTableGridComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('When column is numeric then sorted numerically', () => {
    const tableData: TableData = {
      columns: [{ name: 'storageId', label: 'Storage Id', shown: true }],
      numericMetadataNames: new Set<string>(['storageId']),
      rows: [{ storageId: '1' }, { storageId: '10' }, { storageId: '2' }],
    };
    component.setTableData(tableData);
    fixture.detectChanges();
    const headerElement = fixture.debugElement.query(By.css('th'));
    expect(headerElement.nativeElement.textContent.trim()).toEqual('Storage Id');
    checkColumn(['1', '10', '2'], 0, 1);
    const sortArrow = fixture.debugElement.query(By.css('.mat-sort-header-arrow'));
    sortArrow.nativeElement.click();
    checkColumn(['1', '2', '10'], 0, 1);
    sortArrow.nativeElement.click();
    checkColumn(['10', '2', '1'], 0, 1);
  });

  it('When column is text then sorted alphabetically', () => {
    const tableData: TableData = {
      columns: [
        { name: 'storageId', label: 'Storage Id', shown: true },
        { name: 'text', label: 'Text', shown: true },
      ],
      numericMetadataNames: new Set<string>(['storageId']),
      rows: [
        { storageId: '1', text: 'T1' },
        { storageId: '10', text: 'T10' },
        { storageId: '2', text: 'T2' },
      ],
    };
    component.setTableData(tableData);
    fixture.detectChanges();
    const headerElements = fixture.debugElement.queryAll(By.css('th'));
    expect(headerElements.length).toEqual(2);
    expect(headerElements[0].nativeElement.textContent.trim()).toEqual('Storage Id');
    expect(headerElements[1].nativeElement.textContent.trim()).toEqual('Text');
    checkColumn(['T1', 'T10', 'T2'], 1, 2);
    const sortArrow = fixture.debugElement.queryAll(By.css('.mat-sort-header-arrow'))[1];
    sortArrow.nativeElement.click();
    checkColumn(['T1', 'T10', 'T2'], 1, 2);
    sortArrow.nativeElement.click();
    checkColumn(['T2', 'T10', 'T1'], 1, 2);
  });

  it('When status is not shown then coloring is still done based on status', () => {
    const tableData: TableData = {
      columns: [
        { name: 'storageId', label: 'Storage Id', shown: true },
        { name: 'status', label: 'Status', shown: false },
      ],
      numericMetadataNames: new Set<string>(['storageId']),
      rows: [
        { storageId: '1', status: 'error' },
        { storageId: '10', status: 'success' },
        { storageId: '2', status: 'success' },
      ],
    };
    component.setTableData(tableData);
    fixture.detectChanges();
    const headerElements = fixture.debugElement.queryAll(By.css('th'));
    expect(headerElements.length).toEqual(1);
    expect(headerElements[0].nativeElement.textContent.trim()).toEqual('Storage Id');
    checkStatuses(['statusError', 'statusSuccess', 'statusSuccess']);
    const sortArrow = fixture.debugElement.query(By.css('.mat-sort-header-arrow'));
    sortArrow.nativeElement.click();
    checkStatuses(['statusError', 'statusSuccess', 'statusSuccess']);
    sortArrow.nativeElement.click();
    checkStatuses(['statusSuccess', 'statusSuccess', 'statusError']);
  });

  function checkColumn(expected: string[], columnNumber: number, expectedNumColumns: number): void {
    const rowElements = fixture.debugElement.queryAll(By.css('[data-cy-debug="tableRow"]'));
    expect(rowElements.length).toEqual(expected.length);
    for (const [index, rowElement] of rowElements.entries()) {
      const columnElements = rowElement.nativeElement.querySelectorAll('td');
      expect(columnElements.length).toEqual(expectedNumColumns);
      expect(columnElements[columnNumber].textContent.trim()).toEqual(expected[index]);
    }
  }

  function checkStatuses(expected: string[]): void {
    const rowElements = fixture.debugElement.queryAll(By.css('[data-cy-debug="tableRow"]'));
    expect(rowElements.length).toEqual(expected.length);
    for (const [index, rowElement] of rowElements.entries()) {
      expect(rowElement.classes[expected[index]]).toEqual(true);
      const columnElements = rowElement.nativeElement.querySelectorAll('td');
      for (const columnElement of columnElements) {
        expect(columnElement.classList.contains([expected[index]])).toEqual(true);
      }
    }
  }
});
