import { Report } from './report';

export interface UpdateReportResponse {
  report: Report;
  storageUpdated: boolean;
  xml: string;
}
