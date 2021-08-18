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
  filter: boolean = false;

  @Output()
  emitEvent = new EventEmitter<any>();

  constructor(private modalService: NgbModal, private http: HttpClient) {
  }

  openModal(content: any) {
    this.modalService.open(content);
  }

  toggleFilter() {
    this.filter = !this.filter;
  }

  /*
    Request the data based on storageId and send this data along to the tree (via parent)
   */
  openReport(storageId: string) {
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
