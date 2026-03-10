import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ClientSettingsService {
  private readonly tableSpacingKey = 'tableSpacing';
  private readonly showMultipleRecordsAtATimeKey = 'showMultipleFilesAtATime';
  private readonly defaultAmountOfRecordsInTable: number = 10;
  private readonly amountOfRecordsInTableKey = 'amountOfRecordsInTable';
  private readonly transformationEnabledKey = 'transformationEnabled';
  private readonly forMultipleOmitIfXmlEmptyKey = 'forMultipleOmitIfXmlEmpty';
  private readonly showStorageIdsInTestTabKey = 'showReportStorageIds';

  private showMultipleAtATimeSubject = new BehaviorSubject<boolean>(this.isShowMultipleReportsAtATime());
  private tableSpacingSubject = new BehaviorSubject<number>(this.getTableSpacing());
  private amountOfRecordsInTableSubject = new BehaviorSubject<number>(this.getAmountOfRecordsInTable());
  private showStorageIdsInTestTabSubject = new BehaviorSubject<boolean>(this.isShowStorageIdsInTestTab());

  // Cannot put public properties first because properties cannot be used before their initialization.
  public showMultipleAtATimeObservable = this.showMultipleAtATimeSubject as Observable<boolean>;
  public tableSpacingObservable = this.tableSpacingSubject as Observable<number>;
  public amountOfRecordsInTableObservable = this.amountOfRecordsInTableSubject as Observable<number>;
  public showStorageIdsInTestTabObservable = this.showStorageIdsInTestTabSubject as Observable<boolean>;

  public isShowMultipleReportsAtATime(): boolean {
    return localStorage.getItem(this.showMultipleRecordsAtATimeKey) === 'true';
  }

  public setShowMultipleReportsatATime(value: boolean): void {
    localStorage.setItem(this.showMultipleRecordsAtATimeKey, String(value));
    this.showMultipleAtATimeSubject.next(value);
  }

  public getTableSpacing(): number {
    const MAX_ALLOWED_DROPDOWN_VALUE = 8;
    const temporaryTableSpacing: number = +(localStorage.getItem(this.tableSpacingKey) ?? 1);
    const cappedTableSpacing: number = Math.min(temporaryTableSpacing, MAX_ALLOWED_DROPDOWN_VALUE);
    return cappedTableSpacing;
  }

  public setTableSpacing(value: number): void {
    localStorage.setItem(this.tableSpacingKey, String(value));
    this.tableSpacingSubject.next(this.getTableSpacing());
  }

  public getAmountOfRecordsInTable(): number {
    const raw: string | null = localStorage.getItem(this.amountOfRecordsInTableKey);
    if (raw !== null) {
      const parsed: number = +raw;
      const isInvalid = !Number.isInteger(parsed);
      return isInvalid ? this.defaultAmountOfRecordsInTable : parsed;
    }
    return this.defaultAmountOfRecordsInTable;
  }

  public setAmountOfRecordsInTable(value: number): void {
    localStorage.setItem(this.amountOfRecordsInTableKey, String(value));
    this.amountOfRecordsInTableSubject.next(value);
  }

  public isTransformationEnabled(): boolean {
    const raw: string | null = localStorage.getItem(this.transformationEnabledKey);
    return raw === 'true';
  }

  public setTransformationEnabled(value: boolean): void {
    localStorage.setItem(this.transformationEnabledKey, value ? 'true' : 'false');
  }

  public isForMultipleOmitIfXmlEmpty(): boolean {
    const raw: string | null = localStorage.getItem(this.forMultipleOmitIfXmlEmptyKey);
    return raw === 'true';
  }

  public setForMultipleOmitIfXmlEmpty(value: boolean): void {
    localStorage.setItem(this.forMultipleOmitIfXmlEmptyKey, value ? 'true' : 'false');
  }

  public isShowStorageIdsInTestTab(): boolean {
    const raw: string | null = localStorage.getItem(this.showStorageIdsInTestTabKey);
    return raw === 'true';
  }

  public setShowStorageIdsInTestTab(value: boolean): void {
    localStorage.setItem(this.showStorageIdsInTestTabKey, value ? 'true' : 'false');
    this.showStorageIdsInTestTabSubject.next(value);
  }

  public toggleShowStorageIdsInTestTab(): void {
    console.log(`ClientSettingsService.toggleShowStorageIdsInTestTab()`);
    this.setShowStorageIdsInTestTab(!this.isShowStorageIdsInTestTab());
  }

  public backToFactory(): void {
    this.setAmountOfRecordsInTable(10);
    this.setShowMultipleReportsatATime(false);
    this.setTableSpacing(1);
    this.setTransformationEnabled(true);
    // forMultipleOmitIfXmlEmpty and showStorageIdsInTestTab are not part of the factory reset
    // because these setting are not managed via the debug tab settings dialog.
  }
}
