import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {NgbAlert} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent implements OnInit {
  alerts: any[] = [
    // {type: 'warning', message: 'There is some error wow!'},
    // {type: 'danger', message: 'There is a big error wow!'},
    // {type: 'success', message: 'There is no error wow!'}
    ]

  @ViewChild('staticAlert', {static: false}) staticAlert!: NgbAlert;

  constructor() { }

  ngOnInit(): void {
    setTimeout(() => {
      if (this.staticAlert) {
        this.staticAlert.close();
        this.alerts = [];
      }
    }, 5000)
  }

  close(alert: any) {
    this.alerts.splice(this.alerts.indexOf(alert), 1)
    this.ngOnInit();
  }

  addAlert(alert: any) {
    this.alerts.push(alert)
    this.ngOnInit();
  }
}
