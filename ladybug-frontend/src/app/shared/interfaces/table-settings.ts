import { Report } from './report';

// TODO: Remove this file.
export interface TableSettings {
  reportMetadata: Report[];
  displayAmount: number;
  showFilter: boolean;
  currentFilters: Map<string, string>;
  tableLoaded: boolean;
  numberOfReportsInProgress: number;
  estimatedMemoryUsage: string;
  uniqueValues: Map<string, string[]>;
}
