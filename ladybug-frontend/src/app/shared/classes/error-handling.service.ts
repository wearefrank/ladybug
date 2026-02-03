import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ErrorHandling {
  private toastService = inject(ToastService);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handler(error: HttpErrorResponse): Observable<any> {
    console.warn(error);
    const message = error.error;
    if (error.status > 399 && error.status < 500) {
      this.toastService.showWarning(message);
    } else if (message && typeof message === 'string' && message.includes('- detailed error message -')) {
      const errorMessageParts = message.split('- detailed error message -');
      this.toastService.showDanger(errorMessageParts[0], errorMessageParts[1]);
    } else {
      this.toastService.showDanger(error.message, '');
    }
    return of(error);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  handleError(): (error: HttpErrorResponse) => Observable<any> {
    return this.handler.bind(this);
  }
}
