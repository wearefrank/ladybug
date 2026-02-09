import { inject, Injectable } from '@angular/core';
import { catchError, firstValueFrom, Observable } from 'rxjs';
import { HttpService } from './http.service';
import { OptionsSettings } from '../interfaces/options-settings';
import { Transformation } from '../interfaces/transformation';
import { ErrorHandling } from '../classes/error-handling.service';
import { UploadParameters } from '../interfaces/upload-params';
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
  private errorHandler = inject(ErrorHandling);

  private static INITIALIZATION_IDLE = 0;
  private static INITIALIZATION_BUSY = 1;
  public static INITIALIZATION_SUCCESS = 2;
  public static INITIALIZATION_ERROR = 3;
  private static INITIALIZATION_POLL_INTERVAL_MS = 100;

  private initializationState = SettingsService.INITIALIZATION_IDLE;

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
    return new Promise<void>((resolve, reject) => {
      const settingsPromise: Promise<OptionsSettings> = firstValueFrom(
        this.httpService
          .getSettings()
          .pipe(
            catchError(this.handleErrorWithRethrowMessage('Could not load settings generatorEnabled or regexFilter')),
          ),
      );
      const transformationPromise: Promise<Transformation> = firstValueFrom(
        this.httpService
          .getTransformation()
          .pipe(catchError(this.handleErrorWithRethrowMessage('Could not load default report transformation'))),
      );
      Promise.all([settingsPromise, transformationPromise])
        .then((values) => {
          const optionsSettings: OptionsSettings = values[0];
          const transformation: Transformation = values[1];
          console.log(
            `SettingsService.refreshFrankAppSettings(): _isGeneratorEnabled from ${this._isGeneratorEnabled} to ${optionsSettings.generatorEnabled}`,
          );
          this._isGeneratorEnabled = optionsSettings.generatorEnabled;
          this._regexFilter = optionsSettings.regexFilter;
          this._transformation = transformation.transformation;
          resolve();
        })
        .catch(() => reject('Failed to obtain debug tab settings from server'));
    });
  }

  public async save(settings: ServerSettings): Promise<void> {
    return new Promise((resolve, reject) => {
      const uploadParameters: UploadParameters = {
        generatorEnabled: settings.isGeneratorEnabled,
        regexFilter: settings.regexFilter === null ? '' : settings.regexFilter,
      };
      const settingsPromise: Promise<void> = firstValueFrom(
        this.httpService
          .postSettings(uploadParameters)
          .pipe(
            catchError(this.handleErrorWithRethrowMessage('Could not save settings generatorEnabled and regexFilter')),
          ),
      );
      const transformationPromise: Promise<void> = firstValueFrom(
        this.httpService
          .postTransformation(settings.transformation === null ? '' : settings.transformation)
          .pipe(catchError(this.handleErrorWithRethrowMessage('Could not load default report transformation'))),
      );
      Promise.all([settingsPromise, transformationPromise])
        .then(() => resolve())
        .catch(() => reject('Failed to save debug tab settings toserver'));
    });
  }

  public backToFactory(): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const settingsPromise = firstValueFrom(
        this.httpService
          .resetSettings()
          .pipe(
            catchError(
              this.handleErrorWithRethrowMessage(
                'Failed to restore factory values for generatorEnabled and regexFilter',
              ),
            ),
          ),
      );
      const transformationBackToFactoryPromise: Promise<void> = firstValueFrom(
        this.httpService
          .restoreFactoryTransformation()
          .pipe(catchError(this.handleErrorWithRethrowMessage('Could not restore factory report transformation'))),
      );
      return Promise.all([settingsPromise, transformationBackToFactoryPromise])
        .then(() => this.refresh())
        .then(() => resolve())
        .catch(() => reject('Failed to restore factory settings'));
    });
  }

  private _isGeneratorEnabled = false;
  private _regexFilter: string | null = null;
  private _transformation: string | null = null;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleErrorWithRethrowMessage(message: string): (error: HttpErrorResponse) => Observable<any> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (error: HttpErrorResponse): Observable<any> => {
      this.errorHandler.handleError()(error);
      throw new Error(message);
    };
  }
}
