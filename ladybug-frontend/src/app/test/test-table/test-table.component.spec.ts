import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TestTableComponent } from './test-table.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('TestTableBodyComponent', () => {
  let component: TestTableComponent;
  let fixture: ComponentFixture<TestTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestTableComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestTableComponent);
    component = fixture.componentInstance;
    component.reports = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
