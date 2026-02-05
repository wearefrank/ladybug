import { Directive, HostListener, inject, Input } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';
import { Clipboard } from '@angular/cdk/clipboard';

@Directive({
  selector: '[appCopyTooltip]',
  providers: [MatTooltip, Clipboard],
  standalone: true,
})
export class CopyTooltipDirective {
  @Input({ required: true, alias: 'appCopyTooltip' }) value!: string | number | undefined;

  private tooltipInstance = inject(MatTooltip);
  private clipboard = inject(Clipboard);

  constructor() {
    this.tooltipInstance.message = 'Copied';
  }

  @HostListener('click')
  showTooltip(): void {
    if (this.valueNotNullOrEmpty()) {
      this.clipboard.copy(String(this.value));
      this.tooltipInstance.show();
      setTimeout(() => this.tooltipInstance.hide(), 2000);
    }
  }

  private valueNotNullOrEmpty(): boolean {
    return !!this.value && this.value !== '';
  }
}
