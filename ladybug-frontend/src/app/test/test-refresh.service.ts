import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TestRefreshService {
  private refreshAllSubject = new Subject<void>();

  refreshAll$ = this.refreshAllSubject.asObservable();

  refreshAll(): void {
    this.refreshAllSubject.next();
  }
}
