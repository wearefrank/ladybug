import { inject, Injectable } from '@angular/core';
import { HttpService } from './http.service';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  packageJsonPath = 'assets/package.json';
  frontendVersion?: string;
  backendVersion?: string;

  private httpService = inject(HttpService);

  async getBackendVersion(): Promise<string> {
    if (!this.backendVersion) {
      this.backendVersion = await firstValueFrom(this.httpService.getBackendVersion());
    }
    return this.backendVersion;
  }
}
