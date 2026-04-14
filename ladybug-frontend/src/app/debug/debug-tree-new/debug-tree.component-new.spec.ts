import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DebugTreeNewComponent } from './debug-tree-new.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NgSimpleFileTree } from 'ng-simple-file-tree';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { HierarchicalReport } from 'src/app/shared/interfaces/hierarchical-report';

describe('DebugTreeNewComponent', () => {
  let component: DebugTreeNewComponent;
  let fixture: ComponentFixture<DebugTreeNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NgSimpleFileTree, DebugTreeNewComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DebugTreeNewComponent);
    const reportSubject = new Subject<HierarchicalReport | null>();
    component = fixture.componentInstance;
    component.report$ = reportSubject as Observable<HierarchicalReport>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
