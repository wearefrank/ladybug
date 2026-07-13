export interface TreeItem {
  name: string;
  children: HierarchicalCheckpoint[] | null;
}

// When editing, please compare with backend class ShownReport.
// Fields that are non-null here should be checked in the backend
// not to be null.
export interface HierarchicalReport extends TreeItem {
  description: string | null;
  path: string | null;
  stubStrategy: string | null;
  linkMethod: string | null;
  transformation: string | null;
  storageId: number;
  storageName: string;
  crudStorage: boolean;
  estimatedMemoryUsage: number;
  correlationId: string;
  variables: Record<string, string> | null;
  startTime: number;
  host: string | null;
  application: string | null;

  // The fields below do not come from the backend but are computed
  xml: string;
  // null means - no view so all checkpoints included
  checkpointsFromView: string | null;
}

// When editing, please compare with backend class ShownCheckpoint.
// Fields that are non-null here should be checked in the backend
// not to be null.
export interface HierarchicalCheckpoint extends TreeItem {
  message: string | null;
  encoding: string | null;
  // TODO issue https://github.com/wearefrank/ladybug/issues/863. Fix
  // type mismatch of next field with the backend.
  messageContext: string | null;
  type: number;
  level: number;
  stub: number;
  stubbed: boolean;
  stubNotFound: string | null;
  preTruncatedMessageLength: number;
  typeAsString: string;
  threadName: string | null;
  sourceClassName: string | null;
  messageClassName: string | null;
  uid: string;

  // The fields below are computed

  // This one is not computed by the backend because that would
  // involve iterating over the checkpoints of a report.
  // That is tricky because multiple threats in the backend
  // can create checkpoints and because checkpoints are not
  // always put at the end of the checkpoint list.
  //
  // We are cheating a bit here because the uid was already
  // calculated by iterating over the checkpoints. But you
  // do not see this when reviewing the fix for issue
  // https://github.com/wearefrank/ladybug/issues/674
  id: number;
  report: HierarchicalReport;
}
