import { Report } from './report';

export interface ReranReport {
  id: number;
  originalReport: Report;
  runResultReport: Report;
  color: string;
  resultString: string;
}
