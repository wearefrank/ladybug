import { HierarchicalCheckpoint, HierarchicalReport } from '../../shared/interfaces/hierarchical-report';
import { CheckpointType } from '../../shared/enums/checkpoint-type';
import { TreeViewInputNode, TreeViewNodeType } from '../../shared/components/tree-view/tree-view.component';

// The tree always shows one report, so the root needs no unique key.
export const REPORT_KEY = 'report';
const THROWABLE_ENCODER = 'printStackTrace()';

const NODE_TYPES: Record<CheckpointType, TreeViewNodeType> = {
  [CheckpointType.Startpoint]: 'startpoint',
  [CheckpointType.Endpoint]: 'endpoint',
  [CheckpointType.Abortpoint]: 'abortpoint',
  [CheckpointType.Inputpoint]: 'inputpoint',
  [CheckpointType.Outputpoint]: 'outputpoint',
  [CheckpointType.Infopoint]: 'infopoint',
  // Value 7 is THREAD_CREATEPOINT in the backend.
  [CheckpointType.ThreadStartpointError]: 'threadCreatepoint',
  [CheckpointType.ThreadStartpoint]: 'threadStartpoint',
  [CheckpointType.ThreadEndpoint]: 'threadEndpoint',
};

// The index of the checkpoint in the report. It does not depend on the
// view and it does not include the storage, which can differ when the
// same report is retrieved again.
export function keyOf(checkpoint: HierarchicalCheckpoint): string {
  return `${checkpoint.id}`;
}

// The properties of a report or checkpoint that its tree node is made of.
interface NodeProperties {
  key: string;
  type: TreeViewNodeType;
  text: string;
  valueIsError: boolean;
  level: number | null;
  children: HierarchicalCheckpoint[] | null;
}

// Transforms a report into the nodes of the tree view. While doing so,
// it remembers the report and its checkpoints by the key of their node.
export class ReportTransformer {
  private readonly _valuesByKey = new Map<string, HierarchicalReport | HierarchicalCheckpoint>();

  constructor(
    private readonly searchTerm: string,
    private readonly checkpointAndStorageIdShown: boolean,
  ) {}

  get valuesByKey(): ReadonlyMap<string, HierarchicalReport | HierarchicalCheckpoint> {
    return this._valuesByKey;
  }

  transform(report: HierarchicalReport): TreeViewInputNode {
    this._valuesByKey.clear();
    this._valuesByKey.set(REPORT_KEY, report);
    return this.toNode({
      key: REPORT_KEY,
      type: 'report',
      text: this.getReportName(report),
      valueIsError: false,
      level: null,
      children: report.children,
    });
  }

  private toCheckpointNode(checkpoint: HierarchicalCheckpoint): TreeViewInputNode {
    const key = keyOf(checkpoint);
    this._valuesByKey.set(key, checkpoint);
    return this.toNode({
      key,
      type: NODE_TYPES[checkpoint.type as CheckpointType],
      text: this.getCheckpointName(checkpoint),
      valueIsError: checkpoint.encoding === THROWABLE_ENCODER,
      level: checkpoint.level,
      children: checkpoint.children,
    });
  }

  private toNode({ key, type, text, valueIsError, level, children }: NodeProperties): TreeViewInputNode {
    return {
      key,
      type,
      text,
      valueIsError,
      level,
      highlighted: this.searchTerm !== '' && text.includes(this.searchTerm),
      children: (children ?? []).map((child) => this.toCheckpointNode(child)),
    };
  }

  private getReportName(report: HierarchicalReport): string {
    return this.checkpointAndStorageIdShown ? `${report.name} (${report.storageId})` : report.name;
  }

  private getCheckpointName(checkpoint: HierarchicalCheckpoint): string {
    return this.checkpointAndStorageIdShown ? `${checkpoint.name} (${checkpoint.id})` : checkpoint.name;
  }
}
