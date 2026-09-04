import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DictionaryPipe } from '../../shared/pipes/dictionary.pipe';
import { NgClass, TitleCasePipe } from '@angular/common';
import { catchError, Subscription } from 'rxjs';
import { ErrorHandling } from '../../shared/classes/error-handling.service';
import { FilterService } from '../../shared/services/filter.service';
import { ShortenedTableHeaderPipe } from '../../shared/pipes/shortened-table-header.pipe';
import { MetadataFilter } from '../../shared/services/tab.service';

@Component({
  selector: 'app-active-filters',
  templateUrl: './active-filters.component.html',
  styleUrls: ['./active-filters.component.css'],
  standalone: true,
  imports: [NgClass, TitleCasePipe, ShortenedTableHeaderPipe, DictionaryPipe],
})
export class ActiveFiltersComponent implements OnInit, OnDestroy {
  private columnNameToLabel: Map<string, string> = new Map<string, string>();
  protected activeFilters: MetadataFilter[] = [];
  private subscriptions: Subscription = new Subscription();

  private filterService = inject(FilterService);
  private errorHandler = inject(ErrorHandling);

  ngOnInit(): void {
    this.subscriptions.add(
      this.filterService.userFilterColumns$.subscribe((columns) => {
        if (!columns) {
          return;
        }
        this.columnNameToLabel.clear();
        for (const column of columns) {
          this.columnNameToLabel.set(column.name, column.label);
        }
      }),
    );
    this.subscriptions.add(
      this.filterService.userFilters$.subscribe({
        next: (metadataFilters: MetadataFilter[]) => this.changeFilter(metadataFilters),
        error: () => catchError(this.errorHandler.handleError()),
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  changeFilter(metadataFilers: MetadataFilter[]): void {
    // Use the column name as key of this map, even though the column label is the base of what is shown.
    // We only know for sure that the column name is unique.
    this.activeFilters = [...metadataFilers];
  }

  protected safeColumnNameToLabel(columnName: string): string {
    return this.columnNameToLabel.get(columnName) ?? columnName;
  }

  protected exactPhrase(filter: MetadataFilter): string {
    return filter.exact ? '. Is exact match, column omitted from table' : '.';
  }
}
