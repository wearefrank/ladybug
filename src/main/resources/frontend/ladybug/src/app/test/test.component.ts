import {Component, OnInit, EventEmitter, Output, Input, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {ToastComponent} from "../shared/components/toast/toast.component";
import {catchError} from "rxjs/operators";
import {throwError} from "rxjs";

@Component({
  selector: 'app-test',
  templateUrl: './test.component.html',
  styleUrls: ['./test.component.css']
})
export class TestComponent implements OnInit{
  reports: any[] = [];
  metadata: any = {};
  @Output() openTestReportEvent = new EventEmitter<any>();
  @ViewChild(ToastComponent) toastComponent!: ToastComponent;

  constructor(private modalService: NgbModal, private http: HttpClient) {}

  open(content: any) {
    this.modalService.open(content);
  }

  ngOnInit() {
    this.loadData();
  }

  /**
   * Load in the report data from testStorage
   */
  loadData() {
    this.http.get<any>('/ladybug/metadata/testStorage').subscribe(data => {
      this.reports = data.values
      this.checkAll();
    }, () => {
      this.toastComponent.addAlert({type: 'danger', message: 'Could not retrieve data for testing!'})
    });
  }

  /**
   * Run a test
   */
  run() {
    let data: string = "Hello World";
    this.http.post<any>('ladybug/runner/run/debugStorage', data).pipe(catchError(error => {
      this.toastComponent.addAlert({type: 'danger', message: 'Url could not be found!'});
      console.log(error)
      return throwError(error);
    }))
      .subscribe(response => {
        console.log('response')
        console.log(response)
      })
  }

  /**
   * Rerun a test
   */
  rerun() {
    this.loadData();
    this.run();
  }

  /**
   * Selects the report to be displayed
   * @param storageId - the storageId of the report
   * @param name - the name of the report
   */
  selectReport(storageId: number, name: string) {
    this.http.get<any>('/ladybug/report/debugStorage/' + storageId).subscribe(data => {
      this.openTestReportEvent.emit({data: data, name: name})
    }, () => {
      this.toastComponent.addAlert({type: 'warning', message: 'Could not retrieve data for report!'})
    })
  }

  /**
   * Removes the selected reports
   */
  deleteSelected() {
    this.reports = this.reports.filter(report => !report.checked)
  }

  /**
   * Toggle the checkbox
   * @param report - the report that is toggled
   */
  toggleCheck(report: any) {
    let index = this.reports.indexOf(report);
    this.reports[index].checked = !report.checked
  }

  /**
   * Checks all checkboxes
   */
  checkAll() {
    for (let report of this.reports) {
      report.checked = true;
    }
  }

  /**
   * Unchecks all checkboxes
   */
  uncheckAll() {
    for (let report of this.reports) {
      report.checked = false;
    }
  }
}
