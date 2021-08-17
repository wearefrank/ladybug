import {Component, OnInit} from '@angular/core';
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
  constructor(private modalService: NgbModal, private http: HttpClient) {
  }

  open(content: any) {
    this.modalService.open(content);
  }

  ngOnInit(): void {
    this.http.get<string>('/ladybug/metadata/debugStorage', httpOptions).subscribe(data => {
      console.log(data);
    })
  }
}
