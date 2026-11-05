import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { catchError, Subscription } from 'rxjs';
import { animate, style, transition, trigger } from '@angular/animations';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { Column, FilterService } from '../../shared/services/filter.service';
import { HttpService } from '../../shared/services/http.service';
import { ErrorHandling } from '../../shared/classes/error-handling.service';

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
  protected userFilterableColumns: Column[] = [];
  protected userFilterChoices: Map<string, string[]> = new Map<string, string[]>();
  protected toolTipSuggestions?: Map<string, string>;
  private subscriptions: Subscription = new Subscription();

  protected filterService = inject(FilterService);
  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.setSubscriptions();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private setSubscriptions(): void {
    const columnsSubscription: Subscription = this.filterService.userFilterColumns$.subscribe(
      (userFilterableColumns) => {
        if (userFilterableColumns) {
          this.userFilterableColumns = userFilterableColumns;
          this.toolTipSuggestions = new Map<string, string>();
          this.httpService
            .getUserHelp(
              // Call is safe. Subscription is only fired by FilterService and FilterService
              // will only do so when it has received the view.
              this.filterService.getCurrentView().storageName,
              userFilterableColumns.map((c) => c.name),
            )
            .pipe(catchError(this.errorHandler.handleError()))
            .subscribe({
              next: (response: Record<string, string>) => {
                this.toolTipSuggestions = new Map<string, string>(Object.entries(response));
              },
            });
        }
      },
    );
    this.subscriptions.add(columnsSubscription);
    const userFilterChoicesSubscription: Subscription = this.filterService.userFilterChoices$.subscribe((choices) => {
      if (choices) {
        this.userFilterChoices = choices;
      }
    });
    this.subscriptions.add(userFilterChoicesSubscription);
  }
}
