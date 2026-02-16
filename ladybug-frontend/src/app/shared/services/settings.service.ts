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
  public init(): Promise<number> {
    return new Promise((resolve) => {
      if (this.initializationState === SettingsService.INITIALIZATION_IDLE) {
        this.initializationState = SettingsService.INITIALIZATION_BUSY;
        this.refresh()
          .then(() => {
            this.initializationState = SettingsService.INITIALIZATION_SUCCESS;
            resolve(this.initializationState);
          })
          .catch(() => {
            this.initializationState = SettingsService.INITIALIZATION_ERROR;
            resolve(this.initializationState);
          });
      } else if (this.initializationState === SettingsService.INITIALIZATION_BUSY) {
        setTimeout(() => {
          this.init().then((result) => resolve(result));
        }, SettingsService.INITIALIZATION_POLL_INTERVAL_MS);
      } else {
        resolve(this.initializationState);
      }
    });
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

  public refresh(): Promise<void> {
    return new Promise<void>((resolve) => {
      const settingsPromise: Promise<OptionsSettings> = firstValueFrom(
        this.httpService.getSettings().pipe(catchError(this.errorHandler.handleError())),
      );
      const transformationPromise: Promise<Transformation> = firstValueFrom(
        this.httpService.getTransformation().pipe(catchError(this.errorHandler.handleError())),
      );
      Promise.all([settingsPromise, transformationPromise]).then((values) => {
        const optionsSettings: OptionsSettings = values[0];
        const transformation: Transformation = values[1];
        this._isGeneratorEnabled = optionsSettings.generatorEnabled;
        this._regexFilter = optionsSettings.regexFilter;
        this._transformation = transformation.transformation;
        resolve();
      });
    });
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
    return new Promise((resolve, reject) => {
      Promise.all(requests)
        .then(() => {
          this._isGeneratorEnabled = settings.isGeneratorEnabled;
          this._regexFilter = settings.regexFilter;
          this._transformation = settings.transformation;
        })
        .then(() => resolve())
        .catch(() => reject('Failed to save debug tab settings toserver'));
    });
  }

  public backToFactory(): Promise<void> {
    // All roles have permission to do this. No need to check for changes before doint the HTTP calls.
    return new Promise<void>((resolve, reject) => {
      const settingsPromise = firstValueFrom(
        this.httpService.resetSettings().pipe(catchError(this.errorHandler.handleError())),
      );
      const transformationBackToFactoryPromise: Promise<void> = firstValueFrom(
        this.httpService.restoreFactoryTransformation().pipe(catchError(this.errorHandler.handleError())),
      );
      return Promise.all([settingsPromise, transformationBackToFactoryPromise])
        .then(() => this.refresh())
        .then(() => resolve())
        .catch(() => reject('Failed to restore factory settings'));
    });
  }
}
