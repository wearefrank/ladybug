import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {MonacoEditorComponent} from "../../monaco-editor/monaco-editor.component";
import {HttpClient} from "@angular/common/http";
// @ts-ignore
import beautify from "xml-beautifier";

@Component({
  selector: 'app-display',
  templateUrl: './display.component.html',
  styleUrls: ['./display.component.css']
})
export class DisplayComponent {
  @Input() editing: boolean = false
  @Input() displayReport: boolean = false
  @Input() report: any = {};
  @Output() emitEvent = new EventEmitter<any>();
  @ViewChild(MonacoEditorComponent) monacoEditorComponent!: MonacoEditorComponent;
  stubStrategies: string[] = ["Follow report strategy", "No", "Yes"];

  constructor(private modalService: NgbModal, private http: HttpClient) {
  }

  /**
   * Open a modal
   * @param content - the specific modal to be opened
   */
  openModal(content: any) {
    this.modalService.open(content);
  }

  showReport(report: any) {
    this.report = report;

    // This is for the root which has a specific location for the xml message
    if (report.ladybug.storageId) {
      this.http.get<any>('/ladybug/report/debugStorage/' + this.report.ladybug.storageId + "/?xml=true&globalTransformer=true").subscribe(data => {
        this.monacoEditorComponent?.loadMonaco(beautify(data.xml));
      })
    } else {
      this.monacoEditorComponent?.loadMonaco(beautify(report.ladybug.message));
    }
    this.displayReport = true;
  }

  closeReport() {
    this.emitEvent.next(this.report)
    this.displayReport = false;
    this.report = {};
  }

  editReport() {
    this.editing = true;
    this.monacoEditorComponent.toggleEdit();
  }

  saveChanges() {
    this.editing = false;
    this.modalService.dismissAll();
    this.monacoEditorComponent.toggleEdit();
    console.log("Successfully saved changes!")
  }

  discardChanges() {
    this.editing = false;
    this.modalService.dismissAll();
    this.monacoEditorComponent.toggleEdit();
    console.log("Successfully discarded changes!")
  }
}
