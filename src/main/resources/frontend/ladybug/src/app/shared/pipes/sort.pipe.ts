import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'sort'
})
export class SortPipe implements PipeTransform {

  transform(items: any[], order: string[]): any {
    if (!items || !order) {
      return items;
    }

    let headers: string[] = ["duration", "storageSize"]
    console.log(headers.indexOf(order[0]))

    if (order[1] === "descending") {
      let newItems: any[] = items.sort((a, b) => a - b);
      console.log(newItems)
      console.log(newItems[headers.indexOf(order[0])])
      return newItems
    }
    return items.sort((a, b) => b - a);
  }


}
