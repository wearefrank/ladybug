import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'strReplace',
  standalone: true,
  pure: true,
})
export class StrReplacePipe implements PipeTransform {
  transform(value: string, pattern: string, replace: string): string {
    return value.replace(pattern, replace);
  }
}
