import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DebugReportComponent } from './debug-report.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { routes } from '../../app-routing.module';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { DebugTreeStateService2 } from '../../debug/debug-tree-state2.service';

describe('DebugReportComponent', () => {
  let component: DebugReportComponent;
  let fixture: ComponentFixture<DebugReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugReportComponent, RouterTestingModule.withRoutes(routes)],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting(), DebugTreeStateService2],
    }).compileComponents();

    fixture = TestBed.createComponent(DebugReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
