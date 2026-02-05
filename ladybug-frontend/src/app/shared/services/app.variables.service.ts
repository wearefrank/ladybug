import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
// The idea of this service is to fetch fixed variables from the backend and store them in the service.
export class AppVariablesService {
  private variables: Record<string, string> = {};
  private http = inject(HttpClient);

  fetchCustomReportActionButtonText(): void {
    if (Object.keys(this.variables).length === 0) {
      this.http
        .get<Record<string, string>>('api/report/variables')
        .pipe(
          tap((data: Record<string, string>) => {
            this.variables = data;
          }),
          catchError((error) => {
            console.error('Error fetching custom report action button text', error);
            return of(null);
          }),
        )
        .subscribe();
    }
  }

  getVariable(name: string): string {
    return this.variables[name];
  }
}
