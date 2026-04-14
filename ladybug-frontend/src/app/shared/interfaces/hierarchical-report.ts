export interface TreeItem {
  name: string;
  children: HierarchicalCheckpoint[] | null;
}

export interface HierarchicalReport extends TreeItem {
  description: string | null;
  path: string | null;
  stubStrategy: string;
  linkMethod: string;
  transformation: string | null;
  storageId: number;
  storageName: string;
  estimatedMemoryUsage: number;
  correlationId: string;
}

export interface HierarchicalCheckpoint extends TreeItem {
  message: string | null;
  encoding: string | null;
  messageContext: string | null;
  type: number;
  level: number;
  stub: number;
  stubbed: boolean;
  stubNotFound: string | null;
  preTruncatedMessageLength: number;
  typeAsString: string;
  threadName: string;
  sourceClassName: string | null;
  messageClassName: string | null;
  uid: string;
}
