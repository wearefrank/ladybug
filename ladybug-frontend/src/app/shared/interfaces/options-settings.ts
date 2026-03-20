import { UploadParameters } from './upload-params';

export interface OptionsSettings extends UploadParameters {
  role: string;
  estMemory: string;
  reportsInProgress: number;
  stubStrategies: string[];
  transformation?: string;
  uiTestMode: string;
}
