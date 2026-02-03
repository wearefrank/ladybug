import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MonacoEditorComponent } from './monaco-editor.component';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject } from 'rxjs';

describe('MonacoEditorComponent', () => {
  let component: MonacoEditorComponent;
  let fixture: ComponentFixture<MonacoEditorComponent>;
  let editorContentRequestSubject = new BehaviorSubject<string | undefined>(undefined);
  let editorReadOnlyRequestSubject = new BehaviorSubject<boolean>(true);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MonacoEditorComponent, RouterTestingModule],
      providers: [],
    }).compileComponents();

    fixture = TestBed.createComponent(MonacoEditorComponent);
    component = fixture.componentInstance;
    component.editorContentRequest$ = editorContentRequestSubject.asObservable();
    component.readOnlyRequest$ = editorReadOnlyRequestSubject.asObservable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
