import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  //Show multiple files in debug tree
  public readonly defaultShowMultipleFilesAtATime: boolean = true;
  private showMultipleAtATimeKey = 'showMultipleFilesAtATime';
  private showMultipleAtATimeSubject: Subject<boolean> = new ReplaySubject(1);

  public showMultipleAtATimeObservable: Observable<boolean> = this.showMultipleAtATimeSubject.asObservable();

  //Table spacing settings

  public readonly defaultTableSpacing: number = 1;
  private tableSpacingKey = 'tableSpacing';
  private tableSpacingSubject: Subject<number> = new ReplaySubject(1);

  public tableSpacingObservable: Observable<number> = this.tableSpacingSubject.asObservable();

  //Table settings

  public readonly defaultAmountOfRecordsInTable: number = 10;
  private amountOfRecordsInTableKey = 'amountOfRecordsInTable';
  private amountOfRecordsInTableSubject: Subject<number> = new ReplaySubject(1);

  public amountOfRecordsInTableObservable: Observable<number> = this.amountOfRecordsInTableSubject.asObservable();

  constructor() {
    this.loadSettingsFromLocalStorage();
  }

  public setShowMultipleAtATime(value: boolean = this.defaultShowMultipleFilesAtATime): void {
    this.showMultipleAtATimeSubject.next(value);
    localStorage.setItem(this.showMultipleAtATimeKey, String(value));
  }

  public setTableSpacing(value: number = this.defaultTableSpacing): void {
    this.tableSpacingSubject.next(value);
    localStorage.setItem(this.tableSpacingKey, String(value));
  }

  public setAmountOfRecordsInTable(value: number = this.defaultAmountOfRecordsInTable): void {
    this.amountOfRecordsInTableSubject.next(value);
    localStorage.setItem(this.amountOfRecordsInTableKey, String(value));
  }

  private loadSettingsFromLocalStorage(): void {
    this.setShowMultipleAtATime(localStorage.getItem(this.showMultipleAtATimeKey) === 'true');
    const MAX_ALLOWED_DROPDOWN_VALUE = 8;
    const temporaryTableSpacing: number = +(localStorage.getItem(this.tableSpacingKey) ?? 1);
    const cappedTableSpacing: number = Math.min(temporaryTableSpacing, MAX_ALLOWED_DROPDOWN_VALUE);
    this.setTableSpacing(cappedTableSpacing);
    const amountOfRecordsInTable =
      localStorage.getItem(this.amountOfRecordsInTableKey) ?? this.defaultAmountOfRecordsInTable;
    this.setAmountOfRecordsInTable(+amountOfRecordsInTable);
  }
}
