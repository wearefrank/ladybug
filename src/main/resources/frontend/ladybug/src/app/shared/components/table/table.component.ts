import {Component, OnInit, Output, EventEmitter, Input, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient} from '@angular/common/http';
import {Sort} from "@angular/material/sort";
import {ToastComponent} from "../toast/toast.component";

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
  @ViewChild(ToastComponent) toastComponent!: ToastComponent;
  @Input() // Needed to make a distinction between the two halves in compare component
  get id() { return this._id}
  set id(id: string) {this._id = id}
  private _id: string = "";

  constructor(private modalService: NgbModal, private http: HttpClient) {}

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

  /**
   * Sort the data accordingly
   * @param sort - sort object to handle sorting
   */
  sortData(sort: Sort) {
    const data = this.metadata.values
    if (!sort.active || sort.direction === '') {
      this.sortedData = data;
      return;
    }
    this.sortedData = data.sort((a: (string | number)[], b: (string | number)[]) => {

      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case '0': return this.compare(Number(a[0]), Number(b[0]), isAsc); // Duration
        case '1': return this.compare(Number(a[1]), Number(b[1]), isAsc); // StorageSize
        case '2': return this.compare(a[2], b[2], isAsc);                 // Name
        case '3': return this.compare(a[3], b[3], isAsc);                 // CorrelationId
        case '4': return this.compare(a[4], b[4], isAsc);                 // EndTime
        case '5': return this.compare(Number(a[5]), Number(b[5]), isAsc); // StorageId
        case '6': return this.compare(a[6], b[6], isAsc);                 // Status
        case '7': return this.compare(Number(a[7]), Number(b[7]), isAsc); // NumberOfCheckpoints
        case '8': return this.compare(Number(a[8]), Number(b[8]), isAsc); // EstimatedMemoryUsage
        default: return 0;
      }
    });
  }

  /**
   * Compare two strings or numbers
   * @param a - first string/number
   * @param b - second string/number
   * @param isAsc - whether it is ascending or not
   */
  compare(a: number | string, b: number | string, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
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
    }, () => {
      this.toastComponent.addAlert({type: 'warning', message: 'Could not retrieve data for report!'})
    })
  }

  /**
   * Open all reports
   */
  openReports(amount: number) {
    if (amount === -1) {
      amount = this.metadata.values.length;
    }

    // The index 5 is the storageId
    for (let row of this.metadata.values.slice(0, amount)) {
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
    }, () => {
      this.toastComponent.addAlert({type: 'danger', message: 'Could not retrieve data for table!'})
    });
  }
}
