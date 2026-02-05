import DiffMatchPatch from 'diff-match-patch';

export interface ReportDifference {
  name: string;
  originalValue: string;
  difference: DiffMatchPatch.Diff[] | string;
  colorDifferences: boolean;
}
