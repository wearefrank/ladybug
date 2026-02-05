import { Component, EventEmitter, inject, Output, ViewChild } from '@angular/core';
import { NgbModal, NgbModalOptions, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { TestListItem } from '../../interfaces/test-list-item';

@Component({
  selector: 'app-delete-modal',
  standalone: true,
  imports: [],
  templateUrl: './delete-modal.component.html',
  styleUrl: './delete-modal.component.css',
})
export class DeleteModalComponent {
  @Output() confirmDeleteEvent = new EventEmitter<boolean>();
  @ViewChild('modal') modal!: NgbModal;

  protected activeModal?: NgbModalRef;
  protected reports: TestListItem[] = [];
  protected deleteQuestion?: string;
  protected deleteAllReports?: boolean;

  private modalService = inject(NgbModal);

  open(deleteAllReports: boolean, reportsToBeDeleted?: TestListItem[]): void {
    this.deleteAllReports = deleteAllReports;
    this.deleteQuestion = deleteAllReports
      ? 'Are you sure you want to delete all reports?'
      : 'Are you sure you want to delete the following reports?';
    if (reportsToBeDeleted) {
      this.reports = reportsToBeDeleted;
    }
    const options: NgbModalOptions = {
      modalDialogClass: 'modal-window',
      backdropClass: 'modal-backdrop',
    };
    this.activeModal = this.modalService.open(this.modal, options);
  }

  deleteReports(): void {
    this.confirmDeleteEvent.emit(this.deleteAllReports);
    this.activeModal?.close();
  }
}
