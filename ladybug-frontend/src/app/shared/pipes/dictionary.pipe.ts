/* eslint-disable @typescript-eslint/no-explicit-any */
import { Pipe, PipeTransform } from '@angular/core';
import { KeyValue } from '@angular/common';

@Pipe({
  name: 'dictionary',
  standalone: true,
})
export class DictionaryPipe implements PipeTransform {
  transform(dict: Map<string, string>): KeyValue<string, string>[] | null {
    const keyValues: KeyValue<any, any>[] = [];
    for (let [key, value] of dict.entries()) {
      keyValues.push({ key: key, value: value });
    }
    return keyValues;
  }
}
