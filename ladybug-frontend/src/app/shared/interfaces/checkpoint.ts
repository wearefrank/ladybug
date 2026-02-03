import { CheckpointType } from '../enums/checkpoint-type';
import { Report } from './report';

export interface Checkpoint {
  encoding?: string;
  estimatedMemoryUsage: number;
  index: number;
  level: number;
  message: string | null;
  messageContext: Record<string, string>;
  messageClassName?: string;
  name: string;
  noCloseReceivedForStream?: boolean;
  preTruncatedMessageLength: number;
  sourceClassName?: number;
  // TODO: No longer sent by server, delete. Issue https://github.com/wearefrank/ladybug-frontend/issues/1127.
  showConverted?: boolean;
  streaming?: string;
  stub: number;
  stubNotFound?: string;
  stubbed: boolean;
  threadName: string;
  type: CheckpointType;
  typeAsString: string;
  uid: string;
  waitingForStream: boolean;
  storageId?: number;
  checkpoints?: Checkpoint[];
  icon?: string;
  iconClass?: string;
  parentReport: Report;
}
