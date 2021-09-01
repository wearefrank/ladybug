import {Component, Injector} from '@angular/core';
import {ReportComponent, ReportData} from "./report/report.component";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent {
  injector!: Injector;

  constructor(private inj: Injector) {}

  title = 'ladybug';
  active = 1;
  tabs: {key: string, value: any}[] = []

  openTestReport(data: any) {
    this.injector = Injector.create({providers: [{provide: ReportData, useValue: data.data}], parent: this.inj})
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
