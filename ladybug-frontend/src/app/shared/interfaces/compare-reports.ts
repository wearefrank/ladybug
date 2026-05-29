import { HierarchicalReport } from './hierarchical-report';
import { Report } from './report';

export interface CompareReport {
  report: Report;
  xml: string;
}

export interface CompareHierarchicalReport {
  report: HierarchicalReport;
  xml: string;
}
