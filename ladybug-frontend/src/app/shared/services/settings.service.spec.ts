import { TestBed } from '@angular/core/testing';

import { SettingsService } from './settings.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpService } from './http.service';

describe('SettingsService', () => {
  let service: SettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), HttpService],
    });
    service = TestBed.inject(SettingsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
