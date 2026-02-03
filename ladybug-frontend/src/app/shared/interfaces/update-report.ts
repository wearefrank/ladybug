export interface UpdateCheckpoint {
  checkpointId: string;
  checkpointMessage?: string | null;
  stub?: number;
}

export interface UpdateReport {
  name?: string;
  path?: string | null;
  description?: string | null;
  transformation?: string | null;
  variables?: Record<string, string>;
  stubStrategy?: string;
  checkpoints?: UpdateCheckpoint[];
}
