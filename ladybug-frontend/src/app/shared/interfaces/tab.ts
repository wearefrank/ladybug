import { CompareData } from '../../compare/compare-data';
import { ReportData } from './report-data';

export interface Tab {
  key: string;
  id: string;
  data?: ReportData | CompareData;
  path: string;
}
