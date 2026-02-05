import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'booleanToString',
  standalone: true,
})
export class BooleanToStringPipe implements PipeTransform {
  transform(value: boolean, trueValue: string, falseValue: string): string {
    return value ? trueValue : falseValue;
  }
}
