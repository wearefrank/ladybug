import { Component, inject, Input, OnDestroy, OnInit } from '@angular/core';
import { FilterService } from './filter.service';
import { catchError, Subscription } from 'rxjs';
import { animate, style, transition, trigger } from '@angular/animations';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { View } from '../../shared/interfaces/view';
import { Report } from '../../shared/interfaces/report';
import { HttpService } from '../../shared/services/http.service';
import { ErrorHandling } from 'src/app/shared/classes/error-handling.service';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';

@Component({
  standalone: true,
  selector: 'app-filter-side-drawer',
  templateUrl: './filter-side-drawer.component.html',
  styleUrl: './filter-side-drawer.component.css',
  animations: [
    trigger('removeTrigger', [
      transition(':enter', [
        style({ transform: 'translateX(100%)' }),
        animate('300ms ease-in', style({ transform: 'translateX(0)' })),
      ]),
      transition(':leave', animate('300ms ease-out', style({ transform: 'translateX(100%)' }))),
    ]),
  ],
  imports: [MatAutocompleteModule, FormsModule, TitleCasePipe, ShortenedTableHeaderPipe],
})
export class FilterSideDrawerComponent implements OnDestroy, OnInit {
  @Input({ required: true }) currentView!: View;

  protected shouldShowFilter?: boolean;
  protected metadataLabels?: string[];
  protected currentRecords: Map<string, string[]> = new Map<string, string[]>();
  protected metadataTypes?: Map<string, string>;
  protected toolTipSuggestions?: Report;
  protected filterService = inject(FilterService);

  private subscriptions: Subscription = new Subscription();

  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.setSubscriptions();
    this.getFilterToolTips();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  setSubscriptions(): void {
    const showFilterSubscription: Subscription = this.filterService.showFilter$.subscribe({
      next: (show: boolean) => {
        this.shouldShowFilter = show;
        this.filterService.toggleShowFilterSidePanel(show);
      },
    });
    this.subscriptions.add(showFilterSubscription);
    const metadataLabelsSubscription: Subscription = this.filterService.metadataLabels$.subscribe({
      next: (metadataLabels: string[]) => {
        this.metadataLabels = metadataLabels;
      },
    });
    this.subscriptions.add(metadataLabelsSubscription);
    const currentRecordSubscription: Subscription = this.filterService.currentRecords$.subscribe({
      next: (records: Map<string, string[]>) => (this.currentRecords = records),
    });
    this.subscriptions.add(currentRecordSubscription);
    const metadataTypesSubscription: Subscription = this.filterService.metadataTypes$.subscribe({
      next: (metadataTypes: Map<string, string>) => (this.metadataTypes = metadataTypes),
    });
    this.subscriptions.add(metadataTypesSubscription);
  }

  getFilterToolTips(): void {
    this.httpService
      .getUserHelp(this.currentView.storageName, this.currentView.metadataNames)
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe({
        next: (response: Report) => (this.toolTipSuggestions = response),
      });
  }

  closeFilter(): void {
    this.filterService.setShowFilter(false);
  }

  updateFilter(filter: string, metadataName: string): void {
    this.filterService.updateFilterContext(metadataName, filter);
  }

  removeFilter(metadataName: string): void {
    this.filterService.updateFilterContext(metadataName, '');
  }

  getTooltipSuggestion<K extends keyof Report>(key: K): Report[K] | undefined {
    return this.toolTipSuggestions?.[key];
  }

  // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1126.
  toKeyofReport(raw: string): keyof Report {
    return raw as keyof Report;
  }
}
