import { Component, OnInit } from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-test',
  templateUrl: './test.component.html',
  styleUrls: ['./test.component.css']
})
export class TestComponent implements OnInit {
  constructor(private modalService: NgbModal) {}

  open(content: any) {
    this.modalService.open(content);
  }

  ngOnInit(): void {
  }

}
