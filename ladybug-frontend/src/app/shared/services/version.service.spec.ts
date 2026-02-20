import { TestBed } from '@angular/core/testing';

import { VersionService } from './version.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('VersionService', () => {
  let service: VersionService;

  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(VersionService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set version', async () => {
    const versionPromise = service.getVersion();
    const versionReg = httpTestingController.expectOne('api/testtool/version');
    versionReg.flush('3.0-TEST');
    const version = await versionPromise;
    expect(version).toEqual('3.0-TEST');
  });
});
