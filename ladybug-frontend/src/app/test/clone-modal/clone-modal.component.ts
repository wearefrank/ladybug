/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, EventEmitter, inject, Output, ViewChild } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Report } from '../../shared/interfaces/report';
import { HttpService } from '../../shared/services/http.service';
import { FormsModule, ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { CloneReport } from 'src/app/shared/interfaces/clone-report';
import { catchError } from 'rxjs';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { ToastService } from '../../shared/services/toast.service';
import { TestListItem } from '../../shared/interfaces/test-list-item';

@Component({
  selector: 'app-clone-modal',
  templateUrl: './clone-modal.component.html',
  styleUrls: ['./clone-modal.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule],
})
export class CloneModalComponent {
  @Output() cloneReportEvent = new EventEmitter<any>();
  @ViewChild('modal') modal?: any;
  activeModal?: NgbActiveModal;
  report: Report = {} as Report;
  currentView = {
    storageName: 'Test',
  };
  variablesForm = new UntypedFormGroup({
    variablesCsv: new UntypedFormControl(''),
    message: new UntypedFormControl(''),
  });

  private modalService = inject(NgbModal);
  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);
  private toastService = inject(ToastService);

  open(selectedReport: TestListItem): void {
    this.httpService
      .getReport(selectedReport.storageId, this.currentView.storageName)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (report: Report) => {
          this.report = report;
          this.variablesForm.get('message')?.setValue(this.report.inputCheckpoint?.message);
          this.activeModal = this.modalService.open(this.modal);
        },
      });
  }

  generateClones(): void {
    const map: CloneReport = {
      csv: this.variablesForm.value.variablesCsv,
      message: this.variablesForm.value.message,
    };
    this.httpService
      .cloneReport(this.currentView.storageName, this.report.storageId, map)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (): void => {
          this.cloneReportEvent.emit();
          this.toastService.showSuccess('Report cloned!');
          this.activeModal?.dismiss();
        },
      });
  }
}
