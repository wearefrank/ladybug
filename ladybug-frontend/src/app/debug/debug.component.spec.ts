import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugComponent } from './debug.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { routes } from '../app-routing.module';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('DebugComponent', () => {
  let component: DebugComponent;
  let fixture: ComponentFixture<DebugComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugComponent, RouterTestingModule.withRoutes(routes)],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DebugComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
