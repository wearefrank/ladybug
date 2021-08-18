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
  @Output() emitEvent = new EventEmitter<any>();
  filter: boolean = false;
  metadata: any = {}; // The data that is displayed
  isLoaded: boolean = false; // Wait for the page to be loaded
  displayAmount: number = 10; // The amount of data that is displayed

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

  loadTable() {
    this.http.get<any>('/ladybug/metadata/debugStorage', httpOptions).subscribe(data => {
      this.metadata = data;
      this.isLoaded = true;
    });
  }

  openAll() {
    for (let row of this.metadata.values) {
      this.openReport(row[5]);
    }
  }

  ngOnInit(): void {
    this.loadTable();
  }
}
