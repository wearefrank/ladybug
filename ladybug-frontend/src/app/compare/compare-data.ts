import { Report } from '../shared/interfaces/report';

export interface CompareData {
  originalReport: Report;
  runResultReport: Report;
  viewName?: string;
}
