import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DebugTreeNewComponent } from './debug-tree-new.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NgSimpleFileTree } from 'ng-simple-file-tree';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

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
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
