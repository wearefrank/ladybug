import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportComponent } from './report.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { routes } from '../app-routing.module';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ActivatedRouteSnapshot } from '@angular/router';

describe('ReportComponent', () => {
  let component: ReportComponent;
  let fixture: ComponentFixture<ReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportComponent, RouterTestingModule.withRoutes(routes)],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        {
          provide: ActivatedRouteSnapshot,
          useValue: {
            paramMap: {
              get: (key: string): string | null => {
                if (key === 'storageId') {
                  return '0';
                } else if (key === 'storageName') {
                  return 'dummy';
                } else {
                  return null;
                }
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
