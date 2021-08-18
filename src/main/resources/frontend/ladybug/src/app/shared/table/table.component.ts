import {Component, OnInit, Output, EventEmitter} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient, HttpHeaders} from '@angular/common/http';

const httpOptions = {
  headers: new HttpHeaders({
    'Access-Control-Allow-Origin': '*',
    'Authorization': 'authkey',
    'userid': '1'
  })
};

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css']
})
export class TableComponent implements OnInit {
  metadata: any = {};

  @Output()
  emitEvent = new EventEmitter<any>();

  constructor(private modalService: NgbModal, private http: HttpClient) {
  }

  openModal(content: any) {
    this.modalService.open(content);
  }

  openReport(storageId: string) {
    console.log("Opening " + storageId)
    this.http.get<any>('/ladybug/report/debugStorage/' + storageId).subscribe(data => {
      this.emitEvent.next(data);
    })
  }

  ngOnInit(): void {
    this.http.get<any>('/ladybug/metadata/debugStorage', httpOptions).subscribe(data => {
      this.metadata = data;
    });
  }
}
