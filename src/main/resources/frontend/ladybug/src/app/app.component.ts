import {Component, OnInit} from '@angular/core';
import {DebugComponent} from "./debug/debug.component";
import {TestComponent} from "./test/test.component";
import {CompareComponent} from "./compare/compare.component";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent {
  title = 'ladybug';
  active = 1;
  tabs: {key: string, value: any}[] = [
    {key: 'Debug', value: DebugComponent},
    {key: 'Test', value: TestComponent},
    {key: 'Compare', value: CompareComponent}
  ]
}
