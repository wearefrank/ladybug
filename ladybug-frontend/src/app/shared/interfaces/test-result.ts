import { Report } from './report';

export interface TestResult {
  info: string;
  originalReport: Report;
  runResultReport: Report;
  originalXml: string;
  runResultXml: string;
  equal: boolean;
}
