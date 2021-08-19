import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-display',
  templateUrl: './display.component.html',
  styleUrls: ['./display.component.css']
})
export class DisplayComponent implements OnInit {
  @Input() editing: boolean = false
  @Input() displayReport: boolean = false
  @Input() report: any = {};
  stubStrategies: string[] = ["Follow report strategy", "No", "Yes"];

  constructor() {
  }

  showReport() {
    console.log("Showing report")
    this.displayReport = true;
  }

  ngOnInit(): void {
  }

}
