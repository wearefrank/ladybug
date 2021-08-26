import {Component, OnInit, Output, EventEmitter, Input} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient} from '@angular/common/http';
import {Sort} from "@angular/material/sort";

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
  sortAscending: boolean = true;
  sortedData: any = {};
  @Input() // Needed to make a distinction between the two halves in compare component
  get id() { return this._id}
  set id(id: string) {this._id = id}
  private _id: string = "";


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
   * Change the limit of items shown in table
   * @param event - the new table limit
   */
  changeTableLimit(event: any) {
    this.displayAmount = event.target.value;
  }

  sortData(sort: Sort) {
    const data = this.metadata.values
    if (!sort.active || sort.direction === '') {
      this.sortedData = data;
      return;
    }
    this.sortedData = data.sort((a: (string | number)[], b: (string | number)[]) => {

      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case '0': return this.compare(a[0], b[0], isAsc);
        case '1': return this.compare(a[1], b[1], isAsc);
        case '2': return this.compare(a[2], b[2], isAsc);
        case '3': return this.compare(a[3], b[3], isAsc);
        case '4': return this.compare(a[4], b[4], isAsc);
        case '5': return this.compare(a[5], b[5], isAsc);
        case '6': return this.compare(a[6], b[6], isAsc);
        case '7': return this.compare(a[7], b[7], isAsc);
        case '8': return this.compare(a[8], b[8], isAsc);
        default: return 0;
      }
    });

  }

  compare(a: number | string, b: number | string, isAsc: boolean) {
    let result = (a < b ? -1 : 1) * (isAsc ? 1 : -1);
    console.log(a + ":" + b + "=" + result)
    return result;
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
      data.id = this.id
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

  /**
   * Load in data for the table
   */
  ngOnInit(): void {
    this.http.get<any>('/ladybug/metadata/debugStorage').subscribe(data => {
      this.metadata = data
      this.isLoaded = true;
    });
  }
}
