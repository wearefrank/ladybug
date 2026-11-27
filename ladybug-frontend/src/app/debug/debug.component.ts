import { Component, inject, OnInit } from '@angular/core';
import { ToastService } from '../shared/services/toast.service';
import { HttpService } from '../shared/services/http.service';
import { View } from '../shared/interfaces/view';
import { catchError } from 'rxjs';
import { ErrorHandling } from '../shared/classes/error-handling.service';
import { DebugReportComponent } from '../report/debug-report.component/debug-report.component';
import { ActivatedRoute } from '@angular/router';
import { MetadataFilter, TabService } from '../shared/services/tab.service';
import { FilterService } from '../shared/services/filter.service';
import { DebugTableWithControlsComponent } from './debug-table-with-controls/debug-table-with-controls.component';
import { DebugTreeStateService2 } from './debug-tree-state2.service';

@Component({
  selector: 'app-debug',
  templateUrl: './debug.component.html',
  styleUrls: ['./debug.component.css'],
  standalone: true,
  imports: [DebugTableWithControlsComponent, DebugReportComponent],
  // Shared by the debug table and the debug report, one instance per debug tab.
  providers: [DebugTreeStateService2],
})
export class DebugComponent implements OnInit {
  currentView?: View;
  views?: View[];

  private httpService = inject(HttpService);
  private toastService = inject(ToastService);
  private errorHandler = inject(ErrorHandling);
  private tabService = inject(TabService);
  private filterService = inject(FilterService);
  private route = inject(ActivatedRoute);
  private treeState = inject(DebugTreeStateService2);

  ngOnInit(): void {
    const urlFilters: MetadataFilter[] = this.tabService.routeGetFilters(this.route.snapshot);
    this.filterService.setUrlFilters(urlFilters);
    console.log('Initialized URL filters from route:');
    for (const urlFilter of urlFilters) {
      console.log(`  ${urlFilter.metadataName}=${urlFilter.value}`);
    }
    this.retrieveViews();
    this.tabService.visitDebugTab(this.route.snapshot);
  }

  protected onViewChange(view: View): void {
    this.currentView = view;
    this.retrieveErrorsAndWarnings();
    this.filterService.setCurrentView(view);
    this.treeState.changeView(view);
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
