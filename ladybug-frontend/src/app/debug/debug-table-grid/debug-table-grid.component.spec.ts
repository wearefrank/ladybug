import { ComponentFixture, TestBed } from '@angular/core/testing';

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { DebugTableGridComponent } from './debug-table-grid.component';
import { TableData } from '../../shared/services/filter.service';
import { By } from '@angular/platform-browser';

describe('DebugTableGridComponent', () => {
  let component: DebugTableGridComponent;
  let fixture: ComponentFixture<DebugTableGridComponent>;
  let checkedStorageIdsSpy: jasmine.Spy | undefined;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugTableGridComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(DebugTableGridComponent);
    component = fixture.componentInstance;
    checkedStorageIdsSpy = spyOn(component.checkedStorageIds, 'emit');
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
    const headerElements = fixture.debugElement.queryAll(By.css('th'));
    // Column #0 is the checkbox at the start of the table row.
    expect(headerElements[1].nativeElement.textContent.trim()).toEqual('Storage Id');
    checkColumn(['1', '10', '2'], 1, 2);
    const sortArrow = fixture.debugElement.query(By.css('.mat-sort-header-arrow'));
    sortArrow.nativeElement.click();
    checkColumn(['1', '2', '10'], 1, 2);
    sortArrow.nativeElement.click();
    checkColumn(['10', '2', '1'], 1, 2);
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
    // Checkbox, StorageId, Text.
    expect(headerElements.length).toEqual(3);
    expect(headerElements[1].nativeElement.textContent.trim()).toEqual('Storage Id');
    expect(headerElements[2].nativeElement.textContent.trim()).toEqual('Text');
    checkColumn(['T1', 'T10', 'T2'], 2, 3);
    // Column #0 has no sort arrow, sort arrows have index one lower than columns.
    const sortArrow = fixture.debugElement.queryAll(By.css('.mat-sort-header-arrow'))[1];
    sortArrow.nativeElement.click();
    checkColumn(['T1', 'T10', 'T2'], 2, 3);
    sortArrow.nativeElement.click();
    checkColumn(['T2', 'T10', 'T1'], 2, 3);
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
    // Column #0 is the checkbox.
    expect(headerElements.length).toEqual(2);
    expect(headerElements[1].nativeElement.textContent.trim()).toEqual('Storage Id');
    checkStatuses(['statusError', 'statusSuccess', 'statusSuccess']);
    const sortArrow = fixture.debugElement.query(By.css('.mat-sort-header-arrow'));
    sortArrow.nativeElement.click();
    checkStatuses(['statusError', 'statusSuccess', 'statusSuccess']);
    sortArrow.nativeElement.click();
    checkStatuses(['statusSuccess', 'statusSuccess', 'statusError']);
  });

  describe('Check / uncheck', () => {
    beforeEach(() => {
      const tableData: TableData = {
        columns: [{ name: 'storageId', label: 'Storage Id', shown: true }],
        numericMetadataNames: new Set<string>(['storageId']),
        rows: [{ storageId: '1' }, { storageId: '10' }, { storageId: '2' }],
      };
      component.setTableData(tableData);
      fixture.detectChanges();
    });

    it('When row checked or unchecked then event emitted', () => {
      const dataCheckboxElements = fixture.debugElement.queryAll(By.css('[data-cy-debug="selectOne"]'));
      dataCheckboxElements[1].nativeElement.click();
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual(['10']);
      dataCheckboxElements[1].nativeElement.click();
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual([]);
    });

    it('When multiple rows checked then storage ids reported in ascending numerical order', () => {
      const dataCheckboxElements = fixture.debugElement.queryAll(By.css('[data-cy-debug="selectOne"]'));
      dataCheckboxElements[2].nativeElement.click();
      dataCheckboxElements[1].nativeElement.click();
      dataCheckboxElements[0].nativeElement.click();
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual(['1', '2', '10']);
    });

    it('When some rows checked and all selected checkbox is checked then all are checked', () => {
      const dataCheckboxElements = fixture.debugElement.queryAll(By.css('[data-cy-debug="selectOne"]'));
      dataCheckboxElements[1].nativeElement.click();
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual(['10']);
      const selectAllCheckbox = fixture.debugElement.query(By.css('[data-cy-debug="selectAll"]'));
      selectAllCheckbox.nativeElement.click();
      fixture.detectChanges();
      expect(selectAllCheckbox.nativeElement.checked).toEqual(true);
      for (const dataCheckboxElement of dataCheckboxElements) {
        expect(dataCheckboxElement.nativeElement.checked).toEqual(true);
      }
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual(['1', '2', '10']);
      // Clicking another time should deselect all
      selectAllCheckbox.nativeElement.click();
      fixture.detectChanges();
      expect(selectAllCheckbox.nativeElement.checked).toEqual(false);
      for (const dataCheckboxElement of dataCheckboxElements) {
        expect(dataCheckboxElement.nativeElement.checked).toEqual(false);
      }
      expect(checkedStorageIdsSpy!.calls.mostRecent().args[0]).toEqual([]);
    });
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
