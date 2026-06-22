export interface View {
  metadataLabels: string[];
  metadataTypes: Map<string, string>;
  defaultView: boolean;
  crudStorage: boolean;
  metadataNames: string[];
  storageName: string;
  name: string;
  hasCheckpointMatchers: boolean;
  // Optional per-view filter (metadataName -> value) applied to the report list.
  metadataFilter?: Record<string, string>;
}
