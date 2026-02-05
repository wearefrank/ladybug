import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'shortenedTableHeader',
  standalone: true,
})
export class ShortenedTableHeaderPipe implements PipeTransform {
  private shortenedTableHeaders = new Map<string, string>([
    ['Storage Id', 'Storage Id'],
    ['End time', 'End time'],
    ['Duration', 'Duration'],
    ['Name', 'Name'],
    ['Correlation Id', 'Correlation Id'],
    ['Status', 'Status'],
    ['Number of checkpoints', 'Checkpoints'],
    ['Estimated memory usage', 'Memory'],
    ['Storage size', 'Size'],
    ['TIMESTAMP', 'TIMESTAMP'],
    ['COMPONENT', 'COMPONENT'],
    ['ENDPOINT NAME', 'ENDPOINT'],
    ['CONVERSATION ID', 'CONVERSATION ID'],
    ['CORRELATION ID', 'CORRELATION ID'],
    ['NR OF CHECKPOINTS', 'NR OF CHECKPOINTS'],
    ['STATUS', 'STATUS'],
  ]);

  transform(value: string): string {
    const fromMap = this.shortenedTableHeaders.get(value);
    return fromMap ?? value;
  }
}
