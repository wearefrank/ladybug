import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DictionaryPipe } from '../../shared/pipes/dictionary.pipe';
import { NgClass, TitleCasePipe } from '@angular/common';
import { catchError, Subscription } from 'rxjs';
import { FilterService } from '../filter-side-drawer/filter.service';
import { ErrorHandling } from '../../shared/classes/error-handling.service';

@Component({
  selector: 'app-active-filters',
  templateUrl: './active-filters.component.html',
  styleUrls: ['./active-filters.component.css'],
  standalone: true,
  imports: [NgClass, TitleCasePipe, DictionaryPipe],
})
export class ActiveFiltersComponent implements OnInit, OnDestroy {
  protected activeFilters: Map<string, string> = new Map<string, string>();
  private filterContextSubscription: Subscription = new Subscription();

  private filterService = inject(FilterService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.filterContextSubscription = this.filterService.filterContext$.subscribe({
      next: (context: Map<string, string>) => this.changeFilter(context),
      error: () => catchError(this.errorHandler.handleError()),
    });
  }

  ngOnDestroy(): void {
    this.filterContextSubscription.unsubscribe();
  }

  changeFilter(context: Map<string, string>): void {
    this.activeFilters = new Map<string, string>(context);
  }
}
