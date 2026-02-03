import { Injectable } from '@angular/core';
import { BaseReport } from '../interfaces/base-report';

@Injectable({
  providedIn: 'root',
})
export class HelperService {
  download(queryString: string, storage: string, exportBinary: boolean, exportXML: boolean): void {
    window.open(`api/report/download/${storage}/${exportBinary}/${exportXML}?${queryString.slice(0, -1)}`);
  }

  getSelectedIds(reports: BaseReport[]): number[] {
    let copiedIds: number[] = [];
    for (const report of this.getSelectedReports(reports)) {
      copiedIds.push(report.storageId);
    }
    return copiedIds;
  }

  getSelectedReports(reports: BaseReport[]): BaseReport[] {
    return reports.filter((report) => report.checked);
  }
}
