import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportButtons, ReportButtonsState } from './report-buttons';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { TestResult } from 'src/app/shared/interfaces/test-result';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppVariablesService } from '../../shared/services/app.variables.service';

describe('ReportButtons', () => {
  let component: ReportButtons;
  let fixture: ComponentFixture<ReportButtons>;
  let originalReportStubStrategySubject = new Subject<string>();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [AppVariablesService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
      imports: [ReportButtons],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportButtons);
    component = fixture.componentInstance;
    component.state$ = new BehaviorSubject<ReportButtonsState>({
      isReport: false,
      isCheckpoint: false,
      saveAllowed: false,
    });
    component.originalReportStubStrategy$ = originalReportStubStrategySubject;
    component.rerunResult$ = new Subject<TestResult | undefined>() as Observable<TestResult | undefined>;
    component.reset$ = new Subject<void>();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
