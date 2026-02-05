import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompareTreeComponent } from './compare-tree.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NgSimpleFileTree } from 'ng-simple-file-tree';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CompareTreeComponent', () => {
  let component: CompareTreeComponent;
  let fixture: ComponentFixture<CompareTreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NgSimpleFileTree, CompareTreeComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CompareTreeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
