import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { RefreshCondition } from '../shared/interfaces/refresh-condition';

@Injectable({
  providedIn: 'root',
})
export class DebugTabService {
  private refreshTableSubject = new Subject<RefreshCondition | undefined>();

  refreshTable$: Observable<RefreshCondition | undefined> = this.refreshTableSubject.asObservable();

  // triggers a refresh that refreshes only the debug table
  refreshTable(condition?: RefreshCondition): void {
    this.refreshTableSubject.next(condition);
  }
}
