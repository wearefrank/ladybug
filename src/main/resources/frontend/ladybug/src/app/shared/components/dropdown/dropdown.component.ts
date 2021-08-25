import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-dropdown',
  templateUrl: './dropdown.component.html',
  styleUrls: ['./dropdown.component.css']
})
export class DropdownComponent implements OnInit {
  @Input()
  get items(): string[]{return this._items}
  set items(items: string[]) { this._items = items; }
  private _items: string[] = []

  @Input()
  get title(): string { return this._title }
  set title(title: string) { this._title = title; }
  private _title = "";

  @Input()
  get icon(): string {return this._icon}
  set icon(icon: string){ this._icon = icon; }
  private _icon = "";

  constructor() { }

  ngOnInit(): void {
  }

}
