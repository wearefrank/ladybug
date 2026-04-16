import { CompareData } from '../../compare/compare-data';
import { HierarchicalReportData } from './report-data';

export interface Tab {
  key: string;
  id: string;
  data?: HierarchicalReportData | CompareData;
  path: string;
}
