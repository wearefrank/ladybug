import { View } from './view';
import { Report } from './report';
import { HierarchicalReport } from './hierarchical-report';

export interface ReportData {
  report: Report;
  currentView: View;
}

export interface HierarchicalReportData {
  report: HierarchicalReport;
  currentView: View;
}
