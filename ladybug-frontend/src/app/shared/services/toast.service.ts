import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { Toast, ToastCallback } from '../interfaces/toast';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  readonly TOASTER_LINE_LENGTH: number = 37;
  private toastSubject: Subject<Toast> = new ReplaySubject(1);

  toastObservable: Observable<Toast> = this.toastSubject.asObservable();

  public showDanger(body: string, detailedInfo?: string, toastCallback?: ToastCallback): void {
    this.toastSubject.next({
      type: 'danger',
      message: body,
      detailed: detailedInfo,
      toastCallback: toastCallback,
    } as Toast);
  }

  public showWarning(body: string, toastCallback?: ToastCallback): void {
    this.toastSubject.next({
      type: 'warning',
      message: body,
      toastCallback: toastCallback,
    } as Toast);
  }

  public showSuccess(body: string, toastCallback?: ToastCallback): void {
    this.toastSubject.next({
      type: 'success',
      message: body,
      toastCallback: toastCallback,
    } as Toast);
  }

  public showSuccessLong(body: string, toastCallback?: ToastCallback): void {
    this.toastSubject.next({
      type: 'long-success',
      message: body,
      toastCallback: toastCallback,
    } as Toast);
  }

  public showInfo(body: string, toastCallback?: ToastCallback): void {
    this.toastSubject.next({
      type: 'info',
      message: body,
      toastCallback: toastCallback,
    } as Toast);
  }
}
