// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="../../../node_modules/monaco-editor/monaco.d.ts" />
import { AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { first, Observable, ReplaySubject, Subscription } from 'rxjs';

export interface DiffEditorModel {
  language: string;
  code: string;
}

// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
type AMDRequire = {
  require: {
    // eslint-disable-next-line no-unused-vars
    (imports: string[], callback: () => void): void;
    // eslint-disable-next-line no-unused-vars
    config(config: { paths: Record<string, string> }): void;
  };
};

@Component({
  selector: 'app-monaco-diff-editor',
  imports: [],
  templateUrl: './monaco-diff-editor.component.html',
  styleUrl: './monaco-diff-editor.component.css',
})
export class MonacoDiffEditor implements OnInit, AfterViewInit, OnDestroy {
  @Input() options?: Partial<monaco.editor.IStandaloneDiffEditorConstructionOptions>;
  @Input() originalModelRequest$?: Observable<DiffEditorModel>;
  @Input() modifiedModelRequest$?: Observable<DiffEditorModel>;

  @ViewChild('diffEditor')
  protected diffEditorContainer!: ElementRef;

  diffEditor$: Observable<monaco.editor.IStandaloneDiffEditor>;
  private diffEditorSubject = new ReplaySubject<monaco.editor.IStandaloneDiffEditor>(1);
  private diffEditor?: monaco.editor.IStandaloneDiffEditor;

  private originalModelSubscription?: Subscription;
  private modifiedModelSubscription?: Subscription;
  private requestedOriginal?: DiffEditorModel;
  private requestedModified?: DiffEditorModel;

  constructor() {
    this.diffEditor$ = this.diffEditorSubject.asObservable();
  }

  ngOnInit(): void {
    if (!this.originalModelRequest$) {
      throw new Error('Please provide input originalModelRequest$');
    }
    if (!this.modifiedModelRequest$) {
      throw new Error('Please provide input modifiedModelRequest$');
    }
    this.originalModelSubscription = this.originalModelRequest$!.subscribe((value) => this.showOriginal(value));
    this.modifiedModelSubscription = this.modifiedModelRequest$!.subscribe((value) => this.showModified(value));
  }

  ngOnDestroy(): void {
    this.originalModelSubscription?.unsubscribe();
    this.modifiedModelSubscription?.unsubscribe();
  }

  ngAfterViewInit(): void {
    this.loadMonaco();
  }

  showOriginal(value: DiffEditorModel): void {
    this.requestedOriginal = value;
    this.show();
  }

  showModified(value: DiffEditorModel): void {
    this.requestedModified = value;
    this.show();
  }

  show(): void {
    new Promise<void>((resolve) => {
      this.diffEditor$.pipe(first()).subscribe((diffEditor) => {
        this.showOnDiffEditor(diffEditor);
        resolve();
      });
    });
  }

  private showOnDiffEditor(diffEditor: monaco.editor.IStandaloneDiffEditor): void {
    if (!this.hasValuesToShow()) {
      throw new Error('Values to show have not been initialized');
    }
    console.log(
      `Going to show ${this.requestedOriginal?.code.slice(0, 10)}... and ${this.requestedModified?.code.slice(0, 10)}...`,
    );
    let original = monaco.editor.createModel(this.requestedOriginal!.code, this.requestedOriginal!.language);
    let modified = monaco.editor.createModel(this.requestedModified!.code, this.requestedModified!.language);
    diffEditor.setModel({ original, modified });
  }

  private hasValuesToShow(): boolean {
    return this.requestedOriginal !== undefined && this.requestedModified !== undefined;
  }

  private loadMonaco(): void {
    if (typeof globalThis.monaco === 'object') {
      this.initializeMonaco();
      return;
    }

    if ((globalThis as unknown as AMDRequire).require) {
      this.onAmdLoader();
    } else {
      const loaderScript: HTMLScriptElement = document.createElement('script');
      loaderScript.type = 'text/javascript';
      loaderScript.src = 'assets/monaco/vs/loader.js';
      loaderScript.addEventListener('load', () => this.onAmdLoader());
      document.body.append(loaderScript);
    }
  }

  private onAmdLoader(): void {
    const windowRequire = (globalThis as unknown as AMDRequire).require;
    windowRequire.config({ paths: { vs: 'assets/monaco/vs' } });
    windowRequire(['vs/editor/editor.main'], () => {
      this.initializeMonaco();
    });
  }

  private initializeMonaco(): void {
    this.diffEditor = monaco.editor.createDiffEditor(this.diffEditorContainer.nativeElement, this.options);
    this.diffEditorSubject.next(this.diffEditor);
  }

  /*
  private initializeEvents(): void {
    this.diffEditor$.pipe(first()).subscribe((editor) => {
      editor.onDidChangeModelContent(() => {
        this.actualEditorContentsChange.emit(editor.getValue());
      });
      editor.onDidFocusEditorWidget((): void => {
        this.focusedChange.emit(true);
      });
      editor.onDidBlurEditorWidget((): void => {
        this.focusedChange.emit(false);
      });
    });
  }

  private initializeActions(): void {
    const actionKeyBindings: Record<string, number[]> = {
      ctrlEnter: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
    };
    this.diffEditor$.pipe(first()).subscribe((editor) => {
      for (const [action, descriptor] of Object.entries(this.actions || {})) {
        editor.addAction({
          id: descriptor.id,
          label: descriptor.label,
          keybindings: actionKeyBindings[action],
          run: descriptor.run,
        });
      }
    });
  }

  private initializeMouseEvents(): void {
    this.editor?.onMouseDown((event) => {
      const element = event.target.element;
      switch (element?.className) {
        case 'line-numbers': {
          this.handleLineNumberClick(+(element.textContent || 0));
          break;
        }
        default: {
          return;
        }
      }
    });
  }

  private handleLineNumberClick(lineNumber: number): void {
    if (lineNumber) {
      this.highlightLine(lineNumber);
      this.setPosition(lineNumber);
    }
  }

  private highlightLine(lineNumber: number): void {
    this.highlightRange({
      startLineNumber: lineNumber,
      startColumn: 0,
      endLineNumber: lineNumber,
      endColumn: 0,
    });
  }

  private highlightRange(range: monaco.IRange): void {
    this.diffEditor$.pipe(first()).subscribe((editor) => {
      this.decorationsDelta =
        editor.getModel()?.deltaDecorations(this.decorationsDelta, [
          {
            range: range,
            options: {
              isWholeLine: true,
              overviewRuler: {
                position: monaco.editor.OverviewRulerLane.Full,
                color: '#fdc300',
              },
              className: 'monaco-editor__line--highlighted',
            },
          },
        ]) ?? [];
    });
  }

  private setPosition(lineNumber: number, column = 0): void {
    this.diffEditor$.pipe(first()).subscribe((editor) => {
      editor.setPosition({ lineNumber: lineNumber, column: column });
    });
  }
  */
}
