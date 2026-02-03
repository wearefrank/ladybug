export const enum CheckpointType {
  Startpoint = 1,
  Endpoint,
  Abortpoint,
  Inputpoint,
  Outputpoint,
  Infopoint,
  ThreadStartpointError,
  ThreadStartpoint,
  ThreadEndpoint,
}

export const CHECKPOINT_TYPE_STRINGS: Record<CheckpointType, string> = {
  [CheckpointType.Startpoint]: 'bi bi-arrow-bar-right icon-size bolder-icon',
  [CheckpointType.Endpoint]: 'bi bi-arrow-bar-left icon-size bolder-icon',
  [CheckpointType.Abortpoint]: 'bi bi-x-lg red icon-size scale-down bolder-icon',
  [CheckpointType.Inputpoint]: 'bi bi-arrow-right icon-size scale-down bolder-icon',
  [CheckpointType.Outputpoint]: 'bi bi-arrow-left icon-size scale-down bolder-icon',
  [CheckpointType.Infopoint]: 'bi bi-info-square icon-size scale-down bold-icon',
  [CheckpointType.ThreadStartpointError]: 'bi bi-fast-forward icon-size bold-icon',
  [CheckpointType.ThreadStartpoint]: 'bi bi-chevron-double-right icon-size scale-down bold-icon',
  [CheckpointType.ThreadEndpoint]: 'bi bi-chevron-double-left icon-size scale-down bold-icon',
};
