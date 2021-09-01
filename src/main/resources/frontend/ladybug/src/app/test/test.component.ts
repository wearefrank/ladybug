import {Component, OnInit, EventEmitter, Output, Input} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-test',
  templateUrl: './test.component.html',
  styleUrls: ['./test.component.css']
})
export class TestComponent implements OnInit{
  reports: any[] = [];
  metadata: any = {};
  @Output() openTestReportEvent = new EventEmitter<any>();

  constructor(private modalService: NgbModal, private http: HttpClient) {}

  open(content: any) {
    this.modalService.open(content);
  }

  ngOnInit() {
    this.http.get<any>('/ladybug/metadata/testStorage').subscribe(data => {
      this.reports = data.values
    });
  }

  selectReport(storageId: number, name: string) {
    this.http.get<any>('/ladybug/report/debugStorage/' + storageId).subscribe(data => {
      this.openTestReportEvent.emit({data: data, name: name})
    })
  }



}
