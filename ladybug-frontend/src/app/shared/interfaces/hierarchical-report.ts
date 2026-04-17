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
  crudStorage: boolean;
  estimatedMemoryUsage: number;
  correlationId: string;
  variables: Record<string, string>;
  // The fields below do not come from the backend but are computed
  xml: string;
  // null means - no view so all checkpoints included
  checkpointsFromView: string | null;
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
  id: number;
  uid: string;
  // The fields below are computed
  report: HierarchicalReport;
}
