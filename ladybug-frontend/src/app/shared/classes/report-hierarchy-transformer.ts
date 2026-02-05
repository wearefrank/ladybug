import { Report } from '../interfaces/report';
import { Checkpoint } from '../interfaces/checkpoint';
import { CHECKPOINT_TYPE_STRINGS, CheckpointType } from '../enums/checkpoint-type';

export class ReportHierarchyTransformer {
  private readonly THROWABLE_ENCODER: string = 'printStackTrace()';
  private checkpointsTemplate: Checkpoint[] = [];
  private startPointStack: Checkpoint[] = [];

  transform(report: Report): Report {
    const checkpoints: Checkpoint[] = report.checkpoints;
    for (const checkpoint of checkpoints) {
      checkpoint.parentReport = report;
      checkpoint.iconClass = this.getImage(checkpoint.type, checkpoint.encoding ?? '', checkpoint.level);
      if (checkpoint.type === CheckpointType.Startpoint || checkpoint.type === CheckpointType.ThreadStartpoint) {
        this.handleStartpoint(checkpoint);
      } else if (
        checkpoint.type === CheckpointType.Endpoint ||
        checkpoint.type === CheckpointType.Abortpoint ||
        checkpoint.type === CheckpointType.ThreadEndpoint
      ) {
        this.handleEndpoint(checkpoint);
      } else {
        this.handleIntermediatePoint(checkpoint);
      }
    }

    report.checkpoints = this.checkpointsTemplate;
    return report;
  }

  getImage(type: CheckpointType, encoding: string, level: number): string {
    let iconClass = CHECKPOINT_TYPE_STRINGS[type];
    if (encoding === this.THROWABLE_ENCODER) {
      iconClass += ' red';
    }
    iconClass += level % 2 == 0 ? ' even' : ' odd';
    return iconClass;
  }

  private handleStartpoint(checkpoint: Checkpoint): void {
    if (this.startPointStack.length > 0) {
      this.addCheckpointToParent(checkpoint, this.startPointStack);
    } else {
      this.checkpointsTemplate.push(checkpoint);
    }
    this.startPointStack.push(checkpoint);
  }

  private handleEndpoint(checkpoint: Checkpoint): void {
    if (this.startPointStack.length > 0) {
      const currentStartpoint = this.startPointStack.pop();
      if (currentStartpoint) {
        this.addCheckpointToParent(checkpoint, [currentStartpoint]);
      }
    } else {
      this.checkpointsTemplate.push(checkpoint);
    }
  }

  private handleIntermediatePoint(checkpoint: Checkpoint): void {
    if (this.startPointStack.length > 0) {
      this.addCheckpointToParent(checkpoint, this.startPointStack);
    } else {
      this.checkpointsTemplate.push(checkpoint);
    }
  }

  private addCheckpointToParent(checkpoint: Checkpoint, startPointStack: Checkpoint[]): void {
    const parentStartpoint = startPointStack.at(-1)!;
    if (!parentStartpoint.checkpoints) {
      parentStartpoint.checkpoints = [];
    }
    parentStartpoint.checkpoints.push(checkpoint);
  }
}
