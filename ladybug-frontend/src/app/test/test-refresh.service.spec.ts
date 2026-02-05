import { TestBed } from '@angular/core/testing';

import { TestRefreshService } from './test-refresh.service';

describe('TestRefresh', () => {
  let service: TestRefreshService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TestRefreshService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
