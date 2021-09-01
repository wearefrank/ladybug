import {Component} from '@angular/core';
import {ReportComponent} from "./report/report.component";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent {

  constructor() {}
  title = 'ladybug';
  active = 1;
  tabs: {key: string, value: any}[] = []

  openTestReport(data: any) {
    console.log(data.data)
    this.tabs.push( {key: data.name, value: ReportComponent})
    this.active = this.tabs.length + 3; // Active the tab immediately
  }

  closeTestReport(event: MouseEvent, toRemove: number) {
    this.tabs.splice(toRemove, 1);
    this.active--;
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}
