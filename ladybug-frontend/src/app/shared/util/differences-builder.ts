import { ReportDifference } from '../interfaces/report-difference';
import DiffMatchPatch from 'diff-match-patch';

// Only exported for testing.
export interface DifferenceBase {
  name: string;
  originalValue: string;
  editedValue: string;
  colorDifferences: boolean;
}

export class DifferencesBuilder {
  data: DifferenceBase[] = [];

  nonNullableVariable(originalValue: string, editedValue: string, name: string, color?: boolean): DifferencesBuilder {
    if (editedValue !== originalValue) {
      this.data.push({
        name,
        originalValue,
        editedValue,
        colorDifferences: DifferencesBuilder.getColorDifferences(color),
      });
    }
    return this;
  }

  nullableVariable(
    originalValue: string | null,
    editedValue: string | null,
    name: string,
    color?: boolean,
  ): DifferencesBuilder {
    if (originalValue === null) {
      // If editedValue === null then no differences.
      if (editedValue !== null) {
        if (editedValue == '') {
          this.data.push({
            name: DifferencesBuilder.getNameNullStatus(name),
            colorDifferences: false,
            originalValue: 'null',
            editedValue: 'blank',
          });
        } else {
          this.data.push(
            {
              name: DifferencesBuilder.getNameNullStatus(name),
              colorDifferences: false,
              originalValue: 'null',
              editedValue: 'not null',
            },
            {
              name: DifferencesBuilder.getNameTextDifference(name),
              colorDifferences: DifferencesBuilder.getColorDifferences(color),
              originalValue: '',
              editedValue: editedValue,
            },
          );
        }
      }
    } else {
      // Original value not null.
      if (editedValue === null) {
        if (originalValue === '') {
          this.data.push({
            name: DifferencesBuilder.getNameNullStatus(name),
            colorDifferences: false,
            originalValue: 'blank',
            editedValue: 'null',
          });
        } else {
          this.data.push(
            {
              name: DifferencesBuilder.getNameNullStatus(name),
              colorDifferences: false,
              originalValue: 'not null',
              editedValue: 'null',
            },
            {
              name: DifferencesBuilder.getNameTextDifference(name),
              colorDifferences: DifferencesBuilder.getColorDifferences(color),
              originalValue,
              editedValue: '',
            },
          );
        }
      } else {
        if (originalValue !== editedValue) {
          this.data.push({
            name,
            originalValue,
            editedValue,
            colorDifferences: DifferencesBuilder.getColorDifferences(color),
          });
        }
      }
    }
    return this;
  }

  build(): ReportDifference[] {
    return this.data.map((item) => this.buildItem(item));
  }

  private buildItem(item: DifferenceBase): ReportDifference {
    if (item.colorDifferences) {
      const difference = new DiffMatchPatch().diff_main(item.originalValue, item.editedValue);
      return {
        name: item.name,
        originalValue: item.originalValue,
        colorDifferences: true,
        difference,
      };
    } else {
      return {
        name: item.name,
        originalValue: item.originalValue,
        colorDifferences: false,
        difference: item.editedValue,
      };
    }
  }

  private static getColorDifferences(input?: boolean): boolean {
    return input === undefined ? false : input;
  }

  private static getNameNullStatus(name: string): string {
    return `${name} - null status`;
  }

  private static getNameTextDifference(name: string): string {
    return `${name} - text`;
  }
}
