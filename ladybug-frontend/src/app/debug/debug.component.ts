import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { ToastService } from '../shared/services/toast.service';
import { HttpService } from '../shared/services/http.service';
import { View } from '../shared/interfaces/view';
import { catchError } from 'rxjs';
import { ErrorHandling } from '../shared/classes/error-handling.service';
import { DebugReportComponent } from '../report/debug-report.component/debug-report.component';
import { HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { ActivatedRoute } from '@angular/router';
import { FilterFromUrl, TabService } from '../shared/services/tab.service';
import { Filter2Service } from '../shared/services/filter2.service';
import { TableComponent2 } from './table/table2.component';

@Component({
  selector: 'app-debug',
  templateUrl: './debug.component.html',
  styleUrls: ['./debug.component.css'],
  standalone: true,
  imports: [TableComponent2, DebugReportComponent],
})
export class DebugComponent implements OnInit {
  @ViewChild('reportComponent') customReportComponent!: DebugReportComponent;
  currentView?: View;
  views?: View[];

  private httpService = inject(HttpService);
  private toastService = inject(ToastService);
  private errorHandler = inject(ErrorHandling);
  private tabService = inject(TabService);
  private filterService = inject(Filter2Service);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    const urlFilters: FilterFromUrl[] = this.tabService.routeGetFilters(this.route.snapshot);
    this.filterService.setUrlFilters(urlFilters);
    console.log('Initialized URL filters from route:');
    for (const urlFilter of urlFilters) {
      console.log(`  ${urlFilter.metadataName}=${urlFilter.value}`);
    }
    this.retrieveViews();
    this.tabService.visitDebugTab(this.route.snapshot);
  }

  protected addReportToTree(report: HierarchicalReport): void {
    this.customReportComponent.addReport(report);
  }

  protected onViewChange(view: View): void {
    this.currentView = view;
    this.retrieveErrorsAndWarnings();
    this.filterService.setCurrentView(this.currentView);
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
