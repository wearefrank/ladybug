import { Component, inject } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-overwrite-transformation-modal',
  standalone: true,
  templateUrl: './overwrite-transformation-modal.component.html',
  styleUrl: './overwrite-transformation-modal.component.css',
})
export class OverwriteTransformationComponent {
  dialogReference = inject(MatDialogRef<OverwriteTransformationComponent>);

  onConfirm(): void {
    this.dialogReference.close(true);
  }

  onNoConfirm(): void {
    this.dialogReference.close(false);
  }
}
