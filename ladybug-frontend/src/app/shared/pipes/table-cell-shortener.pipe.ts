import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tableCellShortener',
  standalone: true,
})
export class TableCellShortenerPipe implements PipeTransform {
  transform(value: string): string {
    if (value == undefined) {
      return value;
    }
    return this.removeMillisecondsFromTimestamp(value);
  }

  removeMillisecondsFromTimestamp(value: string): string {
    if (/\b\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\b/.test(value)) {
      return value.slice(0, Math.max(0, value.indexOf('.')));
    }
    return value;
  }
}
