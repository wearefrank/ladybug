import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportAlertMessage2Component } from './report-alert-message2.component';

describe('ReportAlertMessage2Component', () => {
  let component: ReportAlertMessage2Component;
  let fixture: ComponentFixture<ReportAlertMessage2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportAlertMessage2Component],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportAlertMessage2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
