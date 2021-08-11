import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-button',
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.css']
})
export class ButtonComponent implements OnInit {
  @Input()
  get icon(): string {return this._icon}
  set icon(icon: string){ this._icon = icon; }
  private _icon = "";

  @Input()
  get title(): string { return this._title }
  set title(title: string) { this._title = title; }
  private _title = "";

  @Input()
  get text(): string { return this._text }
  set text(text: string) { this._text = text; }
  private _text = "";

  @Input()
  get script(): any { return this._script }
  set script(script: any) { this._script = script; }
  private _script = "";

  ngOnInit(): void {
  }

}
