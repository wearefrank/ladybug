import { FilterFromUrl } from '../../shared/services/tab.service';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-fixed-filter-info',
  templateUrl: './fixed-filter-info.component.html',
  styleUrls: ['./fixed-filter-info.component.css'],
  standalone: true,
})
export class FixedFilterInfoComponent {
  @Input({ required: true }) titleWhenFilters!: string;
  @Input({ required: true }) titleNoFilters!: string;
  @Input({ required: true }) filterKind!: string;
  @Input() filters: FilterFromUrl[] = [];
}
