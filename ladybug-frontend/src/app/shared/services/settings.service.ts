import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HttpService } from './http.service';
import { OptionsSettings } from '../interfaces/options-settings';
import { UploadParameters } from '../interfaces/upload-params';
import { ToastService } from './toast.service';
import { HttpErrorResponse } from '@angular/common/http';

export interface ServerSettings {
  isGeneratorEnabled: boolean;
  regexFilter: string | null;
  transformation: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private httpService = inject(HttpService);
  private toastService = inject(ToastService);
  private static INITIALIZATION_IDLE = 0;
  private static INITIALIZATION_BUSY = 1;
  public static INITIALIZATION_SUCCESS = 2;
  public static INITIALIZATION_ERROR = 3;
  private static INITIALIZATION_POLL_INTERVAL_MS = 100;

  private initializationState = SettingsService.INITIALIZATION_IDLE;
  private _role = 'unknown';
  private _isGeneratorEnabled = false;
  private _regexFilter: string | null = null;
  private _transformation: string | null = null;

  // Life cycle hooks like ngOnInit are not available for services.
  // Therefore this method is introduced. It should be run by every
  // component injecting this service.
  // As a consequence it should work when executed in parallel.
  public async init(): Promise<number> {
    if (this.initializationState === SettingsService.INITIALIZATION_IDLE) {
      this.initializationState = SettingsService.INITIALIZATION_BUSY;
      try {
        await this.refresh();
        this.initializationState = SettingsService.INITIALIZATION_SUCCESS;
        return this.initializationState;
      } catch {
        this.initializationState = SettingsService.INITIALIZATION_ERROR;
        return this.initializationState;
      }
    } else if (this.initializationState === SettingsService.INITIALIZATION_BUSY) {
      await new Promise((resolve) => setTimeout(resolve, SettingsService.INITIALIZATION_POLL_INTERVAL_MS));
      return this.init();
    } else {
      return this.initializationState;
    }
  }

  public getRole(): string {
    return this._role;
  }

  public isDataAdmin(): boolean {
    return this._role === 'dataAdmin' || this._role === 'tester';
  }

  public isGeneratorEnabled(): boolean {
    return this._isGeneratorEnabled;
  }

  public getRegexFilter(): string | null {
    return this._regexFilter;
  }

  public getTransformation(): string | null {
    return this._transformation;
  }

  public async refresh(): Promise<void> {
    try {
      const optionsSettings: OptionsSettings = await firstValueFrom(this.httpService.getSettings());
      this._role = optionsSettings.role;
      this._isGeneratorEnabled = optionsSettings.generatorEnabled!;
      this._regexFilter = optionsSettings.regexFilter!;
      this._transformation = null;
      if (optionsSettings.transformation !== undefined) {
        this._transformation = optionsSettings.transformation;
      }
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse) {
        this.toastService.showDanger(`Failed to load settings: ${error.message}`);
      } else {
        this.toastService.showDanger(`Failed to load settings - unexpected error kind`);
      }
    }
  }

  public async saveAsDataAdmin(settings: ServerSettings): Promise<void> {
    const isSettingsChanged =
      settings.isGeneratorEnabled !== this._isGeneratorEnabled || settings.regexFilter !== this._regexFilter;
    const isTransformationChanged = settings.transformation !== this._transformation;
    if (!(isSettingsChanged || isTransformationChanged)) {
      return;
    }
    const uploadParameters: UploadParameters = {};
    if (isSettingsChanged) {
      uploadParameters.generatorEnabled = settings.isGeneratorEnabled;
      uploadParameters.regexFilter = settings.regexFilter === null ? '' : settings.regexFilter;
    }
    if (isTransformationChanged) {
      uploadParameters.transformation = settings.transformation === null ? '' : settings.transformation;
    }
    try {
      await firstValueFrom(this.httpService.postSettingsAsDataAdmin(uploadParameters));
      this._isGeneratorEnabled = settings.isGeneratorEnabled;
      this._regexFilter = settings.regexFilter;
      this._transformation = settings.transformation;
    } catch {
      throw new Error('Failed to save debug tab settings to server');
    }
  }

  public async saveAsObserver(transformation: string): Promise<void> {
    try {
      await firstValueFrom(this.httpService.postTransformationAsObserver(transformation));
    } catch {
      throw new Error('Failed to save default report transformation');
    }
  }

  public async backToFactory(): Promise<void> {
    try {
      await firstValueFrom(this.httpService.resetSettings());
      this.refresh();
    } catch {
      throw new Error('Failed to restore factory settings');
    }
  }
}
