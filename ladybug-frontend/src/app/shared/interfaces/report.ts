import { Checkpoint } from './checkpoint';
import { BaseReport } from './base-report';

export interface Report extends BaseReport {
  checkpoints: Checkpoint[];
  correlationId: string;
  crudStorage: boolean;
  endTime: number;
  estimatedMemoryUsage: number;
  // TODO: Add "path"? Issues https://github.com/wearefrank/ladybug-frontend/issues/1127 and https://github.com/wearefrank/ladybug-frontend/issues/1129.
  fullPath: string;
  inputCheckpoint: Checkpoint;
  numberOfCheckpoints: number;
  originalEndpointOrAbortpointForCurrentLevel?: Checkpoint;
  originalReport: Report;
  reportFilterMatching: boolean;
  startTime: number;
  // TODO: Remove, is not provided by the backend. https://github.com/wearefrank/ladybug-frontend/issues/1127
  stub: number;
  stubStrategy: string;
  transformation: string;
  variables: string;
  storageName: string;
  xml: string; // Custom for the xml representation of the report
  id: string; // Custom
}
