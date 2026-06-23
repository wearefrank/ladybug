import { TestBed } from '@angular/core/testing';

import { Filter2Service } from './filter2.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('FilterService', () => {
  let service: Filter2Service;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(Filter2Service);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
