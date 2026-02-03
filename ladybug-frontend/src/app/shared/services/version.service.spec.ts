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

  it('should set version from package.json', async () => {
    const frontendVersionPromise = service.getFrontendVersion();
    const backendVersionPromise = service.getBackendVersion();
    const mockPackageJson = { version: '1.0-TEST' };

    const frontendVersionRequest = httpTestingController.expectOne(service.packageJsonPath);

    const backendVersionReg = httpTestingController.expectOne('api/testtool/version');
    frontendVersionRequest.flush(mockPackageJson);
    backendVersionReg.flush('3.0-TEST');

    const frontendVersion = await frontendVersionPromise;
    const backendVersion = await backendVersionPromise;

    expect(frontendVersion).toEqual('1.0-TEST');
    expect(backendVersion).toEqual('3.0-TEST');
  });
});
