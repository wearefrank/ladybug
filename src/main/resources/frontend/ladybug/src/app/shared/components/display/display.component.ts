import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {MonacoEditorComponent} from "../../monaco-editor/monaco-editor.component";
import {HttpClient} from "@angular/common/http";
// @ts-ignore
import beautify from "xml-beautifier"; // TODO: Check if there is a nicer way to do this

@Component({
  selector: 'app-display',
  templateUrl: './display.component.html',
  styleUrls: ['./display.component.css']
})
export class DisplayComponent {
  @Input() editing: boolean = false
  @Input() displayReport: boolean = false
  @Input() report: any = {};
  @Output() closeReportEvent = new EventEmitter<any>();
  @ViewChild(MonacoEditorComponent) monacoEditorComponent!: MonacoEditorComponent;
  stubStrategies: string[] = ["Follow report strategy", "No", "Yes"];

  constructor(private modalService: NgbModal, private http: HttpClient) {}

  /**
   * Open a modal
   * @param content - the specific modal to be opened
   */
  openModal(content: any) {
    this.modalService.open(content);
  }

  /**
   * Show a report in the display
   * @param report - the report to be sown
   */
  showReport(report: any) {
    this.report = report;

    // This is for the root report which has a specific location for the xml message
    if (this.report.ladybug.storageId) {
      this.http.get<any>('/ladybug/report/debugStorage/' + this.report.ladybug.storageId + "/?xml=true&globalTransformer=true").subscribe(data => {
        this.monacoEditorComponent?.loadMonaco(beautify(data.xml)); // TODO: Maybe create a service for this
      })
    } else {
      // All other reports have the message stored normally
      this.monacoEditorComponent?.loadMonaco(beautify(this.report.ladybug.message));
    }
    this.displayReport = true;
  }

  /**
   * Close a report
   */
  closeReport() {
    this.closeReportEvent.next(this.report)
    this.displayReport = false;
    this.report = {};
  }

  /**
   * Start editing a report
   */
  editReport() {
    this.editing = true;
    this.monacoEditorComponent.toggleEdit();
  }

  /**
   * Save the changes made during edit
   */
  saveChanges() {
    this.editing = false;
    this.modalService.dismissAll();
    this.monacoEditorComponent.toggleEdit();
    console.log("Successfully saved changes!")
  }

  /**
   * Discard the changes made during edit
   */
  discardChanges() {
    this.editing = false;
    this.modalService.dismissAll();
    this.monacoEditorComponent.toggleEdit();
    console.log("Successfully discarded changes!")
  }
}
