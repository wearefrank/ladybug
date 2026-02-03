import { Component, inject, NgZone, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { NgbModal, NgbToast } from '@ng-bootstrap/ng-bootstrap';
import { Toast } from '../../interfaces/toast';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass } from '@angular/common';
import { FilterService } from '../../../debug/filter-side-drawer/filter.service';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css'],
  standalone: true,
  imports: [NgbToast, ClipboardModule, NgClass],
})
export class ToastComponent implements OnInit, OnDestroy {
  @ViewChild('modal') modal!: TemplateRef<Element>;
  selected!: Toast;
  toasts: Toast[] = [];
  justCopied = false;
  filterPanelVisible = false;

  private subscriptions: Subscription = new Subscription();

  private modalService = inject(NgbModal);
  private toastService = inject(ToastService);
  private filterService = inject(FilterService);
  private ngZone = inject(NgZone);

  ngOnInit(): void {
    const toastSubscription = this.toastService.toastObservable.subscribe((toast: Toast): void => {
      this.ngZone.run(() => this.toasts.push(toast));
    });
    this.subscriptions.add(toastSubscription);
    const filterSubscription = this.filterService.filterSidePanel$.subscribe((value) => {
      this.filterPanelVisible = value;
    });
    this.subscriptions.add(filterSubscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  close(alert: Toast): void {
    this.toasts.splice(this.toasts.indexOf(alert), 1);
  }

  showDetailedErrorMessages(alert: Toast): void {
    this.selected = alert;
    this.modalService.open(this.modal, { size: 'lg' });
  }

  copyToClipboard(): void {
    this.justCopied = true;
    setTimeout(() => {
      this.justCopied = false;
    }, 2000);
  }

  executeCallback(toast: Toast): void {
    if (toast.toastCallback) {
      toast.toastCallback.callback();
      setTimeout(() => this.close(toast), 500);
    }
  }

  protected getToastClass(toastType: string): string {
    if (toastType.startsWith('long-')) {
      return `bg-${toastType.slice('long-'.length)}`;
    }
    return `bg-${toastType}`;
  }
}
