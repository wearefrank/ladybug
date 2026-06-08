import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { RefreshCondition } from '../shared/interfaces/refresh-condition';

@Injectable({
  providedIn: 'root',
})
export class DebugTabService {
  private refreshAllSubject = new Subject<RefreshCondition | undefined>();
  private refreshTableSubject = new Subject<RefreshCondition | undefined>();

  refreshAll$: Observable<RefreshCondition | undefined> = this.refreshAllSubject.asObservable();
  refreshTable$: Observable<RefreshCondition | undefined> = this.refreshTableSubject.asObservable();

  // triggers a refresh that refreshes both the debug table and the debug tree
  refreshAll(condition: RefreshCondition): void {
    this.refreshAllSubject.next(condition);
  }

  // triggers a refresh that refreshes only the debug table
  refreshTable(condition?: RefreshCondition): void {
    this.refreshTableSubject.next(condition);
  }
}
