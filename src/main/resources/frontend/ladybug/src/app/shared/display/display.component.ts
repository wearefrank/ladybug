import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {MonacoEditorComponent} from "../monaco-editor/monaco-editor.component";

@Component({
  selector: 'app-display',
  templateUrl: './display.component.html',
  styleUrls: ['./display.component.css']
})
export class DisplayComponent implements OnInit {
  @Input() editing: boolean = false
  @Input() displayReport: boolean = false
  @Input() report: any = {};
  @ViewChild(MonacoEditorComponent) monacoEditorComponent!: MonacoEditorComponent;
  stubStrategies: string[] = ["Follow report strategy", "No", "Yes"];

  constructor(private modalService: NgbModal) {
  }

  /**
   * Open a modal
   * @param content - the specific modal to be opened
   */
  openModal(content: any) {
    this.modalService.open(content);
  }

  showReport() {
    console.log("Showing report")
    this.displayReport = true;
    if (this.monacoEditorComponent) {
      this.monacoEditorComponent.loadMonaco();
    }
  }

  ngOnInit(): void {
  }

}
