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
  showFilter: boolean = false;
  metadata: any = {}; // The data that is displayed
  isLoaded: boolean = false; // Wait for the page to be loaded
  displayAmount: number = 26; // The amount of data that is displayed
  filterValue: string = ""; // Value on what table should filter

  constructor(private modalService: NgbModal, private http: HttpClient) {
  }

  /**
   * Open a modal
   * @param content - the specific modal to be opened
   */
  openModal(content: any) {
    this.modalService.open(content);
  }

  /**
   * Change the value on what the table should filter
   * @param event - the keyword
   */
  changeFilter(event: any) {
    this.filterValue = event.target.value;
  }

  /**
   * Refresh the table
   */
  refresh() {
    this.showFilter = false;
    this.metadata = {};
    this.isLoaded = false;
    this.displayAmount = 10;
    this.ngOnInit();
  }

  /**
   * Toggle the filter option
   */
  toggleFilter() {
    this.showFilter = !this.showFilter;
  }

  /**
    Request the data based on storageId and send this data along to the tree (via parent)
   */
  openReport(storageId: string) {
    this.http.get<any>('/ladybug/report/debugStorage/' + storageId).subscribe(data => {
      this.emitEvent.next(data);
    })
  }

  /**
   * Open all reports
   */
  openAll() {
    for (let row of this.metadata.values) {
      this.openReport(row[5]);
    }
  }

  ngOnInit(): void {
    // Load in data needed for the table
    this.http.get<any>('/ladybug/metadata/debugStorage', httpOptions).subscribe(data => {
      this.metadata = data;
      this.isLoaded = true;
    });
  }
}
