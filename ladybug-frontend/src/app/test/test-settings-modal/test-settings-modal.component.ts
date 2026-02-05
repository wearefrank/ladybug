/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, EventEmitter, inject, Output, ViewChild } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';

@Component({
  selector: 'app-test-settings-modal',
  templateUrl: './test-settings-modal.component.html',
  styleUrls: ['./test-settings-modal.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule],
})
export class TestSettingsModalComponent {
  @Output() updateShowStorageIds: EventEmitter<boolean> = new EventEmitter<boolean>();

  @ViewChild('modal') modal!: any;
  settingsForm = new UntypedFormGroup({
    showReportStorageIds: new UntypedFormControl(false),
    showCheckpointIds: new UntypedFormControl(false),
  });

  private modalService = inject(NgbModal);

  open(): void {
    this.loadSettings();
    this.modalService.open(this.modal);
  }

  saveSettings(): void {
    const showStorageIds: string = this.settingsForm.get('showReportStorageIds')?.value.toString();
    const showCheckpointIds: string = this.settingsForm.get('showCheckpointIds')?.value.toString();
    localStorage.setItem('showReportStorageIds', showStorageIds);
    localStorage.setItem('showCheckpointIds', showCheckpointIds);
    this.updateShowStorageIds.next(showStorageIds === 'true');
  }

  resetSettings(): void {
    this.settingsForm.get('showReportStorageIds')?.setValue(false);
    this.settingsForm.get('showCheckpointIds')?.setValue(false);
    this.updateShowStorageIds.next(false);
  }

  loadSettings(): void {
    if (localStorage.getItem('showReportStorageIds')) {
      this.settingsForm.get('showReportStorageIds')?.setValue(localStorage.getItem('showReportStorageIds') === 'true');
    }

    if (localStorage.getItem('showCheckpointIds')) {
      this.settingsForm.get('showCheckpointIds')?.setValue(localStorage.getItem('showCheckpointIds') === 'true');
    }
  }
}
