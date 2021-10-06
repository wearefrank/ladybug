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

  /**
   * Open an extra tab for the test report
   * @param data - the data in the report
   */
  openTestReport(data: any) {
    this.injector = Injector.create({providers: [{provide: ReportData, useValue: data.data}], parent: this.inj})
    this.tabs.push( {key: data.name, value: ReportComponent})
    this.active = this.tabs.length + 3; // Active the tab immediately
  }

  /**
   * Close the extra ta for the test report
   * @param event - mouse event
   * @param toRemove - the index of the report
   */
  closeTestReport(event: MouseEvent, toRemove: number) {
    this.tabs.splice(toRemove, 1);
    this.active--;
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}
