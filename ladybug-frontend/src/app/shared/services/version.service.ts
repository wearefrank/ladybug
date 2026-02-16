import { inject, Injectable } from '@angular/core';
import { HttpService } from './http.service';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  version?: string;

  private httpService = inject(HttpService);

  async getVersion(): Promise<string> {
    if (!this.version) {
      this.version = await firstValueFrom(this.httpService.getVersion());
    }
    return this.version;
  }
}
