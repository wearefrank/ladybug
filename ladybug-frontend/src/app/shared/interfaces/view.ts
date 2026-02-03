export interface View {
  metadataLabels: string[];
  metadataTypes: Map<string, string>;
  defaultView: boolean;
  crudStorage: boolean;
  metadataNames: string[];
  storageName: string;
  name: string;
  hasCheckpointMatchers: boolean;
}
