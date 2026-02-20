import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { VersionService } from './shared/services/version.service';
import { provideRouter } from '@angular/router';

class MockVersionService {
  getVersion = jasmine.createSpy('getVersion').and.returnValue(Promise.resolve('1.0-TEST'));
}

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let titleService: Title;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: VersionService, useClass: MockVersionService },
        Title,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    titleService = TestBed.inject(Title);
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should set the title correctly based on frontend version from version service', async () => {
    spyOn(titleService, 'setTitle');

    await component.fetchAndSetVersion();

    expect(titleService.setTitle).toHaveBeenCalledWith('Ladybug - v1.0-TEST');
    expect(component.version).toEqual('1.0-TEST');
  });
});
