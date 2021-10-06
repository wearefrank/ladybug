import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {MonacoEditorComponent} from "../../monaco-editor/monaco-editor.component";
import {HttpClient} from "@angular/common/http";
// @ts-ignore
import DiffMatchPatch from 'diff-match-patch';
// @ts-ignore
import beautify from "xml-beautifier";
import {ToastComponent} from "../toast/toast.component";
import {catchError} from "rxjs/operators";
import {throwError} from "rxjs"; // TODO: Check if there is a nicer way to do this

@Component({
  selector: 'app-display',
  templateUrl: './display.component.html',
  styleUrls: ['./display.component.css']
})
export class DisplayComponent {
  @Input() editing: boolean = false
  @Input() editingRoot: boolean = false;
  @Input() displayReport: boolean = false
  @Input() report: any = {};
  @Output() closeReportEvent = new EventEmitter<any>();
  @ViewChild(MonacoEditorComponent) monacoEditorComponent!: MonacoEditorComponent;
  @ViewChild(ToastComponent) toastComponent!: ToastComponent;
  @ViewChild('name') name!: ElementRef;
  @ViewChild('description') description!: ElementRef;
  @ViewChild('path') path!: ElementRef;
  @ViewChild('transformation') transformation!: ElementRef;
  monacoBefore: string = '';
  difference: {code: [], name: string, description: string, path: string, transformation: string} = {code: [], name: '', description: '', path: '', transformation: ''};
  stubStrategies: string[] = ["Follow report strategy", "No", "Yes"];
  type: string = '';

  constructor(private modalService: NgbModal, private http: HttpClient) {}

  /**
   * Open a modal
   * @param content - the specific modal to be opened
   * @param type
   */
  openModal(content: any, type: string) {
    console.log(type)
    this.type = type;
    let dmp = new DiffMatchPatch();
    if (!this.report.root) {
      this.monacoBefore = this.report.ladybug.message;
      let monacoAfter = this.monacoEditorComponent?.getValue();
      this.difference.code = dmp.diff_main(this.monacoBefore, monacoAfter);
    } else {
      let beforeName = this.report.ladybug.name === null ? '' : this.report.ladybug.name;
      let beforeDescription = this.report.ladybug.description === null ? '' : this.report.ladybug.description;
      let beforePath = this.report.ladybug.path === null ? '' : this.report.ladybug.path;
      let beforeTransformation = this.report.ladybug.transformation === null ? '' : this.report.ladybug.transformation;

      this.difference.name = dmp.diff_main(beforeName, this.name.nativeElement.value)
      this.difference.description = dmp.diff_main(beforeDescription, this.description.nativeElement.value)
      this.difference.path = dmp.diff_main(beforePath, this.path.nativeElement.value)
      this.difference.transformation = dmp.diff_main(beforeTransformation, this.transformation.nativeElement.value)
    }
    content.type = type;
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
        this.report.ladybug.message = data.xml;
        this.monacoEditorComponent?.loadMonaco(beautify(data.xml)); // TODO: Maybe create a service for this
      }, () => {
        this.toastComponent.addAlert({type: 'warning', message: 'Could not retrieve data for report!'})
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
    if (this.report.root) {
      this.editingRoot = true;
    } else {
      this.toggleMonacoEditor();
    }
  }

  /**
   * Save or discard report changes
   * @param type
   */
  saveOrDiscard(type: string) {
    this.editing = false;
    this.editingRoot = false;
    this.modalService.dismissAll();
    this.toggleMonacoEditor();
    console.log("Successfully " + type + " changes!")
  }

  toggleMonacoEditor() {
    if (!this.report.root) {
      this.monacoEditorComponent.toggleEdit();
    }
  }
}
