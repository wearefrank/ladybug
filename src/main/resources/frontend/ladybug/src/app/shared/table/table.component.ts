import {Component, OnInit, ViewChild} from '@angular/core';
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-table',
  templateUrl: './table.component.html',
  styleUrls: ['./table.component.css']
})
export class TableComponent implements OnInit {
  constructor(private modalService: NgbModal) {}

  open(content: any) {
    this.modalService.open(content);
  }

  ngOnInit(): void {
  }

}
