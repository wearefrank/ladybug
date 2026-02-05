import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportAlertMessageComponent } from './report-alert-message.component';

describe('ReportAlertMessageComponent', () => {
  let component: ReportAlertMessageComponent;
  let fixture: ComponentFixture<ReportAlertMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportAlertMessageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportAlertMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
