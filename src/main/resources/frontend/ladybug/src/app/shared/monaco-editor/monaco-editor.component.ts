/// <reference path="../../../../node_modules/monaco-editor/monaco.d.ts" />
import {AfterViewInit, Component, ElementRef, Input, ViewChild,} from '@angular/core';
import EditorOption = monaco.editor.EditorOption;

let loadedMonaco = false;
let loadPromise: Promise<void>;

@Component({
  selector: 'app-monaco-editor',
  templateUrl: './monaco-editor.component.html',
  styleUrls: ['./monaco-editor.component.css'],
})
export class MonacoEditorComponent implements AfterViewInit {
  @ViewChild('container') editorContainer!: ElementRef;
  codeEditorInstance!: monaco.editor.IStandaloneCodeEditor;
  readonly: boolean = true
  @Input()
  get value(): string {return this._value}
  set value(value: string){ this._value = value; }
  private _value = "";

  constructor() {
  }

  ngAfterViewInit(): void {
    this.loadMonaco();
  }

  loadMonaco(): void {
    if (loadedMonaco) {
      loadPromise.then(() => {
        this.initializeEditor();
      });
    } else {
      loadedMonaco = true;
      loadPromise = new Promise<void>((resolve: any) => {
        if (typeof (window as any).monaco === 'object') {
          resolve();
          return;
        }

        const onAmdLoader: any = () => {
          (window as any).require.config({paths: {vs: 'assets/monaco/vs'}});
          (window as any).require(['vs/editor/editor.main'], () => {
            this.initializeEditor();
            resolve();
          });
        };

        if (!(window as any).require) {
          const loaderScript: HTMLScriptElement = document.createElement(
            'script'
          );
          loaderScript.type = 'text/javascript';
          loaderScript.src = 'assets/monaco/vs/loader.js';
          loaderScript.addEventListener('load', onAmdLoader);
          document.body.appendChild(loaderScript);
        } else {
          onAmdLoader();
        }
      });
    }
  }

  initializeEditor(): void {
    this.codeEditorInstance = monaco.editor.create(
      this.editorContainer.nativeElement,
      {
        value: this._value,
        readOnly: this.readonly,
        language: 'typescript',
        theme: 'vs-light',
      }
    );
  }

  toggleEdit() {
    this.readonly = !this.readonly
    this.codeEditorInstance.updateOptions( {
      readOnly: this.readonly
    })
  }
}
