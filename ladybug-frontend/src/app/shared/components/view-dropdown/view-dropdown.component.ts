import { Component, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { View } from '../../interfaces/view';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-view-dropdown',
  standalone: true,
  templateUrl: './view-dropdown.component.html',
  styleUrl: './view-dropdown.component.css',
})
export class ViewDropdownComponent implements OnChanges {
  @Output() viewChanged = new Subject<View>();
  @Input({ required: true }) views!: View[];
  @Input({ required: true }) currentView!: View;

  viewDropdownBoxWidth!: string;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['views']) {
      this.calculateViewDropDownWidth();
    }
  }

  changeView(index: number): void {
    this.viewChanged.next(this.views[index]);
  }

  calculateViewDropDownWidth(): void {
    if (this.views) {
      let longestViewName = '';
      for (const view of this.views) {
        if (view.name.length > longestViewName.length) {
          longestViewName = view.name;
        }
      }
      this.viewDropdownBoxWidth = `${longestViewName.length / 2}rem`;
    }
  }
}
