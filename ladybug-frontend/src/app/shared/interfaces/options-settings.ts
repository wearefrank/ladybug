import { UploadParameters } from './upload-params';

export interface OptionsSettings extends UploadParameters {
  roles: string[];
  estMemory: string;
  reportsInProgress: number;
  stubStrategies: string[];
  transformation?: string;
  uiTestMode: string;
}
