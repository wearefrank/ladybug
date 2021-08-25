import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'filter'
})
export class FilterPipe implements PipeTransform {

  /**
   * Filter all values in this column (items) based on filter
   * @param items - the column to be filtered
   * @param filter - the word with which to filter
   */
  transform(items: any[], filter: string): any {
    if (!items || !filter || filter === "") {
      return items;
    }

    return items.filter(item => item.includes(filter));
  }

}
