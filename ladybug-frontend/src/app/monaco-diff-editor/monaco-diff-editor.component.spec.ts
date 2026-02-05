import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiffEditorModel, MonacoDiffEditor } from './monaco-diff-editor.component';
import { RouterTestingModule } from '@angular/router/testing';
import { Subject } from 'rxjs';

describe('MonacoDiffEditor', () => {
  let component: MonacoDiffEditor;
  let fixture: ComponentFixture<MonacoDiffEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MonacoDiffEditor, RouterTestingModule],
      providers: [],
    }).compileComponents();

    fixture = TestBed.createComponent(MonacoDiffEditor);
    component = fixture.componentInstance;
    component.originalModelRequest$ = new Subject<DiffEditorModel>();
    component.modifiedModelRequest$ = new Subject<DiffEditorModel>();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
