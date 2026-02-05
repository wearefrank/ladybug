import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { Report } from '../shared/interfaces/report';
import { TableComponent } from './table/table.component';
import { ToastService } from '../shared/services/toast.service';
import { HttpService } from '../shared/services/http.service';
import { View } from '../shared/interfaces/view';
import { catchError } from 'rxjs';
import { ErrorHandling } from '../shared/classes/error-handling.service';
import { ReportComponent } from '../report/report.component';

@Component({
  selector: 'app-debug',
  templateUrl: './debug.component.html',
  styleUrls: ['./debug.component.css'],
  standalone: true,
  imports: [TableComponent, ReportComponent],
})
export class DebugComponent implements OnInit {
  static readonly ROUTER_PATH: string = 'debug';
  @ViewChild('reportComponent') customReportComponent!: ReportComponent;
  currentView?: View;
  views?: View[];

  private httpService = inject(HttpService);
  private toastService = inject(ToastService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.retrieveViews();
  }

  protected addReportToTree(report: Report): void {
    this.customReportComponent.addReportToTree(report);
  }

  protected onViewChange(view: View): void {
    this.currentView = view;
    this.retrieveErrorsAndWarnings();
  }

  private retrieveViews(): void {
    this.httpService
      .getViews()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (views: View[]) => {
          this.views = views;
          if (!this.currentView) {
            this.onViewChange(this.views.find((v: View) => v.defaultView)!);
          }
        },
      });
  }

  private retrieveErrorsAndWarnings(): void {
    if (this.currentView) {
      this.httpService
        .getWarningsAndErrors(this.currentView.storageName)
        .pipe(catchError(this.errorHandler.handleError()))
        .subscribe({
          next: (value: string | undefined): void => {
            if (value) {
              this.showErrorsAndWarnings(value);
            }
          },
        });
    }
  }

  private showErrorsAndWarnings(value: string): void {
    if (value.length > this.toastService.TOASTER_LINE_LENGTH) {
      const errorSnippet: string = value.slice(0, Math.max(0, this.toastService.TOASTER_LINE_LENGTH)).trim();
      this.toastService.showDanger(`${errorSnippet}...`, value);
    } else {
      this.toastService.showDanger(value, value);
    }
  }
}
