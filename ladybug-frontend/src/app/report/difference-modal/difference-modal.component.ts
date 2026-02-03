import { Component, inject, Output, TemplateRef, ViewChild } from '@angular/core';
import { ReportDifference } from '../../shared/interfaces/report-difference';
import { Subject } from 'rxjs';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

// TODO: Obsolete. Issue https://github.com/wearefrank/ladybug-frontend/issues/1130.
export const changesActionConst = ['save', 'discard', 'saveRerun'] as const;
export type ChangesAction = (typeof changesActionConst)[number];

@Component({
  selector: 'app-difference-modal',
  standalone: true,
  templateUrl: './difference-modal.component.html',
  styleUrl: './difference-modal.component.css',
})
export class DifferenceModalComponent {
  @Output() saveChangesEvent: Subject<void> = new Subject<void>();
  @ViewChild('modal') protected modal!: TemplateRef<DifferenceModalComponent>;
  protected reportDifferences?: ReportDifference[];
  protected activeModal?: NgbModalRef;
  protected isConfirmClicked = false;

  private modalService = inject(NgbModal);

  open(differences: ReportDifference[]): void {
    this.isConfirmClicked = false;
    this.reportDifferences = differences;
    this.activeModal = this.modalService.open(this.modal, {
      backdrop: 'static',
      keyboard: false,
    });
  }

  closeModal(): void {
    if (this.activeModal) {
      this.activeModal.close();
    }
  }

  getChunkColorForDifferenceModal(chunk: number): string {
    switch (chunk) {
      case -1: {
        return '#ff7f7f';
      }
      case 1: {
        return '#7cfc00';
      }
      default: {
        return '';
      }
    }
  }

  onClickConfirm(): void {
    this.saveChangesEvent.next();
    this.isConfirmClicked = true;
    // Do not close so parent component can close when saving data is done.
  }
}
