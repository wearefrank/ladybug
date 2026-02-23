import { inject, Injectable } from '@angular/core';
import { catchError, firstValueFrom } from 'rxjs';
import { HttpService } from './http.service';
import { OptionsSettings } from '../interfaces/options-settings';
import { Transformation } from '../interfaces/transformation';
import { ErrorHandling } from '../classes/error-handling.service';
import { UploadParameters } from '../interfaces/upload-params';

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
  private errorHandler = inject(ErrorHandling);
  private static INITIALIZATION_IDLE = 0;
  private static INITIALIZATION_BUSY = 1;
  public static INITIALIZATION_SUCCESS = 2;
  public static INITIALIZATION_ERROR = 3;
  private static INITIALIZATION_POLL_INTERVAL_MS = 100;

  private initializationState = SettingsService.INITIALIZATION_IDLE;
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
    const settingsPromise: Promise<OptionsSettings> = firstValueFrom(
      this.httpService.getSettings().pipe(catchError(this.errorHandler.handleError())),
    );
    const transformationPromise: Promise<Transformation> = firstValueFrom(
      this.httpService.getTransformation().pipe(catchError(this.errorHandler.handleError())),
    );
    await Promise.all([settingsPromise, transformationPromise]);
    const optionsSettings: OptionsSettings = await settingsPromise;
    const transformation: Transformation = await transformationPromise;
    this._isGeneratorEnabled = optionsSettings.generatorEnabled;
    this._regexFilter = optionsSettings.regexFilter;
    this._transformation = transformation.transformation;
  }

  // IbisObserver is not allowed to change enable/disable the report generator or to change the regex.
  // IbisObserver is allowed to change the report transformation.
  // So we send only the transformation update request when possible.
  public async save(settings: ServerSettings): Promise<void> {
    const isSettingsChanged =
      settings.isGeneratorEnabled !== this._isGeneratorEnabled || settings.regexFilter !== this._regexFilter;
    const isTransformationChanged = settings.transformation !== this._transformation;
    const requests: Promise<void>[] = [];
    if (isSettingsChanged) {
      const uploadParameters: UploadParameters = {
        generatorEnabled: settings.isGeneratorEnabled,
        regexFilter: settings.regexFilter === null ? '' : settings.regexFilter,
      };
      requests.push(
        firstValueFrom(
          this.httpService.postSettings(uploadParameters).pipe(catchError(this.errorHandler.handleError())),
        ),
      );
    }
    if (isTransformationChanged) {
      requests.push(
        firstValueFrom(
          this.httpService
            .postTransformation(settings.transformation === null ? '' : settings.transformation)
            .pipe(catchError(this.errorHandler.handleError())),
        ),
      );
    }
    try {
      await Promise.all(requests);
      this._isGeneratorEnabled = settings.isGeneratorEnabled;
      this._regexFilter = settings.regexFilter;
      this._transformation = settings.transformation;
    } catch {
      throw new Error('Failed to save debug tab settings toserver');
    }
  }

  public async backToFactory(): Promise<void> {
    // All roles have permission to do this. No need to check for changes before doint the HTTP calls.
    const settingsPromise = firstValueFrom(
      this.httpService.resetSettings().pipe(catchError(this.errorHandler.handleError())),
    );
    const transformationBackToFactoryPromise: Promise<void> = firstValueFrom(
      this.httpService.restoreFactoryTransformation().pipe(catchError(this.errorHandler.handleError())),
    );
    try {
      await Promise.all([settingsPromise, transformationBackToFactoryPromise]);
      this.refresh();
    } catch {
      throw new Error('Failed to restore factory settings');
    }
  }
}
